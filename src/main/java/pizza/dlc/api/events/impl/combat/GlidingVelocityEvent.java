package pizza.dlc.api.events.impl.combat;

import lombok.Generated;
import net.minecraft.world.phys.Vec3;

public class GlidingVelocityEvent extends pizza.dlc.api.events.Event {
   private Vec3 f_11125;

   @Generated
   public void setVelocity(Vec3 var1) {
      this.f_11125 = var1;
   }

   @Generated
   public GlidingVelocityEvent(Vec3 var1) {
      this.f_11125 = var1;
   }

   @Generated
   public Vec3 getVelocity() {
      return this.f_11125;
   }
}
