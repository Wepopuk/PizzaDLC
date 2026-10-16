package pizza.dlc.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.PizzaDLCClient;

@Mixin(value = Hud.class, remap = false)
public class GpsActionBarMixin {
   @Shadow private int overlayMessageTime;

   @Inject(method = "extractOverlayMessage", at = @At("RETURN"), remap = false)
   private void pizzadlc$gps(GuiGraphicsExtractor ctx, DeltaTracker delta, CallbackInfo ci) {
      var client = PizzaDLCClient.getInstance();
      if (client != null && client.getGpsNavigation() != null) {
         client.getGpsNavigation().renderActionBar(ctx, overlayMessageTime > 0);
      }
   }
}
