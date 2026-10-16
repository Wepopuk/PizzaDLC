package pizza.dlc.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.visuals.Removals;

@Mixin({WeatherEffectRenderer.class})
public class WeatherEffectRendererMixin {
   @Inject(
      method = {"extractRenderState"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeWeatherState(ClientLevel level, float partialTicks, Vec3 cameraPos, WeatherRenderState renderState, CallbackInfo ci) {
      if (Removals.active("Погода")) {
         renderState.reset();
         ci.cancel();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeWeatherRender(Vec3 cameraPos, WeatherRenderState renderState, CallbackInfo ci) {
      if (Removals.active("Погода")) {
         ci.cancel();
      }
   }
}
