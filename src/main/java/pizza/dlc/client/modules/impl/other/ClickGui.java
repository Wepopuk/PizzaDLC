package pizza.dlc.client.modules.impl.other;

import net.minecraft.client.Minecraft;
import pizza.dlc.api.ui.clickgui.ClickGuiScreen;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ClickGUI",
   category = ModuleCategory.OTHER,
   key = 260,
   hidden = true
)
public class ClickGui extends Module {
   @Override
   public void onEnable() {
      super.onEnable();
      if (mc.level != null) {
         Minecraft.getInstance().setScreenAndShow(new ClickGuiScreen());
      } else {
         setEnabled(false);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (mc.gui.screen() instanceof ClickGuiScreen screen) {
         if (screen.isClosing()) {
            mc.setScreenAndShow(null);
         } else {
            screen.onClose();
         }
      }
   }
}
