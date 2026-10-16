package pizza.dlc.api.rotation.combat.impl;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;

import pizza.dlc.client.modules.impl.combat.AttackAura;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public final class LegitAim extends CombatRotationPlan {
   private static final int f_36169 = Integer.MIN_VALUE;

   private final Random f_36178 = new Random();
   private int f_36179;
   private int f_36180;
   private int f_36181;
   private long f_36182;
   private int f_36183;
   private boolean f_36184;
   private boolean f_36185;
   private double f_36186;
   private double f_36187;
   private double f_36188;
   private boolean f_36189;
   private boolean f_36190;
   private int f_36191;
   private int f_36192;
   private int f_36193;
   private float f_36194;
   private float f_36195;
   private float f_36196;
   private boolean f_36197;
   private float f_36198;
   private float f_36199;
   private float f_36200;
   private float f_36201;
   private float f_36202;
   private int f_36203;
   private int f_36204;
   private boolean f_36205;
   private LegitAim.AimPhase f_36206;
   private int f_36207;
   private int f_36208;
   private float f_36209;
   private float f_36210;
   private float f_36211;
   private float f_36212;
   private int f_36213;
   private int f_36214;
   private float f_36215;
   private float f_36216;
   private float f_36217;
   private boolean f_36218;
   private LegitAim.AimPhase f_36219;
   private float f_36220;
   private float f_36221;
   private float f_36222;
   private float f_36223;
   private float f_36224;
   private float f_36225;
   private float f_36226;
   private float f_36227;
   private float f_36228;
   private float f_36229;
   private float f_36230;
   private float f_36231;
   private boolean f_36232;
   private boolean f_36233;
   private static final int f_36234 = Integer.MIN_VALUE;

   private static final int f_36238 = Integer.MIN_VALUE;

   private static final double f_36241 = Math.PI;

   private static final double f_36276 = Math.PI;

   private static final double f_36391 = Math.PI / 2;
   private static final double f_36392 = Math.PI;

   public LegitAim(Vec3 var1) {
      super(var1);
      this.f_36179 = f_36234;
      this.f_36186 = 0.5;
      this.f_36187 = 0.5;
      this.f_36188 = 0.5;
      this.f_36206 = LegitAim.AimPhase.CUBIC_BEZIER;
      this.f_36215 = 1.0F;
      this.f_36219 = LegitAim.AimPhase.CUBIC_BEZIER;
   }

   private Vector2f m_9843(Vec3 var1) {
      Vec3 var2 = var1.subtract(mc.player.getEyePosition());
      double var3 = Math.hypot(var2.x, var2.z);
      return new Vector2f((float)Math.toDegrees(Math.atan2(-var2.x, var2.z)), (float)(-Math.toDegrees(Math.atan2(var2.y, var3))));
   }

   private void m_9844() {
      float var1 = this.f_36178.nextFloat();
      if (var1 < 0.34F) {
         this.f_36226 = this.m_9866(0.12F, 0.24F);
         this.f_36227 = this.m_9866(0.16F, 0.3F);
         this.f_36228 = this.m_9866(0.48F, 0.66F);
         this.f_36229 = this.m_9866(0.82F, 0.94F);
      } else if (var1 < 0.68F) {
         this.f_36226 = this.m_9866(0.25F, 0.38F);
         this.f_36227 = this.m_9866(0.02F, 0.11F);
         this.f_36228 = this.m_9866(0.62F, 0.8F);
         this.f_36229 = this.m_9866(0.86F, 0.97F);
      } else {
         this.f_36226 = this.m_9866(0.17F, 0.31F);
         this.f_36227 = this.m_9866(0.07F, 0.2F);
         this.f_36228 = this.m_9866(0.55F, 0.75F);
         this.f_36229 = this.m_9866(0.84F, 0.96F);
      }

      this.f_36232 = this.f_36178.nextFloat() < 0.22F;
      this.f_36233 = false;
      if (this.f_36232) {
         this.f_36228 = this.m_9866(Math.max(this.f_36226 + 0.16F, 0.68F), 0.84F);
         this.f_36229 = this.m_9866(0.972F, 0.995F);
      }
   }

   private Vector2f m_9845(Entity var1, Vector2f var2) {
      return !this.f_36185 ? var2 : this.m_9843(this.m_9863(var1.getBoundingBox(), this.f_36186, this.f_36187, this.f_36188));
   }

   private void m_9846(Entity var1, Vector2f var2) {
      this.f_36181 = this.f_36180 + 12 + this.f_36178.nextInt(15);
      this.f_36185 = this.f_36178.nextFloat() < 0.2F;
      if (this.f_36185) {
         AABB var3 = var1.getBoundingBox();
         ArrayList var4 = new ArrayList(9);

         for (int var5 = 0; var5 < 9; var5++) {
            double var6 = this.m_9856(0.16, 0.84);
            double var8 = this.m_9856(0.2, 0.82);
            double var10 = this.m_9856(0.16, 0.84);
            Vector2f var12 = this.m_9843(this.m_9863(var3, var6, var8, var10));
            float var13 = Mth.wrapDegrees(var12.x - var2.x);
            float var14 = var12.y - var2.y;
            var4.add(new LegitAim.AimFrame(var6, var8, var10, (float)Math.hypot(var13, var14)));
         }

         var4.sort(Comparator.comparingDouble(LegitAim.AimFrame::rotationDistance));
         int var15;
         if (this.f_36178.nextFloat() < 0.78F) {
            var15 = 1 + this.f_36178.nextInt(Math.min(3, var4.size() - 1));
         } else {
            int var16 = Math.min(4, var4.size() - 1);
            var15 = var16 + this.f_36178.nextInt(var4.size() - var16);
         }

         LegitAim.AimFrame var17 = (LegitAim.AimFrame)var4.get(var15);
         this.f_36186 = var17.x();
         this.f_36187 = var17.y();
         this.f_36188 = var17.z();
      }
   }

   private void m_9847(Vector2f var1, Vector2f var2, boolean var3) {
      float var4 = Mth.wrapDegrees(var2.x - var1.x);
      float var5 = var2.y - var1.y;
      if (!this.f_36197) {
         if (var3 && Math.abs(var4) < 2.25F && Math.abs(var5) < 1.65F) {
            this.f_36197 = true;
            this.f_36198 = var1.x;
            this.f_36199 = var1.y;
            this.f_36216 = this.f_36216 * 0.25F;
            this.f_36217 = this.f_36217 * 0.25F;
         }
      } else if (var3 && !(Math.abs(var4) > 3.35F) && !(Math.abs(var5) > 2.45F)) {
         float var6 = Mth.wrapDegrees(var2.x - this.f_36198);
         float var7 = var2.y - this.f_36199;
         this.f_36198 = this.f_36198 + this.m_9857(var6, 2.25F, 0.48F) * 0.62F;
         this.f_36199 = Mth.clamp(this.f_36199 + this.m_9857(var7, 1.65F, 0.34F) * 0.62F, -90.0F, 90.0F);
      } else {
         this.f_36197 = false;
      }
   }

   private float m_9848() {
      return this.f_36178.nextFloat() * 2.0F - 1.0F;
   }

   private boolean m_9849(Vector2f var1, Vector2f var2, boolean var3) {
      float var4 = Mth.wrapDegrees(var2.x - var1.x);
      float var5 = var2.y - var1.y;
      float var6 = (float)Math.hypot(var4, var5);
      if (var6 < 4.0F) {
         return false;
      }

      float var7 = var6 > 45.0F ? 0.78F : (var6 > 12.0F ? 0.62F : 0.44F);
      if (!var3) {
         var7 *= 0.55F;
      }

      if (this.f_36178.nextFloat() >= var7) {
         return false;
      }

      float var8 = this.m_9868(var4);
      float var9 = this.m_9868(var5);
      this.f_36194 = var8 * Mth.clamp(Math.abs(var4) * this.m_9866(0.035F, 0.075F), 0.42F, 3.8F);
      this.f_36195 = var9 * Mth.clamp(Math.abs(var5) * this.m_9866(0.025F, 0.06F), 0.16F, 1.25F);
      this.f_36190 = Math.abs(var4) >= Math.abs(var5);
      this.f_36196 = this.f_36190 ? var8 : var9;
      this.f_36191 = 0;
      this.f_36192 = Mth.clamp(4 + (int)Math.ceil(var6 / 28.0F), 4, 11);
      this.f_36189 = true;
      this.f_36193 = 0;
      this.f_36213 = Math.abs(var5) > 2.0F && this.f_36178.nextFloat() < 0.36F ? 1 : 0;
      return true;
   }

   private float m_9850(float var1, float var2, float var3) {
      float var4 = 1.0F - var1;
      return 3.0F * var4 * var4 * var1 * var2 + 3.0F * var4 * var1 * var1 * var3 + var1 * var1 * var1;
   }

   private boolean m_9851(Vector2f var1, Vector2f var2) {
      if (!this.f_36189) {
         return false;
      }

      this.f_36191++;
      float var3 = Mth.wrapDegrees(var2.x - var1.x);
      float var4 = var2.y - var1.y;
      float var5 = this.f_36190 ? var3 : var4;
      boolean var6 = var5 * this.f_36196 <= 0.0F;
      if (!var6 && this.f_36191 < this.f_36192) {
         return false;
      }

      this.f_36189 = false;
      this.f_36193 = 2 + this.f_36178.nextInt(3);
      return true;
   }

   @Override
   public void reset() {
      this.f_36179 = f_36238;
      this.f_36180 = 0;
      this.f_36181 = 0;
      this.f_36182 = 0L;
      this.f_36183 = 0;
      this.f_36184 = false;
      this.f_36185 = false;
      this.m_9876();
      this.f_36193 = 0;
      this.f_36197 = false;
      this.m_9869();
      this.f_36207 = 0;
      this.f_36208 = 0;
      this.f_36209 = 0.0F;
      this.f_36210 = 0.0F;
      this.f_36211 = 0.0F;
      this.f_36212 = 0.0F;
      this.f_36213 = 0;
      this.f_36214 = 0;
      this.f_36215 = 1.0F;
      this.f_36216 = 0.0F;
      this.f_36217 = 0.0F;
      this.m_9874();
   }

   private void m_9852() {
      if (this.f_36180 >= this.f_36214) {
         this.f_36215 = this.m_9866(0.92F, 1.08F);
         this.f_36214 = this.f_36180 + 4 + this.f_36178.nextInt(5);
      }
   }

   private float m_9853(Vector2f var1, float var2, float var3) {
      if (this.f_36218 && !(this.f_36224 < 1.0E-4F)) {
         float var4 = Mth.wrapDegrees(var2 - var1.x);
         float var5 = var3 - var1.y;
         float var6 = (float)Math.hypot(var4, var5);
         float var7 = 1.0F - Mth.clamp(var6 / this.f_36224, 0.0F, 1.0F);
         float var8 = Mth.wrapDegrees(var1.x - this.f_36220);
         float var9 = var1.y - this.f_36221;
         float var10 = (float)Math.hypot(var8, var9);
         float var11 = Mth.clamp(var10 / this.f_36224, 0.0F, 1.0F);
         this.f_36225 = Math.max(this.f_36225, Math.max(var7, var11));
         if (this.f_36232 && !this.f_36233 && this.f_36225 >= 0.86F) {
            this.f_36208 = this.f_36180 + Math.max(1, this.f_36207 + 1);
            this.f_36233 = true;
         }

         return this.f_36225;
      } else {
         return 1.0F;
      }
   }

   private float m_9854(LegitAim.AimPhase var1, float var2) {
      float var3 = Mth.clamp(var2, 0.0F, 1.0F);

      return switch (var1) {
         case CUBIC_BEZIER -> this.m_9862(0.0F, 0.0F, 1.0F, 1.0F, var3);
         case SINE_IN_OUT -> (float)(-(Math.cos(f_36276 * var3) - 1.0) * 0.5);
         case QUADRATIC_IN_OUT -> var3 < 0.5F ? 2.0F * var3 * var3 : 1.0F - (float)Math.pow(-2.0F * var3 + 2.0F, 2.0) * 0.5F;
         case QUARTIC_IN_OUT -> var3 < 0.5F ? 8.0F * var3 * var3 * var3 * var3 : 1.0F - (float)Math.pow(-2.0F * var3 + 2.0F, 4.0) * 0.5F;
      };
   }

   private boolean m_9855(Entity var1) {
      AttackAura var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      return var2 != null && var2.getTarget() != null && var2.getTarget().equals(var1) ? var2.m_1111() : false;
   }

   private double m_9856(double var1, double var3) {
      return var1 + this.f_36178.nextDouble() * (var3 - var1);
   }

   private float m_9857(float var1, float var2, float var3) {
      return Math.abs(var1) <= var2 ? 0.0F : var1 - Math.copySign(var3, var1);
   }

   private float m_9858(float var1, float var2, float var3) {
      if (Math.abs(var1) < 1.0E-4F) {
         return 0.0F;
      } else if (Math.abs(var2) < 1.0E-4F) {
         return Math.copySign(Math.min(Math.abs(var1), var3), var1);
      } else {
         return Math.signum(var1) != Math.signum(var2)
            ? Math.copySign(Math.min(Math.abs(var1), var3), var1)
            : Math.copySign(Math.min(Math.abs(var1), Math.abs(var2) + var3), var1);
      }
   }

   private void m_9859(Entity var1) {
      if (!this.f_36205) {
         this.f_36205 = true;
         this.f_36200 = this.f_36178.nextBoolean() ? -15.0F : 15.0F;
         this.f_36201 = this.f_36200;
         this.f_36202 = this.f_36200 == -15.0F ? 15.0F : -15.0F;
         this.f_36203 = 0;
         this.f_36204 = this.m_9867();
         this.f_36206 = this.m_9864();
      }

      this.f_36203 = Math.min(this.f_36203 + 1, this.f_36204);
      float var2 = Mth.clamp((float)this.f_36203 / this.f_36204, 0.0F, 1.0F);
      float var3 = this.m_9854(this.f_36206, var2);
      this.f_36200 = Mth.lerp(var3, this.f_36201, this.f_36202) * Math.clamp(1.5F / var1.distanceTo(mc.player), 0.0F, 1.0F);
      if (this.f_36203 >= this.f_36204) {
         this.f_36201 = this.f_36202;
         this.f_36202 = this.f_36202 <= 0.0F ? 15.0F : -15.0F;
         this.f_36203 = 0;
         this.f_36204 = this.m_9867();
         this.f_36206 = this.m_9864();
      }
   }

   private void m_9860(boolean var1) {
      if (this.f_36207 == 0 && this.f_36180 >= this.f_36208) {
         this.f_36207 = 2 + this.f_36178.nextInt(3);
         float var2 = var1 ? this.m_9866(0.08F, 0.28F) : this.m_9866(0.12F, 0.42F);
         float var3 = var1 ? this.m_9866(0.05F, 0.18F) : this.m_9866(0.07F, 0.26F);
         this.f_36211 = this.m_9848() * var2;
         this.f_36212 = this.m_9848() * var3;
         this.f_36208 = this.f_36180 + this.f_36207 + 3 + this.f_36178.nextInt(6);
      }

      if (this.f_36207 > 0) {
         this.f_36209 = Mth.lerp(0.58F, this.f_36209, this.f_36211);
         this.f_36210 = Mth.lerp(0.54F, this.f_36210, this.f_36212);
         this.f_36207--;
         if (this.f_36207 == 0) {
            this.f_36211 = 0.0F;
            this.f_36212 = 0.0F;
         }
      } else {
         this.f_36209 = Mth.lerp(0.46F, this.f_36209, 0.0F);
         this.f_36210 = Mth.lerp(0.42F, this.f_36210, 0.0F);
      }
   }

   private void m_9861(Entity var1, Vector2f var2) {
      this.f_36179 = var1.getId();
      this.f_36180 = 0;
      this.f_36183 = 1 + this.f_36178.nextInt(2);
      this.f_36184 = true;
      this.f_36197 = false;
      this.f_36193 = 0;
      this.m_9876();
      this.f_36207 = 0;
      this.f_36208 = 2 + this.f_36178.nextInt(4);
      this.f_36209 = 0.0F;
      this.f_36210 = 0.0F;
      this.f_36211 = 0.0F;
      this.f_36212 = 0.0F;
      this.f_36216 = 0.0F;
      this.f_36217 = 0.0F;
      this.f_36214 = 0;
      this.m_9874();
      this.m_9869();
      this.m_9846(var1, var2);
   }

   private float m_9862(float var1, float var2, float var3, float var4, float var5) {
      float var6 = 1.0F - var5;
      return var6 * var6 * var6 * var1 + 3.0F * var6 * var6 * var5 * var2 + 3.0F * var6 * var5 * var5 * var3 + var5 * var5 * var5 * var4;
   }

   private Vec3 m_9863(AABB var1, double var2, double var4, double var6) {
      double var8 = Math.min(var1.getXsize() * 0.12, 0.07);
      double var10 = Math.min(var1.getYsize() * 0.09, 0.11);
      double var12 = Math.min(var1.getZsize() * 0.12, 0.07);
      return new Vec3(
         Mth.lerp(var2, var1.minX + var8, var1.maxX - var8),
         Mth.lerp(var4, var1.minY + var10, var1.maxY - var10),
         Mth.lerp(var6, var1.minZ + var12, var1.maxZ - var12)
      );
   }

   private LegitAim.AimPhase m_9864() {
      LegitAim.AimPhase[] var1 = LegitAim.AimPhase.values();
      return var1[this.f_36178.nextInt(var1.length)];
   }

   private float m_9865(float var1) {
      float var2 = var1;

      for (int var3 = 0; var3 < 5; var3++) {
         float var4 = this.m_9850(var2, this.f_36226, this.f_36228) - var1;
         float var5 = this.m_9873(var2, this.f_36226, this.f_36228);
         if (Math.abs(var4) < 1.0E-4F || Math.abs(var5) < 1.0E-4F) {
            break;
         }

         var2 = Mth.clamp(var2 - var4 / var5, 0.0F, 1.0F);
      }

      return var2;
   }

   private float m_9866(float var1, float var2) {
      return var1 + this.f_36178.nextFloat() * (var2 - var1);
   }

   private int m_9867() {
      return 5 + this.f_36178.nextInt(6);
   }

   @Override
   public Vector2f rotate(Vector2f var1, Vector2f var2) {
      AttackAura var3 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      LivingEntity var4 = var3.getTarget();
      if (var1 == null || var2 == null) {
         return var1 != null ? var1 : var2;
      }

      if (var4 != null && mc.player != null) {
         long var5 = System.nanoTime();
         boolean var7 = this.f_36182 != 0L && var5 - this.f_36182 > 350000000L;
         boolean var8 = var7 || this.f_36179 != var4.getId();
         this.f_36182 = var5;
         if (var8) {
            this.m_9861(var4, var1);
         }

         this.m_9859(var4);
         if (this.m_9878(var4)) {
            return var1;
         }

         if (!var8) {
            this.f_36180++;
         }

         boolean var9 = this.f_36184;
         this.f_36184 = false;
         boolean var10 = false;
         if (this.f_36180 >= this.f_36181) {
            this.m_9846(var4, var1);
            var10 = true;
         }

         Vector2f var11 = this.m_9872(this.m_9845(var4, var2));
         Vector2f var12 = this.m_9872(this.m_9843(var4.getBoundingBox().getCenter()));
         boolean var13 = var4.getDeltaMovement().lengthSqr() < 0.002F;
         boolean var14 = this.f_36197;
         this.m_9847(var1, var12, var13);
         this.m_9860(this.f_36197);
         boolean var15 = var9 || var10 || var14 != this.f_36197;
         if (this.f_36197) {
            var15 |= this.f_36189;
            this.m_9876();
         } else if (var9 || var10) {
            var15 |= this.m_9849(var1, var11, var9);
         }

         float var16;
         float var17;
         if (this.f_36197) {
            var16 = this.f_36198 + this.f_36209;
            var17 = this.f_36199 + this.f_36210;
         } else {
            var15 |= this.m_9851(var1, var11);
            var16 = var11.x + (this.f_36189 ? this.f_36194 : 0.0F) + this.f_36209;
            var17 = var11.y + (this.f_36189 ? this.f_36195 : 0.0F) + this.f_36210;
         }

         float var18 = 1.0F;
         if (this.f_36197) {
            this.m_9874();
         } else {
            this.m_9870(var1, var16, var17, var15);
            float var19 = this.m_9853(var1, var16, var17);
            float var20 = this.m_9879(var19);
            float var21 = (float)Math.sin(f_36241 * var20);
            var16 += this.f_36230 * var21;
            var17 += this.f_36231 * var21;
            var18 = this.m_9875(var19);
         }

         float var33 = Mth.wrapDegrees(var16 - var1.x);
         float var34 = var17 - var1.y;
         float var35 = (float)Math.hypot(var33, var34);
         this.m_9852();
         float var22 = Mth.clamp(var35 / 90.0F, 0.0F, 1.0F);
         float var23 = this.f_36197 ? 0.54F : Mth.lerp(var22, this.f_36193 > 0 ? 0.54F : 0.43F, 0.78F);
         var23 *= var18;
         float var24 = Mth.lerp(var22, 13.0F, 43.0F) * this.f_36215 * var18;
         float var25 = Mth.lerp(var22, 8.0F, 24.0F) * this.f_36215 * var18;
         if (this.f_36193 > 0 && !this.f_36189) {
            var24 = Math.min(var24, 15.5F);
            var25 = Math.min(var25, 9.5F);
         }

         if (this.f_36197) {
            var24 = Math.min(var24, 2.4F);
            var25 = Math.min(var25, 1.6F);
         }

         float var26 = Mth.clamp(var33 * var23, -var24, var24);
         float var27 = Mth.clamp(var34 * var23, -var25, var25);
         if (this.f_36213 > 0 && !this.f_36197) {
            var27 *= 0.24F;
            this.f_36213--;
         }

         float var28 = this.f_36197 ? 0.72F : Mth.lerp(var22, 0.64F, 0.82F);
         if (this.f_36193 > 0 && !this.f_36189) {
            var28 = Math.max(var28, 0.8F);
         }

         float var29 = Mth.lerp(var28, this.f_36216, var26);
         float var30 = Mth.lerp(var28, this.f_36217, var27);
         float var31 = this.f_36193 > 0 ? 0.42F : (this.f_36197 ? 0.035F : 0.1F);
         float var32 = this.f_36193 > 0 ? 0.24F : (this.f_36197 ? 0.025F : 0.07F);
         var29 = this.m_9858(var29, var33, var31);
         var30 = this.m_9858(var30, var34, var32);
         this.f_36216 = var29;
         this.f_36217 = var30;
         if (this.f_36193 > 0 && !this.f_36189) {
            this.f_36193--;
         }

         return new Vector2f(var1.x + var29, Mth.clamp(var1.y + var30, -90.0F, 90.0F));
      } else {
         return var1;
      }
   }

   private float m_9868(float var1) {
      if (Math.abs(var1) > 0.05F) {
         return Math.signum(var1);
      } else {
         return this.f_36178.nextBoolean() ? 1.0F : -1.0F;
      }
   }

   private void m_9869() {
      this.f_36205 = false;
      this.f_36200 = 0.0F;
      this.f_36201 = 0.0F;
      this.f_36202 = 0.0F;
      this.f_36203 = 0;
      this.f_36204 = 1;
      this.f_36206 = LegitAim.AimPhase.CUBIC_BEZIER;
   }

   private void m_9870(Vector2f var1, float var2, float var3, boolean var4) {
      float var5 = Mth.wrapDegrees(var2 - this.f_36222);
      float var6 = var3 - this.f_36223;
      float var7 = (float)Math.hypot(var5, var6);
      float var8 = (float)Math.hypot(Mth.wrapDegrees(var2 - var1.x), var3 - var1.y);
      boolean var9 = this.f_36218 && this.f_36225 >= 0.985F && var8 > 2.5F;
      if (var4 || !this.f_36218 || var7 > 7.0F || var9) {
         this.m_9871(var1, var2, var3);
      }
   }

   private void m_9871(Vector2f var1, float var2, float var3) {
      float var4 = Mth.wrapDegrees(var2 - var1.x);
      float var5 = var3 - var1.y;
      float var6 = (float)Math.hypot(var4, var5);
      if (var6 < 0.65F) {
         this.m_9874();
      } else {
         this.f_36218 = true;
         this.f_36220 = var1.x;
         this.f_36221 = var1.y;
         this.f_36222 = var2;
         this.f_36223 = var3;
         this.f_36224 = var6;
         this.f_36225 = 0.0F;
         this.f_36219 = this.m_9864();
         this.m_9844();
         float var7 = Mth.clamp(var6 * this.m_9866(0.008F, 0.02F), 0.1F, 1.45F);
         if (this.f_36178.nextBoolean()) {
            var7 = -var7;
         }

         this.f_36230 = -var5 / var6 * var7;
         this.f_36231 = var4 / var6 * var7 * this.m_9866(0.72F, 0.96F);
      }
   }

   private Vector2f m_9872(Vector2f var1) {
      return new Vector2f(var1.x + this.f_36200, var1.y);
   }

   private float m_9873(float var1, float var2, float var3) {
      float var4 = 1.0F - var1;
      return 3.0F * var4 * var4 * var2 + 6.0F * var4 * var1 * (var3 - var2) + 3.0F * var1 * var1 * (1.0F - var3);
   }

   private void m_9874() {
      this.f_36218 = false;
      this.f_36219 = LegitAim.AimPhase.CUBIC_BEZIER;
      this.f_36220 = 0.0F;
      this.f_36221 = 0.0F;
      this.f_36222 = 0.0F;
      this.f_36223 = 0.0F;
      this.f_36224 = 0.0F;
      this.f_36225 = 0.0F;
      this.f_36226 = 0.25F;
      this.f_36227 = 0.1F;
      this.f_36228 = 0.7F;
      this.f_36229 = 0.9F;
      this.f_36230 = 0.0F;
      this.f_36231 = 0.0F;
      this.f_36232 = false;
      this.f_36233 = false;
   }

   private float m_9875(float var1) {
      if (!this.f_36218) {
         return 1.0F;
      }

      float var2 = Mth.clamp(var1, 0.0F, 1.0F);
      float var3;
      if (this.f_36219 == LegitAim.AimPhase.CUBIC_BEZIER) {
         float var4 = this.m_9865(var2);
         float var5 = this.m_9873(var4, this.f_36226, this.f_36228);
         float var6 = this.m_9873(var4, this.f_36227, this.f_36229);
         var3 = var6 / Math.max(1.0E-4F, var5);
      } else {
         var3 = this.m_9877(this.f_36219, var2);
      }

      float var7 = this.f_36232 && var1 > 0.78F ? 0.56F : 0.68F;
      return Mth.clamp(var3, var7, 1.28F);
   }

   private void m_9876() {
      this.f_36189 = false;
      this.f_36191 = 0;
      this.f_36192 = 0;
      this.f_36194 = 0.0F;
      this.f_36195 = 0.0F;
      this.f_36196 = 0.0F;
   }

   private float m_9877(LegitAim.AimPhase var1, float var2) {
      float var3 = Mth.clamp(var2, 0.0F, 1.0F);

      return switch (var1) {
         case CUBIC_BEZIER -> 1.0F;
         case SINE_IN_OUT -> (float)(f_36391 * Math.sin(f_36392 * var3));
         case QUADRATIC_IN_OUT -> var3 < 0.5F ? 4.0F * var3 : 4.0F * (1.0F - var3);
         case QUARTIC_IN_OUT -> var3 < 0.5F ? 32.0F * var3 * var3 * var3 : 32.0F * (1.0F - var3) * (1.0F - var3) * (1.0F - var3);
      };
   }

   private boolean m_9878(Entity var1) {
      if (this.f_36183 <= 0) {
         return false;
      } else if (this.m_9855(var1)) {
         this.f_36183 = 0;
         return false;
      } else {
         this.f_36183--;
         this.f_36216 = 0.0F;
         this.f_36217 = 0.0F;
         return true;
      }
   }

   private float m_9879(float var1) {
      if (!this.f_36218) {
         return var1;
      }

      if (this.f_36219 != LegitAim.AimPhase.CUBIC_BEZIER) {
         return this.m_9854(this.f_36219, var1);
      }

      float var2 = this.m_9865(Mth.clamp(var1, 0.0F, 1.0F));
      return this.m_9850(var2, this.f_36227, this.f_36229);
   }

   @Override
   public Vector2f targetAngles() {
      AttackAura var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      LivingEntity var2 = var1.getTarget();
      if (var2 != null && mc.player != null) {
         AABB var3 = var2.getBoundingBox();
         Vec3 var4 = mc.player.getEyePosition();
         return this.m_9843(new Vec3(Mth.clamp(var4.x, var3.minX, var3.maxX), Mth.clamp(var4.y, var3.minY, var3.maxY), Mth.clamp(var4.z, var3.minZ, var3.maxZ)));
      } else {
         return super.targetAngles();
      }
   }

   private record AimFrame(double x, double y, double z, float rotationDistance) {
   }

   private enum AimPhase {
      CUBIC_BEZIER,
      SINE_IN_OUT,
      QUADRATIC_IN_OUT,
      QUARTIC_IN_OUT;
   }
}
