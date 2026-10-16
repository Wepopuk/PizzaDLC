package pizza.dlc.api.util.combat;
import pizza.dlc.api.interfaces.ResolvedRotationAccess;

import pizza.dlc.mixin.client.LivingEntityGravityAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ElytraPrediction {

   private static final float f_11130 = (float) (Math.PI / 180.0);

   public static Vec3 m_3885(LivingEntity var0, float var1) {
      if (var0 != null && Float.isFinite(var1) && var1 != 0.0F) {
         ResolvedRotationAccess var2 = (ResolvedRotationAccess)var0;
         float var3 = var2.getResolvedPitch();
         Vec3 var4 = var0.calculateViewVector(var3, var2.getResolvedYaw());
         double var5 = ((LivingEntityGravityAccessor)var0).pizzadlc$getEffectiveGravity();
         Vec3 var7 = m_3886(var0.getDeltaMovement(), var4, var3, var5, var0.hasEffect(MobEffects.SLOW_FALLING));
         double var8 = var7.lengthSqr();
         if (Double.isFinite(var8) && !(var8 < 1.0E-4)) {
            double var10 = Math.sqrt(var8);
            float var12 = (float)(var10 / 8.0);
            return var7.scale(1.0 / var10).scale(var1 * var12);
         } else {
            return Vec3.ZERO;
         }
      } else {
         return Vec3.ZERO;
      }
   }

   private ElytraPrediction() {
   }

   private static Vec3 m_3886(Vec3 var0, Vec3 var1, float var2, double var3, boolean var5) {
      float var6 = var2 * f_11130;
      double var7 = Math.sqrt(var1.x * var1.x + var1.z * var1.z);
      boolean var9 = var7 > 0.0;
      double var10 = var9 ? var1.x / var7 : 0.0;
      double var12 = var9 ? var1.z / var7 : 0.0;
      double var14 = Mth.square(Math.cos(var6));
      double var16 = -1.0 + var14 * 0.75;
      double var18 = var3 * var16;
      double var20 = Math.min(var3, 0.01) * var16;
      double var22 = -0.1 * var14;
      boolean var24 = var6 < 0.0F && var9;
      double var25 = -Mth.sin(var6) * 0.04;
      double var27 = var0.x;
      double var29 = var0.y;
      double var31 = var0.z;
      double var33 = 0.0;
      double var35 = 0.0;
      double var37 = 0.0;

      for (int var39 = 0; var39 < 8; var39++) {
         double var40 = Math.sqrt(var27 * var27 + var31 * var31);
         var29 += var5 && var29 <= 0.0 ? var20 : var18;
         if (var9 && var29 < 0.0) {
            double var42 = var29 * var22;
            var27 += var10 * var42;
            var29 += var42;
            var31 += var12 * var42;
         }

         if (var24) {
            double var45 = var40 * var25;
            var27 -= var10 * var45;
            var29 += var45 * 3.2;
            var31 -= var12 * var45;
         }

         if (var9) {
            var27 += (var10 * var40 - var27) * 0.1;
            var31 += (var12 * var40 - var31) * 0.1;
         }

         var27 *= 0.99F;
         var29 *= 0.98F;
         var31 *= 0.99F;
         var33 += var27;
         var35 += var29;
         var37 += var31;
      }

      return new Vec3(var33, var35, var37);
   }
}
