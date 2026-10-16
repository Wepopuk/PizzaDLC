package pizza.dlc.api.util.render;

public final class EspIconScope {
   private static boolean active;

   private EspIconScope() {
   }

   public static void begin() {
      active = true;
   }

   public static void end() {
      active = false;
   }

   public static boolean isActive() {
      return active;
   }
}
