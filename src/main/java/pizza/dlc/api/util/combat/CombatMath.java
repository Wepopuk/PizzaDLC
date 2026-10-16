package pizza.dlc.api.util.combat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import lombok.Generated;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class CombatMath {
   private static final double f_1411 = 180.0 / Math.PI;

   private static final double f_1413 = 180.0 / Math.PI;

   public static double m_1300() {
      return pizza.dlc.api.interfaces.QuickImports.mc.getFps() > 0 ? 1.0 / pizza.dlc.api.interfaces.QuickImports.mc.getFps() : 1.0;
   }

   public static float m_1301(float var0, float var1, float var2) {
      return (float)(var0 + (var1 - var0) * Mth.clamp(m_1300() * var2, 0.0, 1.0));
   }

   @Generated
   private CombatMath() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static float m_1302(float var0, float var1) {
      SecureRandom var2 = new SecureRandom();
      double var3 = var2.nextDouble();
      double var5 = var2.nextDouble();
      double var7 = var2.nextGaussian() * 0.02F;
      double var9 = Math.pow(var3, 1.0 + var2.nextDouble() * 0.7);
      double var11 = (var5 * 0.8 + 0.1) * (Math.log1p(var3 * 3.0) * 0.5 + 0.5);
      return (float)(var0 + (var1 - var0) * var9 * var11 + var7);
   }

   public static Vec3 m_1303(Vec3 var0, Vec3 var1, float var2) {
      return new Vec3(Mth.lerp(var2, var0.x, var1.x), Mth.lerp(var2, var0.y, var1.y), Mth.lerp(var2, var0.z, var1.z));
   }

   public static Vec2 m_1304(Vec3 var0) {
      return m_1310(var0.x, var0.y, var0.z);
   }

   public static Vec3 m_1305(Vec3 var0, Vec3 var1, float var2) {
      return new Vec3(
         m_1311((float)var0.x(), (float)var1.x(), var2), m_1311((float)var0.y(), (float)var1.y(), var2), m_1311((float)var0.z(), (float)var1.z(), var2)
      );
   }

   public static double blocksPerSecond(Entity var0) {
      double var1 = var0.getX() - var0.xo;
      double var3 = var0.getY() - var0.yo;
      double var5 = var0.getZ() - var0.zo;
      double var7 = Math.sqrt(var1 * var1 + var3 * var3 + var5 * var5);
      return 20.0 * var7;
   }

   public static Vec3 m_1307(Vec3 var0, AABB var1) {
      return new Vec3(Mth.clamp(var0.x(), var1.minX, var1.maxX), Mth.clamp(var0.y(), var1.minY, var1.maxY), Mth.clamp(var0.z(), var1.minZ, var1.maxZ));
   }

   public static double roundToStep(double var0, double var2) {
      double var4 = Math.round(var0 / var2) * var2;
      BigDecimal var6 = new BigDecimal(var4);
      var6 = var6.setScale(2, RoundingMode.HALF_UP);
      return var6.doubleValue();
   }

   public static float m_1309(float var0, float var1, float var2) {
      return Mth.lerp(var2, var0, var1);
   }

   public static Vec2 m_1310(double var0, double var2, double var4) {
      double var6 = var0 - pizza.dlc.api.interfaces.QuickImports.mc.player.getX();
      double var8 = var2 - pizza.dlc.api.interfaces.QuickImports.mc.player.getEyeY();
      double var10 = var4 - pizza.dlc.api.interfaces.QuickImports.mc.player.getZ();
      double var12 = Mth.sqrt((float)(var6 * var6 + var10 * var10));
      float var14 = (float)(Mth.atan2(var10, var6) * f_1411 - 90.0);
      float var15 = (float)(-Mth.atan2(var8, var12) * f_1413);
      return new Vec2(var14, var15);
   }

   public static float m_1311(float var0, float var1, float var2) {
      return (1.0F - Mth.clamp((float)(m_1300() * var2), 0.0F, 1.0F)) * var0 + Mth.clamp((float)(m_1300() * var2), 0.0F, 1.0F) * var1;
   }
}
