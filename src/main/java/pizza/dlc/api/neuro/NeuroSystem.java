package pizza.dlc.api.neuro;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.impl.combat.AttackAura;
import pizza.dlc.client.modules.impl.misc.*;

/** Game-thread owner of demonstrations and the visible camera. Training and inference never share mutable weights. */
public final class NeuroSystem {
 private static NeuroSystem instance;
 private static final Set<String> AIM_CONFLICTS=Set.of("AnchorAura","CrystalAura","TriggerBot","AutoExplosion","AutoWeb","AutoPotion");
 private static final Set<String> MOVE_CONFLICTS=Set.of("AutoSprint","ElytraJump","ElytraMotion","ElytraBooster","SuperFirework","Fly","AirStuck");
 private final Minecraft mc=Minecraft.getInstance();
 private final Path root;
 private final NeuroWorker worker;
 private final TrainingBots bots=new TrainingBots();
 private final ExecutorService inference=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"neuro-inference");t.setDaemon(true);return t;});
 private final AtomicBoolean predicting=new AtomicBoolean();
 private final ArrayDeque<float[]> history=new ArrayDeque<>();
 private volatile Prediction prediction;private volatile String inferenceError="";
 private volatile ControlPrediction controlPrediction;private float[] pendingControls;
 private boolean recording,using,trainerReadyNotice;private ClientLevel world;private LivingEntity target;
 private Frame pending;private long lastTick,lastMouse,lastFrame,epoch,sequence,segment,lastAttack;
 private int targetId=Integer.MIN_VALUE;private float dyaw,dpitch,prevYaw,prevPitch;
 private Vec3 playerPosition,targetPosition;private final NeuroCameraMotion cameraMotion=new NeuroCameraMotion();
 private float lastPredictedYaw,lastPredictedPitch;private long acceptedPredictions,stalePredictions;
 private boolean attack,hit,miss;private long hits,attempts;private String modelName="default";
 private Evaluation evaluation;private double lastInferenceLatency;
 private record Frame(float[] x,float yaw,float pitch,long time,int target,long segment,long sequence){}
 private record Prediction(float yaw,float pitch,int target,long epoch,long time,double latency){}
 private record ControlPrediction(NeuroMovementModel.Controls controls,int target,long epoch,long time){}

 public NeuroSystem(){this(PizzaDLC.DIRECTORY.toPath().resolve("neuro"));}
 public NeuroSystem(Path directory){root=directory.toAbsolutePath();worker=new NeuroWorker(root);instance=this;PizzaDLC.getInstance().getBus().subscribe(this);Runtime.getRuntime().addShutdownHook(new Thread(()->{worker.shutdown();inference.shutdownNow();},"neuro-shutdown"));}
 public static NeuroSystem get(){return instance;}
 public static boolean blocksCombat(pizza.dlc.client.modules.Module module){
  String name=module.getName().replace(" ","");
  if(collecting()&&(module.getCategory()==ModuleCategory.COMBAT||MOVE_CONFLICTS.contains(name))||applying()&&(AIM_CONFLICTS.contains(name)||(instance.settings().autoWalk.isEnabled()||instance.settings().autoJump.isEnabled())&&MOVE_CONFLICTS.contains(name)))return true;
  if(module instanceof AttackAura a&&a.getRotationMode().is("Neuro")&&(instance==null||!instance.modelReady())){say("Выберите модель: .neuro model select <имя> (список: .neuro model list)");return true;}
  return false;
 }
 public static boolean applying(){return instance!=null&&instance.using;}
 public static boolean collecting(){return instance!=null&&(instance.recording||instance.evaluation!=null&&!instance.using);}
 public static boolean managed(){return instance!=null&&(instance.recording||instance.using||instance.evaluation!=null);}
 public static boolean isBot(Entity entity){return instance!=null&&instance.bots.contains(entity);}
 private NeuroRecorder settings(){return PizzaDLCClient.getInstance().getModuleManager().get(NeuroRecorder.class);}
 private AttackAura aura(){return PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);}
 public boolean modelReady(){return !worker.loading&&!worker.running()&&worker.error.isEmpty()&&worker.model!=null&&worker.model.steps>0;}
 public static void auraEnabled(AttackAura aura){if(instance!=null&&aura.getRotationMode().is("Neuro")){try{instance.startApplying();}catch(IllegalStateException e){say(e.getMessage());aura.setEnabled(false);}}}
 public static void auraDisabled(){if(instance!=null&&instance.using){instance.using=false;instance.resetHistory();instance.clearRotations();say("Применение выключено вместе с Attack Aura");}}
 public static void say(String text){Minecraft.getInstance().execute(()->ChatUtility.send("Neuro: "+text));}
 private void requireWorld(){if(mc.player==null||mc.level==null)throw new IllegalStateException("Сначала зайди в мир");}
 private void requireView(){requireWorld();if(!mc.options.getCameraType().isFirstPerson())throw new IllegalStateException("Нужен вид от первого лица");if(freecam())throw new IllegalStateException("Сначала выключи FreeCam");}
 private boolean freecam(){var f=PizzaDLCClient.getInstance().getModuleManager().get(FreeCam.class);return f!=null&&f.isEnabled();}
 private boolean view(){return mc.player!=null&&mc.level!=null&&mc.gui.screen()==null&&mc.isWindowActive()&&!mc.isPaused()&&mc.mouseHandler.isMouseGrabbed()&&mc.options.getCameraType().isFirstPerson()&&!freecam();}
 public void spawn(int count){requireWorld();if(evaluation!=null)throw new IllegalStateException("Сначала .neuro eval stop");world=mc.level;say("Создано ботов: "+bots.spawn(count,System.nanoTime(),false,settings())+". Попадания локальные, серверного урона нет.");}
 public void remove(){if(evaluation!=null)stopEvaluation();bots.remove();resetHistory();say("Тренировочные боты удалены");}
 public void train(String name){
  requireView();if(evaluation!=null)throw new IllegalStateException("Сначала .neuro eval stop");if(recording||worker.running()||worker.loading)throw new IllegalStateException("Обучение уже запущено, завершается или загружается модель");if(using)throw new IllegalStateException("Сначала .neuro use off");
  name=name==null?(worker.model==null?modelName:worker.model.name):name;if(!NeuroModel.validName(name))throw new IllegalArgumentException("Имя: 1–32 латинские буквы, цифры, _ или -");
  disableCombat();clearRotations();resetHistory();recording=true;world=mc.level;sequence=0;modelName=name;inferenceError="";
  var s=settings();if(s.minimum.getValue()>s.capacity.getValue()){recording=false;throw new IllegalArgumentException("Минимум примеров превышает буфер обучения");}var config=new JsonObject();config.addProperty("lr",s.learningRate.getValue());config.addProperty("batch",(int)s.batch.getValue());config.addProperty("capacity",(int)s.capacity.getValue());config.addProperty("minimum",(int)s.minimum.getValue());config.addProperty("updates",(int)s.updates.getValue());config.addProperty("checkpoint_seconds",s.checkpoint.getValue());
  trainerReadyNotice=false;worker.start(name,s.device.getCurrentMode().toLowerCase(Locale.ROOT),config);say("Запуск тренера: "+name+". Дождись сообщения «Тренер готов», затем наводись и бей вручную. Этап запуска: .neuro status");
 }
 public void stopTraining(){if(!recording&&!worker.running())throw new IllegalStateException("Обучение не запущено");if(worker.stopping())throw new IllegalStateException("Остановка уже выполняется");boolean wasReady=worker.ready();recording=false;resetHistory();worker.stop();say(wasReady?"Сбор остановлен. Тренер завершает очередь и сохраняет модель; проверь .neuro status":"Запуск отменён до готовности; новые примеры и веса ещё не записывались");}
 public void use(boolean enabled){
  if(!enabled){if(evaluation!=null)stopEvaluation();using=false;resetHistory();clearRotations();if(aura().getRotationMode().is("Neuro"))aura().setEnabled(false);say("Применение выключено");return;}
  if(using)throw new IllegalStateException("Применение уже включено");
  if(!modelReady())throw new IllegalStateException("Выберите модель: .neuro model select <имя>");
  aura().getRotationMode().setCurrentMode("Neuro");if(aura().isEnabled())startApplying();else aura().setEnabled(true);
 }
 private void startApplying(){
  requireView();if(evaluation!=null)throw new IllegalStateException("Сначала .neuro eval stop");if(using)throw new IllegalStateException("Применение уже включено");if(recording||worker.running()||worker.loading)throw new IllegalStateException("Сначала останови обучение и дождись сохранения");if(!modelReady())throw new IllegalStateException("Нет модели с обновлёнными весами: train start или model load");
  for(var m:PizzaDLCClient.getInstance().getModuleManager().getModules())if(AIM_CONFLICTS.contains(m.getName().replace(" ","")))m.setEnabled(false);
  clearRotations();resetHistory();acceptedPredictions=stalePredictions=0;lastPredictedYaw=lastPredictedPitch=0;worker.activateModelStatus();using=true;world=mc.level;inferenceError="";say("Нейромодель "+worker.model.name+" включена с Attack Aura. Мышь перехватывает камеру; пауза "+settings().mousePause.getValue()+" сек.");
  if((settings().autoWalk.isEnabled()||settings().autoJump.isEnabled())&&(worker.model.movement==null||!worker.model.movement.ready()))say("Движение в этой модели ещё не обучено: train start → вручную ходи и прыгай → train stop. Наведение работает");
 }
 private void disableCombat(){for(var m:PizzaDLCClient.getInstance().getModuleManager().getModules())if((m.getCategory()==ModuleCategory.COMBAT||MOVE_CONFLICTS.contains(m.getName().replace(" ","")))&&m.isEnabled())m.setEnabled(false);}
 private void clearRotations(){
  if(RotationController.INSTANCE!=null)RotationController.INSTANCE.clear();
  float y=mc.player==null?0:mc.player.getYRot(),p=mc.player==null?0:mc.player.getXRot();
  var state=CombatRotations.state();state.getClientRotation().set(y,p);state.resetForClient();
  for(var plan:CombatRotations.plans()){CombatRotations.plans().clear(plan);plan.reset();}
  if(mc.player!=null){mc.player.setYRot(y);mc.player.setXRot(p);state.getRotation().set(y,p);state.getClientRotation().set(y,p);}
 }
 public void mouse(double x,double y){if(x==0&&y==0)return;lastMouse=System.nanoTime();if(using){resetHistory();clearRotations();}}
 /** Raw keyboard action applied during the interval whose yaw/pitch label closes on the next tick. */
 public void observeInput(net.minecraft.world.entity.player.Input manual){
  if(recording&&pending!=null&&worker.ready()&&view())pendingControls=new float[]{(manual.forward()?1:0)-(manual.backward()?1:0),(manual.left()?1:0)-(manual.right()?1:0),manual.jump()?1:0,manual.sprint()?1:0};
 }
 public net.minecraft.world.entity.player.Input movement(net.minecraft.world.entity.player.Input manual,net.minecraft.world.entity.player.Input ordinary){
  if(!using||!view()||mousePause(System.nanoTime())||manual.shift()||mc.player.isFallFlying()||mc.player.isPassenger())return ordinary;
  var result=controlPrediction;
  if(result==null||result.epoch!=epoch||result.target!=targetId||System.nanoTime()-result.time>150_000_000L||target==null||aura().getTarget()!=target||!visible(target))return ordinary;
  var controls=result.controls;boolean walking=manual.forward()||manual.backward()||manual.left()||manual.right();
  boolean autoWalk=settings().autoWalk.isEnabled()&&!walking;
  int forward=autoWalk?controls.forward():(ordinary.forward()?1:0)-(ordinary.backward()?1:0);
  int strafe=autoWalk?controls.strafe():(ordinary.left()?1:0)-(ordinary.right()?1:0);
  if(autoWalk&&(forward!=0||strafe!=0)){
   double yaw=Math.toRadians(mc.player.getYRot());Vec3 step=new Vec3(strafe*Math.cos(yaw)-forward*Math.sin(yaw),0,strafe*Math.sin(yaw)+forward*Math.cos(yaw)).normalize().scale(.35);
   var next=mc.player.getBoundingBox().move(step);
   if(!mc.level.noCollision(mc.player,next)||mc.player.onGround()&&!mc.level.noCollision(mc.player,mc.player.getBoundingBox().move(0,-.1,0))&&mc.level.noCollision(mc.player,next.move(0,-.1,0)))forward=strafe=0;
  }
  boolean jump=ordinary.jump()||manual.jump()||settings().autoJump.isEnabled()&&mc.player.onGround()&&controls.jump();
  boolean sprint=ordinary.sprint()||autoWalk&&forward>0&&controls.sprint();
  return new net.minecraft.world.entity.player.Input(forward>0,forward<0,strafe>0,strafe<0,jump,ordinary.shift(),sprint);
 }
 private boolean mousePause(long now){return now-lastMouse<settings().mousePause.getValue()*1e9;}
 private void resetHistory(){history.clear();pending=null;pendingControls=null;prediction=null;controlPrediction=null;epoch++;segment++;lastTick=0;lastFrame=0;targetId=Integer.MIN_VALUE;target=null;playerPosition=targetPosition=null;dyaw=dpitch=prevYaw=prevPitch=0;cameraMotion.reset();attack=hit=miss=false;}
 @EventHandler private void world(EventWorldLoad event){leaveWorld();}
 @EventHandler private void packet(pizza.dlc.api.events.PacketEvent.Receive event){
  if(event.getPacket() instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket)mc.execute(()->{event("teleport_or_server_correction");resetHistory();});
 }
 public void lifecycle(){if(world!=null&&(world!=mc.level||mc.player==null))leaveWorld();}
 private void leaveWorld(){recording=using=false;worker.stop();if(aura().getRotationMode().is("Neuro"))aura().setEnabled(false);bots.remove();if(evaluation!=null)stopEvaluation();resetHistory();world=null;clearRotations();}

 /** At local tick HEAD, before every automatic combat listener. Labels close the previous 50 ms interval. */
 public void tick(){long start=System.nanoTime();try{tickInternal();}finally{if(evaluation!=null)evaluation.tickCost((System.nanoTime()-start)/1e6);}}
 private void tickInternal(){
  lifecycle();if(mc.player==null||mc.level==null)return;
  if(!worker.error.isEmpty()&&recording){recording=false;worker.stop();resetHistory();say("Ошибка обучения: "+worker.error);}
  if(!inferenceError.isEmpty()&&using){using=false;aura().setEnabled(false);resetHistory();say("Ошибка применения: "+inferenceError);}
  if(!mc.isPaused())bots.tick(settings());
  if(!recording&&!using&&evaluation==null&&aura().isEnabled()&&aura().getRotationMode().is("Neuro")){
   try{if(!modelReady())throw new IllegalStateException("Выберите модель: .neuro model select <имя>");startApplying();}catch(IllegalStateException e){aura().setEnabled(false);say(e.getMessage());}
  }
  if(!recording&&!using&&evaluation==null)return;
  if(collecting())disableCombat();
  if(using&&(settings().autoWalk.isEnabled()||settings().autoJump.isEnabled()))for(var m:PizzaDLCClient.getInstance().getModuleManager().getModules())if(MOVE_CONFLICTS.contains(m.getName().replace(" ",""))&&m.isEnabled())m.setEnabled(false);
  if(recording&&!worker.ready()){resetHistory();return;}
  if(recording&&!trainerReadyNotice){trainerReadyNotice=true;say("Тренер готов: "+value(worker.status,"device_name")+" / "+value(worker.status,"backend")+". Теперь наводись и бей вручную");}
  if(using&&freecam()){using=false;resetHistory();clearRotations();say("Применение остановлено: включён FreeCam");return;}
  if(using&&(!aura().isEnabled()||!aura().getRotationMode().is("Neuro"))){using=false;resetHistory();clearRotations();say("Применение остановлено: AttackAura выключена или выбран другой режим");return;}
  if(!view()||using&&(!aura().isEnabled()||!aura().getRotationMode().is("Neuro")||mousePause(System.nanoTime()))){resetHistory();return;}
  long now=System.nanoTime();double dt=lastTick==0?.05:(now-lastTick)/1e9;
  if(dt<.025)return;if(dt>.1){resetHistory();dt=.05;}
  LivingEntity next=using?aura().getTarget():manualTarget();
  if(next==null||!next.isAlive()||next.isRemoved()||next.distanceTo(mc.player)>settings().range.getValue()){
   if(target!=null)event("target_lost");resetHistory();return;
  }
  boolean changed=next.getId()!=targetId;
  if(changed){resetHistory();target=next;targetId=next.getId();event("target_changed");dt=.05;}
  if(playerPosition!=null&&(mc.player.position().distanceTo(playerPosition)>5||next.position().distanceTo(targetPosition)>3)){resetHistory();return;}
  if(!Float.isFinite(mc.player.getYRot())||!Float.isFinite(mc.player.getXRot())){resetHistory();return;}
  float newYaw=0,newPitch=0;
  if(pending!=null){newYaw=NeuroSchema.wrap(mc.player.getYRot()-pending.yaw);newPitch=mc.player.getXRot()-pending.pitch;
   if(recording&&!worker.submitSample(new NeuroWorker.Sample(pending.segment,pending.sequence,pending.time,pending.target,pending.x,new float[]{newYaw,newPitch},pendingControls))){resetHistory();return;}
  }else if(lastTick!=0){newYaw=NeuroSchema.wrap(mc.player.getYRot()-prevActualYaw);newPitch=mc.player.getXRot()-prevActualPitch;}
  prevYaw=dyaw;prevPitch=dpitch;dyaw=newYaw;dpitch=newPitch;
  float[] x;try{x=features(next,dt,now,changed);}catch(IllegalArgumentException invalid){resetHistory();return;}if(evaluation!=null)evaluation.add(newYaw,newPitch,dt,rayHits(next,settings().range.getValue()),lastInferenceLatency,Math.hypot(x[7]*180,x[8]*90));
  prevActualYaw=mc.player.getYRot();prevActualPitch=mc.player.getXRot();lastTick=now;playerPosition=mc.player.position();targetPosition=next.position();
  attack=hit=miss=false;
  if(recording){pending=new Frame(x,mc.player.getYRot(),mc.player.getXRot(),now,targetId,segment,sequence++);pendingControls=null;}
  if(using){
   if(!visible(next)){history.clear();prediction=null;cameraMotion.reset();epoch++;return;}
   if(history.isEmpty())for(int i=1;i<NeuroSchema.LENGTH;i++)history.addLast(x.clone());
   history.addLast(x);if(history.size()>NeuroSchema.LENGTH)history.removeFirst();
   if(history.size()==NeuroSchema.LENGTH&&predicting.compareAndSet(false,true)){
    float[][] input=history.stream().map(float[]::clone).toArray(float[][]::new);var model=worker.model;long version=epoch,observed=now;int id=targetId;
    inference.execute(()->{try{float[] output=model.predict(input);var controls=model.movement==null?null:model.movement.predict(input);controlPrediction=controls==null?null:new ControlPrediction(controls,id,version,observed);prediction=new Prediction(output[0],output[1],id,version,observed,(System.nanoTime()-observed)/1e6);}catch(Exception e){inferenceError=e.toString();}finally{predicting.set(false);}});
   }
  }
 }
 private float prevActualYaw,prevActualPitch;
 private LivingEntity manualTarget(){
  if(mc.hitResult instanceof EntityHitResult hit&&hit.getEntity() instanceof LivingEntity e&&eligible(e))return e;
  LivingEntity best=null;double angle=Double.MAX_VALUE;Vec3 eye=mc.player.getEyePosition(),look=mc.player.getLookAngle();
  for(var e:mc.level.entitiesForRendering())if(e instanceof LivingEntity l&&eligible(l)){Vec3 direction=l.getBoundingBox().getCenter().subtract(eye).normalize();double score=1-direction.dot(look);if(score<angle){best=l;angle=score;}}return best;
 }
 private boolean eligible(LivingEntity e){if(e==mc.player||!e.isAlive()||e.isRemoved()||e.isSpectator()||e.distanceTo(mc.player)>settings().range.getValue())return false;return !(e instanceof Player p)||isBot(e)||!PizzaDLC.getInstance().getFriendManager().isFriend(p.getGameProfile().name());}
 private boolean visible(LivingEntity entity){return mc.level.clip(new ClipContext(mc.player.getEyePosition(),entity.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS;}
 private boolean rayHits(LivingEntity entity){return rayHits(entity,mc.player.entityInteractionRange());}
 private boolean rayHits(LivingEntity entity,double range){
  Vec3 start=mc.player.getEyePosition(),end=start.add(mc.player.getLookAngle().scale(range));var intersection=entity.getBoundingBox().clip(start,end);
  return intersection.isPresent()&&mc.level.clip(new ClipContext(start,intersection.get(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()==HitResult.Type.MISS;
 }
 private float[] features(LivingEntity e,double dt,long now,boolean changed){
  Vec3 eye=mc.player.getEyePosition(),r=e.getBoundingBox().getCenter().subtract(eye);
  Vec3 pv=playerPosition==null?mc.player.getDeltaMovement().scale(20):mc.player.position().subtract(playerPosition).scale(1/dt);
  Vec3 tv=targetPosition==null?e.getDeltaMovement().scale(20):e.position().subtract(targetPosition).scale(1/dt),rv=tv.subtract(pv);
  double rad=Math.toRadians(mc.player.getYRot()),sin=Math.sin(rad),cos=Math.cos(rad);boolean seen=visible(e);AABB b=e.getBoundingBox();
  Vec3 rayEnd=eye.add(mc.player.getLookAngle().scale(Math.max(r.length(),1)));Vec3 point=b.clip(eye,eye.add(mc.player.getLookAngle().scale(settings().range.getValue()))).orElse(new Vec3(Math.clamp(rayEnd.x,b.minX,b.maxX),Math.clamp(rayEnd.y,b.minY,b.maxY),Math.clamp(rayEnd.z,b.minZ,b.maxZ)));
  float[] raw={(float)(dt*1000),(float)(-r.x*sin+r.z*cos),(float)(r.x*cos+r.z*sin),(float)r.y,(float)r.length(),e.getBbWidth(),e.getBbHeight(),
   NeuroSchema.wrap((float)Math.toDegrees(Math.atan2(-r.x,r.z))-mc.player.getYRot()),(float)-Math.toDegrees(Math.atan2(r.y,Math.hypot(r.x,r.z)))-mc.player.getXRot(),
   (float)(-pv.x*sin+pv.z*cos),(float)(pv.x*cos+pv.z*sin),(float)pv.y,(float)(-tv.x*sin+tv.z*cos),(float)(tv.x*cos+tv.z*sin),(float)tv.y,(float)rv.dot(r.normalize()),(float)(rv.x*cos+rv.z*sin),
   (float)sin,(float)cos,mc.player.getXRot(),dyaw,dpitch,prevYaw,prevPitch,seen?1:0,seen?0:1,mc.player.getAttackStrengthScale(0),(float)Math.min(5,(now-lastAttack)/1e9),attack?1:0,hit?1:0,miss?1:0,changed?1:0,
   (float)((point.x-b.minX)/Math.max(.01,b.getXsize())*2-1),(float)((point.y-b.minY)/Math.max(.01,b.getYsize())*2-1),(float)((point.z-b.minZ)/Math.max(.01,b.getZsize())*2-1),mc.player.onGround()?1:0,mc.player.isSprinting()?1:0};
  return NeuroSchema.normalize(raw);
 }
 /** Render-time interpolation preserves unfinished learned deltas on both axes; no noise or analytic aim fallback. */
 public void frame(){long start=System.nanoTime();try{applyFrame();}finally{if(evaluation!=null)evaluation.frameCost((System.nanoTime()-start)/1e6);}}
 private void applyFrame(){
  lifecycle();if(!using)return;
  if(!view()){resetHistory();return;}
  if(target==null||mousePause(System.nanoTime())){cameraMotion.reset();return;}
  if(!target.isAlive()||target.isRemoved()||aura().getTarget()!=target||!aura().isEnabled()||!aura().getRotationMode().is("Neuro")||!visible(target)){resetHistory();return;}
  long now=System.nanoTime();double dt=lastFrame==0?0:Math.min(.05,(now-lastFrame)/1e9);lastFrame=now;
  Prediction ready=prediction;
  if(ready!=null){prediction=null;if(ready.epoch==epoch&&ready.target==targetId&&now-ready.time<100_000_000L){
   lastPredictedYaw=ready.yaw;lastPredictedPitch=ready.pitch;acceptedPredictions++;
   cameraMotion.enqueue(Math.clamp(ready.yaw*settings().yawGain.getValue(),-settings().maxYaw.getValue(),settings().maxYaw.getValue()),Math.clamp(ready.pitch*settings().pitchGain.getValue(),-settings().maxPitch.getValue(),settings().maxPitch.getValue()));lastInferenceLatency=ready.latency;
  }else stalePredictions++;}
  if(!cameraMotion.pending()||dt<=0)return;var movement=cameraMotion.advance(dt);float y=movement.yaw(),p=movement.pitch();
  mc.player.setYRot(NeuroSchema.wrap(mc.player.getYRot()+y));mc.player.setXRot(NeuroSchema.pitch(mc.player.getXRot()+p));mc.player.yRotO=mc.player.getYRot();mc.player.xRotO=mc.player.getXRot();
  CombatRotations.state().getRotation().set(mc.player.getYRot(),mc.player.getXRot());CombatRotations.state().getClientRotation().set(CombatRotations.state().getRotation());
 }
 /** Let Attack Aura swing on its ordinary timing even when the learned visible aim misses. */
 public boolean canAutoAttack(Entity victim){return using&&view()&&!mousePause(System.nanoTime())&&history.size()==NeuroSchema.LENGTH&&victim==target&&target!=null&&visible(target);}
 /** In Neuro mode an off-target swing must stay a real miss: Aura otherwise sends an entity-id attack packet. */
 public boolean recordAutoAttack(Entity victim){if(!using||victim!=target||target==null)return false;attack=true;miss=!rayHits(target);lastAttack=System.nanoTime();event(miss?"auto_miss":"auto_attack");return miss;}
 public void manualAttack(){if(!recording&&evaluation==null)return;if(mc.hitResult instanceof EntityHitResult botHit&&isBot(botHit.getEntity()))return;attack=true;miss=!(mc.hitResult instanceof EntityHitResult swingHit&&swingHit.getEntity()==target);lastAttack=System.nanoTime();event(miss?"manual_miss":"manual_attack_unconfirmed");}
 /** Cancel native packet for our negative-ID client entities. This is deliberately NOT server damage. */
 public boolean localAttack(Entity victim){if(!isBot(victim))return false;attempts++;boolean success=rayHits((LivingEntity)victim)&&mc.player.getAttackStrengthScale(0)>=.9;attack=true;hit=success;miss=!success;lastAttack=System.nanoTime();if(success){hits++;((LivingEntity)victim).hurtTime=10;((LivingEntity)victim).hurtDuration=10;}mc.player.resetAttackStrengthTicker();event(success?"local_hit":"local_miss");return true;}
 private void event(String kind){if(!recording||!worker.ready())return;var message=new JsonObject();message.addProperty("cmd","event");message.addProperty("event",kind);message.addProperty("seq",sequence);message.addProperty("segment",segment);message.addProperty("time_ns",System.nanoTime());worker.submit(message);}
 public void load(String name){if(recording||using||worker.running())throw new IllegalStateException("Сначала останови обучение/применение");if(!NeuroModel.validName(name))throw new IllegalArgumentException("Недопустимое имя модели");worker.load(name);modelName=name;inferenceError="";resetHistory();say("Загрузка "+name+"; результат в .neuro status");}
 public void list(){if(!worker.task(()->{try{Path folder=root.resolve("models");if(!Files.isDirectory(folder)){say("Сохранённых моделей нет");return;}try(var files=Files.list(folder)){say("Модели: "+files.filter(p->Files.isRegularFile(p.resolve("current.json"))).map(p->p.getFileName().toString()).sorted().limit(100).toList());}}catch(Exception e){say("Не удалось прочитать модели: "+e.getMessage());}}))say("Очередь файлов занята; повтори позже");}
 public void status(){
  JsonObject s=worker.status;String mode=!worker.error.isEmpty()||!inferenceError.isEmpty()?"Ошибка":worker.loading?"Загрузка":worker.stopping()?"Остановка":using?"Применение":evaluation!=null?"Сравнение manual":recording?(!worker.ready()?"Запуск тренера":number(s,"steps")>0?"Обучение":"Накопление данных"):"Ожидание";
  say(mode+" | модель "+(worker.model!=null&&!recording?worker.model.name:modelName)+" | ботов "+bots.size()+" | попадания только локальные "+hits+"/"+attempts);
  if(using)say("Предсказания: Java TCN / CPU, последняя задержка "+String.format(Locale.ROOT,"%.3f",lastInferenceLatency)+" мс");
  if(using)say(String.format(Locale.ROOT,"Последний шаг модели: yaw %.3f°, pitch %.3f° | множители %.2f / %.2f | принято %d, устарело %d | пауза мыши: %s",lastPredictedYaw,lastPredictedPitch,settings().yawGain.getValue(),settings().pitchGain.getValue(),acceptedPredictions,stalePredictions,mousePause(System.nanoTime())?"да":"нет"));
  if(s.has("stage"))say("Этап: "+value(s,"stage"));
  if(s.has("python"))say("Python: "+value(s,"python"));
  say("Устройство обучения: "+value(s,"device_name")+" / "+value(s,"device")+" | "+value(s,"backend")+" | "+value(s,"fallback"));
  say("Примеров "+value(s,"samples")+", обновлений "+(s.has("steps")?value(s,"steps"):worker.model==null?"0":worker.model.steps)+", train "+value(s,"train_loss")+", validation "+value(s,"validation_loss"));
  say("MAE° "+value(s,"mae_degrees")+"; baseline zero/last "+value(s,"baseline_zero_mae")+" / "+value(s,"baseline_persistence_mae")+"; Δвесов L2 "+value(s,"weight_delta_l2")+"; очередь "+worker.queued()+", пропусков "+worker.dropped);
  say("Движение: примеров "+value(s,"movement_samples")+", обновлений "+value(s,"movement_steps")+", прыжков в примерах "+value(s,"movement_jump_samples")+" | автоходьба "+settings().autoWalk.isEnabled()+", автопрыжок "+settings().autoJump.isEnabled());
  if(s.has("movement_direction_accuracy"))say("Validation движения: направления "+value(s,"movement_direction_accuracy")+", baseline стоять "+value(s,"movement_idle_baseline")+", jump precision/recall "+value(s,"movement_jump_precision")+" / "+value(s,"movement_jump_recall"));
  say("Сохранено: "+(number(s,"saved_at")==0?"ещё нет":java.time.Instant.ofEpochMilli((long)(number(s,"saved_at")*1000)))+". "+(recording&&worker.ready()&&number(s,"steps")==0?"Недостаточно данных: нужны отдельные train и validation фрагменты.":""));
  if(number(s,"mae_degrees")>0&&number(s,"mae_degrees")>=Math.min(number(s,"baseline_zero_mae"),number(s,"baseline_persistence_mae")))say("На validation пока нет преимущества над простым baseline; сходство со стилем не подтверждено.");
  if(!worker.error.isEmpty()||!inferenceError.isEmpty())say(worker.error+" "+inferenceError);
 }
 private static String value(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"—";}
 private static double number(JsonObject o,String key){try{return o.get(key).getAsDouble();}catch(Exception e){return 0;}}
 public void evaluate(String mode,long seed){requireView();if(evaluation!=null||recording||worker.running())throw new IllegalStateException("Сначала останови текущую сессию");if(mode.equals("model")){if(!using)use(true);}else if(using)throw new IllegalStateException("Для manual сначала .neuro use off");else {disableCombat();clearRotations();}bots.remove();bots.spawn(1,seed,true,settings());world=mc.level;resetHistory();evaluation=new Evaluation(mode,seed,settings(),mc.player.position());say("Сравнение "+mode+", seed "+seed+". Сценарий завершается .neuro eval stop; повторяй из той же позиции.");}
 public void stopEvaluation(){if(evaluation==null)throw new IllegalStateException("Сравнение не запущено");JsonObject report=evaluation.finish();evaluation=null;if(!worker.task(()->{try{Path folder=root.resolve("evaluations");Files.createDirectories(folder);Path file=folder.resolve(System.currentTimeMillis()+".json");Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(report));say("Сравнение: "+report+" → "+file);}catch(Exception e){say("Не удалось сохранить сравнение: "+e.getMessage());}}))say("Очередь файлов занята; отчёт не записан");}
 private static final class Evaluation{
  final JsonObject report=new JsonObject();long frames,onTarget;double speed,accel,jerk,prevVY,prevVP,prevAY,prevAP,latency,error,frameCostSum,tickCostSum;long renderFrames,ticks;final double[] latencies=new double[4096],frameCosts=new double[4096];long start=System.nanoTime();
  Evaluation(String mode,long seed,NeuroRecorder s,Vec3 origin){report.addProperty("mode",mode);report.addProperty("seed",seed);report.addProperty("speed_bps",s.botSpeed.getValue());report.addProperty("distance",s.distance.getValue());report.addProperty("strafe",s.strafe.getValue());report.addProperty("origin",origin.toString());}
  void add(float y,float p,double dt,boolean on,double ms,double degrees){double vy=y/dt,vp=p/dt,ay=(vy-prevVY)/dt,ap=(vp-prevVP)/dt;frames++;if(on)onTarget++;speed+=Math.hypot(vy,vp);accel+=Math.hypot(ay,ap);jerk+=Math.hypot(ay-prevAY,ap-prevAP)/dt;latency+=ms;error+=degrees;latencies[(int)((frames-1)%latencies.length)]=ms;prevVY=vy;prevVP=vp;prevAY=ay;prevAP=ap;}
  void frameCost(double ms){frameCosts[(int)(renderFrames++%frameCosts.length)]=ms;frameCostSum+=ms;}
  void tickCost(double ms){ticks++;tickCostSum+=ms;}
  JsonObject finish(){report.addProperty("frames",frames);report.addProperty("seconds",(System.nanoTime()-start)/1e9);report.addProperty("on_target_fraction",frames==0?0:(double)onTarget/frames);report.addProperty("mean_speed_deg_s",speed/Math.max(1,frames));report.addProperty("mean_abs_acceleration_deg_s2",accel/Math.max(1,frames));report.addProperty("mean_abs_jerk_deg_s3",jerk/Math.max(1,frames));report.addProperty("mean_inference_ms",latency/Math.max(1,frames));report.addProperty("mean_center_error_degrees",error/Math.max(1,frames));double[] sorted=Arrays.copyOf(latencies,(int)Math.min(frames,latencies.length));Arrays.sort(sorted);report.addProperty("p95_inference_ms",sorted.length==0?0:sorted[(int)((sorted.length-1)*.95)]);double[] costs=Arrays.copyOf(frameCosts,(int)Math.min(renderFrames,frameCosts.length));Arrays.sort(costs);report.addProperty("mean_neuro_frame_overhead_ms",frameCostSum/Math.max(1,renderFrames));report.addProperty("p95_neuro_frame_overhead_ms",costs.length==0?0:costs[(int)((costs.length-1)*.95)]);report.addProperty("mean_neuro_tick_overhead_ms",tickCostSum/Math.max(1,ticks));return report;}
 }
}
