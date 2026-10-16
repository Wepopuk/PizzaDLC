package pizza.dlc.api.events.impl.game;

import net.minecraft.world.inventory.ContainerInput;
import pizza.dlc.api.events.Event;

public class EventClickSlot extends Event {
   private final ContainerInput containerInput;
   private final int slot;
   private final int button;
   private final int id;

   public ContainerInput getContainerInput() {
      return this.containerInput;
   }

   public int getSlot() {
      return this.slot;
   }

   public int getButton() {
      return this.button;
   }

   public int getId() {
      return this.id;
   }

   public EventClickSlot(ContainerInput containerInput, int slot, int button, int id) {
      this.containerInput = containerInput;
      this.slot = slot;
      this.button = button;
      this.id = id;
   }
}
