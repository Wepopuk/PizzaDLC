package pizza.dlc.api.rotation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.angle.AngleSmoothMode;
import pizza.dlc.api.rotation.angle.LinearSmoothMode;

public class RotationConfig {
   public static RotationConfig DEFAULT = new RotationConfig(new LinearSmoothMode(), true, true);
   private final boolean moveCorrection;
   private final boolean freeCorrection;
   private final AngleSmoothMode angleSmooth;
   private final int resetThreshold;

   public RotationConfig(boolean moveCorrection, boolean freeCorrection) {
      this(new LinearSmoothMode(), moveCorrection, freeCorrection);
   }

   public RotationConfig(boolean moveCorrection) {
      this(new LinearSmoothMode(), moveCorrection, true);
   }

   public RotationConfig(AngleSmoothMode angleSmooth, boolean moveCorrection, boolean freeCorrection) {
      this(angleSmooth, moveCorrection, freeCorrection, 3);
   }

   public RotationConfig(AngleSmoothMode angleSmooth, boolean moveCorrection, boolean freeCorrection, int resetThreshold) {
      this.angleSmooth = angleSmooth;
      this.moveCorrection = moveCorrection;
      this.freeCorrection = freeCorrection;
      this.resetThreshold = resetThreshold;
   }

   public AngleSmoothMode getAngleSmooth() {
      return this.angleSmooth;
   }

   public RotationPlan createRotationPlan(Angle angle, Vec3 vec, Entity entity, int reset) {
      return new RotationPlan(angle, vec, entity, this.angleSmooth, reset, this.resetThreshold, this.moveCorrection, this.freeCorrection);
   }

   public RotationPlan createRotationPlan(Angle angle, Vec3 vec, Entity entity, boolean moveCorrection, boolean freeCorrection) {
      return new RotationPlan(angle, vec, entity, this.angleSmooth, 1, this.resetThreshold, moveCorrection, freeCorrection);
   }
}
