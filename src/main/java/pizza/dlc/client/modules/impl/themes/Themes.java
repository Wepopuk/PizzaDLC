package pizza.dlc.client.modules.impl.themes;

import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Themes",
   category = ModuleCategory.THEMES
)
public class Themes extends Module {
   public final ModeSetting theme = new ModeSetting("Тема", this, ThemeManager.names());

   public Themes() {
      this.setEnabled(true);
   }
}
