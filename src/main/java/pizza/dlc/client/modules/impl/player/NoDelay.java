package pizza.dlc.client.modules.impl.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "NoDelay",
   category = ModuleCategory.PLAYER
)
public class NoDelay extends Module {
   private static final String JUMP = "Прыжок";
   private static final String PLACE = "Установка";
   private static final String BREAK = "Копание";
   private static final String XP = "Опыт";
   public final MultiModeSetting remove = new MultiModeSetting("Убрать", this, JUMP, PLACE, BREAK, XP);

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;

      if (this.remove.get(JUMP).isEnabled()) {
         mc.player.noJumpDelay = 0;
      }

      if (this.remove.get(PLACE).isEnabled()) {
         mc.rightClickDelay = 0;
      }

      if (this.remove.get(BREAK).isEnabled() && mc.gameMode != null) {
         mc.gameMode.destroyDelay = 0;
      }

      if (this.remove.get(XP).isEnabled()
         && (mc.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE) || mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE))) {
         mc.rightClickDelay = 0;
         ItemCooldowns cd = mc.player.getCooldowns();
         cd.removeCooldown(cd.getCooldownGroup(new ItemStack(Items.EXPERIENCE_BOTTLE)));
      }
   }
}
