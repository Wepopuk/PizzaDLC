package pizza.dlc.api.events.impl.game;

import pizza.dlc.api.events.Event;

public class EventMessage extends Event {
   final String message;

   public String getMessage() {
      return this.message;
   }

   public EventMessage(String message) {
      this.message = message;
   }
}
