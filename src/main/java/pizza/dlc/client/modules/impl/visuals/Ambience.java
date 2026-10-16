package pizza.dlc.client.modules.impl.visuals;

import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Ambience",
   description = "Визуалы мира: яркость, время суток, туман",
   category = ModuleCategory.VISUALS
)
public class Ambience extends Module {
   private static final String BRIGHT = "Яркость";
   private static final String TIME = "Время";
   private static final String FOG = "Туман";
   public final MultiModeSetting modes = new MultiModeSetting("Режимы", this, BRIGHT, TIME, FOG);
   public final SliderSetting bright = new SliderSetting(BRIGHT, this, 100.0F, 1.0F, 100.0F, 1.0F).setVisible(() -> this.modes.get(BRIGHT).isEnabled());
   public final SliderSetting time = new SliderSetting(TIME, this, 12000.0F, 0.0F, 24000.0F, 1.0F).setVisible(() -> this.modes.get(TIME).isEnabled());
   public final SliderSetting fogDst = new SliderSetting("Дистанция тумана", this, 128.0F, 3.0F, 256.0F, 1.0F)
      .setVisible(() -> this.modes.get(FOG).isEnabled());

   private static Ambience active() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         Ambience a = cc.getModuleManager().get(Ambience.class);
         return a != null && a.isEnabled() ? a : null;
      }

      return null;
   }

   public static boolean brightOn() {
      Ambience a = active();
      return a != null && a.modes.get(BRIGHT).isEnabled();
   }

   public static float gammaBoost() {
      Ambience a = active();
      return a == null ? 0.0F : a.bright.getValue() / 10.0F;
   }

   public static boolean timeOn() {
      Ambience a = active();
      return a != null && a.modes.get(TIME).isEnabled();
   }

   public static long timeValue() {
      Ambience a = active();
      return a == null ? 0L : (long)a.time.getValue();
   }

   public static boolean fogOn() {
      Ambience a = active();
      return a != null && a.modes.get(FOG).isEnabled();
   }

   public static int fogColor() {
      return ThemeManager.accent();
   }

   public static float fogDistance() {
      Ambience a = active();
      return a == null ? 128.0F : a.fogDst.getValue();
   }
}
