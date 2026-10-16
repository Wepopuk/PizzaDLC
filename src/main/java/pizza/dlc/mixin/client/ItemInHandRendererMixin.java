package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.visuals.ViewModel;

@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Unique
   private float pizzadlc$equip;

   @Shadow
   private void applyItemArmTransform(PoseStack poseStack, HumanoidArm arm, float inverseArmHeight) {
      throw new AssertionError();
   }

   @Shadow
   private void swingArm(float attack, PoseStack poseStack, int invert, HumanoidArm arm) {
      throw new AssertionError();
   }

   @Unique
   private static HumanoidArm pizzadlc$arm(AbstractClientPlayer player, InteractionHand hand) {
      return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
   }

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER,
         ordinal = 0
      )}
   )
   private void pizzadlc$viewModelPosition(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      ViewModel vm = ViewModel.INSTANCE;
      if (vm != null && vm.isEnabled()) {
         if (!itemStack.isEmpty() && !itemStack.has(DataComponents.MAP_ID)) {
            vm.applyHandPosition(poseStack, pizzadlc$arm(player, hand));
         }
      }
   }

   @Inject(
      method = {"submitArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )}
   )
   private void pizzadlc$viewModelScale(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      ViewModel vm = ViewModel.INSTANCE;
      if (vm != null && vm.isEnabled()) {
         vm.applyHandScale(poseStack, pizzadlc$arm(player, hand));
      }
   }

   @Redirect(
      method = {"submitArmWithItem"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
         ordinal = 4
      )
   )
   private void pizzadlc$cancelEquipForCustomSwing(ItemInHandRenderer self, PoseStack poseStack, HumanoidArm arm, float inverseArmHeight) {
      ViewModel vm = ViewModel.INSTANCE;
      if (vm != null && vm.swingActive() && arm == HumanoidArm.RIGHT) {
         this.pizzadlc$equip = inverseArmHeight;
      } else {
         this.applyItemArmTransform(poseStack, arm, inverseArmHeight);
      }
   }

   @Redirect(
      method = {"submitArmWithItem"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V",
         ordinal = 2
      )
   )
   private void pizzadlc$customSwing(ItemInHandRenderer self, float attack, PoseStack poseStack, int invert, HumanoidArm arm) {
      ViewModel vm = ViewModel.INSTANCE;
      if (vm != null && vm.swingActive() && arm == HumanoidArm.RIGHT) {
         vm.renderSwordAnimation(poseStack, attack, this.pizzadlc$equip, arm);
      } else {
         this.swingArm(attack, poseStack, invert, arm);
      }
   }
}
