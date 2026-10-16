package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.interfaces.AkrienOldState;
import pizza.dlc.api.util.render.AkrienOldRenderType;
import pizza.dlc.api.util.render.AkrienOldScope;

@Mixin(value = SubmitNodeCollection.class, remap = false)
public class SubmitAkrienOldMixin {
   @Shadow @Final public SimpleFeatureRenderPhase alwaysOnTop;

   @Inject(method = "submitModel", at = @At("HEAD"))
   private <S> void pizzadlc$akrienModel(Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int tint,
      TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, CallbackInfo ci) {
      if (!AkrienOldScope.active() || type.pipeline().getLocation().getPath().startsWith("pipeline/chams_")) return;
      alwaysOnTop.submit(new ModelFeatureRenderer.Submit<>(AkrienOldRenderType.of(type), pose.last().copy(), model, state, light, overlay, tint, sprite, null));
   }

   @Inject(method = "submitCustomGeometry", at = @At("HEAD"))
   private void pizzadlc$akrienGeometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer, CallbackInfo ci) {
      if (!AkrienOldScope.active()) return;
      alwaysOnTop.submit(new CustomFeatureRenderer.Submit(pose.last().copy(), AkrienOldRenderType.of(type), renderer));
   }

   @Inject(method = "submitItem", at = @At("HEAD"))
   private void pizzadlc$akrienItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints,
      List<BakedQuad> quads, ItemStackRenderState.FoilType foil, CallbackInfo ci) {
      if (!AkrienOldScope.active()) return;
      var item = new ItemFeatureRenderer.Submit(pose.last().copy(), context, light, overlay, outline, tints, quads, foil);
      ((AkrienOldState)(Object)item).pizzadlc$setAkrienOld(true);
      alwaysOnTop.submit(item);
   }

   @Inject(method = "submitBlockModel", at = @At("HEAD"))
   private void pizzadlc$akrienBlock(PoseStack pose, RenderType type, List<BlockStateModelPart> parts, int[] tints, int light, int overlay, int tint, CallbackInfo ci) {
      if (!AkrienOldScope.active()) return;
      alwaysOnTop.submit(new BlockModelFeatureRenderer.Submit(pose.last().copy(), AkrienOldRenderType.of(type), parts, tints, light, overlay, tint, null));
   }
}
