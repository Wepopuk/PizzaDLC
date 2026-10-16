package pizza.dlc.mixin.client;

import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.emote.Axis;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmotePart;
import pizza.dlc.api.emote.EmotePoseCarrier;

// Runs after vanilla setupAnim, so walk/idle still drive every axis the emote has no keyframes for;
// the pose weight blends from that vanilla pose while an emote starts or stops.
@Mixin({PlayerModel.class})
public abstract class PlayerModelEmoteMixin {
   @Inject(method = {"setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V"}, at = {@At("TAIL")})
   private void pizzadlc$applyEmotePose(AvatarRenderState state, CallbackInfo ci) {
      EmoteEngine.Pose pose = ((EmotePoseCarrier)state).pizzadlc$getEmotePose();
      if (pose == null || pose.weight() <= 0.0F) return;

      HumanoidModel<?> model = (HumanoidModel<?>)(Object)this;
      for (Map.Entry<EmotePart, float[]> entry : pose.parts().entrySet()) {
         ModelPart part = switch (entry.getKey()) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_LEG -> model.rightLeg;
            case LEFT_LEG -> model.leftLeg;
         };
         pizzadlc$apply(part, entry.getValue(), pose.weight());
      }
   }

   @Unique
   private static void pizzadlc$apply(ModelPart part, float[] values, float weight) {
      part.x = pizzadlc$mix(part.x, values[Axis.POS_X.ordinal()], weight);
      part.y = pizzadlc$mix(part.y, values[Axis.POS_Y.ordinal()], weight);
      part.z = pizzadlc$mix(part.z, values[Axis.POS_Z.ordinal()], weight);
      part.xRot = pizzadlc$mix(part.xRot, values[Axis.ROT_PITCH.ordinal()], weight);
      part.yRot = pizzadlc$mix(part.yRot, values[Axis.ROT_YAW.ordinal()], weight);
      part.zRot = pizzadlc$mix(part.zRot, values[Axis.ROT_ROLL.ordinal()], weight);
   }

   @Unique
   private static float pizzadlc$mix(float vanilla, float emote, float weight) {
      if (Float.isNaN(emote)) return vanilla;

      return weight >= 1.0F ? emote : vanilla + (emote - vanilla) * weight;
   }
}
