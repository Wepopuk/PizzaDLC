package pizza.dlc.api.util;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class LegacyText {
   private LegacyText() {
   }

   public static String toLegacy(Component component) {
      StringBuilder out = new StringBuilder();
      component.visit((style, text) -> {
         out.append(codes(style)).append(text);
         return Optional.empty();
      }, Style.EMPTY);
      return out.toString();
   }

   private static String codes(Style style) {
      StringBuilder b = new StringBuilder();
      TextColor color = style.getColor();
      if (color != null) {
         String named = legacyColor(color);
         b.append(named != null ? named : hexColor(color));
      }

      if (style.isBold()) {
         b.append(ChatFormatting.BOLD);
      }

      if (style.isItalic()) {
         b.append(ChatFormatting.ITALIC);
      }

      if (style.isUnderlined()) {
         b.append(ChatFormatting.UNDERLINE);
      }

      if (style.isStrikethrough()) {
         b.append(ChatFormatting.STRIKETHROUGH);
      }

      if (style.isObfuscated()) {
         b.append(ChatFormatting.OBFUSCATED);
      }

      return b.toString();
   }

   private static String legacyColor(TextColor color) {
      for (ChatFormatting f : ChatFormatting.values()) {
         TextColor named = TextColor.fromLegacyFormat(f);
         if (named != null && named.getValue() == color.getValue()) {
            return f.toString();
         }
      }

      return null;
   }

   private static String hexColor(TextColor color) {
      String hex = String.format("%06x", color.getValue() & 0xFFFFFF);
      StringBuilder b = new StringBuilder("§x");

      for (int i = 0; i < 6; i++) {
         b.append('§').append(hex.charAt(i));
      }

      return b.toString();
   }
}
