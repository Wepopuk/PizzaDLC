package pizza.dlc.api.rotation.angle;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;

public class ElytraSmoothMode extends AngleSmoothMode {
   private static final float EASE = 1.0F;

   public ElytraSmoothMode() {
      super("Elytra");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
      float yawStep = Mth.clamp(delta.getYaw() * EASE, -180.0F, 180.0F);
      float pitchStep = Mth.clamp(delta.getPitch() * EASE, -180.0F, 180.0F);
      return new Angle(currentAngle.getYaw() + yawStep, Mth.clamp(currentAngle.getPitch() + pitchStep, -90.0F, 90.0F));
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}
