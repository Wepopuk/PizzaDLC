package pizza.dlc.api.util.game;

import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.movement.AutoSprint;
import pizza.dlc.client.modules.impl.movement.InventoryMove;

public class InventoryUtility implements QuickImports {
   public static int find(Item item) {
      if (mc.player == null) {
         return -1;
      }

      NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();

      for (int i = 0; i < main.size(); i++) {
         if (((ItemStack)main.get(i)).getItem() != item) continue;

         return i < 9 ? i + 36 : i;
      }

      return -1;
   }

   public static void swap(int from, int to) {
      if (mc.player == null || from == to || from < 0 || to < 0) return;
      if (!SwapSystem.isInternalClick() && SwapSystem.grimDefer(() -> swap(from, to))) return;

      if (to >= 36 && to <= 44) {
         sendClick(from, to - 36, ContainerInput.SWAP);
      } else if (from >= 36 && from <= 44) {
         sendClick(to, from - 36, ContainerInput.SWAP);
      } else if (to == 45) {
         sendClick(from, 40, ContainerInput.SWAP);
      } else if (from == 45) {
         sendClick(to, 40, ContainerInput.SWAP);
      } else {
         sendClick(from, 0, ContainerInput.PICKUP);
         sendClick(to, 0, ContainerInput.PICKUP);
         sendClick(from, 0, ContainerInput.PICKUP);
      }
   }

   public static void useItem(int slot, boolean swing) {
      useItem(slot, swing, false);
   }

   public static void useItem(int slot, boolean swing, boolean cooldown) {
      if (mc.player == null || slot == -1) return;

      boolean isHotbar = slot >= 36 && slot <= 44;
      int currentSlot = mc.player.getInventory().getSelectedSlot();
      if (isHotbar) {
         int hotbarId = slot - 36;
         if (currentSlot != hotbarId) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarId));
         }

         sendUse(swing);
         if (currentSlot != hotbarId) {
            SwapSystem.schedule(1, () -> {
               if (mc.player != null) {
                  mc.player.connection.send(new ServerboundSetCarriedItemPacket(currentSlot));
               }
            });
         }
      } else {
         int targetHotbar = cooldown ? (currentSlot + 1) % 9 : currentSlot;
         int targetSlotId = targetHotbar + 36;
         swap(slot, targetSlotId);
         if (currentSlot != targetHotbar) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(targetHotbar));
         }

         sendUse(swing);
         SwapSystem.schedule(1, () -> {
            if (mc.player != null) {
               swap(slot, targetSlotId);
               if (currentSlot != targetHotbar) {
                  mc.player.connection.send(new ServerboundSetCarriedItemPacket(currentSlot));
               }

               mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            }
         });
      }
   }

   private static void sendUse(boolean swing) {
      if (mc.player == null || mc.level == null) return;

      BlockStatePredictionHandler prediction = mc.level.getBlockStatePredictionHandler().startPredicting();

      try {
         mc.player
            .connection
            .send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, prediction.currentSequence(), mc.player.getYRot(), mc.player.getXRot()));
      } catch (Throwable ex) {
         if (prediction != null) {
            try {
               prediction.close();
            } catch (Throwable ex2) {
               ex.addSuppressed(ex2);
            }
         }

         throw ex;
      }

      if (prediction != null) {
         prediction.close();
      }

      if (swing) {
         mc.player.swing(InteractionHand.MAIN_HAND);
      }
   }

   public static void sendClick(int slotId, int button, ContainerInput action) {
      if (mc.player == null || mc.gameMode == null) return;

      if (SwapSystem.isInternalClick() || !SwapSystem.grimDefer(() -> rawClick(slotId, button, action))) {
         rawClick(slotId, button, action);
      }
   }

   private static void rawClick(int slotId, int button, ContainerInput action) {
      if (mc.player == null || mc.gameMode == null) return;

      if (InventoryMove.shouldResetSprint() && mc.player.isSprinting()) {
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.STOP_SPRINTING));
         mc.player.setSprinting(false);
         mc.player.wasSprinting = false;
         AutoSprint as = autoSprint();
         if (as != null) {
            as.setTickStop(2);
         }
      }

      boolean previousInternal = SwapSystem.isInternalClick();
      SwapSystem.setInternalClick(true);

      try {
         mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slotId, button, action, mc.player);
      } finally {
         SwapSystem.setInternalClick(previousInternal);
      }
   }

   private static AutoSprint autoSprint() {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      return cc != null && cc.getModuleManager() != null ? cc.getModuleManager().get(AutoSprint.class) : null;
   }
}
