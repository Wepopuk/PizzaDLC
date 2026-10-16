package pizza.dlc.mixin.client;

import zako.opt.entity.EntityLod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.api.interfaces.IChamsState;
import pizza.dlc.api.interfaces.CustomModelCarrier;
import pizza.dlc.api.util.render.ChamsRenderType;
import pizza.dlc.api.util.render.model.ChickenMesh;
import pizza.dlc.api.util.render.model.RabbitModel;
import pizza.dlc.api.util.render.model.VerityMesh;
import pizza.dlc.api.util.render.model.ImportedPlayerMesh;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.CustomModels;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererChamsMixin {
   @Shadow
   protected EntityModel<?> model;

   @Redirect(
      method = {"submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
      )
   )
   private void pizzadlc$chamsBody(
      SubmitNodeCollector collector,
      Model model,
      Object state,
      PoseStack poseStack,
      RenderType renderType,
      int lightCoords,
      int overlayCoords,
      int tintedColor,
      TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumbling
   ) {
      if (state instanceof CustomModelCarrier carrier && carrier.pizzadlc$customModel() != null
         && state instanceof AvatarRenderState avatarState) {
         String custom = carrier.pizzadlc$customModel();
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         CustomModels models = cc != null && cc.getModuleManager() != null ? cc.getModuleManager().get(CustomModels.class) : null;
         if (CustomModels.RABBIT.equals(custom) && model instanceof PlayerModel playerModel) {
            RabbitModel.of(avatarState.id)
               .submit(playerModel, avatarState, poseStack, collector, lightCoords, overlayCoords, tintedColor, outlineColor);
         } else if (CustomModels.VERITY.equals(custom) && models != null) {
            VerityMesh.submit(avatarState, poseStack, collector, lightCoords, overlayCoords, models.verityTexture(), models.size());
         } else if ((CustomModels.PUTIN.equals(custom) || CustomModels.BOYKISSER.equals(custom) || CustomModels.HITLER.equals(custom)) && models != null) {
            String id = switch (custom) {
               case CustomModels.PUTIN -> "putin";
               case CustomModels.BOYKISSER -> "boykisser";
               case CustomModels.HITLER -> "hitler";
               default -> throw new IllegalStateException("Unknown imported model: " + custom);
            };
            if (!ImportedPlayerMesh.submit(id, avatarState, poseStack, collector, lightCoords, overlayCoords, models.size())) {
               collector.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumbling);
            }
         } else {
            ChickenMesh.submit(avatarState, poseStack, collector, lightCoords, overlayCoords);
         }
         return;
      }
      IChamsState holder = state instanceof IChamsState h ? h : null;
      int solid = holder == null ? 0 : holder.pizzadlc$getChamsSolid();
      int glow = holder == null ? 0 : holder.pizzadlc$getChamsGlow();
      if (holder == null || !holder.pizzadlc$isChamsHideSkin() || pizza.dlc.api.util.render.AkrienOldScope.active()) {
         collector.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumbling);
      }

      if (solid != 0) {
         this.pizzadlc$chamsPass(collector, state, poseStack, ChamsRenderType.solid(), solid);
      }

      if (glow != 0) {
         this.pizzadlc$chamsPass(collector, state, poseStack, ChamsRenderType.glow(), glow);
      }
   }

   @Redirect(
      method = {"submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;shouldRenderLayers(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Z"
      )
   )
   private boolean pizzadlc$chamsLayers(LivingEntityRenderer<?, ?, ?> self, LivingEntityRenderState state) {
      return (!(state instanceof IChamsState holder && holder.pizzadlc$isChamsHideSkin()) || pizza.dlc.api.util.render.AkrienOldScope.active())
         && this.shouldRenderLayers(state) && !EntityLod.far(state.distanceToCameraSq);
   }

   @Shadow
   protected abstract boolean shouldRenderLayers(LivingEntityRenderState state);

   @Unique
   private void pizzadlc$chamsPass(SubmitNodeCollector collector, Object state, PoseStack poseStack, RenderType type, int color) {
      ((OrderedSubmitNodeCollector)collector).submitModel((Model)this.model, state, poseStack, type, 15728880, OverlayTexture.NO_OVERLAY, color, null, 0, null);
   }
}
