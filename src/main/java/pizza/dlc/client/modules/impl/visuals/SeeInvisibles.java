package pizza.dlc.client.modules.impl.visuals;

import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "SeeInvisibles",
   category = ModuleCategory.VISUALS
)
public class SeeInvisibles extends Module {
   public final SliderSetting alpha = new SliderSetting("Прозрачность", this, 0.5F, 0.3F, 1.0F, 0.1F);
}
