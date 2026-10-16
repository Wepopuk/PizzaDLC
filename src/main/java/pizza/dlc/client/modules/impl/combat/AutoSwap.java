package pizza.dlc.client.modules.impl.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.game.InventoryNotify;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "AutoSwap",
   category = ModuleCategory.COMBAT
)
public class AutoSwap extends Module {
   private static final int OFFHAND_SLOT_ID = 45;
   private final ModeSetting firstItem = new ModeSetting("Первый", this, "Totem", "Shield");
   public final BindSetting swapBind = new BindSetting("Бинд", this, -1);

   @EventHandler
   public void onKey(EventKey e) {
      if (e.getAction() != 1 || mc.gui.screen() != null) return;

      if (this.swapBind.getKey() == e.getKey()) {
         this.handleSwap();
         e.setCancelled(true);
      }
   }

   private void handleSwap() {
      if (mc.player == null) return;

      Item first = this.firstItem.is("Shield") ? Items.SHIELD : Items.TOTEM_OF_UNDYING;
      ItemStack offhand = mc.player.getOffhandItem();
      boolean holdingFirst = offhand.getItem() == first;
      Item second = InventoryUtility.find(Items.GOLDEN_APPLE) == -1 && InventoryUtility.find(Items.ENCHANTED_GOLDEN_APPLE) != -1
         ? Items.ENCHANTED_GOLDEN_APPLE
         : Items.GOLDEN_APPLE;
      Item desired = holdingFirst ? second : first;
      if (desired != offhand.getItem()) {
         int slot = InventoryUtility.find(desired);
         if (slot == -1) {
            InventoryNotify.missing(desired);
         } else {
            InventoryUtility.swap(slot, OFFHAND_SLOT_ID);
         }
      }
   }
}
