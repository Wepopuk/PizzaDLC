package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.SaturationRenderer;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.Saturation;

@Mixin({GuiRenderer.class})
public class GuiRendererSaturationMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$saturation(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         if (cc != null && cc.getModuleManager() != null) {
            Saturation mod = cc.getModuleManager().get(Saturation.class);
            if (mod != null && mod.isEnabled()) {
               float v = mod.saturation.getValue();
               if (!(Math.abs(v - 1.0F) < 0.001F)) {
                  SaturationRenderer.apply(v);
               }
            }
         }
      }
   }
}
