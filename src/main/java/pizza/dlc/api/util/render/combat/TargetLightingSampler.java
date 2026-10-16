package pizza.dlc.api.util.render.combat;

import java.util.Arrays;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TargetLightingSampler implements pizza.dlc.api.interfaces.QuickImports {

   private final int[] f_10592 = new int[1024];
   private final IdentityHashMap<BlockState, Integer> f_10593 = new IdentityHashMap<>();
   private final MutableBlockPos f_10594 = new MutableBlockPos();
   private final Vector3f f_10595 = new Vector3f();
   private long f_10596;
   private int f_10597;
   private int f_10598;
   private int f_10599;
   private int f_10600;
   private int f_10601;
   private int f_10602;
   private int f_10603;
   private int f_10604;
   private int f_10605;
   private static final long f_10606 = Long.MIN_VALUE;

   private static final long f_10610 = Long.MIN_VALUE;

   public float m_3631(Vec3 var1) {
      return (float)(this.f_10599 - var1.z);
   }

   private static int m_3632(BlockState var0) {
      int[] var1 = new int[]{0};
      var0.getOcclusionShape().forAllBoxes((var1x, var3, var5, var7, var9, var11) -> {
         for (int var13 = 0; var13 < 8; var13++) {
            double var14 = 0.25 + (var13 & 1) * 0.5;
            double var16 = 0.25 + (var13 >> 1 & 1) * 0.5;
            double var18 = 0.25 + (var13 >> 2 & 1) * 0.5;
            if (var14 >= var1x && var14 < var7 && var16 >= var3 && var16 < var9 && var18 >= var5 && var18 < var11) {
               var1[0] |= 1 << var13;
            }
         }
      });
      return var1[0];
   }

   public int[] m_3633() {
      return this.f_10592;
   }

   public void m_3634() {
      this.f_10596 = f_10610;
      this.f_10593.clear();
      Arrays.fill(this.f_10592, 0);
   }

   public void m_3635(Matrix4f var1, Vector3f[] var2, float[] var3, int var4, Vec3 var5) {
      double var6 = Double.POSITIVE_INFINITY;
      double var8 = var6;
      double var10 = var6;
      double var12 = Double.NEGATIVE_INFINITY;
      double var14 = var12;
      double var16 = var12;

      for (int var18 = 0; var18 < var4; var18++) {
         var1.transformPosition(var2[var18], this.f_10595);
         double var19 = var5.x + this.f_10595.x;
         double var21 = var5.y + this.f_10595.y;
         double var23 = var5.z + this.f_10595.z;
         float var25 = var3[var18];
         var6 = Math.min(var6, var19 - var25);
         var12 = Math.max(var12, var19 + var25);
         var8 = Math.min(var8, var21 - var25);
         var14 = Math.max(var14, var21 + var25);
         var10 = Math.min(var10, var23 - var25);
         var16 = Math.max(var16, var23 + var25);
      }

      int var39 = (int)Math.floor((var6 + var12) * 0.5) - 8;
      int var40 = (int)Math.floor((var8 + var14) * 0.5) - 8;
      int var20 = (int)Math.floor((var10 + var16) * 0.5) - 8;
      int var41 = Math.max(var39, (int)Math.floor(var6));
      int var22 = Math.min(var39 + 15, (int)Math.floor(var12));
      int var42 = Math.max(var40, (int)Math.floor(var8));
      int var24 = Math.min(var40 + 15, (int)Math.floor(var14));
      int var43 = Math.max(var20, (int)Math.floor(var10));
      int var26 = Math.min(var20 + 15, (int)Math.floor(var16));
      long var27 = mc.level.getGameTime();
      if (this.f_10596 != var27
         || this.f_10597 != var39
         || this.f_10598 != var40
         || this.f_10599 != var20
         || this.f_10600 != var41
         || this.f_10603 != var22
         || this.f_10601 != var42
         || this.f_10604 != var24
         || this.f_10602 != var43
         || this.f_10605 != var26) {
         this.f_10597 = var39;
         this.f_10598 = var40;
         this.f_10599 = var20;
         this.f_10600 = var41;
         this.f_10603 = var22;
         this.f_10601 = var42;
         this.f_10604 = var24;
         this.f_10602 = var43;
         this.f_10605 = var26;
         this.f_10596 = var27;
         Arrays.fill(this.f_10592, 0);

         for (int var29 = var43; var29 <= var26; var29++) {
            for (int var30 = var42; var30 <= var24; var30++) {
               for (int var31 = var41; var31 <= var22; var31++) {
                  BlockState var32 = mc.level.getBlockState(this.f_10594.set(var31, var30, var29));
                  if (var32.canOcclude()) {
                     int var33 = this.f_10593.computeIfAbsent(var32, TargetLightingSampler::m_3632);

                     for (int var34 = 0; var34 < 8; var34++) {
                        if ((var33 & 1 << var34) != 0) {
                           int var35 = (var31 - var39) * 2 + (var34 & 1);
                           int var36 = (var30 - var40) * 2 + (var34 >> 1 & 1);
                           int var37 = (var29 - var20) * 2 + (var34 >> 2 & 1);
                           int var38 = var35 + var36 * 32 + var37 * 1024;
                           this.f_10592[var38 >> 5] = this.f_10592[var38 >> 5] | 1 << (var38 & 31);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public TargetLightingSampler() {
      this.f_10596 = f_10606;
   }

   public float m_3636(Vec3 var1) {
      return (float)(this.f_10598 - var1.y);
   }

   public float m_3637(Vec3 var1) {
      return (float)(this.f_10597 - var1.x);
   }
}
