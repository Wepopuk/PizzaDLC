package pizza.dlc.api.rotation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.rotation.angle.AngleSmoothMode;

public class RotationPlan implements QuickImports {
   private final Angle angle;
   private final Vec3 vec3d;
   private final Entity entity;
   private final AngleSmoothMode angleSmooth;
   private final int ticksUntilReset;
   private final float resetThreshold;
   private final boolean moveCorrection;
   private final boolean freeCorrection;

   public Angle nextRotation(Angle fromAngle, boolean isResetting) {
      return isResetting
         ? this.angleSmooth.limitAngleChange(fromAngle, AngleUtil.cameraAngle())
         : this.angleSmooth.limitAngleChange(fromAngle, this.angle, this.vec3d, this.entity);
   }

   public Angle getAngle() {
      return this.angle;
   }

   public Vec3 getVec3d() {
      return this.vec3d;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public AngleSmoothMode getAngleSmooth() {
      return this.angleSmooth;
   }

   public int getTicksUntilReset() {
      return this.ticksUntilReset;
   }

   public float getResetThreshold() {
      return this.resetThreshold;
   }

   public boolean isMoveCorrection() {
      return this.moveCorrection;
   }

   public boolean isFreeCorrection() {
      return this.freeCorrection;
   }

   public RotationPlan(
      Angle angle,
      Vec3 vec3d,
      Entity entity,
      AngleSmoothMode angleSmooth,
      int ticksUntilReset,
      float resetThreshold,
      boolean moveCorrection,
      boolean freeCorrection
   ) {
      this.angle = angle;
      this.vec3d = vec3d;
      this.entity = entity;
      this.angleSmooth = angleSmooth;
      this.ticksUntilReset = ticksUntilReset;
      this.resetThreshold = resetThreshold;
      this.moveCorrection = moveCorrection;
      this.freeCorrection = freeCorrection;
   }
}
