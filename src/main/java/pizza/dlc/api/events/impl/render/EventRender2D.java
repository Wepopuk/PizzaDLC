package pizza.dlc.api.events.impl.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.events.Event;

public class EventRender2D extends Event {
   GuiGraphicsExtractor context;
   DeltaTracker delta;

   public EventRender2D(GuiGraphicsExtractor context, DeltaTracker delta) {
      this.context = context;
      this.delta = delta;
   }

   public GuiGraphicsExtractor getContext() {
      return this.context;
   }

   public DeltaTracker getDelta() {
      return this.delta;
   }
}
