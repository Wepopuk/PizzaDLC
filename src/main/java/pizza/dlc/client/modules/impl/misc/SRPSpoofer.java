package pizza.dlc.client.modules.impl.misc;

import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "SRPSpoofer",
   description = "Заход без серверного ресурспака (спуф статуса установки)",
   category = ModuleCategory.OTHER
)
public class SRPSpoofer extends Module {
   public static SRPSpoofer INSTANCE;

   public SRPSpoofer() {
      INSTANCE = this;
   }
}
