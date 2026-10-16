package pizza.dlc.api.rotation.angle;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;
import pizza.dlc.api.util.math.MathUtility;

public class SnapSmoothMode extends AngleSmoothMode {
   public SnapSmoothMode() {
      super("Snap");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawDelta = angleDelta.getYaw();
      float pitchDelta = angleDelta.getPitch();
      if (entity != null) {
         return new Angle(currentAngle.getYaw() + yawDelta, currentAngle.getPitch() + pitchDelta);
      }

      float rotationDifference = (float)Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
      if (rotationDifference < 0.01F) {
         return new Angle(currentAngle.getYaw() + yawDelta, currentAngle.getPitch() + pitchDelta);
      }

      float speed = 0.4F;
      float lineYaw = Math.abs(yawDelta / rotationDifference) * 180.0F;
      float linePitch = Math.abs(pitchDelta / rotationDifference) * 180.0F;
      float moveYaw = Mth.clamp(yawDelta, -lineYaw, lineYaw);
      float movePitch = Mth.clamp(pitchDelta, -linePitch, linePitch);
      Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
      moveAngle.setYaw(Mth.lerp(MathUtility.random(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
      moveAngle.setPitch(Mth.lerp(MathUtility.random(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch));
      return new Angle(moveAngle.getYaw(), moveAngle.getPitch());
   }

   @Override
   public Vec3 randomValue() {
      return new Vec3(0.12F, 0.12F, 0.12F);
   }
}
