package pizza.dlc.api.rotation.combat;
import pizza.dlc.api.events.impl.combat.CombatCameraEvent;
import pizza.dlc.api.events.impl.combat.CombatMouseEvent;
import pizza.dlc.api.events.impl.combat.CombatRotationEvent;
import pizza.dlc.api.events.impl.combat.CombatSyncEvent;
import pizza.dlc.api.events.impl.combat.CombatTickEvent;
import pizza.dlc.api.events.impl.combat.FastAimEvent;
import pizza.dlc.api.events.impl.combat.MediumAimEvent;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import pizza.dlc.api.rotation.combat.CombatRotationPlans;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.rotation.combat.impl.FuntimeSnapAim;
import pizza.dlc.api.util.combat.CombatRuntime;

import pizza.dlc.client.modules.impl.combat.AttackAura;
import lombok.Generated;
import net.minecraft.util.Mth;
import org.joml.Vector2f;

public class CombatRotationState implements pizza.dlc.api.interfaces.QuickImports {
   private final Vector2f previousSmoothedRotation = new Vector2f();
   private final Vector2f smoothedRotation = new Vector2f();
   private final Vector2f visualRotation = new Vector2f();
   private final Vector2f clientRotation = new Vector2f();
   private final Vector2f rotation = new Vector2f();

   private CombatRotationPlan activePlan = null;

   private int previousTick = -1;
   private int frameTicks;

   @Generated
   public Vector2f getSmoothedRotation() {
      return this.smoothedRotation;
   }

   @Generated
   public Vector2f getPreviousSmoothedRotation() {
      return this.previousSmoothedRotation;
   }

