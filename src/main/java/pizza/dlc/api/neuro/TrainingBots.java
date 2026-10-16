package pizza.dlc.api.neuro;
import com.mojang.authlib.GameProfile;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.client.modules.impl.misc.NeuroRecorder;

public final class TrainingBots {
 private static final AtomicInteger IDS=new AtomicInteger(-1700000000);
 private final List<Bot> bots=new ArrayList<>();private ClientLevel world;
 public int size(){return bots.size();} public List<Bot> entities(){return List.copyOf(bots);} public boolean contains(Entity e){return bots.contains(e);}
 public int spawn(int count,long seed,boolean fixed,NeuroRecorder s){
  var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)throw new IllegalStateException("Сначала зайди в мир");
  if(!bots.isEmpty())throw new IllegalStateException("Боты уже созданы: .neuro bots remove");if(count<1||count>8)throw new IllegalArgumentException("Количество ботов: 1–8");
  world=mc.level;Vec3 origin=mc.player.position();double yaw=fixed?0:Math.toRadians(mc.player.getYRot());
  Vec3 sideways=new Vec3(-Math.cos(yaw),0,-Math.sin(yaw));
  for(int i=0;i<count;i++){
   double angle=i*Math.PI*2/count+yaw;
   Vec3 start=origin.add(-Math.sin(angle)*s.distance.getValue(),0,Math.cos(angle)*s.distance.getValue());
   var bot=new Bot(world,new GameProfile(new UUID(seed,i+1),"NeuroBot_"+(i+1)),start,sideways,seed+i*7919);bot.setId(IDS.getAndIncrement());
   bot.setPos(start);bot.setNoGravity(true);
   if(!world.noCollision(bot)){bot.discard();continue;}bot.setHealth(20);world.addEntity(bot);bots.add(bot);
  }
  if(bots.isEmpty()){world=null;throw new IllegalStateException("Нет свободного места; нужна открытая площадка");}return bots.size();
 }
 public void remove(){if(world!=null)for(var b:bots)world.removeEntity(b.getId(),Entity.RemovalReason.DISCARDED);bots.clear();world=null;}
 public void tick(NeuroRecorder s){
  var mc=Minecraft.getInstance();if(world==null)return;if(mc.level!=world||mc.player==null){remove();return;}
  for(var b:bots){if(b.isRemoved())continue;b.setOldPosAndRot();b.tickCount++;
   Vec3 velocity=b.motion.step(b.position(),s);Vec3 before=b.position();
   var next=b.getBoundingBox().move(velocity);
   boolean ledge=!world.noCollision(b,b.getBoundingBox().move(0,-.1,0))&&world.noCollision(b,next.move(0,-.1,0));
   // Check the entire step before moving: no wall stepping, falling or vertical drift.
   if(ledge||!world.noCollision(b,next))b.motion.reverse();else b.move(MoverType.SELF,velocity);
   b.setDeltaMovement(b.position().subtract(before));b.walkAnimation.update((float)Math.min(1,b.position().subtract(before).horizontalDistance()*4),.4F,1F);
   Vec3 f=mc.player.position().subtract(b.position());b.setYRot((float)Math.toDegrees(Math.atan2(-f.x,f.z)));b.yHeadRot=b.yBodyRot=b.getYRot();if(b.hurtTime>0)b.hurtTime--;
  }
 }
 /** Fixed ten-block lane, aligned with the player's left/right at spawn time. */
 public static final class Motion {
  public static final double HALF_LENGTH=5;
  private final Vec3 origin,axis;private int direction;private double velocity;
  public Motion(Vec3 origin,Vec3 sideways,long seed){
   Vec3 horizontal=sideways.multiply(1,0,1);
   if(!Double.isFinite(horizontal.lengthSqr())||horizontal.lengthSqr()<1e-12)throw new IllegalArgumentException("Invalid strafe axis");
   this.origin=origin;axis=horizontal.normalize();direction=new SplittableRandom(seed).nextBoolean()?1:-1;
  }
  public Vec3 step(Vec3 position,NeuroRecorder s){
   double offset=position.subtract(origin).dot(axis),distance=HALF_LENGTH-direction*offset;
   double speed=s.botSpeed.getValue()/20*s.strafe.getValue();
   if(speed<=0){velocity=0;return Vec3.ZERO;}
   double acceleration=speed*.22;
   double desired=direction*Math.min(speed,Math.sqrt(acceleration*acceleration+2*acceleration*Math.max(0,distance))-acceleration);
   velocity+=Math.clamp(desired-velocity,-acceleration,acceleration);
   double step=Math.clamp(velocity,-HALF_LENGTH-offset,HALF_LENGTH-offset);
   if(distance<.001){step=direction*Math.max(0,distance);direction=-direction;velocity=0;}
   return axis.scale(step);
  }
  public void reverse(){direction=-direction;velocity=0;}
 }
 public static final class Bot extends RemotePlayer {
  final Motion motion;
  Bot(ClientLevel level,GameProfile profile,Vec3 start,Vec3 sideways,long seed){super(level,profile);noPhysics=false;motion=new Motion(start,sideways,seed);}
  @Override public void tick(){} @Override public boolean isSpectator(){return false;}
 }
}
