package pizza.dlc.api.events.impl.game;

import pizza.dlc.api.events.Event;

public class EventTrace extends Event {
   private float yaw;
   private float pitch;

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public EventTrace(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }
}
