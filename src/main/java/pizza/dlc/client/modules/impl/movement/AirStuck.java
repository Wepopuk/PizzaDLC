package pizza.dlc.client.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import pizza.dlc.api.events.impl.game.EventMotion;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.events.impl.input.EventTravel;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Air Stuck",
   category = ModuleCategory.MOVEMENT,
   description = "Замораживает в воздухе на крит-момент"
)
public class AirStuck extends Module {
   private static final double FREEZE_FALL_DISTANCE = 0.12;
   private static final int MAX_ROTATION_PACKETS = 12;
   private final BooleanSetting changeKillauraDistance = new BooleanSetting("Изменять дистанцию киллауры", this);
   private final SliderSetting killauraDistance = new SliderSetting("Дистанция киллауры", this, 3.5F, 2.0F, 6.0F, 0.1F);
   private final BooleanSetting freezeMode = new BooleanSetting("Ловить момент", this);
   private final BooleanSetting sendRotation = new BooleanSetting("Слать ротацию", this);
   private double maxY = Double.NaN;
   private boolean frozen = false;
   private int rotationPackets;

   public AirStuck() {
      this.freezeMode.setEnabled(true);
      this.sendRotation.setEnabled(true);
      this.killauraDistance.setVisible(this.changeKillauraDistance::isEnabled);
   }

   @Override
   public void onEnable() {
      this.maxY = Double.NaN;
      this.rotationPackets = 0;
      this.frozen = !this.freezeMode.isEnabled();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.frozen = false;
      super.onDisable();
   }

   public float getCustomKillauraDistance() {
      return this.isEnabled() && this.changeKillauraDistance.isEnabled() && mc.player != null && !mc.player.onGround()
         ? this.killauraDistance.getValue()
         : -1.0F;
   }

   public boolean isCritMoment() {
      return this.isEnabled() && this.freezeMode.isEnabled() && this.frozen && mc.player != null && !mc.player.onGround();
   }

   @EventHandler
   public void onTravel(EventTravel e) {
      if (mc.player == null || e.getEntity() != mc.player) return;

      if (!this.frozen && this.freezeMode.isEnabled()) {
         this.trackFall();
      }

      if (this.frozen) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void onMotion(EventMotion e) {
      if (!this.frozen || mc.player == null) return;

      e.setCancelled(true);
      if (this.sendRotation.isEnabled() && this.rotationPackets < MAX_ROTATION_PACKETS) {
         e.setRotationOnly(true);
         e.addPostAction(() -> this.rotationPackets++);
      }
   }

   @EventHandler
   public void onWorldLoad(EventWorldLoad e) {
      this.setEnabled(false);
   }

   private void trackFall() {
      if (mc.player.onGround()) {
         this.maxY = Double.NaN;
         return;
      }

      double y = mc.player.getY();
      if (Double.isNaN(this.maxY) || y > this.maxY) {
         this.maxY = y;
      } else if (y <= this.maxY - FREEZE_FALL_DISTANCE) {
         this.frozen = true;
         this.rotationPackets = 0;
      }
   }
}
