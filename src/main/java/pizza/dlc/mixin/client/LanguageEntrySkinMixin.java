package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.skin.UiSkin;

@Mixin(targets = "net.minecraft.client.gui.screens.options.LanguageSelectScreen$LanguageSelectionList$Entry")
public class LanguageEntrySkinMixin {
   @Redirect(
      method = "extractContent",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
      )
   )
   private void pizzadlc$languageText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
      ScreenFrame.Frame f = ScreenFrame.of(Minecraft.getInstance().gui.screen());
      int cx = f != null ? x + f.contentX() : x;
      if (!UiSkin.drawCentered(graphics, text, cx, y + 4.5F, color)) {
         graphics.centeredText(font, text, cx, y, color);
      }
   }
}
