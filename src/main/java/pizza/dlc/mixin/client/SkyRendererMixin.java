package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.CustomSkyRenderer;

@Mixin({SkyRenderer.class})
public class SkyRendererMixin {
   @Inject(
      method = {"renderSkyDisc"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$customSky(int skyColor, CallbackInfo ci) {
      if (CustomSkyRenderer.shouldReplace() && CustomSkyRenderer.renderSky()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSunMoonAndStars"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$noSunStars(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
      if (CustomSkyRenderer.shouldReplace()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderSunriseAndSunset"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$noSunrise(PoseStack poseStack, float sunAngle, int sunriseAndSunsetColor, CallbackInfo ci) {
      if (CustomSkyRenderer.shouldReplace()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderDarkDisc"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$noDarkDisc(CallbackInfo ci) {
      if (CustomSkyRenderer.shouldReplace()) {
         ci.cancel();
      }
   }
}