   @meteordevelopment.orbit.EventHandler(priority = -1)
   public void onTick(CombatTickEvent var1) {
      if (pizza.dlc.api.neuro.NeuroSystem.managed()) return;
      this.previousSmoothedRotation.set(this.smoothedRotation);
      this.smoothedRotation.x = this.smoothedRotation.x + (this.visualRotation.x - this.smoothedRotation.x) * 0.5F;
      this.smoothedRotation.y = this.smoothedRotation.y + (this.visualRotation.y - this.smoothedRotation.y) * 0.5F;
      if (this.m_2661()) {
         this.m_2659();
      } else {
         this.m_2653(CombatRotationPlan.UpdateRate.SLOW, 2);
      }
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onSync(CombatSyncEvent var1) {
   }

   private void m_2653(CombatRotationPlan.UpdateRate var1, int var2) {
      CombatRotationPlans var3 = CombatRotations.plans();
      if (var1 == CombatRotationPlan.UpdateRate.SLOW) {
         var3.tick();
      }

      CombatRotationPlan var4 = var3.getActive();
      if (this.getActivePlan() == null || this.getActivePlan().getUpdateRate() == var1) {
         if (var4 == null && this.activePlan != null) {
            this.activePlan.setReturning(true);
            this.rotation.set(this.activePlan.rotate(this.rotation, this.clientRotation));
            mc.player.setYRot(this.rotation.x);
            mc.player.setXRot(this.rotation.y);
            if (var3.hasExpired(this.activePlan)) {
               this.finishRotation();
            } else {
               Vector2f var7 = new Vector2f(Mth.wrapDegrees(this.clientRotation.x), this.clientRotation.y);
               Vector2f var6 = new Vector2f(Mth.wrapDegrees(this.rotation.x), this.rotation.y);
               if (var7.distance(var6) < 5.0F) {
                  this.finishRotation();
               }
            }
         } else if (var4 != null && var4.getTargetPoint() != null) {
            if (this.activePlan != var4) {
               if (this.activePlan == null && var4.hasMovementCorrection()) {
                  this.clientRotation.set(mc.player.getYRot(), mc.player.getXRot());
               }

               this.activePlan = var4;
            }

            this.rotation.set(var4.rotate(this.rotation, var4.targetAngles()));
            CombatRotationEvent var5 = new CombatRotationEvent(this.rotation);
            CombatRuntime.post(var5);
            mc.player.setYRot(var5.getRotation().x);
            mc.player.setXRot(var5.getRotation().y);
         }
      }
   }

   @Generated
   public Vector2f getRotation() {
      return this.rotation;
   }

   @meteordevelopment.orbit.EventHandler(priority = -1)
   public void onMediumFrame(MediumAimEvent var1) {
      if (pizza.dlc.api.neuro.NeuroSystem.managed()) return;
      if (this.m_2661()) {
         this.m_2664();
      } else {
         this.m_2653(CombatRotationPlan.UpdateRate.MEDIUM, 6);
      }
   }

   public CombatRotationPlan getActivePlan() {
      CombatRotationPlan var1 = CombatRotations.plans().getActive();
      return var1 != null ? var1 : this.activePlan;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   private void onCamera(CombatCameraEvent var1) {
      if (pizza.dlc.api.neuro.NeuroSystem.managed()) return;
      AttackAura var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      if (var2 != null && var2.m_1128()) {
         float var4 = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         var2.m_1058(var4);
         var1.setYaw(var2.m_1055());
         var1.setPitch(var2.m_1132());
      } else {
         CombatRotationPlan var3 = this.activePlan != null ? this.activePlan : CombatRotations.plans().getActive();
         if (var3 != null) {
            if (var3.usesPlayerCamera()) {
               return;
            }

            var1.setYaw(this.clientRotation.x);
            var1.setPitch(this.clientRotation.y);
         }
      }
   }

   @meteordevelopment.orbit.EventHandler(priority = -1)
   public void onFastFrame(FastAimEvent var1) {
      if (pizza.dlc.api.neuro.NeuroSystem.managed()) return;
      if (!this.m_2661()) {
         this.m_2653(CombatRotationPlan.UpdateRate.FAST, 24);
      }
   }

   private void m_2659() {
      CombatRotationPlans var1 = CombatRotations.plans();
      CombatRotationPlan var2 = var1.getActive();
      if (this.getActivePlan() == null || this.getActivePlan().getUpdateRate() == CombatRotationPlan.UpdateRate.SLOW) {
         var1.tick();
         if (var2 == null && this.activePlan != null) {
            this.activePlan.setReturning(true);
            this.rotation.set(this.activePlan.rotate(this.rotation, this.clientRotation));
            mc.player.setYRot(this.rotation.x);
            mc.player.setXRot(this.rotation.y);
            if (var1.hasExpired(this.activePlan)) {
               this.finishRotation();
            } else {
               Vector2f var5 = new Vector2f(Mth.wrapDegrees(this.clientRotation.x), this.clientRotation.y);
               Vector2f var4 = new Vector2f(Mth.wrapDegrees(this.rotation.x), this.rotation.y);
               if (var5.distance(var4) < 5.0F) {
                  this.finishRotation();
               }
            }
         } else if (var2 != null && var2.getTargetPoint() != null) {
            if (this.activePlan != var2) {
               if (this.activePlan == null && var2.hasMovementCorrection()) {
                  this.clientRotation.set(mc.player.getYRot(), mc.player.getXRot());
               }

               this.activePlan = var2;
            }

            this.rotation.set(var2.rotate(this.rotation, var2.targetAngles()));
            CombatRotationEvent var3 = new CombatRotationEvent(this.rotation);
            CombatRuntime.post(var3);
            mc.player.setYRot(var3.getRotation().x);
            mc.player.setXRot(var3.getRotation().y);
         }
      }
   }

   public void finishRotation() {
      if (this.activePlan == null) return;
      CombatRotationPlans var1 = CombatRotations.plans();
      boolean var2 = this.activePlan == var1.funtimeSnap();
      boolean var3 = var1.finishExpired(this.activePlan);
      this.activePlan.reset();
      this.activePlan = null;
      if (var3) {
         mc.player.setYRot(this.clientRotation.x);
         mc.player.setXRot(this.clientRotation.y);
      } else {
         if (this.clientRotation.x != 0.0F) {
            if (var2) {
               mc.player.setYRot(this.clientRotation.x * (mc.player.getYRot() / this.clientRotation.x));
            } else {
               mc.player.setYRot(mc.player.getYRot() + Mth.wrapDegrees(this.clientRotation.x - mc.player.getYRot()));
            }
         }

         mc.player.setXRot(this.clientRotation.y);
      }
   }

   private boolean m_2661() {
      AttackAura var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      CombatRotationPlans var2 = CombatRotations.plans();
      FuntimeSnapAim var3 = var2.funtimeSnap();
      return var1 != null && var1.isEnabled() && var1.getRotationMode().is("FuntimeSnap") || this.activePlan == var3 || var2.getActive() == var3;
   }

   public void cancelPlan(CombatRotationPlan var1) {
      if (this.activePlan == var1) {
         this.activePlan = null;
         this.rotation.set(this.clientRotation);
         if (mc.player != null) {
            mc.player.setYRot(this.clientRotation.x);
            mc.player.setXRot(this.clientRotation.y);
         }
      }
   }

   @Generated
   public Vector2f getClientRotation() {
      return this.clientRotation;
   }

   private void m_2664() {
      AttackAura var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      CombatRotationPlans var2 = CombatRotations.plans();
      var2.tick();
      CombatRotationPlan var3 = var2.getActive();
      CombatRotationPlan var4 = this.activePlan != null ? this.activePlan : var2.getActive();
      if (var4 == null || var4.getUpdateRate() == CombatRotationPlan.UpdateRate.FAST || var4 == var2.hvh()) {
         if (var3 != null || this.activePlan != null) {
            if (var3 == null) {
               this.activePlan.setReturning(true);
               this.rotation.set(this.activePlan.rotate(this.rotation, this.clientRotation));
               mc.player.setYRot(this.rotation.x);
               mc.player.setXRot(this.rotation.y);
               if (var2.hasExpired(this.activePlan)) {
                  this.finishRotation();
               } else {
                  Vector2f var7 = new Vector2f(Mth.wrapDegrees(this.clientRotation.x), this.clientRotation.y);
                  Vector2f var6 = new Vector2f(Mth.wrapDegrees(this.rotation.x), this.rotation.y);
                  if (var7.distance(var6) < 5.0F) {
                     this.finishRotation();
                  }
               }
            } else if (var3.getTargetPoint() != null) {
               this.activePlan = var3;
               this.rotation.set(var3.rotate(this.rotation, var3.targetAngles()));
               CombatRotationEvent var5 = new CombatRotationEvent(this.rotation);
               CombatRuntime.post(var5);
               mc.player.setYRot(this.rotation.x);
               mc.player.setXRot(this.rotation.y);
            }
         }
      }
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   private void onMouse(CombatMouseEvent var1) {
      if (pizza.dlc.api.neuro.NeuroSystem.managed()) return;
      AttackAura var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      if (var2 != null && var2.m_1128()) {
         var1.setCancelled(true);
      } else {
         CombatRotationPlan var3 = this.activePlan != null ? this.activePlan : CombatRotations.plans().getActive();
         if (var3 != null) {
            if (var3.usesPlayerCamera()) {
               return;
            }

            this.clientRotation.x = this.clientRotation.x + var1.getY() * 0.15F;
            this.clientRotation.y = this.clientRotation.y + var1.getX() * 0.15F;
            this.clientRotation.y = Math.clamp(this.clientRotation.y, -90.0F, 90.0F);
            var1.setCancelled(true);
         } else {
            this.clientRotation.set(mc.player.getYRot(), mc.player.getXRot());
            this.rotation.set(mc.player.getYRot(), mc.player.getXRot());
         }

         this.visualRotation.x = this.visualRotation.x + var1.getY() * 0.15F;
         this.visualRotation.y = this.visualRotation.y + var1.getX() * 0.15F;
         this.visualRotation.y = Math.clamp(this.clientRotation.y, -90.0F, 90.0F);
      }
   }

   @Generated
   public Vector2f getVisualRotation() {
      return this.visualRotation;
   }

   public void resetForClient() {
      if (mc.player != null && this.activePlan != null) this.finishRotation();
      this.activePlan = null;
      if (mc.player != null) {
         this.rotation.set(mc.player.getYRot(), mc.player.getXRot());
         this.clientRotation.set(this.rotation);
         this.visualRotation.set(this.rotation);
         this.smoothedRotation.set(this.rotation);
         this.previousSmoothedRotation.set(this.rotation);
      }
   }
}
