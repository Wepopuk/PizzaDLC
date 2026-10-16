package pizza.dlc.mixin.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.events.impl.game.EventMotion;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.player.EventSlowDown;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.util.game.SentMotion;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.impl.player.NoPush;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;

@Mixin({LocalPlayer.class})
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer implements QuickImports, SentMotion {
   @Unique
   private EventMotion pizzadlc$motion;

   @Shadow
   private double xLast;

   @Shadow
   private double yLast;

   @Shadow
   private double zLast;

   @Shadow
   private int positionReminder;

   @Shadow
   private float yRotLast;

   @Shadow
   private float xRotLast;

   @Shadow
   private boolean lastOnGround;

   @Shadow
   private boolean lastHorizontalCollision;

   @Shadow
   protected abstract boolean isSlowDueToUsingItem();

   @Shadow
   protected abstract void sendIsSprintingIfNeeded();

   public ClientPlayerEntityMixin(ClientLevel level, GameProfile profile) {
      super(level, profile);
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$onUpdate(CallbackInfo ci) {
      if (pizza.dlc.api.neuro.NeuroSystem.get()!=null) pizza.dlc.api.neuro.NeuroSystem.get().tick();
      new EventUpdate().call();
   }

   @Inject(method = "tick()V", at = @At("TAIL"))
   private void pizzadlc$movementAnimationTick(CallbackInfo ci) {
      AnimatedHands.movementTick((LocalPlayer)(Object)this);
   }

   @Inject(
      method = {"sendPosition()V"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$flushGrimBeforeFlying(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         SwapSystem.flushGrim();
      }
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$flushGrimFallback(CallbackInfo ci) {
      if ((Object)this == mc.player && mc.player.isPassenger()) {
         SwapSystem.flushGrim();
      }
   }

   @Inject(
      method = {"closeContainer()V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$grimClose(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         LocalPlayer self = (LocalPlayer)(Object)this;
         if (SwapSystem.grimDefer(self::closeContainer)) {
            ci.cancel();
         } else {
            SwapSystem.testMaskBefore();
         }
      }
   }

   @Inject(
      method = {"closeContainer()V"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$grimCloseAfter(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         SwapSystem.testMaskAfter();
      }
   }

   @Redirect(
      method = {"modifyInput"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"
      )
   )
   private boolean pizzadlc$noSlowItem(LocalPlayer instance) {
      EventSlowDown event = new EventSlowDown(EventSlowDown.Type.ITEM).call();
      return !event.isCancelled() && instance.isUsingItem();
   }

   @Redirect(
      method = {"canStartSprinting"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"
      )
   )
   private boolean pizzadlc$noSlowSprint(LocalPlayer instance) {
      EventSlowDown event = new EventSlowDown(EventSlowDown.Type.SPRINT).call();
      return !event.isCancelled() && this.isSlowDueToUsingItem();
   }

   @Inject(
      method = {"moveTowardsClosestSpace(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$noPushBlocks(double x, double z, CallbackInfo ci) {
      if ((Object)this == mc.player && NoPush.noBlocks()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendPosition"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onSendPosition(CallbackInfo ci) {
      EventMotion event = new EventMotion(
         this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround(), this.isShiftKeyDown(), this.isSprinting()
      )
         .call();
      this.pizzadlc$motion = event;
      if (event.isCancelled()) {
         this.sendIsSprintingIfNeeded();
         if (!event.getReplacement().isEmpty()) {
            this.pizzadlc$sendReplacement(event);
         } else if (event.isRotationOnly()) {
            this.pizzadlc$sendRotationOnly(event);
         }

         ci.cancel();
      }
   }

   @Override
   public float pizzadlc$sentYaw() {
      return this.yRotLast;
   }

   @Override
   public float pizzadlc$sentPitch() {
      return this.xRotLast;
   }

   @Unique
   private void pizzadlc$sendReplacement(EventMotion event) {
      LocalPlayer self = (LocalPlayer)(Object)this;
      for (Packet<?> packet : event.getReplacement()) {
         self.connection.send(packet);
         if (packet instanceof ServerboundMovePlayerPacket move) {
            if (move.hasPosition()) {
               this.xLast = move.getX(this.xLast);
               this.yLast = move.getY(this.yLast);
               this.zLast = move.getZ(this.zLast);
               this.positionReminder = 0;
            }

            if (move.hasRotation()) {
               this.yRotLast = move.getYRot(this.yRotLast);
               this.xRotLast = move.getXRot(this.xRotLast);
            }

            this.lastOnGround = move.isOnGround();
            this.lastHorizontalCollision = move.horizontalCollision();
         }
      }

      event.executePostActions();
   }

   @Unique
   private void pizzadlc$sendRotationOnly(EventMotion event) {
      float yaw = event.getYaw();
      float pitch = event.getPitch();
      if (yaw == this.yRotLast && pitch == this.xRotLast) return;

      ((LocalPlayer)(Object)this).connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, this.lastOnGround, this.lastHorizontalCollision));
      this.yRotLast = yaw;
      this.xRotLast = pitch;
      event.executePostActions();
   }

   @Inject(
      method = {"sendPosition"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$onSendPositionTail(CallbackInfo ci) {
      if (this.pizzadlc$motion != null) {
         this.pizzadlc$motion.executePostActions();
      }
   }

   @Redirect(
      method = {"sendPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"
      )
   )
   private double pizzadlc$motionX(LocalPlayer instance) {
      return this.pizzadlc$motion != null ? this.pizzadlc$motion.getX() : instance.getX();
   }

   @Redirect(
      method = {"sendPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"
      )
   )
   private double pizzadlc$motionY(LocalPlayer instance) {
      return this.pizzadlc$motion != null ? this.pizzadlc$motion.getY() : instance.getY();
   }

   @Redirect(
      method = {"sendPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"
      )
   )
   private double pizzadlc$motionZ(LocalPlayer instance) {
      return this.pizzadlc$motion != null ? this.pizzadlc$motion.getZ() : instance.getZ();
   }

   @Redirect(
      method = {"sendPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"
      )
   )
   private float pizzadlc$motionYaw(LocalPlayer instance) {
      return this.pizzadlc$motion != null ? this.pizzadlc$motion.getYaw() : instance.getYRot();
   }

   @Redirect(
      method = {"sendPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"
      )
   )
   private float pizzadlc$motionPitch(LocalPlayer instance) {
      return this.pizzadlc$motion != null ? this.pizzadlc$motion.getPitch() : instance.getXRot();
   }

   @Redirect(
      method = {"sendPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"
      )
   )
   private boolean pizzadlc$motionOnGround(LocalPlayer instance) {
      return this.pizzadlc$motion != null ? this.pizzadlc$motion.isOnGround() : instance.onGround();
   }

   @Redirect(
      method = {"sendPosition"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private Vec3 pizzadlc$motionPosition(LocalPlayer instance) {
      return this.pizzadlc$motion != null ? new Vec3(this.pizzadlc$motion.getX(), this.pizzadlc$motion.getY(), this.pizzadlc$motion.getZ()) : instance.position();
   }
}
