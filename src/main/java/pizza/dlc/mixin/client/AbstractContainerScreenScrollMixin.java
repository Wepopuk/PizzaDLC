package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.misc.ItemScroller;

@Mixin({AbstractContainerScreen.class})
public abstract class AbstractContainerScreenScrollMixin {
   @Shadow
   protected Slot hoveredSlot;

   @Inject(
      method = {"mouseDragged"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$itemScrollerDrag(MouseButtonEvent e, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
      if (e.button() == 0 && ItemScroller.isShiftDown() && !ItemScroller.isCtrlDown()) {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         if (cc != null && cc.getModuleManager() != null) {
            ItemScroller m = cc.getModuleManager().get(ItemScroller.class);
            if (m != null && m.isEnabled()) {
               Slot hovered = this.hoveredSlot;
               if (hovered != null) {
                  AbstractContainerMenu menu = ((AbstractContainerScreen)(Object)this).getMenu();
                  int menuIndex = menu.slots.indexOf(hovered);
                  if (menuIndex >= 0) {
                     if (m.shouldQuickMoveSlot(menuIndex)) {
                        if (hovered.hasItem()) {
                           Minecraft mc = Minecraft.getInstance();
                           if (mc.player != null && mc.gameMode != null) {
                              mc.gameMode.handleContainerInput(menu.containerId, menuIndex, 0, ContainerInput.QUICK_MOVE, mc.player);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
