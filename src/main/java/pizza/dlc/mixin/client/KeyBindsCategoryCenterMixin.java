package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pizza.dlc.api.ui.menu.ScreenFrame;

@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$CategoryEntry")
public class KeyBindsCategoryCenterMixin {
   @ModifyArg(
      method = "extractContent",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/FocusableTextWidget;setPosition(II)V"),
      index = 0
   )
   private int pizzadlc$categoryX(int x) {
      ScreenFrame.Frame f = ScreenFrame.of(Minecraft.getInstance().gui.screen());
      return f != null ? x + f.contentX() : x;
   }
}
