package pizza.dlc.api.util.color;

import java.awt.Color;
import java.util.Objects;
import net.minecraft.util.Mth;

public class ColorRGBA {
   public static final ColorRGBA WHITE = new ColorRGBA(255, 255, 255);
   public static final ColorRGBA BLACK = new ColorRGBA(0, 0, 0);
   public static final ColorRGBA GREEN = new ColorRGBA(0, 255, 0);
   public static final ColorRGBA RED = new ColorRGBA(255, 0, 0);
   public static final ColorRGBA BLUE = new ColorRGBA(0, 0, 255);
   public static final ColorRGBA YELLOW = new ColorRGBA(255, 255, 0);
   public static final ColorRGBA GRAY = new ColorRGBA(88, 87, 93);
   public static final ColorRGBA TRANSPARENT = new ColorRGBA(0, 0, 0, 0);
   private transient float[] hsbValues;
   private final int red;
   private final int green;
   private final int blue;
   private final int alpha;

   public ColorRGBA(int color) {
      this(ColorUtility.red(color), ColorUtility.green(color), ColorUtility.blue(color), ColorUtility.alpha(color));
   }

   public ColorRGBA(Color color) {
      this(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
   }

   public ColorRGBA(int red, int green, int blue) {
      this(red, green, blue, 255);
   }

   public ColorRGBA(int red, int green, int blue, int alpha) {
      this.red = Mth.clamp(red, 0, 255);
      this.green = Mth.clamp(green, 0, 255);
      this.blue = Mth.clamp(blue, 0, 255);
      this.alpha = Mth.clamp(alpha, 0, 255);
   }

   public int getRGB() {
      return (this.alpha & 0xFF) << 24 | (this.red & 0xFF) << 16 | (this.green & 0xFF) << 8 | this.blue & 0xFF;
   }

   public static int toArgbWithMinAlpha(ColorRGBA color, int extraAlpha, float minAlpha01) {
      if (color == null) {
         return 0;
      }

      int baseAlpha = Mth.clamp(color.getAlpha(), 0, 255);
      int minA = Math.max(1, Math.round(Mth.clamp(minAlpha01, 0.0F, 1.0F) * 255.0F));
      int clampedBase = Math.max(minA, baseAlpha);
      float extraA01 = Mth.clamp(extraAlpha, 0, 255) / 255.0F;
      int a = Mth.clamp(Math.round(clampedBase * extraA01), 0, 255);
      return a << 24 | (color.getRed() & 0xFF) << 16 | (color.getGreen() & 0xFF) << 8 | color.getBlue() & 0xFF;
   }

   private int clamp(float value) {
      return (int)Math.max(0.0F, Math.min(255.0F, value));
   }

   public static ColorRGBA fromHex(String hex) {
      String sanitized = hex.startsWith("#") ? hex.substring(1) : hex;
      if (sanitized.length() != 6 && sanitized.length() != 8) {
         throw new IllegalArgumentException("Hex color must be in the format #RRGGBB or #RRGGBBAA");
      }

      int red = Integer.parseInt(sanitized.substring(0, 2), 16);
      int green = Integer.parseInt(sanitized.substring(2, 4), 16);
      int blue = Integer.parseInt(sanitized.substring(4, 6), 16);
      int alpha = sanitized.length() == 8 ? Integer.parseInt(sanitized.substring(6, 8), 16) : 255;
      return new ColorRGBA(red, green, blue, alpha);
   }

   public static ColorRGBA lerp(ColorRGBA startColor, ColorRGBA endColor, float delta) {
      float clampedDelta = Mth.clamp(delta, 0.0F, 1.0F);
      int r = (int)Mth.lerp(clampedDelta, startColor.getRed(), endColor.getRed());
      int g = (int)Mth.lerp(clampedDelta, startColor.getGreen(), endColor.getGreen());
      int b = (int)Mth.lerp(clampedDelta, startColor.getBlue(), endColor.getBlue());
      int a = (int)Mth.lerp(clampedDelta, startColor.getAlpha(), endColor.getAlpha());
      return new ColorRGBA(r, g, b, a);
   }

   public static ColorRGBA fromInt(int colorInt) {
      int alpha = colorInt >> 24 & 0xFF;
      int red = colorInt >> 16 & 0xFF;
      int green = colorInt >> 8 & 0xFF;
      int blue = colorInt & 0xFF;
      return new ColorRGBA(red, green, blue, alpha);
   }

   public ColorRGBA withAlpha(float newAlpha) {
      return new ColorRGBA(this.red, this.green, this.blue, this.clamp((int)(255.0F * newAlpha)));
   }

   public ColorRGBA withAlpha(int newAlpha) {
      return new ColorRGBA(this.red, this.green, this.blue, newAlpha);
   }

   public ColorRGBA mulAlpha(float percent) {
      return this.withAlpha((int)(this.alpha * percent));
   }

   public ColorRGBA mix(ColorRGBA color2, float amount) {
      amount = Mth.clamp(amount, 0.0F, 1.0F);
      return new ColorRGBA(
         (int)Mth.lerp(amount, this.getRed(), color2.getRed()),
         (int)Mth.lerp(amount, this.getGreen(), color2.getGreen()),
         (int)Mth.lerp(amount, this.getBlue(), color2.getBlue()),
         (int)Mth.lerp(amount, this.getAlpha(), color2.getAlpha())
      );
   }

   public ColorRGBA darker(float amount) {
      amount = Mth.clamp(amount, 0.0F, 1.0F);
      return new ColorRGBA((int)(this.red * (1.0F - amount)), (int)(this.green * (1.0F - amount)), (int)(this.blue * (1.0F - amount)), this.alpha);
   }

   public static ColorRGBA fromHSB(float hue, float saturation, float brightness) {
      if (saturation == 0.0F) {
         int grayValue = (int)(brightness * 255.0F + 0.5F);
         return new ColorRGBA(grayValue, grayValue, grayValue);
      }

      float h = (hue - (float)Math.floor(hue)) * 6.0F;
      float f = h - (float)Math.floor(h);
      float p = brightness * (1.0F - saturation);
      float q = brightness * (1.0F - saturation * f);
      float t = brightness * (1.0F - saturation * (1.0F - f));
      float r = 0.0F;
      float g = 0.0F;
      float b = 0.0F;
      switch ((int)h) {
         case 0:
            r = brightness;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = brightness;
            b = p;
            break;
         case 2:
            r = p;
            g = brightness;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = brightness;
            break;
         case 4:
            r = t;
            g = p;
            b = brightness;
            break;
         case 5:
            r = brightness;
            g = p;
            b = q;
      }

      return new ColorRGBA((int)(r * 255.0F), (int)(g * 255.0F), (int)(b * 255.0F));
   }

   public float getHue() {
      return this.getHSBValues()[0];
   }

   public float getSaturation() {
      return this.getHSBValues()[1];
   }

   public float getBrightness() {
      return this.getHSBValues()[2];
   }

   private float[] getHSBValues() {
      if (this.hsbValues == null) {
         this.hsbValues = this.calculateHSB();
      }

      return this.hsbValues;
   }

   private float[] calculateHSB() {
      float r = this.red / 255.0F;
      float g = this.green / 255.0F;
      float b = this.blue / 255.0F;
      float maxC = Math.max(r, Math.max(g, b));
      float minC = Math.min(r, Math.min(g, b));
      float delta = maxC - minC;
      float hue = 0.0F;
      if (delta != 0.0F) {
         if (maxC == r) {
            hue = (g - b) / delta;
         } else if (maxC == g) {
            hue = (b - r) / delta + 2.0F;
         } else {
            hue = (r - g) / delta + 4.0F;
         }

         hue /= 6.0F;
         if (hue < 0.0F) {
            hue++;
         }
      }

      float saturation = maxC == 0.0F ? 0.0F : delta / maxC;
      return new float[]{hue, saturation, maxC};
   }

   public ColorRGBA brighter(float amount) {
      amount = Mth.clamp(amount, 0.0F, 1.0F);
      return new ColorRGBA(
         (int)(this.red + (255.0F - this.red) * amount),
         (int)(this.green + (255.0F - this.green) * amount),
         (int)(this.blue + (255.0F - this.blue) * amount),
         this.alpha
      );
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         ColorRGBA colorRGBA = (ColorRGBA)o;
         return this.red == colorRGBA.red && this.green == colorRGBA.green && this.blue == colorRGBA.blue && this.alpha == colorRGBA.alpha;
      }

      return false;
   }

   public float difference(ColorRGBA colorRGBA) {
      return Math.abs(this.getHue() - colorRGBA.getHue())
         + Math.abs(this.getBrightness() - colorRGBA.getBrightness())
         + Math.abs(this.getSaturation() - colorRGBA.getSaturation());
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.red, this.green, this.blue, this.alpha);
   }

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
}
