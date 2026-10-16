package pizza.dlc.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin({Hud.class})
public class VanillaHudMixin {
   @Unique
   private static final float PIZZADLC_CHAT_SHIFT = 16.0F;
   @Unique
   private float pizzadlc$hudShift;

   @Inject(
      method = {"extractEffects"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$hideVanillaEffectsWhenPotionsHudEnabled(GuiGraphicsExtractor ctx, DeltaTracker deltaTracker, CallbackInfo ci) {
      PizzaDLCClient client = PizzaDLCClient.getInstance();
      if (client != null && client.getModuleManager() != null) {
         pizza.dlc.client.modules.impl.visuals.Hud hud = client.getModuleManager().get(pizza.dlc.client.modules.impl.visuals.Hud.class);
         if (hud != null && hud.isPotionsHudActive()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"extractHotbarAndDecorations"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$raiseHudUnderChat(GuiGraphicsExtractor ctx, DeltaTracker delta, CallbackInfo ci) {
      this.pizzadlc$hudShift = !CleanRenderScope.isActive() && Minecraft.getInstance().gui.screen() instanceof ChatScreen ? PIZZADLC_CHAT_SHIFT : 0.0F;
      if (this.pizzadlc$hudShift != 0.0F) {
         ctx.pose().pushMatrix();
         ctx.pose().translate(0.0F, -this.pizzadlc$hudShift);
      }
   }

   @Inject(
      method = {"extractHotbarAndDecorations"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$restoreHudUnderChat(GuiGraphicsExtractor ctx, DeltaTracker delta, CallbackInfo ci) {
      if (this.pizzadlc$hudShift != 0.0F) {
         ctx.pose().popMatrix();
         this.pizzadlc$hudShift = 0.0F;
      }
   }
}
