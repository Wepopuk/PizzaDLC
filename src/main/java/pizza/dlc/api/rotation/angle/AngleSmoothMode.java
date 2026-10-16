package pizza.dlc.api.rotation.angle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.rotation.Angle;

public abstract class AngleSmoothMode implements QuickImports {
   private final String name;

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle) {
      return this.limitAngleChange(currentAngle, targetAngle, null, null);
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d) {
      return this.limitAngleChange(currentAngle, targetAngle, vec3d, null);
   }

   public abstract Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 targetPoint, Entity entity);

   public abstract Vec3 randomValue();

   public String getName() {
      return this.name;
   }

   public AngleSmoothMode(String name) {
      this.name = name;
   }
}
