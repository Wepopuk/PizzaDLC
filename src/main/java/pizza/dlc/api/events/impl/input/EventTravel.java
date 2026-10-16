package pizza.dlc.api.events.impl.input;

import net.minecraft.world.entity.LivingEntity;
import pizza.dlc.api.events.Event;

public class EventTravel extends Event {
   private LivingEntity entity;

   public LivingEntity getEntity() {
      return this.entity;
   }

   public EventTravel(LivingEntity entity) {
      this.entity = entity;
   }
}
