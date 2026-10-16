package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmotePoseCarrier;
import pizza.dlc.api.interfaces.CustomModelCarrier;
import pizza.dlc.api.util.render.model.ChickenMesh;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.CustomModels;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;

@Mixin({AvatarRenderer.class})
public abstract class AvatarRendererMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$captureCustomModel(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
      CustomModels models = models();
      boolean emoteSkin = EmoteEngine.INSTANCE.skinOverrideFor(avatar.getUUID()) != null;
      ((CustomModelCarrier) state).pizzadlc$setCustomModel(models == null || emoteSkin ? null : models.modelFor(avatar));
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$captureEmotePose(Avatar avatar, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
      EmotePoseCarrier carrier = (EmotePoseCarrier)state;
      EmoteEngine.Pose emote = EmoteEngine.INSTANCE.poseFor(avatar.getUUID());
      carrier.pizzadlc$setEmotePose(emote);
      carrier.pizzadlc$setMovementPose(emote == null ? AnimatedHands.movementPose(avatar, state, tickDelta) : null);
   }

   @Inject(
      method = {"shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$hideLayersOnCustomModel(AvatarRenderState state, CallbackInfoReturnable<Boolean> cir) {
      if (((CustomModelCarrier) state).pizzadlc$customModel() != null) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$customRightHand(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci) {
      CustomModels models = models();
      if (models != null && models.handFromModel()) {
         ChickenMesh.submitArm(pose, collector, light, true);
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$customLeftHand(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin, boolean sleeve, CallbackInfo ci) {
      CustomModels models = models();
      if (models != null && models.handFromModel()) {
         ChickenMesh.submitArm(pose, collector, light, false);
         ci.cancel();
      }
   }

   private static CustomModels models() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      return cc != null && cc.getModuleManager() != null ? cc.getModuleManager().get(CustomModels.class) : null;
   }
}
