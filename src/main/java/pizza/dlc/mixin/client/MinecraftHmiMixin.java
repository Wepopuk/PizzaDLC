package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiSwing;

@Mixin(Minecraft.class)
public abstract class MinecraftHmiMixin {
   @Shadow
   public LocalPlayer player;

   @Inject(
      method = "startAttack",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V")
   )
   private void pizzadlc$hmiAttackSwing(CallbackInfoReturnable<Boolean> cir) {
      if (AnimatedHands.active() && this.player instanceof HmiSwing swing) {
         swing.pizzadlc$hmiResetMainSwing(false);
      }
   }

   @WrapOperation(
      method = "startUseItem",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V")
   )
   private void pizzadlc$hmiUseSwing(LocalPlayer self, InteractionHand hand, Operation<Void> original) {
      if (AnimatedHands.active() && self instanceof HmiSwing swing) {
         if (hand == InteractionHand.MAIN_HAND) {
            swing.pizzadlc$hmiResetMainSwing(true);
         } else {
            swing.pizzadlc$hmiResetOffSwing(true);
         }
      }
      original.call(self, hand);
   }
}
