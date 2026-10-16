package pizza.dlc;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.neuro.*;
import pizza.dlc.api.util.combat.TargetSelection;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.misc.NeuroRecorder;

/** Real Fabric client + integrated server, isolated flat test arena. Never installed in the production jar. */
public final class NeuroClientGameTest implements FabricClientGameTest {
 private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 @Override public void runTest(ClientGameTestContext context){
  final TrainingBots.Bot[] disconnectBot={null};
  try(var world=context.worldBuilder().setUseConsistentSettings(true).create()){
   world.getServer().runCommand("fill -12 64 -12 12 64 12 minecraft:stone");
   world.getServer().runCommand("tp @p 0 65 0 0 0");
   world.getConnection().waitForChunksDownload();context.waitTicks(40);
   context.runOnClient(mc->{mc.gui.setScreen(null);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.setYRot(0);mc.player.setXRot(0);NeuroSystem.get().spawn(2);});
   List<TrainingBots.Bot> bots=context.computeOnClient(mc->{var out=new ArrayList<TrainingBots.Bot>();for(var e:mc.level.entitiesForRendering())if(e instanceof TrainingBots.Bot b)out.add(b);return out;});
   check(bots.size()==2,"Real ClientLevel did not contain two practice bots");
   int id=bots.getFirst().getId();check(id<0,"Practice entity ID must not collide with server IDs");
   check(world.getServer().computeOnServer(server->world.getConnection().getServerLevel().getEntity(id)==null),"Practice bot unexpectedly exists on server");
   context.runOnClient(mc->{
    var bot=bots.getFirst();Vec3 r=bot.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-r.x,r.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(r.y,Math.hypot(r.x,r.z))));
    mc.gameMode.attack(mc.player,bot);check(bot.hurtTime>0,"Native attack hook did not register a local training hit");
    check(TargetSelection.m_7570(12,true,true,false,false).contains(bot),"Native combat target selection rejected the owned bot");
   });
   Vec3 before=context.computeOnClient(mc->bots.getFirst().position());context.waitTicks(60);Vec3 after=context.computeOnClient(mc->bots.getFirst().position());
   check(Math.abs(after.y-before.y)<.3&&after.y>=64.99,"RemotePlayer practice bot fell through the arena floor: "+before+" -> "+after);
   check(before.distanceTo(after)>.1,"Practice bot does not move in the real client");
   check(Math.abs(after.z-before.z)<1e-6&&Math.abs(after.y-before.y)<1e-6,"Practice bot left its horizontal strafe lane");
   double centerX=context.computeOnClient(mc->mc.player.getX()),minX=before.x,maxX=before.x;
   for(int i=0;i<40;i++){
    context.waitTicks(10);Vec3 current=context.computeOnClient(mc->bots.getFirst().position());
    check(Math.abs(current.y-before.y)<1e-6&&Math.abs(current.z-before.z)<1e-6,"Practice bot moved down/up or forward/backward");
    check(Math.abs(current.x-centerX)<=5.000001,"Practice bot exceeded five-block strafe bounds");
    minX=Math.min(minX,current.x);maxX=Math.max(maxX,current.x);
   }
   check(minX-centerX< -4.9&&maxX-centerX>4.9,"Practice bot did not visit both ends of its lane");
   check(bots.stream().noneMatch(b->b.isRemoved()),"Practice bot was unexpectedly removed");
   context.runOnClient(mc->{Vec3 r=bots.getFirst().getBoundingBox().getCenter().subtract(mc.player.getEyePosition());mc.player.setYRot((float)Math.toDegrees(Math.atan2(-r.x,r.z)));mc.player.setXRot((float)-Math.toDegrees(Math.atan2(r.y,Math.hypot(r.x,r.z))));});
   context.waitTicks(2);context.takeScreenshot("neuro-practice-bots");
   if(Boolean.getBoolean("pizzadlc.neuro.liveCheck"))liveCheck(context);
   context.runOnClient(mc->{NeuroSystem.get().remove();for(var e:mc.level.entitiesForRendering())check(!(e instanceof TrainingBots.Bot),"Removed bot remained in real ClientLevel");NeuroSystem.get().spawn(1);for(var e:mc.level.entitiesForRendering())if(e instanceof TrainingBots.Bot b)disconnectBot[0]=b;});
  }
  context.runOnClient(mc->{check(!NeuroSystem.managed(),"Disconnect left Neuro active");check(disconnectBot[0]!=null&&!NeuroSystem.isBot(disconnectBot[0])&&disconnectBot[0].isRemoved(),"Disconnect did not remove owned bots");});
  System.out.println("PASS real Minecraft: practice bot construction/movement/hitboxes, local native attack, server isolation, native target selection, removal and disconnect");
 }
 private static NeuroWorker worker(){try{var f=NeuroSystem.class.getDeclaredField("worker");f.setAccessible(true);return (NeuroWorker)f.get(NeuroSystem.get());}catch(Exception e){throw new AssertionError(e);}}
 /** Artificial demonstrator for integration testing only. Never used as a production training target. */
 private static void liveCheck(ClientGameTestContext context){
  var worker=worker();String existing=System.getProperty("pizzadlc.neuro.liveModel");String name=existing==null?"synthetic_live_"+System.currentTimeMillis():existing;
  context.runOnClient(mc->{
   NeuroSystem.get().remove();var s=PizzaDLCClient.getInstance().getModuleManager().get(NeuroRecorder.class);s.minimum.setValue(64);s.updates.setValue(4);s.botSpeed.setValue(2);s.checkpoint.setValue(5);
   mc.gui.setScreen(null);mc.mouseHandler.grabMouse();NeuroSystem.get().spawn(1);if(existing==null)NeuroSystem.get().train(name);
  });
  if(existing==null){
  for(int i=0;i<1000;i++){
   context.runOnClient(mc->{
    TrainingBots.Bot bot=null;for(var e:mc.level.entitiesForRendering())if(e instanceof TrainingBots.Bot b)bot=b;
    check(bot!=null,"Live training lost its bot");Vec3 r=bot.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());float yaw=(float)Math.toDegrees(Math.atan2(-r.x,r.z)),pitch=(float)-Math.toDegrees(Math.atan2(r.y,Math.hypot(r.x,r.z)));
    mc.player.setYRot(mc.player.getYRot()+NeuroSchema.wrap(yaw-mc.player.getYRot())*.3F);mc.player.setXRot(mc.player.getXRot()+(pitch-mc.player.getXRot())*.3F);
   });context.waitTick();
  }
  context.runOnClient(mc->NeuroSystem.get().stopTraining());context.waitFor(mc->!worker.running(),1200);
  check(worker.error.isEmpty()&&worker.model!=null&&worker.model.steps>0,"Live training failed: "+worker.error+" / "+worker.status);
  System.out.println("LIVE synthetic training: "+worker.status);
  }
  context.runOnClient(mc->NeuroSystem.get().load(name));context.waitFor(mc->!worker.loading,600);
  check(worker.error.isEmpty(),"Live model load failed: "+worker.error);
  context.runOnClient(mc->NeuroSystem.get().use(true));float initial=context.computeOnClient(mc->mc.player.getYRot());long onTarget=0;
  for(int i=0;i<200;i++){
   context.waitTick();if(context.computeOnClient(mc->{for(var e:mc.level.entitiesForRendering())if(e instanceof TrainingBots.Bot b){Vec3 eye=mc.player.getEyePosition();return b.getBoundingBox().clip(eye,eye.add(mc.player.getLookAngle().scale(12))).isPresent();}return false;}))onTarget++;
  }
  float after=context.computeOnClient(mc->mc.player.getYRot());check(Math.abs(NeuroSchema.wrap(after-initial))>.1,"Live trained model did not move the actual first-person camera");
  System.out.println("LIVE closed loop: initial yaw="+initial+", final yaw="+after+", on-target="+onTarget+"/200; artificial demonstrator only");
  context.takeScreenshot("neuro-live-model-camera");
  context.runOnClient(mc->{NeuroSystem.get().mouse(2,1);mc.player.turn(2,1);});float manual=context.computeOnClient(mc->mc.player.getYRot());context.waitTicks(4);check(Math.abs(NeuroSchema.wrap(context.computeOnClient(mc->mc.player.getYRot())-manual))<.01,"Live model ignored mouse override");
  context.runOnClient(mc->NeuroSystem.get().use(false));long previous=worker.model.steps;
  context.runOnClient(mc->NeuroSystem.get().train(null));for(int i=0;i<40;i++){context.runOnClient(mc->mc.player.turn(.4,.1));context.waitTick();}
  context.runOnClient(mc->NeuroSystem.get().stopTraining());context.waitFor(mc->!worker.running(),1200);check(worker.error.isEmpty()&&worker.model.steps>previous,"Live optimizer continuation failed: "+worker.error);
  System.out.println("PASS LIVE mouse override and optimizer continuation: "+previous+" -> "+worker.model.steps);
 }

}
