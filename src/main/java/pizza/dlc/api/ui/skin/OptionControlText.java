package pizza.dlc.api.ui.skin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Uses the actual translated option message, including units and named endpoint values. */
public record OptionControlText(String label, String value) {
   /** Vanilla translations use non-breaking spaces/hyphens and invisible wrapping markers. */
   public static String display(String text) {
      return UiText.display(text);
   }
   public static OptionControlText from(Component message) {
      String full = message.getString();
      if (message.getContents() instanceof TranslatableContents translation) {
         Object[] args = translation.getArgs();
         if (args.length >= 2 && args[0] instanceof Component caption) {
            String label = caption.getString();
            if (full.startsWith(label)) {
               String value = full.substring(label.length()).stripLeading().replaceFirst("^[:：]\\s*", "");
               if (!value.isEmpty()) return new OptionControlText(label, value);
            }
         }
      }
      int colon = full.indexOf(": ");
      if (colon < 0) colon = full.indexOf("：");
      if (colon > 0) return new OptionControlText(full.substring(0, colon), full.substring(colon + 1).strip());
      return new OptionControlText(full, "");
   }
}
