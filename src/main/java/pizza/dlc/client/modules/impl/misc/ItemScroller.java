package pizza.dlc.client.modules.impl.misc;

import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import org.lwjgl.glfw.GLFW;
import pizza.dlc.api.events.impl.game.EventClickSlot;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ItemScroller",
   description = "Shift-драг quick-move + Ctrl-клик масс-перемещение",
   category = ModuleCategory.OTHER
)
public class ItemScroller extends Module {
   private static final long MASS_COOLDOWN_MS = 50L;
   private final SliderSetting delayMs = new SliderSetting("Задержка мс", this, 0.0F, 0.0F, 200.0F, 10.0F);
   private long scrollerTimer;
   private long massTimer;
   private int lastHoverSlotId = -1;
   private boolean quickMovedOnThisHover;
   private boolean massMoving;

   @Override
   public void onEnable() {
      this.resetHover();
      this.scrollerTimer = System.currentTimeMillis();
      this.massTimer = this.scrollerTimer;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.resetHover();
      super.onDisable();
   }

   private void resetHover() {
      this.lastHoverSlotId = -1;
      this.quickMovedOnThisHover = false;
   }

   public boolean shouldQuickMoveSlot(int hoveredSlotId) {
      if (hoveredSlotId != this.lastHoverSlotId) {
         this.lastHoverSlotId = hoveredSlotId;
         this.quickMovedOnThisHover = false;
      }

      if (hoveredSlotId < 0 || !this.isEnabled() || !isShiftDown() || isCtrlDown() || this.quickMovedOnThisHover) return false;
      if (System.currentTimeMillis() - this.scrollerTimer < (long)this.delayMs.getValue()) return false;
      this.quickMovedOnThisHover = true;
      this.scrollerTimer = System.currentTimeMillis();
      return true;
   }

   @EventHandler
   public void onClickSlot(EventClickSlot event) {
      if (!this.isEnabled() || this.massMoving || SwapSystem.isInternalClick() || !isCtrlDown()) return;
      ContainerInput input = event.getContainerInput();
      if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE || event.getButton() != 0) return;
      if (System.currentTimeMillis() - this.massTimer < MASS_COOLDOWN_MS || mc.player == null || mc.gameMode == null) return;

      AbstractContainerMenu menu = mc.player.containerMenu;
      int slotId = event.getSlot();
      if (menu == null || slotId < 0 || slotId >= menu.slots.size() || !menu.getCarried().isEmpty()) return;
      Slot source = menu.getSlot(slotId);
      if (source == null || !source.hasItem()) return;

      Item item = source.getItem().getItem();
      event.setCancelled(true);
      this.massMoving = true;
      try {
         for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.hasItem() && slot.container == source.container && slot.getItem().getItem() == item) {
               mc.gameMode.handleContainerInput(event.getId(), i, 0, ContainerInput.QUICK_MOVE, mc.player);
            }
         }
      } finally {
         this.massMoving = false;
      }

      this.massTimer = System.currentTimeMillis();
   }

   public static boolean isShiftDown() {
      return InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
   }

   public static boolean isCtrlDown() {
      return InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
   }
}
