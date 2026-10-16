package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.visuals.Removals;

@Mixin({MapRenderer.class})
public class MapRendererMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$removeMapMarkers(
      MapRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, boolean showOnlyFrame, int lightCoords, CallbackInfo ci
   ) {
      if (Removals.active("Метки карты")) {
         state.decorations.clear();
      }
   }
}
