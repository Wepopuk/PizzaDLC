package pizza.dlc.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.api.ui.menu.ScreenFrame;

@Mixin(
   targets = {
      "net.minecraft.client.gui.components.OptionsList$Entry",
      "net.minecraft.client.gui.components.OptionsList$HeaderEntry",
      "net.minecraft.client.gui.screens.worldselection.WorldSelectionList$LoadingHeader"
   }
)
   public class ListEntryCenterMixin {
   @Redirect(method = "extractContent", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/Screen;width:I", opcode = 180))
   private int pizzadlc$contentCenter(Screen screen) {
      ScreenFrame.Frame f = ScreenFrame.of(screen);
      return f != null ? f.contentCenter() * 2 : screen.width;
   }
}
