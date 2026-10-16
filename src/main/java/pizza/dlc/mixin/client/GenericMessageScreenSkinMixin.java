package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin(GenericMessageScreen.class)
public abstract class GenericMessageScreenSkinMixin extends Screen {
   protected GenericMessageScreenSkinMixin(Component title) {
      super(title);
   }

   @Redirect(
      method = "extractBackground",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/GenericMessageScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V")
   )
   private void pizzadlc$skinBackground(GenericMessageScreen screen, GuiGraphicsExtractor graphics, float partialTick) {
      if (CleanRenderScope.isActive()) this.extractPanorama(graphics, partialTick);
      else MenuKit.background(graphics, this.width, this.height);
   }
}
