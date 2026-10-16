package pizza.dlc.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmotePoseCarrier;

/** Applies to the player and humanoid armor models; skin overlays are children of these bones. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelAnimationsMixin {
   @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
   private void pizzadlc$movementPose(HumanoidRenderState state, CallbackInfo ci) {
      if (!(state instanceof AvatarRenderState)) return;
      EmoteEngine.Pose pose = ((EmotePoseCarrier)state).pizzadlc$getMovementPose();
      if (pose == null) return;
      HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
      pose.parts().forEach((part, values) -> {
         ModelPart bone = switch (part) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
         };
         // Keep the slim model's shoulder offset while moving its anchor with the torso.
         float armY = part == pizza.dlc.api.emote.EmotePart.RIGHT_ARM || part == pizza.dlc.api.emote.EmotePart.LEFT_ARM
            ? bone.getInitialPose().y() - 2 : 0;
         bone.x = pizzadlc$blend(bone.x, values[0], pose.weight());
         bone.y = pizzadlc$blend(bone.y, values[1] + armY, pose.weight());
         bone.z = pizzadlc$blend(bone.z, values[2], pose.weight());
         bone.xRot = pizzadlc$blend(bone.xRot, values[3], pose.weight());
         bone.yRot = pizzadlc$blend(bone.yRot, values[4], pose.weight());
         bone.zRot = pizzadlc$blend(bone.zRot, values[5], pose.weight());
      });
   }

   @Unique
   private static float pizzadlc$blend(float value, float target, float weight) {
      return Float.isNaN(target) ? value : value + (target - value) * weight;
   }
}
