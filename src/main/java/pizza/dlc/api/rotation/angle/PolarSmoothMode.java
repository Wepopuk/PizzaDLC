package pizza.dlc.api.rotation.angle;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;

public class PolarSmoothMode extends AngleSmoothMode {
   private final float aimSharpness;
   private final float returnSharpness;
   private static final float MIN_SHARPNESS = 0.05F;
   private static final float MAX_SHARPNESS = 5.0F;

   public PolarSmoothMode(float aimSharpness, float returnSharpness) {
      super("Polar");
      this.aimSharpness = Mth.clamp(aimSharpness, MIN_SHARPNESS, MAX_SHARPNESS);
      this.returnSharpness = Mth.clamp(returnSharpness, MIN_SHARPNESS, MAX_SHARPNESS);
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      if (rotationDifference < 0.001F) {
         return new Angle(targetAngle.getYaw(), targetAngle.getPitch());
      }

      float lineYaw = Math.abs(yawDelta / rotationDifference) * 180.0F;
      float linePitch = Math.abs(pitchDelta / rotationDifference) * 180.0F;
      float moveYaw = Mth.clamp(yawDelta, -lineYaw, lineYaw);
      float movePitch = Mth.clamp(pitchDelta, -linePitch, linePitch);
      float angleFactor = Mth.clamp(rotationDifference / 180.0F, 0.0F, 1.0F);
      float speed = entity != null
         ? Mth.clamp(0.72F + this.normalizeSharpness(this.aimSharpness) * 0.14F + angleFactor * 0.28F, 0.72F, 1.0F)
         : Mth.clamp(0.28F + this.normalizeSharpness(this.returnSharpness) * 0.16F + angleFactor * 0.24F, 0.28F, 0.72F);
      return new Angle(
         Mth.lerp(speed, currentAngle.getYaw(), currentAngle.getYaw() + moveYaw),
         Mth.lerp(speed, currentAngle.getPitch(), currentAngle.getPitch() + movePitch)
      );
   }

   private float normalizeSharpness(float sharpness) {
      return Mth.clamp((sharpness - MIN_SHARPNESS) / 4.95F, 0.0F, 1.0F);
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}
