package pizza.dlc.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.skin.UiSkin;

@Mixin(GuiGraphicsExtractor.class)
public class GuiSpriteSkinMixin {
   @Inject(
      method = "blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void pizzadlc$skinSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, int color, CallbackInfo ci) {
      if (UiSkin.drawSprite((GuiGraphicsExtractor)(Object)this, sprite, x, y, width, height, color)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void pizzadlc$skinTexture(
      RenderPipeline pipeline,
      Identifier texture,
      int x,
      int y,
      float u,
      float v,
      int width,
      int height,
      int uWidth,
      int vHeight,
      int textureWidth,
      int textureHeight,
      int color,
      CallbackInfo ci
   ) {
      if (UiSkin.drawTexture((GuiGraphicsExtractor)(Object)this, texture, x, y, width, height, color)) {
         ci.cancel();
      }
   }
}
