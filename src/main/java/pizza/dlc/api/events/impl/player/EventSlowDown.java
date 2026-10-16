package pizza.dlc.api.events.impl.player;

import pizza.dlc.api.events.Event;

public class EventSlowDown extends Event {
   private Type type;

   public Type getType() {
      return this.type;
   }

   public void setType(Type type) {
      this.type = type;
   }

   public EventSlowDown(Type type) {
      this.type = type;
   }

   public static enum Type {
      SPRINT,
      ITEM,
      WEB,
      SOUL_SAND;
   }
}
