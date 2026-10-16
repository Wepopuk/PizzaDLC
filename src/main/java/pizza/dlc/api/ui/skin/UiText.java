package pizza.dlc.api.ui.skin;

/** Normalizes typographic spacing and wrapping markers before measuring and drawing menu text. */
public final class UiText {
   private UiText() { }

   public static String display(String text) {
      return text.replace('\u00A0', ' ').replace('\u202F', ' ').replace('\u2010', '-').replace('\u2011', '-')
         .replace("\u00AD", "").replace("\u200B", "").replace("\u2060", "");
   }
}
