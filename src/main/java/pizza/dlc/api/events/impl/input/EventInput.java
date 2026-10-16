package pizza.dlc.api.events.impl.input;

import pizza.dlc.api.events.Event;

public class EventInput extends Event {
   int key;
   int scancode;
   boolean released;

   public boolean isPressed(int key) {
      return !this.released && key == this.key && key != -1;
   }

   public boolean isReleased(int key) {
      return this.released && key == this.key && key != -1;
   }

   public int getKey() {
      return this.key;
   }

   public int getScancode() {
      return this.scancode;
   }

   public boolean isReleased() {
      return this.released;
   }

   public EventInput(int key, int scancode, boolean released) {
      this.key = key;
      this.scancode = scancode;
      this.released = released;
   }
}
