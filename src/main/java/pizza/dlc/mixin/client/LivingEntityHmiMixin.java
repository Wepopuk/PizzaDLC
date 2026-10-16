package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiRuntime;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiSwing;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHmiMixin implements HmiSwing {
   @Unique
   private boolean pizzadlc$hmiInteractMain;
   @Unique
   private boolean pizzadlc$hmiInteractOff;
   @Unique
   private boolean pizzadlc$hmiBreaking;
   @Unique
   private int pizzadlc$hmiMainTicks;
   @Unique
   private boolean pizzadlc$hmiMainSwinging;
   @Unique
   private float pizzadlc$hmiMainProgress;
   @Unique
   private float pizzadlc$hmiMainProgressO;
   @Unique
   private int pizzadlc$hmiOffTicks;
   @Unique
   private boolean pizzadlc$hmiOffSwinging;
   @Unique
   private float pizzadlc$hmiOffProgress;
   @Unique
   private float pizzadlc$hmiOffProgressO;
   @Unique
   private int pizzadlc$hmiSwingCount;
   @Unique
   private boolean pizzadlc$hmiMainEvent;
   @Unique
   private boolean pizzadlc$hmiOffEvent;

   @Shadow
   public abstract ItemStack getMainHandItem();

   @Shadow
   public abstract ItemStack getOffhandItem();

   @Inject(method = "jumpFromGround", at = @At("TAIL"))
   private void pizzadlc$basketballJump(CallbackInfo ci) {
      AnimatedHands.jumped((LivingEntity)(Object)this);
   }

   @Override
   public int pizzadlc$hmiSwingCount() {
      return this.pizzadlc$hmiSwingCount;
   }

   @Override
   public void pizzadlc$hmiResetMainSwing(boolean interact) {
      this.pizzadlc$hmiMainTicks = 0;
      this.pizzadlc$hmiMainSwinging = true;
      this.pizzadlc$hmiMainEvent = !this.pizzadlc$hmiMainEvent;
      this.pizzadlc$hmiSwingCount++;
      this.pizzadlc$hmiInteractMain = interact;
      if (pizzadlc$isDestroying()) this.pizzadlc$hmiBreaking = true;
   }

   @Override
   public void pizzadlc$hmiResetOffSwing(boolean interact) {
      this.pizzadlc$hmiOffTicks = 0;
      this.pizzadlc$hmiOffSwinging = true;
      this.pizzadlc$hmiOffEvent = !this.pizzadlc$hmiOffEvent;
      this.pizzadlc$hmiInteractOff = interact;
   }

   @Override
   public float pizzadlc$hmiMainSwing(float partialTick) {
      return pizzadlc$lerpSwing(this.pizzadlc$hmiMainProgressO, this.pizzadlc$hmiMainProgress, partialTick);
   }

   @Override
   public float pizzadlc$hmiOffSwing(float partialTick) {
      return pizzadlc$lerpSwing(this.pizzadlc$hmiOffProgressO, this.pizzadlc$hmiOffProgress, partialTick);
   }

   @Override
   public boolean pizzadlc$hmiMainInteract() {
      return this.pizzadlc$hmiInteractMain;
   }

   @Override
   public boolean pizzadlc$hmiOffInteract() {
      return this.pizzadlc$hmiInteractOff;
   }

   @Override
   public boolean pizzadlc$hmiBlockBreaking() {
      return this.pizzadlc$hmiBreaking;
   }

   @Override
   public boolean pizzadlc$hmiMainEvent() {
      return this.pizzadlc$hmiMainEvent;
   }

   @Override
   public boolean pizzadlc$hmiOffEvent() {
      return this.pizzadlc$hmiOffEvent;
   }

   @Inject(method = "baseTick", at = @At("HEAD"))
   private void pizzadlc$hmiTick(CallbackInfo ci) {
      if (!AnimatedHands.active()) return;
      this.pizzadlc$hmiOffProgressO = this.pizzadlc$hmiOffProgress;
      this.pizzadlc$hmiMainProgressO = this.pizzadlc$hmiMainProgress;
      int mainDuration = HmiRuntime.swingTicks(this.getMainHandItem());
      if (this.pizzadlc$hmiMainSwinging) {
         if (++this.pizzadlc$hmiMainTicks >= mainDuration) {
            this.pizzadlc$hmiMainTicks = 0;
            this.pizzadlc$hmiMainSwinging = false;
         }
      } else {
         this.pizzadlc$hmiInteractMain = false;
         this.pizzadlc$hmiBreaking = false;
         this.pizzadlc$hmiMainTicks = 0;
      }
      this.pizzadlc$hmiMainProgress = (float)this.pizzadlc$hmiMainTicks / mainDuration;

      int offDuration = HmiRuntime.swingTicks(this.getOffhandItem());
      if (this.pizzadlc$hmiOffSwinging) {
         if (++this.pizzadlc$hmiOffTicks >= offDuration) {
            this.pizzadlc$hmiOffTicks = 0;
            this.pizzadlc$hmiOffSwinging = false;
         }
      } else {
         this.pizzadlc$hmiInteractOff = false;
         this.pizzadlc$hmiOffTicks = 0;
      }
      this.pizzadlc$hmiOffProgress = (float)this.pizzadlc$hmiOffTicks / offDuration;
   }

   @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
   private void pizzadlc$hmiSwing(InteractionHand hand, boolean updateSelf, CallbackInfo ci) {
      if (!AnimatedHands.active()) return;
      if (hand == InteractionHand.OFF_HAND) {
         int duration = HmiRuntime.swingTicks(this.getOffhandItem());
         if (!this.pizzadlc$hmiOffSwinging || this.pizzadlc$hmiOffTicks >= duration / 2 || this.pizzadlc$hmiOffTicks < 0) {
            this.pizzadlc$hmiOffTicks = -1;
            this.pizzadlc$hmiOffSwinging = true;
         }
         return;
      }

      int duration = HmiRuntime.swingTicks(this.getMainHandItem());
      if (!this.pizzadlc$hmiMainSwinging || this.pizzadlc$hmiMainTicks >= duration / 2 || this.pizzadlc$hmiMainTicks < 0) {
         this.pizzadlc$hmiMainTicks = -1;
         this.pizzadlc$hmiMainSwinging = true;
         if (pizzadlc$isDestroying()) this.pizzadlc$hmiBreaking = true;
      }
   }

   @Unique
   private static float pizzadlc$lerpSwing(float previous, float current, float partialTick) {
      float delta = current - previous;
      if (delta < 0.0F) delta += 1.0F;
      return previous + delta * partialTick;
   }

   @Unique
   private static boolean pizzadlc$isDestroying() {
      Minecraft mc = Minecraft.getInstance();
      return mc.gameMode != null && mc.gameMode.isDestroying();
   }
}
