package pizza.dlc.client.modules.impl.combat;

import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "HitBox",
   description = "Расширяет хитбокс целей для попаданий",
   category = ModuleCategory.COMBAT
)
public class HitBox extends Module {
   public final SliderSetting size = new SliderSetting("Размер", this, 0.3F, 0.0F, 1.0F, 0.05F);

   public static float extraMargin() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         HitBox m = cc.getModuleManager().get(HitBox.class);
         return m != null && m.isEnabled() ? m.size.getValue() : 0.0F;
      }

      return 0.0F;
   }
}
