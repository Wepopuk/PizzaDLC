package pizza.dlc.client.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Trigger Bot",
   description = "Бьёт цель под прицелом автоматически",
   category = ModuleCategory.COMBAT
)
public class TriggerBot extends Module {
   private static final String V18 = "1.8";
   private static final String V19 = "1.9";
   public final MultiModeSetting targets = new MultiModeSetting(
      "Цели", this, CombatTargets.PLAYERS, CombatTargets.NAKED, CombatTargets.MOBS, CombatTargets.ANIMALS, CombatTargets.FRIENDS
   );
   public final BooleanSetting onlyCrits = new BooleanSetting("Только криты", this);
   public final ModeSetting pvp = new ModeSetting("ПВП", this, V19, V18);
   public final SliderSetting cps = new SliderSetting("КПС", this, 10.0F, 1.0F, 20.0F, 1.0F);
   private long lastAttack;

   public TriggerBot() {
      this.targets.get(CombatTargets.PLAYERS).setEnabled(true);
      this.targets.get(CombatTargets.NAKED).setEnabled(true);
      this.targets.get(CombatTargets.MOBS).setEnabled(true);
      this.cps.setVisible(() -> this.pvp.is(V18));
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null || mc.level == null || mc.gameMode == null || mc.gui.screen() != null || mc.player.isUsingItem()) return;
      if (!(mc.crosshairPickEntity instanceof LivingEntity target) || !CombatTargets.accepts(this.targets, target)) return;
      if (this.onlyCrits.isEnabled() && !this.critState()) return;
      if (!this.ready()) return;

      mc.gameMode.attack(mc.player, target);
      mc.player.swing(InteractionHand.MAIN_HAND);
      mc.player.resetAttackStrengthTicker();
      this.lastAttack = System.currentTimeMillis();
   }

   private boolean ready() {
      if (this.pvp.is(V19)) return mc.player.getAttackStrengthScale(0.5F) >= 1.0F;
      long interval = (long)(1000.0F / Math.max(1.0F, this.cps.getValue()));
      return System.currentTimeMillis() - this.lastAttack >= interval;
   }

   private boolean critState() {
      return !mc.player.onGround() && mc.player.fallDistance > 0.011F && !mc.player.isInWater() && !mc.player.isInLava();
   }
}
