package pizza.dlc;
import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.blaze3d.platform.Window;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import pizza.dlc.api.neuro.*;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.command.*;
import pizza.dlc.client.command.commands.NeuroCommand;
import pizza.dlc.client.modules.ModuleManager;
import pizza.dlc.client.modules.impl.combat.AttackAura;
import pizza.dlc.client.modules.impl.misc.NeuroRecorder;

public final class NeuroCheck {
 private static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 private static Object allocate(Class<?> type)throws Exception{var u=Class.forName("sun.misc.Unsafe");var f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);}
 private static Field field(Class<?> type,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 private static void set(Class<?> type,Object object,String name,Object value)throws Exception{field(type,name).set(object,value);}
 private static Object get(Class<?> type,Object object,String name)throws Exception{return field(type,name).get(object);}
 private static void expect(Runnable action){try{action.run();throw new AssertionError("Invalid action accepted");}catch(IllegalArgumentException|IllegalStateException expected){}}
 private static void position(Entity e,Vec3 p,int id)throws Exception{set(Entity.class,e,"position",p);set(Entity.class,e,"id",id);set(Entity.class,e,"eyeHeight",1.6F);set(Entity.class,e,"dimensions",EntityDimensions.scalable(.6F,1.8F));set(Entity.class,e,"bb",new AABB(p.x-.3,p.y,p.z-.3,p.x+.3,p.y+1.8,p.z+.3));}
 public static void main(String[] args)throws Exception{
  check(NeuroSchema.wrap(181)==-179&&NeuroSchema.wrap(-181)==179&&NeuroSchema.wrap(720)==0,"Yaw wrap");
  check(NeuroSchema.pitch(100)==90&&NeuroSchema.pitch(-100)==-90,"Pitch clamp");
  for(int fps:new int[]{20,30,60,144,240}){
   var camera=new NeuroCameraMotion();double wantedYaw=0,wantedPitch=0,actualYaw=0,actualPitch=0,next=0;int count=0;
   for(int frame=0;frame<fps*6;frame++){
    double time=frame/(double)fps;
    if(count<100&&time>=next){float y=(float)(3+Math.sin(count*.3)),p=(float)(-1.5+Math.cos(count*.21));camera.enqueue(y,p);wantedYaw+=y;wantedPitch+=p;next+=.05+(count%2==0?.012:-.012);count++;}
    var step=camera.advance(1.0/fps);actualYaw+=step.yaw();actualPitch+=step.pitch();
   }
   var tail=camera.advance(1);actualYaw+=tail.yaw();actualPitch+=tail.pitch();
   check(count==100&&Math.abs(wantedYaw-actualYaw)<.0001&&Math.abs(wantedPitch-actualPitch)<.0001,"Render interpolation lost yaw/pitch at "+fps+" FPS");
   camera.enqueue(8,-4);camera.reset();check(camera.advance(.05).equals(new NeuroCameraMotion.Step(0,0)),"Mouse/reset retained camera movement");
   expect(()->camera.enqueue(Float.NaN,1));expect(()->camera.advance(Double.NaN));
  }
  System.out.println("PASS: 100 overlapping camera steps conserve both axes at 20/30/60/144/240 FPS; reset/finite guards");
  float[] invalid=new float[37];invalid[3]=Float.NaN;expect(()->NeuroSchema.normalize(invalid));
  check(!NeuroModel.validName("../bad")&&!NeuroModel.validName("a/b")&&NeuroModel.validName("personal_1"),"Model path validation");
  Path fixture=Path.of("build/neuro-check/parity.json");NeuroModel model=null;
  String fixtureText;
  if(Files.exists(fixture))fixtureText=Files.readString(fixture);
  else try(var in=NeuroCheck.class.getResourceAsStream("/neuro-parity.json")){if(in==null)throw new AssertionError("Missing PyTorch parity fixture");fixtureText=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
  {
   var data=JsonParser.parseString(fixtureText).getAsJsonObject();model=new NeuroModel("fixture",data.getAsJsonObject("model"));
   for(var row:data.getAsJsonArray("sequences")){
    var sequence=row.getAsJsonObject();float[][] x=new Gson().fromJson(sequence.get("x"),float[][].class);float[] y=new Gson().fromJson(sequence.get("y"),float[].class),actual=model.predict(x);
    for(int i=0;i<2;i++)check(Math.abs(actual[i]-y[i])<.0002,"Java/PyTorch inference differs");
    if(sequence.has("movement_logits")){float[] expected=new Gson().fromJson(sequence.get("movement_logits"),float[].class),logits=model.movement.logits(x);for(int i=0;i<11;i++)check(Math.abs(expected[i]-logits[i])<.0002,"Movement Java/PyTorch inference differs");}
   }
   if(Files.isRegularFile(Path.of("build/neuro-check/models").resolve(data.get("name").getAsString()).resolve("current.json"))){var loaded=NeuroModel.load(Path.of("build/neuro-check"),data.get("name").getAsString());check(loaded.steps==model.steps,"Java checkpoint load");}
   var broken=data.getAsJsonObject("model").deepCopy();broken.getAsJsonArray("features").set(0,new JsonPrimitive("future"));expect(()->new NeuroModel("bad",broken));
   if(model.movement!=null){var oldMovement=data.getAsJsonObject("model").getAsJsonObject("movement").deepCopy();oldMovement.addProperty("architecture","causal-controls-v1");new NeuroMovementModel(oldMovement);}
   var m=model;expect(()->m.predict(new float[15][37]));var bad=new float[16][37];bad[5][4]=Float.NaN;expect(()->m.predict(bad));
   System.out.println("PASS: Java/PyTorch parity <0.0002 degrees, checkpoint integrity/schema/finite guards");
  }
  String personalModel=System.getProperty("pizzadlc.neuro.checkModel");
  if(personalModel!=null)model=NeuroModel.load(Path.of(System.getProperty("pizzadlc.neuro.checkRoot")),personalModel);
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
  var mc=(Minecraft)allocate(Minecraft.class);set(Minecraft.class,null,"instance",mc);set(Minecraft.class,mc,"gameThread",Thread.currentThread());
  var gui=(Gui)allocate(Gui.class);set(Minecraft.class,mc,"gui",gui);
  var options=(Options)allocate(Options.class);set(Options.class,options,"cameraType",CameraType.FIRST_PERSON);set(Minecraft.class,mc,"options",options);
  var window=(Window)allocate(Window.class);set(Window.class,window,"focused",true);set(Minecraft.class,mc,"window",window);
  var mouse=(MouseHandler)allocate(MouseHandler.class);set(MouseHandler.class,mouse,"mouseGrabbed",true);set(Minecraft.class,mc,"mouseHandler",mouse);
  var pizzadlc=new PizzaDLC();set(PizzaDLC.class,null,"instance",pizzadlc);pizzadlc.friendManager=new pizza.dlc.api.filemanager.impl.FriendManager();
  pizzadlc.getBus().registerLambdaFactory("pizza.dlc",(lookup,type)->MethodHandles.privateLookupIn(type,MethodHandles.lookup()));
  var client=new PizzaDLCClient();set(PizzaDLCClient.class,null,"instance",client);var modules=new ModuleManager();set(PizzaDLCClient.class,client,"moduleManager",modules);
  var settings=new NeuroRecorder();var aura=new AttackAura();var utility=new pizza.dlc.client.modules.Module("AutoTotem","",pizza.dlc.client.modules.api.ModuleCategory.COMBAT);var conflict=new pizza.dlc.client.modules.Module("TriggerBot","",pizza.dlc.client.modules.api.ModuleCategory.COMBAT);modules.registerModules(settings,aura,utility,conflict);
  for(double yaw:new double[]{0,Math.PI/2,.73}){
   Vec3 start=new Vec3(13,65,-7),axis=new Vec3(-Math.cos(yaw),0,-Math.sin(yaw));
   var motion=new TrainingBots.Motion(start,axis,731);var repeat=new TrainingBots.Motion(start,axis,731);
   Vec3 a=start,b=start,old=Vec3.ZERO;double min=99,max=-99;int reversals=0;double previousDirection=0;
   for(int i=0;i<1600;i++){
    var v=motion.step(a,settings);var w=repeat.step(b,settings);check(v.equals(w),"Bot scenario not reproducible");
    check(v.length()<=settings.botSpeed.getValue()/20+.00001,"Bot speed exceeded");
    check(v.subtract(old).length()<=settings.botSpeed.getValue()/20*.45+.00001,"Bot acceleration jump");
    a=a.add(v);b=b.add(w);old=v;Vec3 relative=a.subtract(start);double offset=relative.dot(axis);
    check(Math.abs(offset)<=5.000001,"Bot overshot the five-block endpoint");
    check(a.y==start.y&&relative.subtract(axis.scale(offset)).length()<1e-8,"Bot moved vertically or forward/backward");
    min=Math.min(min,offset);max=Math.max(max,offset);
    double currentDirection=Math.signum(v.dot(axis));if(currentDirection!=0){if(previousDirection!=0&&currentDirection!=previousDirection)reversals++;previousDirection=currentDirection;}
   }
   check(min<=-4.99999&&max>=4.99999&&reversals>=5,"Bot did not repeatedly reach both five-block endpoints");
   var v=motion.step(a,settings);motion.reverse();var reversed=motion.step(a,settings);check(v.dot(axis)*reversed.dot(axis)<0,"Obstacle did not reverse strafe");
   settings.strafe.setValue(0);check(motion.step(a,settings).equals(Vec3.ZERO),"Zero strafe intensity still moves bots");settings.strafe.setValue(.8F);
  }
  System.out.println("PASS: fixed sideways lanes at three headings, exact +/-5 endpoints, repeated smooth turns, constant height, obstacle reversal and zero intensity");
  var self=(TestPlayer)allocate(TestPlayer.class);position(self,Vec3.ZERO,1);var enemy=(TestPlayer)allocate(TestPlayer.class);position(enemy,new Vec3(0,0,3),2);
  var level=(TestLevel)allocate(TestLevel.class);level.entities=List.of(enemy);mc.player=self;mc.level=level;set(Entity.class,self,"level",level);set(Entity.class,enemy,"level",level);
  var system=new NeuroSystem(Path.of("build/neuro-check/native"));
  var worker=(NeuroWorker)get(NeuroSystem.class,system,"worker");
  if(Boolean.getBoolean("pizzadlc.neuro.processCheck")){
   var cancelledWorker=new NeuroWorker(Path.of("build/neuro-check/cancelled-start"));
   cancelledWorker.start("cancelled","cpu",new JsonObject());long cancelDeadline=System.nanoTime()+30_000_000_000L;
   while(cancelledWorker.status.size()==0&&cancelledWorker.running()&&System.nanoTime()<cancelDeadline)Thread.sleep(10);
   check(cancelledWorker.running()&&!cancelledWorker.ready()&&cancelledWorker.status.has("stage"),"Startup progress was not exposed before expensive imports");
   check(cancelledWorker.queued()==0,"Startup filled the training pipe");
   Process cancelledProcess=(Process)get(NeuroWorker.class,cancelledWorker,"process");var cancelledChildren=cancelledProcess.descendants().toList();
   long cancelledAt=System.nanoTime();cancelledWorker.stop();
   while(cancelledWorker.running()&&System.nanoTime()-cancelledAt<5_000_000_000L)Thread.sleep(10);
   check(!cancelledWorker.running()&&cancelledWorker.error.isEmpty()&&cancelledWorker.model==null,"Startup cancellation hung or falsely claimed a saved model: "+cancelledWorker.error);
   check(!cancelledProcess.isAlive()&&cancelledChildren.stream().noneMatch(ProcessHandle::isAlive),"Cancellation left an orphan Windows Python interpreter");
   cancelledWorker.shutdown();System.out.println("PASS: import-stage progress and prompt cancellation before trainer readiness; no false checkpoint");
   set(net.minecraft.world.entity.player.Player.class,enemy,"gameProfile",new com.mojang.authlib.GameProfile(UUID.randomUUID(),"synthetic_enemy"));
   settings.minimum.setValue(64);settings.capacity.setValue(1024);settings.updates.setValue(1);settings.checkpoint.setValue(5);
   String probe="native_"+System.currentTimeMillis();system.train(probe);expect(()->system.train(probe));
   long readyDeadline=System.nanoTime()+190_000_000_000L;
   while(!worker.ready()&&worker.running()&&worker.error.isEmpty()&&System.nanoTime()<readyDeadline){system.tick();check(worker.queued()==0&&worker.dropped==0,"Demonstrations were queued/dropped before trainer readiness");Thread.sleep(50);}
   check(worker.ready(),"Trainer startup failed: "+worker.error+" / "+worker.status);
   for(int i=0;i<900&&(i<300||!worker.status.has("steps")||worker.status.get("steps").getAsLong()==0);i++){self.setYRot((float)(12*Math.sin(i*.11)));self.setXRot((float)(10+3*Math.cos(i*.07)));system.tick();Thread.sleep(50);}
   check(NeuroSystem.collecting(),"Collection stopped: "+worker.error+" / "+worker.status);system.stopTraining();expect(()->system.use(true));long deadline=System.nanoTime()+60_000_000_000L;
   while(worker.running()&&System.nanoTime()<deadline)Thread.sleep(50);
   check(!worker.running()&&worker.error.isEmpty()&&worker.model!=null&&worker.model.steps>0,"Native trainer failed: "+worker.error+" / "+worker.status);
   long steps=worker.model.steps;system.load(probe);while(worker.loading)Thread.sleep(20);check(worker.error.isEmpty(),"Native load failed");
   system.train(null);readyDeadline=System.nanoTime()+190_000_000_000L;
   while(!worker.ready()&&worker.running()&&worker.error.isEmpty()&&System.nanoTime()<readyDeadline){system.tick();Thread.sleep(50);}
   check(worker.ready(),"Resumed trainer startup failed: "+worker.error+" / "+worker.status);
   for(int i=0;i<24;i++){self.setYRot((float)Math.sin(i*.13)*5);system.tick();Thread.sleep(50);}system.stopTraining();deadline=System.nanoTime()+60_000_000_000L;
   while(worker.running()&&System.nanoTime()<deadline)Thread.sleep(50);
   check(worker.error.isEmpty()&&worker.model.steps>steps,"Native continuation did not update weights: "+worker.error+" / "+worker.status);
   System.out.println("PASS: native observations -> bounded Java pipe -> "+worker.status+" -> checkpoint load -> continued optimizer updates (synthetic inputs)");
  }
  worker.model=null;
  var dispatcher=new CommandDispatcher<PizzaDLCCommandSource>();new NeuroCommand().registerTo(dispatcher);
  for(String command:List.of("neuro help","neuro bots spawn 1","neuro bots remove","neuro train start one","neuro train stop","neuro status","neuro model load test","neuro model select test","neuro model list","neuro use on","neuro use off","neuro eval start manual 731","neuro eval stop"))check(!dispatcher.parse(command,PizzaDLCCommandSource.INSTANCE).getReader().canRead(),"Command grammar: "+command);
  check(dispatcher.parse("neuro bots spawn 20",PizzaDLCCommandSource.INSTANCE).getReader().canRead(),"Bot bounds accepted");
  expect(()->system.stopTraining());expect(()->system.use(true));expect(()->system.load("../bad"));
  aura.getRotationMode().setCurrentMode("Neuro");aura.setEnabled(true);check(!aura.isEnabled()&&!NeuroSystem.applying(),"Neuro aura enabled without a selected model");
  set(NeuroSystem.class,system,"recording",true);aura.setEnabled(true);check(!aura.isEnabled(),"Automatic combat enabled while collecting");expect(()->system.train("again"));set(NeuroSystem.class,system,"recording",false);
  if(model!=null){utility.setEnabled(true);conflict.setEnabled(true);worker.model=model;aura.setEnabled(true);check(NeuroSystem.applying(),"Normal aura enable did not start the selected Neuro model");check(utility.isEnabled()&&!conflict.isEnabled(),"Compatible defense disabled or competing attack not blocked");check(aura.getRotationMode().is("Neuro")&&aura.isEnabled(),"Native aura was not integrated");
   // Exercise the complete feedback loop: current native observations -> worker prediction -> real yaw/pitch -> next observations.
   aura.setTarget(enemy);self.setYRot(-25);self.setXRot(0);float initialYaw=self.getYRot(),minPitch=0,maxPitch=0;double peakTickMs=0;
   for(int i=0;i<(personalModel==null?45:200);i++){
    long begin=System.nanoTime();system.tick();peakTickMs=Math.max(peakTickMs,(System.nanoTime()-begin)/1e6);
    for(int frame=0;frame<5;frame++){Thread.sleep(10);system.frame();minPitch=Math.min(minPitch,self.getXRot());maxPitch=Math.max(maxPitch,self.getXRot());}
   }
   check(Float.isFinite(self.getYRot())&&Float.isFinite(self.getXRot())&&Math.abs(self.getYRot()-initialYaw)>.01,"Learned model did not move the actual camera in closed loop");
   System.out.println("Closed-loop "+(personalModel==null?"SYNTHETIC model":personalModel+" (read-only checkpoint)")+": native yaw "+initialYaw+" -> "+self.getYRot()+", pitch "+self.getXRot()+", pitch range ["+minPitch+", "+maxPitch+"], peak tick ms "+peakTickMs+"; controlled observations, not a live human-style quality measurement");
   var camera=(NeuroCameraMotion)get(NeuroSystem.class,system,"cameraMotion");camera.reset();camera.enqueue(8,-4);self.setYRot(0);self.setXRot(0);
   set(NeuroSystem.class,system,"target",enemy);set(NeuroSystem.class,system,"lastFrame",System.nanoTime()-25_000_000L);
   system.frame();check(self.getYRot()>0&&self.getXRot()<0,"Visible actual yaw/pitch camera did not move");check(CombatRotations.state().getRotation().x==self.getYRot()&&CombatRotations.state().getRotation().y==self.getXRot(),"Combat and camera angles diverged");
   camera.reset();self.setYRot(0);self.setXRot(0);settings.yawGain.setValue(2);settings.pitchGain.setValue(1.5F);
   var prediction=Class.forName("pizza.dlc.api.neuro.NeuroSystem$Prediction").getDeclaredConstructors()[0];prediction.setAccessible(true);
   set(NeuroSystem.class,system,"prediction",prediction.newInstance(8F,-4F,enemy.getId(),(long)get(NeuroSystem.class,system,"epoch"),System.nanoTime(),1D));
   set(NeuroSystem.class,system,"lastFrame",System.nanoTime()-60_000_000L);system.frame();
   check(Math.abs(self.getYRot()-16)<.001&&Math.abs(self.getXRot()+6)<.001,"Independent yaw/pitch gains did not reach the native camera");settings.yawGain.setValue(1);settings.pitchGain.setValue(1);
   var controlType=Class.forName("pizza.dlc.api.neuro.NeuroSystem$ControlPrediction").getDeclaredConstructors()[0];controlType.setAccessible(true);
   Object control=controlType.newInstance(new NeuroMovementModel.Controls(1,-1,true,true,.95F),enemy.getId(),(long)get(NeuroSystem.class,system,"epoch"),System.nanoTime());
   set(NeuroSystem.class,system,"controlPrediction",control);
   var empty=net.minecraft.world.entity.player.Input.EMPTY;
   check(system.movement(empty,empty).equals(empty),"Disabled movement settings changed input");
   settings.autoWalk.setEnabled(true);var walked=system.movement(empty,empty);check(walked.forward()&&walked.right()&&walked.sprint()&&!walked.jump(),"Learned walking did not reach native keyboard input");
   settings.autoWalk.setEnabled(false);settings.autoJump.setEnabled(true);var jumped=system.movement(empty,empty);check(jumped.jump()&&!jumped.forward()&&!jumped.right(),"Learned jump toggle was not independent");
   settings.autoWalk.setEnabled(true);var manualMove=new net.minecraft.world.entity.player.Input(false,true,true,false,false,false,false);var overridden=system.movement(manualMove,manualMove);check(overridden.backward()&&overridden.left()&&!overridden.forward(),"Manual WASD did not override learned walking");
   var crouch=new net.minecraft.world.entity.player.Input(false,false,false,false,false,true,false);check(system.movement(crouch,crouch).equals(crouch),"Manual sneak did not suppress auto movement");
   set(Window.class,window,"focused",false);check(system.movement(empty,empty).equals(empty),"Lost focus retained learned movement");set(Window.class,window,"focused",true);
   set(NeuroSystem.class,system,"controlPrediction",controlType.newInstance(new NeuroMovementModel.Controls(1,0,true,true,.95F),enemy.getId(),(long)get(NeuroSystem.class,system,"epoch"),System.nanoTime()-200_000_000L));check(system.movement(empty,empty).equals(empty),"Stale learned movement was applied");
   settings.autoWalk.setEnabled(false);settings.autoJump.setEnabled(false);
   float aimedYaw=self.getYRot();self.setYRot(90);check(system.canAutoAttack(enemy),"Neuro suppressed a normal Aura swing when learned aim misses");check(system.recordAutoAttack(enemy),"Neuro sent a direct target packet instead of registering a genuine miss");Vec3 aim=enemy.getBoundingBox().getCenter().subtract(self.getEyePosition());self.setYRot((float)Math.toDegrees(Math.atan2(-aim.x,aim.z)));self.setXRot((float)-Math.toDegrees(Math.atan2(aim.y,Math.hypot(aim.x,aim.z))));check(!system.recordAutoAttack(enemy),"On-target Neuro swing was incorrectly suppressed as a miss");self.setYRot(aimedYaw);
   system.mouse(2,1);float manual=self.getYRot();system.frame();check(self.getYRot()==manual&&!camera.pending(),"Mouse did not immediately stop rotation");
   level.blocked=true;camera.enqueue(8,4);system.frame();check(self.getYRot()==manual&&!system.canAutoAttack(enemy),"Obstacle did not stop camera/attacks");level.blocked=false;
   aura.setEnabled(false);check(!NeuroSystem.applying(),"Aura bind disable did not stop Neuro");aura.setEnabled(true);check(NeuroSystem.applying(),"Aura bind re-enable did not resume selected model");
   set(NeuroSystem.class,system,"world",level);mc.level=null;system.lifecycle();check(!NeuroSystem.managed()&&!camera.pending()&&!aura.isEnabled(),"World exit did not clean state");mc.level=level;
  }
  pizzadlc.getBus().post(new EventWorldLoad());check(!NeuroSystem.managed(),"World event cleanup");worker.shutdown();
  System.out.println("PASS: commands, angle boundaries, deterministic sideways bots, manual isolation, visible camera/combat sync, mouse takeover, obstacles, world lifecycle");
 }
 private static final class TestPlayer extends LocalPlayer{
  private TestPlayer(){super(null,null,null,null,null,null,false,null);}
  @Override public boolean isShiftKeyDown(){return false;}
  @Override public net.minecraft.world.item.ItemStack getMainHandItem(){return net.minecraft.world.item.ItemStack.EMPTY;}
  @Override public net.minecraft.world.item.ItemStack getOffhandItem(){return net.minecraft.world.item.ItemStack.EMPTY;}
  @Override public boolean isAlive(){return true;}@Override public boolean isSpectator(){return false;}
  @Override public boolean isSprinting(){return false;}@Override public boolean onGround(){return true;}
  @Override public boolean isFallFlying(){return false;}
  @Override public Vec3 getDeltaMovement(){return Vec3.ZERO;}@Override public double entityInteractionRange(){return 3;}
  @Override public float getAttackStrengthScale(float partial){return 1;}
  @Override public void sendSystemMessage(Component message){}
 }
 private static final class TestLevel extends ClientLevel{
  List<Entity> entities;boolean blocked;private TestLevel(){super(null,null,null,null,0,0,null,false,0,0);}
  @Override public Iterable<Entity> entitiesForRendering(){return entities;}
  @Override public boolean noCollision(Entity entity,AABB bounds){return !blocked;}
  @Override public BlockHitResult clip(ClipContext context){Vec3 p=context.getTo();return blocked?new BlockHitResult(p,Direction.UP,BlockPos.containing(p),false):BlockHitResult.miss(p,Direction.UP,BlockPos.containing(p));}
 }
}
