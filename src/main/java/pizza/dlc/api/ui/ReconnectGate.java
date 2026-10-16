package pizza.dlc.api.ui;

public final class ReconnectGate {
   private static volatile boolean armed = false;

   private ReconnectGate() {
   }

   public static void arm() {
      armed = true;
   }

   public static boolean consume() {
      boolean a = armed;
      armed = false;
      return a;
   }
}
