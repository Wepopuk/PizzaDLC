package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.MenuBackdropScreen;
import pizza.dlc.api.util.profiler.PizzaDLCProfiler;
import pizza.dlc.api.util.render.KawaseBlur;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin({GuiRenderer.class})
public class GuiRendererBlurMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$prepareBlur(CallbackInfo ci) {
      if (CleanRenderScope.isActive()) return;
      Minecraft mc = Minecraft.getInstance();
      boolean glassOnScreen = KawaseBlur.pollAndRoll();
      long prof = PizzaDLCProfiler.begin();

      try {
         if (mc.level != null) {
            if (glassOnScreen) {
               KawaseBlur.prepare();
            }
         } else if (!(mc.gui.overlay() instanceof LoadingOverlay) && (mc.gui.screen() instanceof TitleScreen || mc.gui.screen() instanceof MenuBackdropScreen)) {
            KawaseBlur.renderMenuBackground();
            KawaseBlur.prepare();
         }
      } finally {
         PizzaDLCProfiler.end("Blur (стекло)", prof);
      }
   }
}
