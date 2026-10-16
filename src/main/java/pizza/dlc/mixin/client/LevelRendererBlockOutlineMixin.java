package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.Highlight;

@Mixin({LevelRenderer.class})
public class LevelRendererBlockOutlineMixin {
   @Inject(
      method = {"submitBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$suppressVanillaOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
      if (PizzaDLCClient.getInstance() != null) {
         Highlight h = PizzaDLCClient.getInstance().getModuleManager().get(Highlight.class);
         if (h != null && h.isEnabled()) {
            ci.cancel();
         }
      }
   }
}
