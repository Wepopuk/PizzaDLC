package pizza.dlc.api.translation;

import net.minecraft.client.Minecraft;

/** Use Minecraft's own language resources for every vanilla menu, item and tooltip. */
public final class MinecraftLanguageSync {
   private MinecraftLanguageSync() {}
   public static void applySavedLanguage() {
      TranslationManager manager = TranslationManager.getInstance();
      if (manager.needsSelection()) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc == null || mc.options == null) return;
      String code = manager.language().minecraftCode();
      boolean reload = !code.equals(mc.getLanguageManager().getSelected());
      mc.getLanguageManager().setSelected(code);
      if (!code.equals(mc.options.languageCode)) {
         mc.options.languageCode = code;
         mc.options.save();
      }
      if (reload) mc.reloadResourcePacks();
   }
}
