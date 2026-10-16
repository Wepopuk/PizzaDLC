package pizza.dlc.api.events.impl.combat;

import lombok.Generated;

public class CombatKeyEvent extends pizza.dlc.api.events.Event {
   private final int key;
   private final int action;

   @Generated
   public int getKey() {
      return this.action;
   }

   @Generated
   public CombatKeyEvent(int var1, int var2) {
      this.key = var1;
      this.action = var2;
   }

   @Generated
   public int getAction() {
      return this.key;
   }
}
