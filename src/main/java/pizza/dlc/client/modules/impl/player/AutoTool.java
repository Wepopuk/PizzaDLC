package pizza.dlc.client.modules.impl.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import pizza.dlc.api.util.game.ToolSpeed;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.client.modules.impl.combat.CrystalAura;
import pizza.dlc.mixin.client.MultiPlayerGameModeAccessor;

@ModuleInfo(name = "AutoTool", description = "Берёт лучший инструмент (лопата/кирка) в т.ч. для подкопа CrystalAura", category = ModuleCategory.PLAYER)
public class AutoTool extends Module {
   private static final int HOTBAR_SIZE = 9;
   private static final int INVENTORY_SIZE = 36;

   private static final float INVENTORY_MARGIN = 1.05F;
   public final BooleanSetting useInventory = new BooleanSetting("Из инвентаря", this);
   public final ModeSetting mode = new ModeSetting("Режим", this, "Silent", "Normal");

   private int originalHotbarSlot = -1;
   private int swappedInventorySlot = -1;
   private int silentServerSlot = -1;

   public AutoTool() {
      this.useInventory.setEnabled(true);
   }

   @Override
   public void onDisable() {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player != null) {
         if (this.swappedInventorySlot != -1 && this.originalHotbarSlot != -1) {
            try {
               InventoryUtility.swap(this.swappedInventorySlot, this.originalHotbarSlot + 36);
            } catch (Throwable ignored) {
            }
         }
         this.restoreSilentServerSlot(player);
         if (this.originalHotbarSlot != -1) {
            try {
               player.getInventory().setSelectedSlot(this.originalHotbarSlot);
            } catch (Throwable ignored) {
            }
         }
      }
      this.originalHotbarSlot = -1;
      this.swappedInventorySlot = -1;
      this.silentServerSlot = -1;
      super.onDisable();
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      Minecraft client = Minecraft.getInstance();
      LocalPlayer player = client.player;
      if (player == null || client.level == null || player.isCreative() || client.gui.screen() != null) {
         this.restoreNow(player);
         return;
      }

      BlockState auraState = this.crystalAuraBreakState(client);
      boolean breaking = auraState != null || this.isBreakingBlock(client);
      if (!breaking) {
         this.restoreNow(player);
         return;
      }

      BlockState blockState = auraState != null ? auraState
         : client.level.getBlockState(((BlockHitResult) client.hitResult).getBlockPos());

