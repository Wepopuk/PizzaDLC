package pizza.dlc.api.events.impl.combat;

import lombok.Generated;

public class CombatMouseEvent extends pizza.dlc.api.events.Event {
   private float x;
   private float y;

   @Generated
   public CombatMouseEvent(float var1, float var2) {
      this.x = var1;
      this.y = var2;
   }

   @Generated
   public float getX() {
      return this.y;
   }

   @Generated
   public void setX(float var1) {
      this.x = var1;
   }

   @Generated
   public void setY(float var1) {
      this.y = var1;
   }

   @Generated
   public float getY() {
      return this.x;
   }
}
