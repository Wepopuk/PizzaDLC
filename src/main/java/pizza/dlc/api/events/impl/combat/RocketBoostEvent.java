package pizza.dlc.api.events.impl.combat;

import lombok.Generated;
import net.minecraft.world.entity.LivingEntity;

public class RocketBoostEvent extends pizza.dlc.api.events.Event {
   private LivingEntity entity;
   private float xPower;
   private float yPower;
   private float zPower;

   @Generated
   public float getZPower() {
      return this.zPower;
   }

   @Generated
   public LivingEntity getEntity() {
      return this.entity;
   }

   @Generated
   public void setZPower(float var1) {
      this.xPower = var1;
   }

   public RocketBoostEvent(float var1, float var2, float var3) {
      this(null, var1, var2, var3);
   }

   public RocketBoostEvent(LivingEntity var1, float var2, float var3, float var4) {
      this.entity = var1;
      this.xPower = var2;
      this.yPower = var3;
      this.zPower = var4;
   }

   @Generated
   public void setEntity(LivingEntity var1) {
      this.entity = var1;
   }

   @Generated
   public void setXPower(float var1) {
      this.yPower = var1;
   }

   @Generated
   public void setYPower(float var1) {
      this.zPower = var1;
   }

   @Generated
   public float getYPower() {
      return this.yPower;
   }

   @Generated
   public float getXPower() {
      return this.xPower;
   }
}