      int bestSlot = this.findBestToolSlot(player, blockState);
      if (bestSlot == -1) return;
      if (this.originalHotbarSlot == -1) {
         this.originalHotbarSlot = player.getInventory().getSelectedSlot();
      }
      if (bestSlot < HOTBAR_SIZE) {
         if (this.swappedInventorySlot != -1) {
            try {
               InventoryUtility.swap(this.swappedInventorySlot, this.originalHotbarSlot + 36);
            } catch (Throwable ignored) {
            }
            this.swappedInventorySlot = -1;
         }
         this.selectHotbarTool(player, bestSlot);
         return;
      }
      if (!this.useInventory.isEnabled()) return;
      if (this.swappedInventorySlot == bestSlot) {
         player.getInventory().setSelectedSlot(this.originalHotbarSlot);
         return;
      }
      if (this.swappedInventorySlot != -1) {
         try {
            InventoryUtility.swap(this.swappedInventorySlot, this.originalHotbarSlot + 36);
         } catch (Throwable ignored) {
         }
      }
      InventoryUtility.swap(bestSlot, this.originalHotbarSlot + 36);
      this.swappedInventorySlot = bestSlot;
      player.getInventory().setSelectedSlot(this.originalHotbarSlot);
      this.gateDestroy();
   }

   public boolean swapForBlock(BlockState state) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (!this.isEnabled() || player == null || state == null) return false;
      int bestSlot = this.findBestToolSlot(player, state);
      if (bestSlot < 0) return false;
      if (this.originalHotbarSlot == -1) {
         this.originalHotbarSlot = player.getInventory().getSelectedSlot();
      }
      if (bestSlot < HOTBAR_SIZE) {
         if (this.swappedInventorySlot != -1) {
            try {
               InventoryUtility.swap(this.swappedInventorySlot, this.originalHotbarSlot + 36);
            } catch (Throwable ignored) {
            }
            this.swappedInventorySlot = -1;
         }
         this.selectHotbarTool(player, bestSlot);
         return true;
      }
      if (!this.useInventory.isEnabled()) return false;
      if (this.swappedInventorySlot == bestSlot) {
         player.getInventory().setSelectedSlot(this.originalHotbarSlot);
         return true;
      }
      InventoryUtility.swap(bestSlot, this.originalHotbarSlot + 36);
      this.swappedInventorySlot = bestSlot;
      player.getInventory().setSelectedSlot(this.originalHotbarSlot);
      this.gateDestroy();
      return false;
   }

   private boolean isBreakingBlock(Minecraft client) {
      return client.options.keyAttack.isDown()
         && client.hitResult != null
         && client.hitResult.getType() == HitResult.Type.BLOCK;
   }

   private BlockState crystalAuraBreakState(Minecraft client) {
      try {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         if (cc == null || cc.getModuleManager() == null) return null;
         CrystalAura aura = cc.getModuleManager().get(CrystalAura.class);
         if (aura == null || !aura.isEnabled() || !aura.getAutoKrot().isEnabled() || aura.isLegitMine() || client.level == null) return null;
         CrystalAura.PlaceData data = aura.getLastData();
         if (data == null || data.blockPos() == null || data.placeResult() != CrystalAura.PlaceResult.BREAK) return null;
         return client.level.getBlockState(data.blockPos());
      } catch (Throwable ignored) {
         return null;
      }
   }

   private int findBestToolSlot(LocalPlayer player, BlockState state) {
      int limit = this.useInventory.isEnabled() ? INVENTORY_SIZE : HOTBAR_SIZE;
      int bestSlot = -1;
      float best = ToolSpeed.progressPerTick(ItemStack.EMPTY, state, player.level()) * 1.0001F;
      for (int slot = 0; slot < limit; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.isEmpty() || stack.getDestroySpeed(state) <= 1.0F) continue;
         float progress = ToolSpeed.progressPerTick(stack, state, player.level());
         if (progress > best * (slot >= HOTBAR_SIZE ? INVENTORY_MARGIN : 1.0F)) {
            best = progress;
            bestSlot = slot;
         }
      }
      return bestSlot;
   }

   private void selectHotbarTool(LocalPlayer player, int slot) {
      Minecraft client = Minecraft.getInstance();
      if (this.mode.is("Normal")) {
         this.restoreSilentServerSlot(player);
         player.getInventory().setSelectedSlot(slot);
         if (client.gameMode instanceof MultiPlayerGameModeAccessor acc) {
            try {
               acc.invokerEnsureHasSentCarriedItem();
            } catch (Throwable ignored) {
            }
         }
         return;
      }
      if (slot == player.getInventory().getSelectedSlot()) {
         this.restoreSilentServerSlot(player);
         return;
      }
      if (this.silentServerSlot != slot) {
         player.connection.send(new ServerboundSetCarriedItemPacket(slot));
         this.silentServerSlot = slot;
      }
   }

   private void restoreNow(LocalPlayer player) {
      if (this.swappedInventorySlot != -1 && this.originalHotbarSlot != -1) {
         try {
            InventoryUtility.swap(this.swappedInventorySlot, this.originalHotbarSlot + 36);
         } catch (Throwable ignored) {
         }
         this.swappedInventorySlot = -1;
         this.originalHotbarSlot = -1;
         if (player != null) this.restoreSilentServerSlot(player);
         return;
      }
      if (player != null) {
         this.restoreSilentServerSlot(player);
         if (this.originalHotbarSlot != -1) {
            try {
               player.getInventory().setSelectedSlot(this.originalHotbarSlot);
            } catch (Throwable ignored) {
            }
         }
      }
      this.originalHotbarSlot = -1;
   }

   private void gateDestroy() {
      Minecraft client = Minecraft.getInstance();
      if (client.gameMode instanceof MultiPlayerGameModeAccessor acc) {
         try {
            acc.setDestroyDelay(2);
         } catch (Throwable ignored) {
         }
      }
   }

   private void restoreSilentServerSlot(LocalPlayer player) {
      if (this.silentServerSlot == -1) return;
      try {
         player.connection.send(new ServerboundSetCarriedItemPacket(player.getInventory().getSelectedSlot()));
      } catch (Throwable ignored) {
      }
      this.silentServerSlot = -1;
   }
}
