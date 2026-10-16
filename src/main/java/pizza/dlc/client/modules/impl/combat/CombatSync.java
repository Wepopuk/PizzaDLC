package pizza.dlc.client.modules.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;

public final class CombatSync {
   public static final int PRIORITY_CRYSTAL = 1;
   public static final int PRIORITY_WEB = 2;
   public static final int PRIORITY_ANCHOR = 2;
   private static final long TARGET_FRESH_MS = 250L;
   private static Object owner;
   private static int ownerPriority;
   private static boolean ownerLocked;
   private static long ownerUntil;
   private static LivingEntity target;
   private static long targetMs;

   private CombatSync() {
   }

   public static boolean claim(Object who, int priority, int ticks, boolean locked) {
      if (blocked(who, priority)) return false;
      owner = who;
      ownerPriority = priority;
      ownerLocked = locked;
      ownerUntil = System.currentTimeMillis() + ticks * 50L;
      return true;
   }

   public static boolean blocked(Object who, int priority) {
      return owner != null && owner != who && System.currentTimeMillis() < ownerUntil && (ownerLocked || ownerPriority >= priority);
   }

   public static void free(Object who) {
      if (owner == who) owner = null;
   }

   public static void shareTarget(LivingEntity p) {
      target = p;
      targetMs = System.currentTimeMillis();
   }

   public static LivingEntity sharedTarget(double maxDist) {
      LocalPlayer self = Minecraft.getInstance().player;
      LivingEntity t = target;
      if (self == null || t == null || System.currentTimeMillis() - targetMs > TARGET_FRESH_MS) return null;
      if (!t.isAlive() || t.isRemoved() || t.level() != self.level()) return null;
      return self.distanceToSqr(t) <= maxDist * maxDist ? t : null;
   }

   public static boolean userBusy() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer p = mc.player;
      if (p == null || mc.gui.screen() != null) return false;
      if (mc.options.keyUse.isDown()) return true;
      return mc.options.keyAttack.isDown() && p.pick(p.blockInteractionRange(), 1.0F, false).getType() == HitResult.Type.BLOCK;
   }

   public static void reset() {
      owner = null;
      target = null;
   }
}
