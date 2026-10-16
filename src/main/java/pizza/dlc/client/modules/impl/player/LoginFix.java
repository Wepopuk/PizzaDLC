package pizza.dlc.client.modules.impl.player;

import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "LoginFix",
   description = "Терпит login с playerId=0 (Via/auth-прокси сервера)",
   category = ModuleCategory.PLAYER
)
public class LoginFix extends Module {
   public static boolean active() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         LoginFix m = cc.getModuleManager().get(LoginFix.class);
         return m != null && m.isEnabled();
      }

      return false;
   }
}
