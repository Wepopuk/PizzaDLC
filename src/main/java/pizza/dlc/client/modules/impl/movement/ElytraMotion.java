package pizza.dlc.client.modules.impl.movement;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.client.modules.impl.combat.AttackAura;

import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.impl.combat.ElytraTarget;
import pizza.dlc.api.util.combat.CombatMath;
import pizza.dlc.api.events.impl.combat.GlidingVelocityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ElytraMotion extends Module {
   private final CombatSliderSetting stopDistance;
   private final BooleanSetting restoreSpeed;
   private Vec3 savedVelocity;
   private int flightGraceTicks;
   private LivingEntity flightTarget;
   public void preserveFlightForTargetSwitch() {
      if (savedVelocity != null && mc.player != null) { mc.player.setDeltaMovement(savedVelocity); savedVelocity=null; }
      flightGraceTicks=10;
      flightTarget=pizza.dlc.client.modules.impl.combat.CombatTarget.current();
   }
   @meteordevelopment.orbit.EventHandler private void tick(pizza.dlc.api.events.impl.combat.CombatTickEvent event) { if (flightGraceTicks>0) flightGraceTicks--; }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   private void onGlidingVelocity(GlidingVelocityEvent var1) {
      if (flightGraceTicks>0) return;
      LivingEntity var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class).getTarget();
      ElytraTarget var3 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
      if (var2==flightTarget && var2!=null && var2.onGround()) return;
      if (var2!=flightTarget || var2==null || !var2.onGround()) flightTarget=null;
      if (var2 != null
         && mc.player.isFallFlying()
         && mc.player.getEyePosition().distanceTo(var2.getBoundingBox().getCenter().add(0.0, (var2.getY() - var2.yo) * 2.0, 0.0))
            < this.stopDistance.getValue()
         && (CombatMath.blocksPerSecond(var2) < var3.getPredictionBps().getValue() || !var3.getOvertake().isEnabled())) {
         if (this.restoreSpeed.isEnabled() && this.savedVelocity == null) {
            this.savedVelocity = var1.getVelocity();
         }

         var1.setVelocity(Vec3.ZERO);
      } else if (this.restoreSpeed.isEnabled() && this.savedVelocity != null) {
         var1.setVelocity(this.savedVelocity);
         this.savedVelocity = null;
      }
   }

   public ElytraMotion() {
      super("ElytraMotion", "Останавливает полёт рядом с целью", ModuleCategory.MOVEMENT);
      this.stopDistance = new CombatSliderSetting("Дистанция", this, 3.0F, 1.0F, 5.0F).withStep(0.1F);
      this.restoreSpeed = CombatSettings.bool("Возвращать скорость", this, true);
      this.savedVelocity = null;

      this.getSettings().clear();
      this.addSettings(this.stopDistance, this.restoreSpeed);
   }

   public void resetState() {
      this.savedVelocity = null;
      flightGraceTicks=0;flightTarget=null;
   }

   @Override public void onEnable() {
      super.onEnable();
      CombatRuntime.enabled(this);
   }
   @Override public void onDisable() {
      try { resetState(); } finally { CombatRuntime.disabled(this); super.onDisable(); }
   }
}
