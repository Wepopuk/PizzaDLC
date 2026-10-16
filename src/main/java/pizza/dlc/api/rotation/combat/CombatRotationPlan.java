package pizza.dlc.api.rotation.combat;

import lombok.Generated;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class CombatRotationPlan implements pizza.dlc.api.interfaces.QuickImports {
   private CombatRotationPlan.UpdateRate updateRate = CombatRotationPlan.UpdateRate.SLOW;
   private boolean playerCamera = false;
   private boolean movementCorrection = false;
   protected boolean returning = false;
   protected Vec3 targetPoint;

   @Generated
   public boolean isReturning() {
      return this.returning;
   }

   public Vector2f targetAngles() {
      Vec3 var1 = mc.player.getEyePosition();
      Vec3 var2 = this.targetPoint.subtract(var1).normalize();
      float var3 = (float)Math.toDegrees(Math.atan2(-var2.x, var2.z));
      float var4 = (float)Math.toDegrees(Math.asin(var2.y));
      return new Vector2f(var3, -var4);
   }

   @Generated
   public void setReturning(boolean var1) {
      this.returning = var1;
   }

   @Generated
   public Vec3 getTargetPoint() {
      return this.targetPoint;
   }

   public void setTargetPoint(Vec3 var1) {
      this.targetPoint = var1;
      this.returning = false;
   }

   public void prepare() {
   }

   @Generated
   public CombatRotationPlan.UpdateRate getUpdateRate() {
      return this.updateRate;
   }

   public Vector2f rotate(Vector2f var1, Vector2f var2) {
      return new Vector2f();
   }

   @Generated
   public boolean hasMovementCorrection() {
      return this.movementCorrection;
   }

   public CombatRotationPlan(Vec3 var1) {
      this.targetPoint = var1;
   }

   @Generated
   public void setUpdateRate(CombatRotationPlan.UpdateRate var1) {
      this.updateRate = var1;
   }

   public void reset() {
   }

   @Generated
   public void setMovementCorrection(boolean var1) {
      this.movementCorrection = var1;
   }

   public Vector2f m_4621(Vector2f var1, Vector2f var2) {
      return new Vector2f();
   }

   @Generated
   public boolean usesPlayerCamera() {
      return this.playerCamera;
   }

   public CombatRotationPlan(Vec3 var1, CombatRotationPlan.UpdateRate var2) {
      this.targetPoint = var1;
      this.updateRate = var2;
   }

   @Generated
   public void setPlayerCamera(boolean var1) {
      this.playerCamera = var1;
   }

   public enum UpdateRate {
      SLOW,
      MEDIUM,
      FAST;
   }
}
