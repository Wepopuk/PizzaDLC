package pizza.dlc.api.events;

import pizza.dlc.PizzaDLC;

public class Event {
   boolean cancelled;

   public <T extends Event> T call() {
      PizzaDLC.getInstance().getBus().post(this);
      return (T)this;
   }

   public boolean isCancelled() {
      return this.cancelled;
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }
}
