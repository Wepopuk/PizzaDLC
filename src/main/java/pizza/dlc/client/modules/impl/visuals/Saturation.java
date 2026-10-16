package pizza.dlc.client.modules.impl.visuals;

import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Saturation",
   description = "Насыщенность сцены (пост-процесс)",
   category = ModuleCategory.VISUALS
)
public class Saturation extends Module {
   public final SliderSetting saturation = new SliderSetting("Насыщенность", this, 1.0F, 0.0F, 2.0F, 0.05F);
}
