package pizza.dlc.client.modules.impl.misc;

import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Tweaks",
   description = "Улучшения ванильных интерфейсов",
   category = ModuleCategory.OTHER
)
public class Tweaks extends Module {
   public final BooleanSetting copyChat = new BooleanSetting("Копирование сообщений", this);

   public Tweaks() {
      this.copyChat.setEnabled(true);
   }

   private static Tweaks get() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      return cc != null && cc.getModuleManager() != null ? cc.getModuleManager().get(Tweaks.class) : null;
   }

   public static boolean copyChatEnabled() {
      Tweaks m = get();
      return m != null && m.isEnabled() && m.copyChat.isEnabled();
   }

   public static boolean hideScoreboardNumbers() {
      Tweaks m = get();
      return m != null && m.isEnabled();
   }
}
