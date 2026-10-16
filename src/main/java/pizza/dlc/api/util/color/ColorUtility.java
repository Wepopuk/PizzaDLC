package pizza.dlc.api.util.color;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.Mth;
import pizza.dlc.api.interfaces.QuickImports;

public final class ColorUtility implements QuickImports {
   private static final ConcurrentHashMap<ColorKey, CacheEntry> colorCache = new ConcurrentHashMap<>();
   private static final ScheduledExecutorService cacheCleaner = Executors.newScheduledThreadPool(1,
      Thread.ofPlatform().name("PizzaDLC color cache").daemon(true).factory());
   private static final DelayQueue<CacheEntry> cleanupQueue = new DelayQueue<>();

   public static ColorRGBA lerp(int speed, int index, ColorRGBA start, ColorRGBA end) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      float progress = (float)(Math.sin(Math.toRadians(angle)) * 0.5 + 0.5);
      return start.mix(end, progress);
   }

   public static int red(int c) {
      return c >> 16 & 0xFF;
   }

   public static int green(int c) {
      return c >> 8 & 0xFF;
   }

   public static int blue(int c) {
      return c & 0xFF;
   }

   public static int alpha(int c) {
      return c >> 24 & 0xFF;
   }

   public static float redf(int c) {
      return red(c) / 255.0F;
   }

   public static float greenf(int c) {
      return green(c) / 255.0F;
   }

   public static float bluef(int c) {
      return blue(c) / 255.0F;
   }

   public static float alphaf(int c) {
      return alpha(c) / 255.0F;
   }

   public static int multAlpha(int color, float percent01) {
      return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
   }

   public static int getColor(int red, int green, int blue, int alpha) {
      ColorKey key = new ColorKey(red, green, blue, alpha);
      CacheEntry cacheEntry = colorCache.computeIfAbsent(key, k -> {
         CacheEntry newEntry = new CacheEntry(k, computeColor(red, green, blue, alpha), 60000L);
         cleanupQueue.offer(newEntry);
         return newEntry;
      });
      return cacheEntry.getColor();
   }

   private static int computeColor(int red, int green, int blue, int alpha) {
      return Mth.clamp(alpha, 0, 255) << 24 | Mth.clamp(red, 0, 255) << 16 | Mth.clamp(green, 0, 255) << 8 | Mth.clamp(blue, 0, 255);
   }

   private ColorUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static {
      cacheCleaner.scheduleWithFixedDelay(() -> {
         for (CacheEntry entry = cleanupQueue.poll(); entry != null; entry = cleanupQueue.poll()) {
            if (!entry.isExpired()) continue;

            colorCache.remove(entry.getKey());
         }
      }, 0L, 1L, TimeUnit.SECONDS);
   }

   private static class CacheEntry implements Delayed {
      private final ColorKey key;
      private final int color;
      private final long expirationTime;

      CacheEntry(ColorKey key, int color, long ttl) {
         this.key = key;
         this.color = color;
         this.expirationTime = System.currentTimeMillis() + ttl;
      }

      @Override
      public long getDelay(TimeUnit unit) {
         long delay = this.expirationTime - System.currentTimeMillis();
         return unit.convert(delay, TimeUnit.MILLISECONDS);
      }

      public int compareTo(Delayed other) {
         return other instanceof CacheEntry ? Long.compare(this.expirationTime, ((CacheEntry)other).expirationTime) : 0;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() > this.expirationTime;
      }

      public ColorKey getKey() {
         return this.key;
      }

      public int getColor() {
         return this.color;
      }

      public long getExpirationTime() {
         return this.expirationTime;
      }
   }

   private static class ColorKey {
      final int red;
      final int green;
      final int blue;
      final int alpha;

      public int getRed() {
         return this.red;
      }

      public int getGreen() {
         return this.green;
      }

      public int getBlue() {
         return this.blue;
      }

      public int getAlpha() {
         return this.alpha;
      }

      public ColorKey(int red, int green, int blue, int alpha) {
         this.red = red;
         this.green = green;
         this.blue = blue;
         this.alpha = alpha;
      }

      @Override
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         }

         if (!(o instanceof ColorKey other)) {
            return false;
         }

         if (!other.canEqual(this)) {
            return false;
         }

         if (this.getRed() != other.getRed()) {
            return false;
         }

         if (this.getGreen() != other.getGreen()) {
            return false;
         }

         return this.getBlue() == other.getBlue() && this.getAlpha() == other.getAlpha();
      }

      protected boolean canEqual(Object other) {
         return other instanceof ColorKey;
      }

      @Override
      public int hashCode() {
         int PRIME = 59;
         int result = 1;
         result = result * 59 + this.getRed();
         result = result * 59 + this.getGreen();
         result = result * 59 + this.getBlue();
         return result * 59 + this.getAlpha();
      }
   }
}
