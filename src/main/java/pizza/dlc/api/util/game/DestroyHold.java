package pizza.dlc.api.util.game;

import java.util.IdentityHashMap;
import java.util.Map;

public final class DestroyHold {
   private static final long HOLD_MS = 150L;
   private static final Map<Object, Long> HOLDS = new IdentityHashMap<>();

   private DestroyHold() {
   }

   public static void hold(Object owner) {
      HOLDS.put(owner, System.currentTimeMillis() + HOLD_MS);
   }

   public static void release(Object owner) {
      HOLDS.remove(owner);
   }

   public static boolean active() {
      if (HOLDS.isEmpty()) return false;

      long now = System.currentTimeMillis();
      HOLDS.values().removeIf(until -> until <= now);
      return !HOLDS.isEmpty();
   }
}
