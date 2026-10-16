package pizza.dlc.api.util.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import pizza.dlc.client.modules.impl.visuals.Removals;

public final class BoatLimiter {
   public static final int MAX_BOATS = 10;

   private static Set<Integer> allowedIds = new HashSet<>();
   private static long lastUpdateNanos = 0;

   private BoatLimiter() {}

   public static boolean isBoat(Entity e) {
      return e instanceof AbstractBoat;
   }

   public static boolean shouldCull(Entity entity) {
      if (!Removals.active("Лодки")) return false;
      if (!(entity instanceof AbstractBoat)) return false;
      ensureUpdated();
      return !allowedIds.contains(entity.getId());
   }

   public static boolean shouldBlockSpawn(Entity entity) {
      if (!Removals.active("Лодки")) return false;
      if (!(entity instanceof AbstractBoat)) return false;
      ensureUpdated();
      int count = getBoatCount();
      if (count >= MAX_BOATS) {
         return !allowedIds.contains(entity.getId());
      }
      return false;
   }

   public static int getBoatCount() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return 0;
      int c = 0;
      for (Entity e : mc.level.entitiesForRendering()) {
         if (e instanceof AbstractBoat) c++;
      }
      return c;
   }

   private static void ensureUpdated() {
      long now = System.nanoTime();
      if (now - lastUpdateNanos < 200_000_000L) return;
      lastUpdateNanos = now;
      recompute();
   }

   private static void recompute() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.player == null) {
         allowedIds = new HashSet<>();
         return;
      }
      if (!Removals.active("Лодки")) {
         allowedIds = new HashSet<>();
         return;
      }
      List<AbstractBoat> boats = new ArrayList<>();
      for (Entity e : mc.level.entitiesForRendering()) {
         if (e instanceof AbstractBoat b) boats.add(b);
      }
      if (boats.size() <= MAX_BOATS) {
         Set<Integer> s = new HashSet<>();
         for (AbstractBoat b : boats) s.add(b.getId());
         allowedIds = s;
         return;
      }
      double px = mc.player.getX();
      double py = mc.player.getY();
      double pz = mc.player.getZ();
      boats.sort(Comparator.comparingDouble(b -> {
         double dx = b.getX() - px;
         double dy = b.getY() - py;
         double dz = b.getZ() - pz;
         return dx * dx + dy * dy + dz * dz;
      }));
      Set<Integer> s = new HashSet<>();
      for (int i = 0; i < MAX_BOATS && i < boats.size(); i++) {
         s.add(boats.get(i).getId());
      }
      allowedIds = s;
   }

   public static void invalidate() {
      lastUpdateNanos = 0;
   }
}
