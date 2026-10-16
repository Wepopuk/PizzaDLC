package pizza.dlc.api.rotation.combat.neuro;
import pizza.dlc.api.rotation.combat.neuro.AimFeatureSchema;

import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AimFeatures implements pizza.dlc.api.interfaces.QuickImports {

   private static final Vec3 f_18938 = new Vec3(0.0, 1.0, 0.0);
   private double f_18939;
   private UUID f_18940;
   private Object f_18941;
   private AimFeatures.AimSample f_18942;

   public AimFeatures() {
      this.f_18939 = 0.82F;
   }

   public AimFeatures.FeatureFrame m_6333(LocalPlayer var1, LivingEntity var2, float var3, long var4, float var6, float var7, boolean var8) {
      if (Float.isFinite(var6) && Float.isFinite(var7) && !(var7 < -90.0F) && !(var7 > 90.0F) && Float.isFinite(var3)) {
         ClientLevel var9 = mc.level;
         AimFeatures.AimSample var10 = AimFeatures.AimSample.capture(var1, var2, var3, this.f_18939, var4, var6, var7);
         UUID var11 = var2.getUUID();
         if (var11.equals(this.f_18940) && this.f_18942 != null && var9 == this.f_18941) {
            long var12 = var10.nanoTime - this.f_18942.nanoTime;
            if (var12 > 0L && var12 >= 8000000L) {
               double var14 = var12 / 1.0E9;
               Vec3 var16 = var10.targetPoint.subtract(this.f_18942.targetPoint);
               Vec3 var17 = var10.playerEye.subtract(this.f_18942.playerEye);
               float var18 = Mth.wrapDegrees(var10.playerYaw - this.f_18942.playerYaw);
               float var19 = var10.playerPitch - this.f_18942.playerPitch;
               if (Double.isFinite(var14)
                  && !(var14 < 0.008)
                  && !(var14 > 0.03334)
                  && !(var16.length() > 2.0)
                  && !(var17.length() > 2.0)
                  && !(Math.abs(var18) > 90.0F)
                  && !(Math.abs(var19) > 60.0F)) {
                  AimFeatures.RelativeVelocity var20 = AimFeatures.RelativeVelocity.m_6330(var16, var17, var18, var19, var14);
                  AimFeatures.ViewBasis var21 = AimFeatures.ViewBasis.m_6331(var10.playerYaw, var10.playerPitch);
                  Vec3 var22 = var10.targetPoint.subtract(var10.playerEye);
                  Vec3 var23 = var21.project(var22);
                  Vec3 var24 = var21.project(var20.targetVelocity.subtract(var20.playerVelocity));
                  double var25 = Math.toRadians(Mth.wrapDegrees(var2.getYRot(var3) - var10.playerYaw));
                  AimFeatures.FeatureWriter var27 = new AimFeatures.FeatureWriter();
                  var27.m_6329("sample_dt_ms", var14 * 1000.0);
                  var27.m_6327("world_tick_changed", var10.worldTime != this.f_18942.worldTime);
                  var27.m_6329("world_tick_phase", Mth.clamp(var3, 0.0F, 1.0F));
                  var27.m_6328("rel_pos", var23);
                  var27.m_6329("distance", var22.length());
                  var27.m_6328("rel_vel", var24);
                  var27.m_6329("aim_error_yaw", Mth.wrapDegrees(var10.idealYaw - var10.playerYaw));
                  var27.m_6329("aim_error_pitch", var10.idealPitch - var10.playerPitch);
                  var27.m_6329("yaw_velocity", var20.yawVelocity);
                  var27.m_6329("pitch_velocity", var20.pitchVelocity);
                  var27.m_6329("target_relative_yaw_sin", Math.sin(var25));
                  var27.m_6329("target_relative_yaw_cos", Math.cos(var25));
                  var27.m_6327("target_on_ground", var2.onGround());
                  var27.m_6327("target_sprinting", var2.isSprinting());
                  var27.m_6327("target_sneaking", var2.isShiftKeyDown());
                  var27.m_6329("target_hurt_time", var2.hurtTime);
                  var27.m_6327("player_on_ground", var1.onGround());
                  var27.m_6327("player_sprinting", var1.isSprinting());
                  var27.m_6327("player_sneaking", var1.isShiftKeyDown());
                  var27.m_6327("input_forward", mc.options.keyUp.isDown());
                  var27.m_6327("input_back", mc.options.keyDown.isDown());
                  var27.m_6327("input_left", mc.options.keyLeft.isDown());
                  var27.m_6327("input_right", mc.options.keyRight.isDown());
                  var27.m_6327("input_jump", mc.options.keyJump.isDown());
                  var27.m_6327("input_attack", mc.options.keyAttack.isDown());
                  var27.m_6327("exact_crosshair", var8);
                  this.f_18942 = var10;
                  return new AimFeatures.FeatureFrame(var27.m_6326(), var10.playerYaw, var10.playerPitch, false);
               } else {
                  this.f_18942 = var10;
                  return AimFeatures.FeatureFrame.reset();
               }
            } else {
               return AimFeatures.FeatureFrame.skip();
            }
         } else {
            this.f_18940 = var11;
            this.f_18941 = var9;
            this.f_18942 = var10;
            return AimFeatures.FeatureFrame.reset();
         }
      } else {
         throw new IllegalArgumentException("Invalid observed look or partial tick");
      }
   }

   public void m_6334(double var1) {
      if (Double.isFinite(var1) && !(var1 < 0.0) && !(var1 > 1.0)) {
         if (Double.compare(var1, this.f_18939) != 0) {
            this.f_18939 = var1;
            this.m_6335();
         }
      } else {
         throw new IllegalArgumentException("heightRatio must be within 0..1");
      }
   }

   public void m_6335() {
      this.f_18940 = null;
      this.f_18941 = null;
      this.f_18942 = null;
   }

   public static Vec3 m_6336(LivingEntity var0, float var1, double var2) {
      Vec3 var4 = var0.getPosition(var1);
      return new Vec3(var4.x, var4.y + var0.getBbHeight() * var2, var4.z);
   }

   public AimFeatures.FeatureFrame m_6337(LocalPlayer var1, LivingEntity var2, float var3, long var4) {
      return this.m_6333(var1, var2, var3, var4, var1.getYRot(), var1.getXRot(), mc.crosshairPickEntity == var2);
   }

   private static final class FeatureWriter {
      public final float[] f_18930 = new float[AimFeatureSchema.f_15738];
      public int f_18931;

      public float[] m_6326() {
         if (this.f_18931 != this.f_18930.length) {
            throw new IllegalStateException("" + this.f_18931 + this.f_18930.length);
         } else {
            return this.f_18930;
         }
      }

      public void m_6327(String var1, boolean var2) {
         this.m_6329(var1, var2 ? 1.0 : 0.0);
      }

      public void m_6328(String var1, Vec3 var2) {
         this.m_6329(var1 + "", var2.x);
         this.m_6329(var1 + "", var2.y);
         this.m_6329(var1 + "", var2.z);
      }

      public FeatureWriter() {
      }

      public void m_6329(String var1, double var2) {
         if (this.f_18931 < this.f_18930.length && AimFeatureSchema.f_15737[this.f_18931].equals(var1)) {
            float var4 = (float)var2;
            if (Double.isFinite(var2) && Float.isFinite(var4)) {
               this.f_18930[this.f_18931++] = var4;
            } else {
               throw new IllegalArgumentException(var1 + "");
            }
         } else {
            throw new IllegalStateException(this.f_18931 + var1);
         }
      }
   }

   private record RelativeVelocity(Vec3 targetVelocity, Vec3 playerVelocity, float yawVelocity, float pitchVelocity) {
      public static AimFeatures.RelativeVelocity m_6330(Vec3 var0, Vec3 var1, float var2, float var3, double var4) {
         double var6 = 1.0 / var4;
         return new AimFeatures.RelativeVelocity(var0.scale(var6), var1.scale(var6), (float)(var2 * var6), (float)(var3 * var6));
      }
   }

   public record FeatureFrame(float[] features, float playerYaw, float playerPitch, boolean resetHistory) {
      public static AimFeatures.FeatureFrame reset() {
         return new AimFeatures.FeatureFrame(null, 0.0F, 0.0F, true);
      }

      public static AimFeatures.FeatureFrame skip() {
         return new AimFeatures.FeatureFrame(null, 0.0F, 0.0F, false);
      }
   }

   private record ViewBasis(Vec3 right, Vec3 up, Vec3 forward) {
      public static AimFeatures.ViewBasis m_6331(float var0, float var1) {
         double var2 = Math.toRadians(var0);
         double var4 = Math.toRadians(var1);
         Vec3 var6 = new Vec3(-Math.sin(var2), 0.0, Math.cos(var2)).normalize();
         Vec3 var7 = var6.cross(AimFeatures.f_18938).normalize();
         Vec3 var8 = new Vec3(var6.x * Math.cos(var4), -Math.sin(var4), var6.z * Math.cos(var4)).normalize();
         return new AimFeatures.ViewBasis(var7, var7.cross(var8).normalize(), var8);
      }

      public Vec3 m_6332() {
         return this.up;
      }

      public Vec3 project(Vec3 var1) {
         return new Vec3(var1.dot(this.right), var1.dot(this.up), var1.dot(this.forward));
      }
   }

   private record AimSample(
      long nanoTime, long worldTime, Vec3 playerEye, Vec3 targetPoint, float playerYaw, float playerPitch, float idealYaw, float idealPitch
   ) {
      public static AimFeatures.AimSample capture(
         LocalPlayer var0, LivingEntity var1, float var2, double var3, long var5, float var7, float var8
      ) {
         Vec3 var9 = var0.getEyePosition(var2);
         Vec3 var10 = AimFeatures.m_6336(var1, var2, var3);
         Vec3 var11 = var10.subtract(var9);
         float var12 = (float)Math.toDegrees(Math.atan2(var11.z, var11.x)) - 90.0F;
         float var13 = (float)(-Math.toDegrees(Math.atan2(var11.y, Math.hypot(var11.x, var11.z))));
         return new AimFeatures.AimSample(var5, pizza.dlc.api.interfaces.QuickImports.mc.level.getGameTime(), var9, var10, var7, var8, var12, var13);
      }
   }
}
