package pizza.dlc.api.events.impl.combat;

import lombok.Generated;

public class CombatCameraEvent extends pizza.dlc.api.events.Event {
   private float yaw;
   private float pitch;

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public CombatCameraEvent(float var1, float var2) {
      this.yaw = var1;
      this.pitch = var2;
   }

   @Generated
   public void setYaw(float var1) {
      this.yaw = var1;
   }

   @Generated
   public void setPitch(float var1) {
      this.pitch = var1;
   }

   @Generated
   public float getPitch() {
      return this.pitch;
   }
}
