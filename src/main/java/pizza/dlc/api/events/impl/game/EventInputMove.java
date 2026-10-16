package pizza.dlc.api.events.impl.game;

import net.minecraft.util.Mth;
import pizza.dlc.api.events.Event;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.util.game.PlayerUtility;

public class EventInputMove extends Event implements QuickImports {
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneaking;
   private boolean sprint;

   public boolean isMoving() {
      return this.getForward() != 0.0F || this.getStrafe() != 0.0F;
   }

   public void setYaw(float yaw, float direction) {
      float forward = this.getForward();
      float strafe = this.getStrafe();
      double angle = Mth.wrapDegrees(Math.toDegrees(PlayerUtility.direction(direction, forward, strafe)));
      if (forward != 0.0F || strafe != 0.0F) {
         float closestForward = 0.0F;
         float closestStrafe = 0.0F;
         float closestDifference = Float.MAX_VALUE;

         for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
               if (predictedStrafe == 0.0F && predictedForward == 0.0F) continue;

               double predictedAngle = Mth.wrapDegrees(Math.toDegrees(PlayerUtility.direction(yaw, predictedForward, predictedStrafe)));
               double difference = Math.abs(Mth.wrapDegrees(angle - predictedAngle));
               if (difference < closestDifference) {
                  closestDifference = (float)difference;
                  closestForward = predictedForward;
                  closestStrafe = predictedStrafe;
               }
            }
         }

         this.setForward(closestForward);
         this.setStrafe(closestStrafe);
      }
   }

   public void setYaw(float yaw) {
      this.setYaw(yaw, mc.player.getYRot());
   }

   public float getForward() {
      return this.forward;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public boolean isJump() {
      return this.jump;
   }

   public boolean isSneaking() {
      return this.sneaking;
   }

   public boolean isSprint() {
      return this.sprint;
   }

   public void setForward(float forward) {
      this.forward = forward;
   }

   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   public void setJump(boolean jump) {
      this.jump = jump;
   }

   public void setSneaking(boolean sneaking) {
      this.sneaking = sneaking;
   }

   public void setSprint(boolean sprint) {
      this.sprint = sprint;
   }

   public EventInputMove(float forward, float strafe, boolean jump, boolean sneaking, boolean sprint) {
      this.forward = forward;
      this.strafe = strafe;
      this.jump = jump;
      this.sneaking = sneaking;
      this.sprint = sprint;
   }
}
