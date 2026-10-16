package pizza.dlc.api.util.render.combat;

import lombok.Generated;
import net.minecraft.resources.Identifier;

public final class CombatResources {

   public static Identifier m_9310(String var0) {
      String path = var0.startsWith("fonts/") ? var0 : "fonts/" + var0;
      return Identifier.fromNamespaceAndPath("pizzadlc", path);
   }

   @Generated
   private CombatResources() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static Identifier m_9311(String var0) {
      String path = var0.startsWith("images/") ? var0 : "images/" + var0;
      if (!path.endsWith(".png")) {
         path = path + ".png";
      }
      return Identifier.fromNamespaceAndPath("pizzadlc", path);
   }

   public static Identifier m_9312(String var0) {
      String path = var0.startsWith("core/") ? var0 : "core/" + var0;
      return Identifier.fromNamespaceAndPath("pizzadlc", path);
   }
}
