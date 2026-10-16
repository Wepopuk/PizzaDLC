package pizza.dlc.api.util.render;

/** Marks only one player's submissions; deferred held items carry their own flag. */
public final class AkrienOldScope implements AutoCloseable {
   private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
   private final boolean previous;

   public AkrienOldScope(boolean enabled) {
      previous = ACTIVE.get();
      ACTIVE.set(enabled);
   }

   public static boolean active() { return ACTIVE.get(); }
   @Override public void close() { ACTIVE.set(previous); }
}
