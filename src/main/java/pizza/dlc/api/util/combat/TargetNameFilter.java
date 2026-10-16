package pizza.dlc.api.util.combat;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

public final class TargetNameFilter {

   private static volatile String f_16000;

   public static boolean m_5564(String var0) {
      return m_5567() && var0 != null && var0.equalsIgnoreCase(f_16000);
   }

   private TargetNameFilter() {
   }

   public static boolean m_5565(Player var0) {
      return var0 != null && m_5564(var0.getGameProfile().name());
   }

   public static String m_5566() {
      return f_16000;
   }

   public static boolean m_5567() {
      return f_16000 != null && !f_16000.isBlank();
   }

   public static void m_5568(String var0) {
      f_16000 = var0 != null && !var0.isBlank() ? var0 : null;
   }

   public static Component m_5569(Component var0) {
      if (var0 == null) {
         return null;
      }

      MutableComponent var1 = Component.empty();
      var0.visit((var1x, var2) -> {
         if (!var2.isEmpty()) {
            var1.append(Component.literal(var2).setStyle(var1x.withColor(16733525)));
         }

         return Optional.empty();
      }, Style.EMPTY);
      return var1;
   }

   public static void m_5571() {
      f_16000 = null;
   }
}
