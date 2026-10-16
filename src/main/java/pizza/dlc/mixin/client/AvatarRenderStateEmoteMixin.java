package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmotePoseCarrier;

@Mixin({AvatarRenderState.class})
public abstract class AvatarRenderStateEmoteMixin implements EmotePoseCarrier {
   @Unique
   private EmoteEngine.Pose pizzadlc$emotePose;
   @Unique
   private EmoteEngine.Pose pizzadlc$movementPose;

   @Override
   public EmoteEngine.Pose pizzadlc$getMovementPose() { return this.pizzadlc$movementPose; }

   @Override
   public void pizzadlc$setMovementPose(EmoteEngine.Pose pose) { this.pizzadlc$movementPose = pose; }

   @Override
   public EmoteEngine.Pose pizzadlc$getEmotePose() {
      return this.pizzadlc$emotePose;
   }

   @Override
   public void pizzadlc$setEmotePose(EmoteEngine.Pose pose) {
      this.pizzadlc$emotePose = pose;
   }
}
