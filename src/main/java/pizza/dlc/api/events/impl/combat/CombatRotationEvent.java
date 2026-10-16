package pizza.dlc.api.events.impl.combat;

import lombok.Generated;
import org.joml.Vector2f;

public class CombatRotationEvent extends pizza.dlc.api.events.Event {
   private final Vector2f rotation;

   @Generated
   public Vector2f getRotation() {
      return this.rotation;
   }

   @Generated
   public CombatRotationEvent(Vector2f var1) {
      this.rotation = var1;
   }
}
