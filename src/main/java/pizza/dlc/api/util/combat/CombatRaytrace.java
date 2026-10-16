package pizza.dlc.api.util.combat;

import lombok.Generated;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class CombatRaytrace implements pizza.dlc.api.interfaces.QuickImports {
   private static final float f_20860 = (float) (Math.PI / 180.0);
   private static final float f_20861 = (float) Math.PI;
   private static final float f_20862 = (float) (Math.PI / 180.0);

   public static boolean m_6784(float var0, float var1, double var2, Entity var4, boolean var5) {
      Vec3 var6 = mc.player.getEyePosition();
      Vec3 var7 = m_6785(var1, var0);
      Vec3 var8 = var6.add(var7.scale(var2));
      AABB var9 = var4.getBoundingBox();
      var9 = var9.inflate(pizza.dlc.client.modules.impl.combat.HitBox.extraMargin());

      boolean var13 = var9.contains(var6) || var9.clip(var6, var8).isPresent();
      if (!var13) {
         return false;
      }

      if (var5) {
         Vec3 var11;
         if (var9.contains(var6)) {
            var11 = var6;
         } else {
            var11 = (Vec3)var9.clip(var6, var8).get();
         }

         ClipContext var12 = new ClipContext(var6, var11, Block.OUTLINE, Fluid.NONE, mc.player);
         return mc.level.clip(var12).getType() == Type.MISS;
      } else {
         return true;
      }
   }

   @Generated
   private CombatRaytrace() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static Vec3 m_6785(float var0, float var1) {
      float var2 = -var1 * f_20860 - f_20861;
      float var3 = -var0 * f_20862;
      float var4 = Mth.cos(var2);
      float var5 = Mth.sin(var2);
      float var6 = -Mth.cos(var3);
      float var7 = Mth.sin(var3);
      return new Vec3(var5 * var6, var7, var4 * var6);
   }

   public static HitResult m_6786(double var0, float var2, float var3, Entity var4) {
      Vec3 var5 = mc.player.getEyePosition();
      Vec3 var6 = m_6785(var3, var2);
      Vec3 var7 = var5.add(var6.x * var0, var6.y * var0, var6.z * var0);
      return mc.level.clip(new ClipContext(var5, var7, Block.OUTLINE, Fluid.NONE, var4));
   }
}
