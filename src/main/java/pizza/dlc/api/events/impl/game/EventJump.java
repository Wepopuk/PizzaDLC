package pizza.dlc.api.events.impl.game;

import pizza.dlc.api.events.Event;

public class EventJump extends Event {
   private float motion;
   private float yaw;

   public float getMotion() {
      return this.motion;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setMotion(float motion) {
      this.motion = motion;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public EventJump(float motion, float yaw) {
      this.motion = motion;
      this.yaw = yaw;
   }
}
