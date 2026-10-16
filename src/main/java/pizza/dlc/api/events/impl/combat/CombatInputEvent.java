package pizza.dlc.api.events.impl.combat;

import lombok.Generated;

public class CombatInputEvent extends pizza.dlc.api.events.Event {
   private float forward;
   private float strafe;
   private boolean forwardPressed;
   private boolean backwardPressed;
   private boolean leftPressed;
   private boolean rightPressed;
   private boolean jumping;
   private boolean sneaking;
   private boolean sprinting;

   @Generated
   public CombatInputEvent(float var1, float var2, boolean var3, boolean var4, boolean var5, boolean var6, boolean var7, boolean var8, boolean var9) {
      this.forward = var1;
      this.strafe = var2;
      this.forwardPressed = var3;
      this.backwardPressed = var4;
      this.leftPressed = var5;
      this.rightPressed = var6;
      this.jumping = var7;
      this.sneaking = var8;
      this.sprinting = var9;
   }

   @Generated
   public void setForwardPressed(boolean var1) {
      this.forwardPressed = var1;
   }

   @Generated
   public float getForward() {
      return this.forward;
   }

   @Generated
   public boolean getBackwardPressed() {
      return this.backwardPressed;
   }

   @Generated
   public void setForward(float var1) {
      this.forward = var1;
   }

   @Generated
   public boolean isSprinting() {
      return this.sprinting;
   }

   @Generated
   public void setSneaking(boolean var1) {
      this.sneaking = var1;
   }

   @Generated
   public void setSprinting(boolean var1) {
      this.sprinting = var1;
   }

   @Generated
   public void setRightPressed(boolean var1) {
      this.rightPressed = var1;
   }

   @Generated
   public boolean getForwardPressed() {
      return this.forwardPressed;
   }

   @Generated
   public void setStrafe(float var1) {
      this.strafe = var1;
   }

   @Generated
   public void setBackwardPressed(boolean var1) {
      this.backwardPressed = var1;
   }

   @Generated
   public boolean isJumping() {
      return this.jumping;
   }

   @Generated
   public void setLeftPressed(boolean var1) {
      this.leftPressed = var1;
   }

   @Generated
   public boolean isRightPressed() {
      return this.rightPressed;
   }

   @Generated
   public boolean isSneaking() {
      return this.sneaking;
   }

   @Generated
   public boolean isLeftPressed() {
      return this.leftPressed;
   }

   @Generated
   public float getStrafe() {
      return this.strafe;
   }

   @Generated
   public void setJumping(boolean var1) {
      this.jumping = var1;
   }
}
