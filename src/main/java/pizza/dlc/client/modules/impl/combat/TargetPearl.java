package pizza.dlc.client.modules.impl.combat;

import java.util.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.HitResult.Type;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.settings.impl.*;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.api.util.game.InventorySwapQueue;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(name="TargetPearl",category=ModuleCategory.COMBAT,description="Кидает перл в точку приземления вражеского перла")
public class TargetPearl extends Module {
   public final ModeSetting activation=new ModeSetting("Активация",this,"Авто","Бинд");
   public final BindSetting bind=new BindSetting("Бинд",this,-1);
   public final ModeSetting targetMode=new ModeSetting("Цель",this,"Все","Цель ауры");
   public final SliderSetting memorySeconds=new SliderSetting("Память таргета",this,12,1,30,1);
   public final SliderSetting nearDistance=new SliderSetting("Минимальная дистанция",this,10,8,20,1);
   public final SliderSetting accuracy=new SliderSetting("Точность (блоков)",this,1.75F,.5F,3.5F,.1F);
   public final BooleanSetting jumpCompensation=new BooleanSetting("Компенсировать прыжком",this);
   public final BooleanSetting avoidEntities=new BooleanSetting("Не кидать в энтити",this);
   public final BooleanSetting accountWater=new BooleanSetting("Учитывать воду",this);
   public final BooleanSetting autoTrap=new BooleanSetting("Авто ловушка",this);
   public final ModeSetting trapType=new ModeSetting("Тип ловушки",this,"Эндер Ловушка","Ловушка","Любая");
   private final Set<UUID> handled=new HashSet<>();
   private UUID rememberedTarget, jumpingPearl, trapTarget;
   private long rememberedAt,lastThrow,trapDeadline;
   private Vec3 jumpingLanding,trapLanding;
   private int jumpingTicks,pendingTicks;
   private boolean queued;

