package pizza.dlc.client.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.player.EventSlowDown;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "NoSlow",
   category = ModuleCategory.MOVEMENT
)
public class NoSlow extends Module {
   private static final String VANILLA = "Vanilla";
   private static final String GRIM = "Grim";
   private static final String MATRIX = "Matrix";
   private final ModeSetting mode = new ModeSetting("Режим", this, VANILLA, GRIM, MATRIX);
   private boolean fastTick;

   @EventHandler
   public void onSlowDown(EventSlowDown e) {
      switch (this.mode.get()) {
         case VANILLA:
         case MATRIX:
            e.setCancelled(true);
            break;
         case GRIM:
            if (e.getType() == EventSlowDown.Type.ITEM) {
               this.fastTick = !this.fastTick;
               if (this.fastTick) {
                  e.setCancelled(true);
               }
            }
      }
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;

      if (mc.player.isUsingItem() && !mc.player.isPassenger() && !mc.player.isShiftKeyDown()) {
         switch (this.mode.get()) {
            case MATRIX:
               if (mc.player.getTicksUsingItem() % 4 == 0) {
                  this.resendSlot();
               }
         }
      }
   }

   private void resendSlot() {
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(mc.player.getInventory().getSelectedSlot()));
   }
}
