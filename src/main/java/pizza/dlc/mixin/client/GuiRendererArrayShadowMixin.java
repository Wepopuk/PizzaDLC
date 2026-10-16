package pizza.dlc.mixin.client;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.ArrayListShadow;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin({GuiRenderer.class})
public class GuiRendererArrayShadowMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$arrayShadow(CallbackInfo ci) {
      if (CleanRenderScope.isActive()) return;
      ArrayListShadow.render();
   }
}
