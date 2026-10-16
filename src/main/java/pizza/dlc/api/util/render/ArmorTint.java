package pizza.dlc.api.util.render;

import net.minecraft.world.item.ItemStack;

public final class ArmorTint {
   private static final float[] STOPS = new float[]{0.0F, 0.35F, 0.7F, 1.0F};
   private static final int[] COLORS = new int[]{0xFFE03828, 0xFFE89838, 0xFFD8D040, 0xFF50D050};

   private ArmorTint() {
   }

   public static int multiply(int base, int tint) {
      int a = base >>> 24 & 0xFF;
      int r = (base >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255;
      int g = (base >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
      int b = (base & 0xFF) * (tint & 0xFF) / 255;
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int tintArgb(float frac) {
      if (frac >= 1.0F) {
         return -1;
      }

      float f = Math.max(0.0F, frac);

      for (int i = STOPS.length - 1; i > 0; i--) {
         if (f >= STOPS[i - 1]) {
            float t = (f - STOPS[i - 1]) / (STOPS[i] - STOPS[i - 1]);
            return lerp(COLORS[i - 1], COLORS[i], t);
         }
      }

      return COLORS[0];
   }

   private static int lerp(int c1, int c2, float t) {
      int a = lerpCh(c1 >>> 24 & 0xFF, c2 >>> 24 & 0xFF, t);
      int r = lerpCh(c1 >> 16 & 0xFF, c2 >> 16 & 0xFF, t);
      int g = lerpCh(c1 >> 8 & 0xFF, c2 >> 8 & 0xFF, t);
      int b = lerpCh(c1 & 0xFF, c2 & 0xFF, t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static int lerpCh(int a, int b, float t) {
      return Math.round(a + (b - a) * t);
   }

   public static float durabilityFraction(ItemStack stack) {
      int max = stack.getMaxDamage();
      return Math.max(0.0F, Math.min(1.0F, (float)(max - stack.getDamageValue()) / max));
   }
}
