package pizza.dlc.api.util.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.PreparedText;
import net.minecraft.network.chat.Component;

public final class NameTagCache {
   private static final int MAX = 1024;
   private static final Map<Key, PreparedText> CACHE = new ConcurrentHashMap<>();
   private static volatile Object lastLevel;

   private NameTagCache() {
   }

   public static PreparedText get(Font font, Component text, float x, float y, int color, int bg) {
      Object level = Minecraft.getInstance().level;
      if (level != lastLevel) {
         CACHE.clear();
         lastLevel = level;
      }

      if (CACHE.size() > MAX) {
         CACHE.clear();
      }

      return CACHE.computeIfAbsent(new Key(text, color, bg), k -> font.prepareText(text.getVisualOrderText(), x, y, color, false, false, bg));
   }

   public static void clear() {
      CACHE.clear();
   }

   private record Key(Component text, int color, int bg) {
   }
}
