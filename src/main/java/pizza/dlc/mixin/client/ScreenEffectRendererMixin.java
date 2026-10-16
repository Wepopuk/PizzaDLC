package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.visuals.Removals;

@Mixin({ScreenEffectRenderer.class})
public class ScreenEffectRendererMixin {
   @Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$hideLocalTotemActivation(PoseStack pose, float partialTick, SubmitNodeCollector collector, CallbackInfo ci) {
      if (Removals.active("Анимация тотема")) ci.cancel();
   }

   @Inject(
      method = {"submitFire"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void pizzadlc$removeFireOverlay(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite, CallbackInfo ci) {
      if (Removals.active("Огонь")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"submitBlockSprite"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void pizzadlc$removeBlockOverlay(
      TextureAtlasSprite sprite, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, CallbackInfo ci
   ) {
      if (Removals.active("Камера")) {
         ci.cancel();
      }
   }
}
