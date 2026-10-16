package pizza.dlc.api.util.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.theme.ThemeManager;

public final class ChatUtility implements QuickImports {
   private ChatUtility() {
   }

   public static void send(String message) {
      send(message, false);
   }

   public static void sendError(String message) {
      send(message, true);
   }

   private static void send(String message, boolean error) {
      if (mc.player == null) return;

      mc.player.sendSystemMessage(format(message, error));
   }

   private static Component format(String message, boolean error) {
      MutableComponent prefix = gradient("PizzaDLC").append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY));
      return prefix.append(Component.literal(message).withStyle(error ? ChatFormatting.RED : ChatFormatting.WHITE));
   }

   private static MutableComponent gradient(String text) {
      int from = ThemeManager.accent();
      int to = ThemeManager.secondary();
      MutableComponent out = Component.empty();
      int n = text.length();

      for (int i = 0; i < n; i++) {
         float t = n <= 1 ? 0.0F : (float)i / (n - 1);
         int c = lerpRgb(from, to, t);
         out.append(Component.literal(String.valueOf(text.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(c))));
      }

      return out;
   }

   private static int lerpRgb(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      return Math.round(ar + (br - ar) * t) << 16 | Math.round(ag + (bg - ag) * t) << 8 | Math.round(ab + (bb - ab) * t);
   }
}
