package pizza.dlc.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.ui.menu.StartupLogo;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin(LoadingOverlay.class)
public class LoadingOverlaySkinMixin {
   @Unique
   private static final int BACKGROUND = 0xFF0A0A0B;
   @Shadow
   @Final
   private boolean fadeIn;
   @Shadow
   private float currentProgress;
   @Unique
   private boolean pizzadlc$startupLogoDrawn;

   @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
   private int pizzadlc$backgroundColor(IntSupplier brand) {
      return !CleanRenderScope.isActive() ? BACKGROUND : brand.getAsInt();
   }

   @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
   private void pizzadlc$background(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
      if (CleanRenderScope.isActive()) {
         graphics.fill(x0, y0, x1, y1, color);
      } else if (this.fadeIn) {
         MenuKit.background(graphics, x1 - x0, y1 - y0, (color >>> 24 & 0xFF) / 255.0F);
      } else {
         StartupLogo.background(graphics, x0, y0, x1, y1, color >>> 24 & 0xFF);
      }
   }

   @Redirect(
      method = "extractRenderState",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
         ordinal = 0
      )
   )
   private void pizzadlc$logo(
      GuiGraphicsExtractor graphics,
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
      int color
   ) {
      this.pizzadlc$startupLogoDrawn = false;
      if (this.fadeIn && !CleanRenderScope.isActive()) {
         MenuKit.loadingLogo(graphics, x + width, y + height / 2.0F, height, (color >>> 24 & 0xFF) / 255.0F);
      } else if (!CleanRenderScope.isActive() && StartupLogo.logo(graphics, pipeline, x + width, y + height / 2.0F, height, color)) {
         this.pizzadlc$startupLogoDrawn = true;
      } else {
         graphics.blit(pipeline, texture, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight, color);
      }
   }

   @Redirect(
      method = "extractRenderState",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V",
         ordinal = 1
      )
   )
   private void pizzadlc$logoRightHalf(
      GuiGraphicsExtractor graphics,
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
      int color
   ) {
      if ((!this.fadeIn || CleanRenderScope.isActive()) && !this.pizzadlc$startupLogoDrawn) {
         graphics.blit(pipeline, texture, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight, color);
      }
   }

   @Inject(method = "extractProgressBar", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$progressBar(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, float opacity, CallbackInfo ci) {
      if (CleanRenderScope.isActive()) return;
      if (this.fadeIn) {
         MenuKit.loadingBar(graphics, x0, y0, x1, y1, this.currentProgress, opacity);
      } else {
         StartupLogo.progressBar(graphics, x0, y0, x1, y1, this.currentProgress, opacity);
      }
      ci.cancel();
   }
}
