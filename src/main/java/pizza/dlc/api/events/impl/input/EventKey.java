package pizza.dlc.api.events.impl.input;

import pizza.dlc.api.events.Event;

public class EventKey extends Event {
   private final int key;
   private final int action;

   public int getKey() {
      return this.key;
   }

   public int getAction() {
      return this.action;
   }

   public EventKey(int key, int action) {
      this.key = key;
      this.action = action;
   }
}
