package pizza.dlc.api.emote;

// Duck interface on AvatarRenderState: carries the sampled emote pose from extractRenderState to PlayerModel.setupAnim.
public interface EmotePoseCarrier {
   EmoteEngine.Pose pizzadlc$getEmotePose();

   void pizzadlc$setEmotePose(EmoteEngine.Pose pose);

   EmoteEngine.Pose pizzadlc$getMovementPose();

   void pizzadlc$setMovementPose(EmoteEngine.Pose pose);
}
