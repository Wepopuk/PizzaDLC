package pizza.dlc.api.translation;

public final class Translations {
   private static final TranslationManager MANAGER = TranslationManager.getInstance();

   private Translations() {
   }

   public static String tr(String key) {
      return MANAGER.translate(key);
   }

   public static String tr(String key, Object... args) {
      return MANAGER.translate(key, args);
   }

   public static String getCurrentLanguage() {
      return MANAGER.getCurrentLanguage();
   }

   public static String moduleName(String source) {
      return MANAGER.text(ClientLanguage.ENGLISH, source);
   }

   public static String text(String source) {
      return MANAGER.text(source);
   }
}
