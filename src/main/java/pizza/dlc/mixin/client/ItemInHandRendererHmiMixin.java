package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiHandRenderer;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererHmiMixin implements HmiHandRenderer.Access {
   @Unique
   private static Object pizzadlc$bothHands;
   @Unique
   private boolean pizzadlc$hmiReady;

   @Shadow
   private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, float inverseArmHeight, float attack, HumanoidArm arm) {
      throw new AssertionError();
   }

   @Shadow
   private void renderMap(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ItemStack itemStack) {
      throw new AssertionError();
   }

   @Override
   public void pizzadlc$hmiRenderArm(PoseStack pose, SubmitNodeCollector collector, int light, HumanoidArm arm) {
      this.renderPlayerArm(pose, collector, light, 0.0F, 0.0F, arm);
   }

   @Override
   public void pizzadlc$hmiRenderMap(PoseStack pose, SubmitNodeCollector collector, int light, ItemStack stack) {
      this.renderMap(pose, collector, light, stack);
   }

   @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
   private void pizzadlc$hmiFrame(float frameInterp, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int lightCoords, CallbackInfo ci) {
      this.pizzadlc$hmiReady = AnimatedHands.prepareFrame();
   }

   @WrapOperation(
      method = "submitHandsWithItems",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )
   )
   private void pizzadlc$hmiArm(
      ItemInHandRenderer self,
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int lightCoords,
      Operation<Void> original
   ) {
      if (this.pizzadlc$hmiReady) {
         HmiHandRenderer.render(self, player, frameInterp, hand, attack, itemStack, inverseArmHeight, poseStack, collector, lightCoords);
      } else {
         original.call(self, player, frameInterp, xRot, hand, attack, itemStack, inverseArmHeight, poseStack, collector, lightCoords);
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   @Inject(method = "evaluateWhichHandsToRender", at = @At("RETURN"), cancellable = true)
   private static void pizzadlc$hmiBothHands(LocalPlayer player, CallbackInfoReturnable cir) {
      if (!AnimatedHands.active()) return;
      Object both = pizzadlc$bothHands(cir.getReturnValue());
      if (both != null) cir.setReturnValue(both);
   }

   @Unique
   @SuppressWarnings({"rawtypes", "unchecked"})
   private static Object pizzadlc$bothHands(Object sample) {
      if (pizzadlc$bothHands == null && sample instanceof Enum<?> e) {
         pizzadlc$bothHands = Enum.valueOf((Class)e.getDeclaringClass(), "RENDER_BOTH_HANDS");
      }
      return pizzadlc$bothHands;
   }
}
