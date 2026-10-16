package pizza.dlc.api.util.render.combat;
import pizza.dlc.api.util.combat.CombatMath;

import java.awt.Color;
import lombok.Generated;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.joml.Vector4f;

public final class CombatColors {

   private static final double f_5955 = Math.PI;

   public static int m_2297(int var0) {
      return var0 >> 24 & 0xFF;
   }

   public static Vector4f m_2298(int var0) {
      float var1 = (var0 >> 24 & 0xFF) / 255.0F;
      float var2 = (var0 >> 16 & 0xFF) / 255.0F;
      float var3 = (var0 >> 8 & 0xFF) / 255.0F;
      float var4 = (var0 & 0xFF) / 255.0F;
      return new Vector4f(var2, var3, var4, var1);
   }

   public static int m_2299(int var0) {
      return var0 >> 8 & 0xFF;
   }

   public static int m_2300(int var0, int var1, int var2, int var3) {
      int var4 = (int)((System.currentTimeMillis() / var3 + var2) % 360L);
      var4 = (var4 > 180 ? 360 - var4 : var4) + 180;
      int var5 = m_2312(var0, var1, Mth.clamp(var4 / 180.0F - 1.0F, 0.0F, 1.0F));
      float[] var6 = m_2301(var5);
      float[] var7 = Color.RGBtoHSB((int)(var6[0] * 255.0F), (int)(var6[1] * 255.0F), (int)(var6[2] * 255.0F), null);
      var7[1] *= 1.5F;
      var7[1] = Math.min(var7[1], 1.0F);
      return Color.HSBtoRGB(var7[0], var7[1], var7[2]);
   }

   public static float[] m_2301(int var0) {
      return new float[]{(var0 >> 16 & 0xFF) / 255.0F, (var0 >> 8 & 0xFF) / 255.0F, (var0 & 0xFF) / 255.0F, (var0 >> 24 & 0xFF) / 255.0F};
   }

   public static int m_2302(int var0, int var1, float var2) {
      Vector4f var3 = m_2298(var0).mul(255.0F);
      Vector4f var4 = m_2298(var1).mul(255.0F);
      return m_2303(
         (int)CombatMath.m_1309(var3.x, var4.x, var2),
         (int)CombatMath.m_1309(var3.y, var4.y, var2),
         (int)CombatMath.m_1309(var3.z, var4.z, var2),
         CombatMath.m_1309(var3.w, var4.w, var2)
      );
   }

   @Generated
   private CombatColors() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static int m_2303(int var0, int var1, int var2, double var3) {
      return (int)var3 << 24 | var0 << 16 | var1 << 8 | var2;
   }

   public static int m_2304(int var0) {
      if (false) {
         float var1 = ((float)System.currentTimeMillis() / 8.0F + var0 * 16) % 360.0F / 360.0F;
         if (var1 < 0.0F) {
            var1++;
         }

         return Color.HSBtoRGB(var1, 0.85F, 1.0F) | -16777216;
      } else {
         return m_2307(
            pizza.dlc.api.theme.ThemeManager.accent(),
            pizza.dlc.api.theme.ThemeManager.secondary(),
            var0 * 16.0F / 360.0F,
            255
         );
      }
   }

   public static int m_2305(String var0) {
      String var1 = var0.trim();
      if (var1.startsWith("#")) {
         var1 = var1.substring(1);
      }

      if (var1.startsWith("0x") || var1.startsWith("0X")) {
         var1 = var1.substring(2);
      }

      if (var1.length() == 3 || var1.length() == 4) {
         char var2 = var1.charAt(0);
         char var3 = var1.charAt(1);
         char var4 = var1.charAt(2);
         char var5 = var1.length() == 4 ? var1.charAt(3) : 'F';
         var1 = "" + var2 + var2 + var3 + var3 + var4 + var4 + var5 + var5;
      }

      if (var1.length() == 6) {
         int var7 = Integer.parseInt(var1.substring(0, 2), 16);
         int var9 = Integer.parseInt(var1.substring(2, 4), 16);
         int var11 = Integer.parseInt(var1.substring(4, 6), 16);
         return m_2303(var7, var9, var11, 255.0);
      } else if (var1.length() == 8) {
         int var6 = Integer.parseInt(var1.substring(0, 2), 16);
         int var8 = Integer.parseInt(var1.substring(2, 4), 16);
         int var10 = Integer.parseInt(var1.substring(4, 6), 16);
         int var12 = Integer.parseInt(var1.substring(6, 8), 16);
         return m_2303(var6, var8, var10, var12);
      } else {
         throw new IllegalArgumentException(var0 + "");
      }
   }

   public static int m_2306(int var0, float var1) {
      int var2 = Math.min(255, Math.max(0, (int)(var1 * 255.0F)));
      return m_2303(m_2309(var0), m_2299(var0), m_2308(var0), var2);
   }

   public static int m_2307(int var0, int var1, float var2, int var3) {
      if ((var0 & 16777215) == (var1 & 16777215)) {
         return m_2303(m_2309(var0), m_2299(var0), m_2308(var0), var3);
      }

      float var4 = ((float)(System.currentTimeMillis() % 10000L) / 10000.0F + var2) % 1.0F;
      if (var4 < 0.0F) {
         var4++;
      }

      float var5 = 0.5F - 0.5F * (float)Math.cos(var4 * f_5955 * 2.0);
      int var6 = m_2312(var0, var1, var5);
      return m_2303(m_2309(var6), m_2299(var6), m_2308(var6), var3);
   }

   public static int m_2308(int var0) {
      return var0 & 0xFF;
   }

   public static int m_2309(int var0) {
      return var0 >> 16 & 0xFF;
   }

   public static int m_2310(int var0, int var1, int var2) {
      return -16777216 | var0 << 16 | var1 << 8 | var2;
   }

   public static Component m_2311(String var0) {
      MutableComponent var1 = Component.empty();

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         MutableComponent var4 = Component.literal(String.valueOf(var3)).setStyle(Style.EMPTY.withColor(new Color(m_2304(var2)).getRGB()));
         var1.append(var4);
      }

      return var1;
   }

   public static int m_2312(int var0, int var1, float var2) {
      float[] var3 = m_2301(var0);
      float[] var4 = m_2301(var1);
      return m_2303(
         (int)CombatMath.m_1309(var3[0] * 255.0F, var4[0] * 255.0F, var2),
         (int)CombatMath.m_1309(var3[1] * 255.0F, var4[1] * 255.0F, var2),
         (int)CombatMath.m_1309(var3[2] * 255.0F, var4[2] * 255.0F, var2),
         (int)CombatMath.m_1309(var3[3] * 255.0F, var4[3] * 255.0F, var2)
      );
   }

   public static int m_2313(int var0, float var1) {
      int var2 = (int)(m_2309(var0) * var1);
      int var3 = (int)(m_2299(var0) * var1);
      int var4 = (int)(m_2308(var0) * var1);
      int var5 = (int)(m_2297(var0) * var1);
      return m_2303(var2, var3, var4, var5);
   }
}
