package pizza.dlc.api.events.impl.render;

import pizza.dlc.api.events.Event;

public class EventGameRender extends Event {
   private final GameRendererEvent type;

   public GameRendererEvent getType() {
      return this.type;
   }

   public EventGameRender(GameRendererEvent type) {
      this.type = type;
   }
}
