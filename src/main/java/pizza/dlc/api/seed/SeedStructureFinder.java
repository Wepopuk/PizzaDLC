package pizza.dlc.api.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class SeedStructureFinder {
   public static final List<Placement> ALL = List.of(
      new Placement("Деревня", 10387312, 34, 8, false, -8090),
      new Placement("Аванпост", 165745296, 32, 8, false, -30147),
      new Placement("Пустынный храм", 14357617, 32, 8, false, -1451866),
      new Placement("Храм в джунглях", 14357619, 32, 8, false, -9124746),
      new Placement("Иглу", 14357618, 32, 8, false, -4987396),
      new Placement("Ведьмина хижина", 14357620, 32, 8, false, -7442512),
      new Placement("Монумент", 10387313, 32, 5, true, -12532481),
      new Placement("Особняк", 10387319, 80, 20, true, -4776932),
      new Placement("Океан. руины", 14357621, 20, 8, false, -11677471),
      new Placement("Кораблекрушение", 165745295, 24, 4, false, -7508381),
      new Placement("Разрушенный портал", 34222645, 40, 15, false, -3109889),
      new Placement("Крепость/Бастион", 30084232, 27, 4, false, -44462),
      new Placement("Энд-сити", 10387313, 20, 11, true, -3238952),
      new Placement("Древний город", 20083232, 24, 8, false, -7297874),
      new Placement("Испыт. камера", 94251327, 34, 12, false, -14235994),
      new Placement("Тропа-руины", 83469867, 34, 8, false, -6190977)
   );

   private SeedStructureFinder() {
   }

   public static Placement byName(String name) {
      for (Placement p : ALL) {
         if (!p.name().equals(name)) continue;

         return p;
      }

      return null;
   }

   public static int[] candidateChunk(long seed, int regionX, int regionZ, Placement p) {
      long mixed = regionX * 341873128712L + regionZ * 132897987541L + seed + p.salt();
      Random r = new Random(mixed);
      int bound = p.spacing() - p.separation();
      int offX;
      int offZ;
      if (p.triangular()) {
         offX = (r.nextInt(bound) + r.nextInt(bound)) / 2;
         offZ = (r.nextInt(bound) + r.nextInt(bound)) / 2;
      } else {
         offX = r.nextInt(bound);
         offZ = r.nextInt(bound);
      }

      return new int[]{regionX * p.spacing() + offX, regionZ * p.spacing() + offZ};
   }

   public static List<Found> find(
      long seed, int centerChunkX, int centerChunkZ, int radiusChunks, Predicate<String> enabled, int maxResults
   ) {
      List<Found> out = new ArrayList<>();
      long rSq = (long)radiusChunks * radiusChunks;

      for (Placement p : ALL) {
         if (!enabled.test(p.name())) continue;

         int minRegX = Math.floorDiv(centerChunkX - radiusChunks, p.spacing());
         int maxRegX = Math.floorDiv(centerChunkX + radiusChunks, p.spacing());
         int minRegZ = Math.floorDiv(centerChunkZ - radiusChunks, p.spacing());
         int maxRegZ = Math.floorDiv(centerChunkZ + radiusChunks, p.spacing());

         for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {
               int[] c = candidateChunk(seed, rx, rz, p);
               long dx = c[0] - centerChunkX;
               long dz = c[1] - centerChunkZ;
               if (dx * dx + dz * dz <= rSq) {
                  out.add(new Found(p.name(), c[0] * 16 + 8, c[1] * 16 + 8, p.color()));
                  if (out.size() >= maxResults) {
                     return out;
                  }
               }
            }
         }
      }

      return out;
   }

   public record Found(String name, int blockX, int blockZ, int color) {
   }

   public record Placement(String name, int salt, int spacing, int separation, boolean triangular, int color) {
   }
}
