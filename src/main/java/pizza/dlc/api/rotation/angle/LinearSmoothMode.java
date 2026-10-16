package pizza.dlc.api.rotation.angle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;

public class LinearSmoothMode extends AngleSmoothMode {
   public LinearSmoothMode() {
      super("Linear");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      if (rotationDifference < 1.0E-4F) {
         return targetAngle;
      }

      float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
      float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;
      return new Angle(
         currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw),
         currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch)
      );
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.0, 0.0, 0.0);
   }
}
