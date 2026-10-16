package pizza.dlc.api.rotation.angle;

import java.security.SecureRandom;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.Angle;

public class IntaveSmoothMode extends AngleSmoothMode {
   private final SecureRandom rng = new SecureRandom();
   private float wanderMag = 3.0F;
   private float wanderAng = 0.0F;
   private static final float MAX_YAW_STEP = 20.0F;
   private static final float MAX_PITCH_STEP = 22.0F;
   private static final float WANDER_MIN = 2.0F;
   private static final float WANDER_MAX = 4.5F;
   private static final float PITCH_SCALE = 1.2F;

   public IntaveSmoothMode() {
      super("Intave");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      boolean aiming = entity != null;
      float aimYaw = targetAngle.getYaw();
      float aimPitch = targetAngle.getPitch();
      if (aiming) {
         this.wanderMag = Mth.clamp(this.wanderMag + this.randf(-0.35F, 0.35F), WANDER_MIN, WANDER_MAX);
         this.wanderAng = this.wanderAng + this.randf(0.2F, 0.45F);
         aimYaw += this.wanderMag * (float)Math.cos(this.wanderAng);
         aimPitch += this.wanderMag * PITCH_SCALE * (float)Math.sin(this.wanderAng);
      }

      aimPitch = Mth.clamp(aimPitch, -90.0F, 90.0F);
      float dYaw = Mth.wrapDegrees(aimYaw - currentAngle.getYaw());
      float dPitch = Mth.wrapDegrees(aimPitch - currentAngle.getPitch());
      float dist = (float)Math.hypot(dYaw, dPitch);
      if (dist < 1.0E-4F) {
         return new Angle(aimYaw, aimPitch);
      }

      float angleFactor = Mth.clamp(dist / 90.0F, 0.0F, 1.0F);
      float base = aiming ? 0.55F : 0.3F;
      float speed = Mth.clamp(base + angleFactor * 0.25F + this.randf(-0.05F, 0.06F), 0.12F, 0.9F);
      float stepYaw = Mth.clamp(dYaw * speed, -MAX_YAW_STEP, MAX_YAW_STEP);
      float stepPitch = Mth.clamp(dPitch * speed, -MAX_PITCH_STEP, MAX_PITCH_STEP);
      return new Angle(currentAngle.getYaw() + stepYaw, Mth.clamp(currentAngle.getPitch() + stepPitch, -90.0F, 90.0F));
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }

   private float randf(float min, float max) {
      return min + this.rng.nextFloat() * (max - min);
   }
}
