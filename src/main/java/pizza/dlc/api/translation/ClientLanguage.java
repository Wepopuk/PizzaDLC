package pizza.dlc.api.translation;

import java.util.Locale;

public enum ClientLanguage {
   ENGLISH("en", "English", "English", "gb"),
   RUSSIAN("ru", "Русский", "Russian", "ru"),
   UKRAINIAN("uk", "Українська", "Ukrainian", "ua"),
   POLISH("pl", "Polski", "Polish", "pl"),
   GERMAN("de", "Deutsch", "German", "de");

   private final String code, nativeName, englishName, flag;

   ClientLanguage(String code, String nativeName, String englishName, String flag) {
      this.code = code; this.nativeName = nativeName; this.englishName = englishName; this.flag = flag;
   }

   public String code() { return code; }
   public String nativeName() { return nativeName; }
   public String englishName() { return englishName; }
   public String flag() { return flag; }
   public String minecraftCode() {
      return switch (this) {
         case ENGLISH -> "en_us";
         case RUSSIAN -> "ru_ru";
         case UKRAINIAN -> "uk_ua";
         case POLISH -> "pl_pl";
         case GERMAN -> "de_de";
      };
   }

   public static ClientLanguage byCode(String code) {
      if (code == null) return null;
      String normalized = code.toLowerCase(Locale.ROOT);
      if (normalized.equals("ua")) normalized = "uk";
      for (ClientLanguage language : values()) if (language.code.equals(normalized)) return language;
      return null;
   }
}
