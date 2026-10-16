package pizza.dlc.api.rotation.angle;

import java.security.SecureRandom;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;
import pizza.dlc.api.rotation.RotationController;

public class GrimSmoothMode extends AngleSmoothMode {
   private final SecureRandom rng = new SecureRandom();
   private final boolean instant;
   private int tick;

   public GrimSmoothMode() {
      this(false);
   }

   public GrimSmoothMode(boolean instant) {
      super("Grim");
      this.instant = instant;
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      this.tick++;
      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float dist = (float)Math.hypot(yawDelta, pitchDelta);
      float q = (float)Angle.computeGcd();
      boolean aiming = entity != null;
      if (dist < Math.max(0.2F, 3.0F * q)) {
         float sign = (this.tick & 1) == 0 ? 1.0F : -1.0F;
         float pitchOff = sign * q;
         return new Angle(currentAngle.getYaw() + yawDelta, Mth.clamp(currentAngle.getPitch() + pitchDelta + pitchOff, -90.0F, 90.0F));
      }

      if (this.instant) {
         return new Angle(currentAngle.getYaw() + yawDelta, Mth.clamp(currentAngle.getPitch() + pitchDelta, -90.0F, 90.0F));
      }

      float lineYaw = Math.abs(yawDelta / dist) * 180.0F;
      float linePitch = Math.abs(pitchDelta / dist) * 180.0F;
      float moveYaw = Mth.clamp(yawDelta, -lineYaw, lineYaw);
      float movePitch = Mth.clamp(pitchDelta, -linePitch, linePitch);
      float angleFactor = Mth.clamp(dist / 180.0F, 0.0F, 1.0F);
      float base = aiming ? (RotationController.attackReady ? 0.95F : 0.88F) : 0.45F;
      float speed = Mth.clamp(base + angleFactor * 0.35F + this.randf(-0.05F, 0.07F), 0.14F, 0.98F);
      return new Angle(
         Mth.lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw),
         Mth.clamp(Mth.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch), -90.0F, 90.0F)
      );
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }

   private float randf(float min, float max) {
      return min + this.rng.nextFloat() * (max - min);
   }
}
