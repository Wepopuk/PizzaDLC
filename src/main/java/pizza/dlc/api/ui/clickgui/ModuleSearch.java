package pizza.dlc.api.ui.clickgui;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.translation.ClientLanguage;
import pizza.dlc.api.translation.TranslationManager;
import pizza.dlc.client.modules.Module;

/** Search original and translated labels, including unselected mode choices. */
public final class ModuleSearch {
   private static final Map<Module, Entry> INDEXES = new WeakHashMap<>();
   private static final Pattern MARKS = Pattern.compile("\\p{M}+");
   private record Entry(int settingCount, String text) {}
   private ModuleSearch() {}
   public static boolean matches(Module module, String query, TranslationManager translations) {
      if (normalize(query).isEmpty()) return true;
      Entry entry = INDEXES.get(module);
      if (entry == null || entry.settingCount() != module.getSettings().size()) {
         entry = new Entry(module.getSettings().size(), createIndex(module, translations));
         INDEXES.put(module, entry);
      }
      for (String word : normalize(query).split("\\s+")) if (!entry.text().contains(word)) return false;
      return true;
   }
   private static String createIndex(Module module, TranslationManager translations) {
      StringBuilder index = new StringBuilder();
      append(index, module.getName(), translations);
      append(index, module.getDescription(), translations);
      for (Setting setting : module.getSettings()) {
         append(index, setting.getName(), translations);
         append(index, setting.getDescription(), translations);
         append(index, setting.getGroup(), translations);
         if (setting instanceof ModeSetting mode) {
            for (String value : mode.getModes()) append(index, value, translations);
         } else if (setting instanceof MultiModeSetting multi) {
            for (var value : multi.getBoolSettings()) {
               append(index, value.getName(), translations);
               append(index, value.getDescription(), translations);
            }
         }
      }
      return normalize(index.toString());
   }
   private static void append(StringBuilder index, String value, TranslationManager translations) {
      if (value == null || value.isBlank()) return;
      index.append(' ').append(value);
      for (ClientLanguage language : ClientLanguage.values()) index.append(' ').append(translations.text(language, value));
   }
   public static boolean matchesIndex(String index, String query) {
      String normalized = normalize(index);
      for (String word : normalize(query).split("\\s+")) if (!normalized.contains(word)) return false;
      return true;
   }
   private static String normalize(String value) {
      if (value == null) return "";
      return MARKS.matcher(Normalizer.normalize(value, Normalizer.Form.NFKD)).replaceAll("")
         .toLowerCase(Locale.ROOT).replace('ł', 'l').trim();
   }
}
