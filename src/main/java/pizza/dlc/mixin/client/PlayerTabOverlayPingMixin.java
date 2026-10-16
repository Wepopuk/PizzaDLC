package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.render.Render2DUtil;

@Mixin({PlayerTabOverlay.class})
public class PlayerTabOverlayPingMixin {
   @Unique
   private static final float PIZZADLC_PING_SIZE = 7.5F;

   @Inject(
      method = {"extractPingIcon"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$numericPing(GuiGraphicsExtractor ctx, int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
      if (pizza.dlc.api.util.render.streamproof.CleanRenderScope.isActive()) return;
      if (info != null) {
         MsdfFont font = PizzaDLCFonts.sfpro();
         int latency = info.getLatency();
         String text = latency < 0 ? "?" : Integer.toString(latency);
         float tw = Render2DUtil.msdfWidth(font, text, PIZZADLC_PING_SIZE);
         float tx = x + width - 1 - tw;
         float ty = y + 4.0F - 2.625F;
         Render2DUtil.msdfText(ctx, font, text, tx, ty, PIZZADLC_PING_SIZE, pizzadlc$pingColor(latency));
         ci.cancel();
      }
   }

   @Unique
   private static int pizzadlc$pingColor(int ms) {
      if (ms < 0) {
         return 0xFFAAAAAA;
      }

      if (ms < 80) {
         return 0xFF4CD964;
      }

      if (ms < 150) {
         return 0xFF8BD94C;
      }

      if (ms < 300) {
         return 0xFFD9C84C;
      }

      return ms < 600 ? 0xFFD9974C : 0xFFD9534C;
   }
}
