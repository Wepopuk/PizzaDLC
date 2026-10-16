package pizza.dlc.client.modules.impl.movement;

import pizza.dlc.mixin.client.LocalPlayerSprintAccessor;
import meteordevelopment.orbit.EventHandler;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "AutoSprint",
   category = ModuleCategory.MOVEMENT
)
public class AutoSprint extends Module {
   private boolean canSprint = true;
   private boolean auraStop = true;
   private int tickStop = 0;

   @EventHandler
   public void onTick(EventTick e) {
      if (this.tickStop > 0) {
         this.tickStop--;
      }

      if (mc.player != null
         && this.isSprintAllowed()
         && mc.player.zza > 0.0F
         && !this.hardWallCollision()
         && ((LocalPlayerSprintAccessor)mc.player).pizzadlc$canStartSprinting()) {
         mc.player.setSprinting(true);
      }
   }

   private boolean hardWallCollision() {
      return mc.player.horizontalCollision && !mc.player.minorHorizontalCollision;
   }

   public boolean isSprintAllowed() {
      return this.auraStop && this.canSprint && this.tickStop <= 0;
   }

   public void setCanSprint(boolean canSprint) {
      this.canSprint = canSprint;
   }

   public void setAuraStop(boolean auraStop) {
      this.auraStop = auraStop;
   }

   public void setTickStop(int tickStop) {
      this.tickStop = tickStop;
   }

   public boolean isCanSprint() {
      return this.canSprint;
   }

   public boolean isAuraStop() {
      return this.auraStop;
   }

   public int getTickStop() {
      return this.tickStop;
   }
}
