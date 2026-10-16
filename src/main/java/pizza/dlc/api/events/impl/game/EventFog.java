package pizza.dlc.api.events.impl.game;

import pizza.dlc.api.events.Event;

public class EventFog extends Event {
   float distance;
   int color;

   public float getDistance() {
      return this.distance;
   }

   public int getColor() {
      return this.color;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }

   public void setColor(int color) {
      this.color = color;
   }
}
