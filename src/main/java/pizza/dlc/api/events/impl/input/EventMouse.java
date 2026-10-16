package pizza.dlc.api.events.impl.input;

import pizza.dlc.api.events.Event;

public class EventMouse extends Event {
   private final int button;
   private final int action;

   public int getButton() {
      return this.button;
   }

   public int getAction() {
      return this.action;
   }

   public EventMouse(int button, int action) {
      this.button = button;
      this.action = action;
   }
}
