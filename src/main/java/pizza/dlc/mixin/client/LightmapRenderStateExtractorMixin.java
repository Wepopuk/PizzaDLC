package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.client.modules.impl.visuals.Ambience;

@Mixin({LightmapRenderStateExtractor.class})
public class LightmapRenderStateExtractorMixin {
   @Redirect(
      method = {"extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V"},
      at = @At(
         value = "INVOKE",
         target = "Ljava/lang/Double;floatValue()F",
         ordinal = 0
      )
   )
   private float pizzadlc$brightness(Double gammaOption) {
      float g = gammaOption.floatValue();
      return Ambience.brightOn() ? Math.max(g, Ambience.gammaBoost()) : g;
   }
}