   public TargetPearl() {
      bind.setVisible(() -> activation.is("Бинд")); memorySeconds.setVisible(() -> targetMode.is("Цель ауры"));
      trapType.setVisible(autoTrap::isEnabled);
      jumpCompensation.setEnabled(true); avoidEntities.setEnabled(true); accountWater.setEnabled(true);
   }
   @Override public void onEnable() { super.onEnable(); resetState(); }
   @Override public void onDisable() { resetState(); super.onDisable(); }
   @EventHandler private void world(EventWorldLoad event) { resetState(); }
   private void resetState() {
      InventorySwapQueue.cancel(this);handled.clear();rememberedTarget=jumpingPearl=trapTarget=null;
      jumpingLanding=trapLanding=null;rememberedAt=lastThrow=trapDeadline=0;queued=false;jumpingTicks=pendingTicks=0;
   }
   @EventHandler public void onUpdate(EventUpdate event) {
      if (mc.player==null || mc.level==null || mc.gameMode==null || !mc.player.isAlive()) { resetState();return; }
      var current=CombatTarget.current();
      if (current!=null && current.isAlive()) { rememberedTarget=current.getUUID();rememberedAt=System.currentTimeMillis(); }
      handled.removeIf(id -> entity(id)==null);
      if (mc.player.containerMenu!=mc.player.inventoryMenu || !mc.player.containerMenu.getCarried().isEmpty() || mc.gui.screen()!=null
          || activation.is("Бинд") && !keyHeld(bind.getKey())) { cancelPending(); return; }
      if (queued) { if (++pendingTicks>40) cancelPending();return; }
      if (accountWater.isEnabled() && (mc.player.isUnderWater() || mc.player.isEyeInFluid(FluidTags.WATER))) { cancelPending();return; }
      if (jumpingLanding!=null) { finishJump();return; }
      if (System.currentTimeMillis()-lastThrow<1000 || pearlStack().isEmpty() || mc.player.getCooldowns().isOnCooldown(pearlStack())) { tickTrap();return; }
      ThrownEnderpearl best=null;Vec3 landing=null;
      for (Entity candidate:mc.level.entitiesForRendering()) {
         if (!(candidate instanceof ThrownEnderpearl pearl) || !validPearl(pearl)) continue;
         Vec3 predicted=predictLanding(pearl);Vec3 origin=origin(pearl);
         if (Math.hypot(predicted.x-origin.x,predicted.z-origin.z)<nearDistance.getValue()) { handled.add(pearl.getUUID());continue; }
         if (mc.player.distanceToSqr(predicted)>64*64) continue;
         if (best==null || pearl.tickCount<best.tickCount) { best=pearl;landing=predicted; }
      }
      if (best!=null) prepareThrow(best,landing);
      tickTrap();
   }
   private void cancelPending() { InventorySwapQueue.cancel(this);queued=false;jumpingLanding=null;jumpingPearl=null;jumpingTicks=0; }
   private ItemStack pearlStack() {
      if (mc.player.getOffhandItem().is(Items.ENDER_PEARL)) return mc.player.getOffhandItem();
      int slot=InventoryUtility.find(Items.ENDER_PEARL);
      return slot<0 ? ItemStack.EMPTY : mc.player.inventoryMenu.getSlot(slot).getItem();
   }
   private Entity entity(UUID id) {
      if (id==null || mc.level==null) return null;
      for (Entity e:mc.level.entitiesForRendering()) if (id.equals(e.getUUID())) return e;
      return null;
   }
   private LivingEntity rememberedTarget() {
      var current=CombatTarget.current();if (current!=null && current.isAlive()) return current;
      if (System.currentTimeMillis()-rememberedAt>memorySeconds.getValue()*1000) return null;
      return entity(rememberedTarget) instanceof LivingEntity living && living.isAlive() && !living.isSpectator() ? living : null;
   }
   private boolean validPearl(ThrownEnderpearl pearl) {
      if (handled.contains(pearl.getUUID())) return false;
      Entity owner=estimateOwner(pearl);
      if (owner==mc.player) { handled.add(pearl.getUUID()); return false; }
      if (owner!=null && PizzaDLC.getInstance().getFriendManager().isFriend(owner.getName().getString())) return false;
      if (!targetMode.is("Цель ауры")) return true;
      LivingEntity target=rememberedTarget();
      return target!=null && (owner==null ? nearTargetOrigin(pearl,target) : target.getUUID().equals(owner.getUUID()));
   }
   private Vec3 origin(ThrownEnderpearl pearl) {
      Entity owner=estimateOwner(pearl);if (owner!=null) return owner.position();
      Vec3 velocity=pearl.getDeltaMovement();
      return velocity.lengthSqr()<.0001 ? pearl.position() : pearl.position().subtract(velocity.normalize().scale(Math.min(3,velocity.length()*2)));
   }
   private Vec3 spawnPoint() { return new Vec3(mc.player.getX(),mc.player.getEyeY()-.1,mc.player.getZ()); }
   private void prepareThrow(ThrownEnderpearl pearl,Vec3 landing) {
      Shot shot=solveShot(spawnPoint(),throwMovement(false),landing);
      Shot jump=jumpCompensation.isEnabled() && mc.player.onGround() ? solveShot(spawnPoint(),throwMovement(true),landing) : null;
      if (jump!=null && (shot==null || jump.error+.25<shot.error)) shot=jump.withJump(true);
      if (shot==null) shot=fallbackAim(landing);
      if (shot==null || shot.error>accuracy.getValue() || avoidEntities.isEnabled() && shot.entityHit) return;
      if (shot.jump && mc.player.onGround()) {
         jumpingPearl=pearl.getUUID();jumpingLanding=landing;jumpingTicks=0;mc.player.jumpFromGround();
      } else throwPearl(pearl.getUUID(),landing,shot);
   }
   private void finishJump() {
      if (++jumpingTicks>6) { cancelPending();return; }
      if (mc.player.onGround()) return;
      Vec3 landing=entity(jumpingPearl) instanceof ThrownEnderpearl pearl ? predictLanding(pearl) : jumpingLanding;
      Shot shot=solveShot(spawnPoint(),throwMovement(false),landing);if (shot==null) shot=fallbackAim(landing);
      if (shot!=null && shot.error<=accuracy.getValue() && (!avoidEntities.isEnabled() || !shot.entityHit)) {
         UUID id=jumpingPearl;jumpingLanding=null;jumpingPearl=null;throwPearl(id,landing,shot);
      }
   }
   private void throwPearl(UUID pearl,Vec3 landing,Shot shot) {
      queued=true;pendingTicks=0;
      boolean accepted=SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(shot.yaw,shot.pitch),this,"pearl",
         InventorySwapQueue.configuredMode(),() -> {
            handled.add(pearl);lastThrow=System.currentTimeMillis();queued=false;
            if (autoTrap.isEnabled() && trapServer() && trapSlot()>=0) {
               trapLanding=landing;var target=rememberedTarget();trapTarget=target==null ? rememberedTarget : target.getUUID();
               trapDeadline=lastThrow+4000;
            }
         });
      if (!accepted) queued=false;
   }
   private boolean trapServer() {
      var server=mc.getCurrentServer();return server!=null && server.ip.toLowerCase(Locale.ROOT).contains("reallyworld");
   }
   private int trapSlot() {
      int normal=-1,ender=-1;
      for (int i=0;i<36;i++) {
         ItemStack stack=mc.player.getInventory().getItem(i);if (!stack.is(Items.HEART_OF_THE_SEA)) continue;
         String name=stack.getHoverName().getString().replaceAll("§.","").toLowerCase(Locale.ROOT).replace('ё','е').replaceAll("\\s+"," ").trim();
         if (name.equals("ловушка")) normal=i<9 ? i+36 : i;
         if (name.equals("эндер ловушка")) ender=i<9 ? i+36 : i;
      }
      return trapType.is("Эндер Ловушка") ? ender : trapType.is("Ловушка") ? normal : ender>=0 ? ender : normal;
   }
   private void tickTrap() {
      if (trapLanding==null) return;
      if (!autoTrap.isEnabled() || !trapServer() || System.currentTimeMillis()>trapDeadline) { trapLanding=null;return; }
      if (mc.player.distanceToSqr(trapLanding)>25) return;
      LivingEntity target=entity(trapTarget) instanceof LivingEntity living ? living : rememberedTarget();
      int slot=trapSlot();if (slot<0 || target==null || !target.isAlive() || mc.player.distanceTo(target)>6) return;
      Vec3 direction=target.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());
      float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
      float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z)));
      // Use the selected trap variant, not another heart of the sea earlier in the inventory.
      SwapSystem.useItemTrackedFromSlot(slot,new Angle(yaw,pitch),this,"trap",InventorySwapQueue.configuredMode(),() -> trapLanding=null);
   }
   private static boolean keyHeld(int key) {
      if (key==-1 || key==-200 || key==-201) return false;
      var window=net.minecraft.client.Minecraft.getInstance().getWindow();
      return key<=-100 ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(pizza.dlc.api.util.render.streamproof.StreamProofRenderer.inputHandle(window.handle()),-100-key)==1
         : com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,key);
   }
   private boolean nearTargetOrigin(ThrownEnderpearl var1, LivingEntity var2) {
      Vec3 var3 = var1.position();
      Vec3 var4 = var1.getDeltaMovement();
      if (var2.distanceToSqr(var3) <= 256.0) {
         return true;
      }

      if (var4.lengthSqr() < 1.0E-4) {
         return false;
      }

      Vec3 var5 = var3.subtract(var4.normalize().scale(Math.min(6.0, var4.length() * 4.0)));
      return var2.distanceToSqr(var5) <= 64.0;
   }

   private Entity estimateOwner(ThrownEnderpearl var1) {
      Entity var2 = var1.getOwner();
      if (var2 != null) {
         return var2;
      }

      Vec3 var3 = var1.position();
      Vec3 var4 = var1.getDeltaMovement();
      Vec3 var5 = var4.lengthSqr() < 1.0E-4 ? var3 : var3.subtract(var4.normalize().scale(Math.min(3.0, var4.length() * 2.0)));
      Player var6 = null;
      double var7 = 256.0;

      for (Player var10 : mc.level.players()) {
         if (var10.isAlive()) {
            double var11 = var10.distanceToSqr(var5);
            if (var11 < var7) {
               var7 = var11;
               var6 = var10;
            }
         }
      }

      return var6;
   }

   private TargetPearl.Shot fallbackAim(Vec3 var1) {
      Vec3 var2 = spawnPoint();
      Vec3 var3 = var1.subtract(var2);
      float var4 = (float)Math.toDegrees(Math.atan2(-var3.x, var3.z));
      double var5 = Math.sqrt(var3.x * var3.x + var3.z * var3.z);
      float var7 = (float)(-Math.toDegrees(Math.atan2(var3.y, var5)));
      TargetPearl.Shot var8 = null;

      for (float var9 = var7 - 25.0F; var9 <= var7 + 25.0F; var9 += 0.35F) {
         TargetPearl.Shot var10 = this.evaluateShot(var2, this.throwMovement(false), var1, var4, var9);
         if (var10 != null && (!avoidEntities.isEnabled() || !var10.entityHit) && (var8 == null || var10.error < var8.error)) {
            var8 = var10;
         }
      }

      if (var8 != null) {
         return var8.withJump(false);
      }

      for (float var11 = var7 - 25.0F; var11 <= var7 + 25.0F; var11 += 0.35F) {
         TargetPearl.Shot var12 = this.evaluateShot(var2, this.throwMovement(false), var1, var4, var11);
         if (var12 != null && (var8 == null || var12.error < var8.error)) {
            var8 = var12;
         }
      }

      return var8 == null ? null : var8.withJump(false);
   }

   private Vec3 entityCollision(Vec3 var1, Vec3 var2) {
      AABB var3 = new AABB(var1, var2).inflate(1.3);
      List var4 = mc.level.getEntities(mc.player, var3, this::collidable);
      if (var4.isEmpty()) {
         return null;
      }

      Vec3 var5 = null;
      double var6 = Double.MAX_VALUE;

      for (Entity var9 : (Iterable<Entity>) (Iterable) var4) {
         Optional var10 = var9.getBoundingBox().inflate(0.3).clip(var1, var2);
         if (!var10.isEmpty()) {
            double var11 = var1.distanceToSqr((Vec3)var10.get());
            if (!(var11 >= var6)) {
               var5 = (Vec3)var10.get();
               var6 = var11;
            }
         }
      }

      return var5;
   }

   private TargetPearl.Shot evaluateShot(Vec3 var1, Vec3 var2, Vec3 var3, float var4, float var5) {
      TargetPearl.Impact var6 = this.simulateShot(var1, var2, var4, var5, true);
      return var6 == null
         ? null
         : new TargetPearl.Shot(var4, Mth.clamp(var5, -90.0F, 90.0F), var6.point.distanceTo(var3), false, var6.entityHit);
   }

   private Vec3 predictLanding(ThrownEnderpearl var1) {
      Vec3 var2 = var1.position();
      Vec3 var3 = var1.getDeltaMovement();

      for (int var4 = 0; var4 < 320; var4++) {
         Vec3 var5 = var2;
         var2 = var2.add(var3);
         var3 = var3.scale(var1.isInWater() ? 0.8 : 0.99).add(0.0, -0.03, 0.0);
         BlockHitResult var6 = mc.level.clip(new ClipContext(var5.add(0.0, 0.1, 0.0), var2, Block.COLLIDER, Fluid.NONE, mc.player));
         if (var6.getType() == Type.BLOCK) {
            return var6.getLocation();
         }
      }

      return var2;
   }

   private boolean waterAt(Vec3 var1) {
      BlockPos var2 = BlockPos.containing(var1);
      FluidState var3 = mc.level.getFluidState(var2);
      return var3.is(FluidTags.WATER) && var3.getHeight(mc.level, var2) > 0.0;
   }

   private Vec3 throwMovement(boolean var1) {
      Vec3 var2 = mc.player.getDeltaMovement();
      if (var1) {
         return new Vec3(var2.x, 0.42, var2.z);
      } else {
         return mc.player.onGround() ? new Vec3(var2.x, 0.0, var2.z) : var2;
      }
   }

   private boolean collidable(Entity var1) {
      if (var1 == null || var1 == mc.player || !var1.isAlive() || var1.isSpectator()) {
         return false;
      } else {
         return var1 instanceof ThrownEnderpearl ? false : var1.canBeHitByProjectile();
      }
   }

   private TargetPearl.Impact simulateShot(Vec3 var1, Vec3 var2, float var3, float var4, boolean var5) {
      float var6 = var3 * (float) (Math.PI / 180.0);
      float var7 = var4 * (float) (Math.PI / 180.0);
      Vec3 var8 = new Vec3(-Mth.sin(var6) * Mth.cos(var7), -Mth.sin(var7), Mth.cos(var6) * Mth.cos(var7)).normalize();
      Vec3 var9 = var1;
      Vec3 var10 = var8.scale(1.5).add(var2);

      for (int var11 = 0; var11 < 320; var11++) {
         Vec3 var12 = var9;
         var9 = var9.add(var10);
         if (var5) {
            Vec3 var13 = this.entityCollision(var12, var9);
            if (var13 != null) {
               return new TargetPearl.Impact(var13, true);
            }
         }

         BlockHitResult var16 = mc.level.clip(new ClipContext(var12.add(0.0, 0.1, 0.0), var9, Block.COLLIDER, Fluid.NONE, mc.player));
         if (var16.getType() != Type.MISS) {
            return new TargetPearl.Impact(var16.getLocation(), false);
         }

         double var14 = this.waterAt(var9) ? 0.8 : 0.99;
         var10 = var10.scale(var14).add(0.0, -0.03, 0.0);
      }

      return new TargetPearl.Impact(var9, false);
   }

   private TargetPearl.Shot solveShot(Vec3 var1, Vec3 var2, Vec3 var3) {
      double var4 = var3.x - var1.x;
      double var6 = var3.z - var1.z;
      float var8 = (float)Math.toDegrees(Math.atan2(-var4, var6));
      TargetPearl.Shot var9 = null;

      for (float var10 = -70.0F; var10 <= 70.0F; var10++) {
         TargetPearl.Shot var11 = this.evaluateShot(var1, var2, var3, var8, var10);
         if (var11 != null && (!avoidEntities.isEnabled() || !var11.entityHit) && (var9 == null || var11.error < var9.error)) {
            var9 = var11;
         }
      }

      if (var9 == null) {
         for (float var15 = -70.0F; var15 <= 70.0F; var15++) {
            TargetPearl.Shot var17 = this.evaluateShot(var1, var2, var3, var8, var15);
            if (var17 != null && (var9 == null || var17.error < var9.error)) {
               var9 = var17;
            }
         }
      }

      if (var9 == null) {
         return null;
      }

      float var16 = Mth.clamp(var9.pitch - 1.0F, -90.0F, 90.0F);
      float var18 = Mth.clamp(var9.pitch + 1.0F, -90.0F, 90.0F);

      for (float var12 = var16; var12 <= var18; var12 += 0.05F) {
         for (float var13 = -0.6F; var13 <= 0.6F; var13 += 0.1F) {
            TargetPearl.Shot var14 = this.evaluateShot(var1, var2, var3, var8 + var13, var12);
            if (var14 != null && (!avoidEntities.isEnabled() || !var14.entityHit) && (var9 == null || var14.error < var9.error)) {
               var9 = var14;
            }
         }
      }

      if (var9 == null) {
         return null;
      } else if (var9.error > 1.75) {
         return var9.error < 3.5 ? var9 : null;
      } else {
         return var9;
      }
   }
   public record Shot(float yaw,float pitch,double error,boolean jump,boolean entityHit) {
      public Shot withJump(boolean value) { return new Shot(yaw,pitch,error,value,entityHit); }
   }
   private record Impact(Vec3 point,boolean entityHit) {}
}
