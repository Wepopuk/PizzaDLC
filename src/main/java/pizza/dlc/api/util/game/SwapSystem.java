package pizza.dlc.api.util.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.modules.impl.player.SwapSettings;
import pizza.dlc.mixin.client.MultiPlayerGameModeAccessor;

public class SwapSystem implements QuickImports {
   private static final List<DeferredAction> deferred = new ArrayList<>();
   private static boolean grimStall = false;
   private static boolean grimApplied = false;
   private static boolean grimFlushing = false;
   private static boolean internalClick = false;
   private static final List<Runnable> grimQueue = new ArrayList<>();
   private static boolean testMasked = false;

   public static boolean useItem(Item item) {
      return useItemAimed(item, null);
   }

   public static boolean useItemAimed(Item item, Float xRot) {
      int slot = InventoryUtility.find(item);
      if (slot == -1) {
         InventoryNotify.missing(item);
         return false;
      }

      return useItemFromSlot(slot, xRot);
   }

   public static boolean useItemFromSlot(int containerSlot) {
      return useItemFromSlot(containerSlot, null);
   }

   public static boolean useItemFromSlot(int containerSlot, Float xRot) {
      if (mc.player == null) {
         return false;
      }

      boolean isHotbar = containerSlot >= 36 && containerSlot <= 44;
      Runnable op = () -> {
         if (isHvH()) {
            useHvH(containerSlot, xRot);
         } else {
            useDefault(containerSlot, xRot);
         }
      };
      if ((!isHotbar || InventorySwapQueue.configuredMode() != InventorySwapQueue.Mode.RAGE) && grimDefer(op)) {
         return true;
      }

      op.run();
      return true;
   }

   public static boolean useItemTracked(Item item, Angle angle, Object owner, String name,
                                        InventorySwapQueue.Mode mode, Runnable delivered) {
      return useItemTracked(item,angle,owner,name,mode,0,delivered);
   }
   public static boolean useItemTracked(Item item, Angle angle, Object owner, String name,
                                        InventorySwapQueue.Mode mode, int restoreTicks, Runnable delivered) {
      if (mc.player == null || mc.level == null || mc.gameMode == null) return false;
      int slot=InventoryUtility.find(item);
      boolean offhand=mc.player.getOffhandItem().is(item);
      if (slot == -1 && !offhand) return false;
      return useItemTrackedSlot(slot,offhand,angle,owner,name,mode,restoreTicks,delivered);
   }
   public static boolean useItemTrackedFromSlot(int slot, Angle angle, Object owner, String name,
                                               InventorySwapQueue.Mode mode, Runnable delivered) {
      if (mc.player==null || mc.level==null || mc.gameMode==null || slot<9 || slot>44) return false;
      return useItemTrackedSlot(slot,false,angle,owner,name,mode,0,delivered);
   }
   private static boolean useItemTrackedSlot(int slot, boolean offhand, Angle angle, Object owner, String name,
                                            InventorySwapQueue.Mode mode, int restoreTicks, Runnable delivered) {
      var player=mc.player; var level=mc.level;
      float yaw=angle == null ? player.getYRot() : angle.getYaw();
      float pitch=angle == null ? player.getXRot() : angle.getPitch();
      var expected=offhand ? player.getOffhandItem().copy() : player.inventoryMenu.getSlot(slot).getItem().copy();
      class UseTransaction {
         boolean started,finished,swapped;int previous,hotbar;
         net.minecraft.world.item.ItemStack previousItem;
         void restore() {
            if (!started || finished) return;
            finished=true;
            if (mc.player!=player || mc.level!=level) return;
            if (!offhand) selectSlot(previous);
            if (swapped && net.minecraft.world.item.ItemStack.matches(player.inventoryMenu.getSlot(slot).getItem(),previousItem))
               InventoryUtility.sendClick(slot,hotbar,ContainerInput.SWAP);
         }
         void use() {
            if (mc.player != player || mc.level != level) return;
            var stack=offhand ? player.getOffhandItem() : player.inventoryMenu.getSlot(slot).getItem();
            if (!net.minecraft.world.item.ItemStack.matches(stack,expected) || player.getCooldowns().isOnCooldown(stack)) return;
            previous=player.getInventory().getSelectedSlot();hotbar=slot >= 36 ? slot-36 : (previous+1)%9;
            swapped=!offhand && slot < 36;previousItem=offhand ? net.minecraft.world.item.ItemStack.EMPTY : player.getInventory().getItem(hotbar).copy();
            started=true;boolean sent=false;
            try {
               if (swapped) InventoryUtility.sendClick(slot,hotbar,ContainerInput.SWAP);
               if (!offhand) selectSlot(hotbar);
               if (angle != null) player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                  player.getX(),player.getY(),player.getZ(),yaw,pitch,player.onGround(),player.horizontalCollision));
               try (var prediction=level.getBlockStatePredictionHandler().startPredicting()) {
                  player.connection.send(new ServerboundUseItemPacket(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                     prediction.currentSequence(),yaw,pitch));
               }
               sent=true;if (delivered != null) delivered.run();
            } finally { if (!sent || restoreTicks<=0) restore(); }
         }
      }
      var transaction=new UseTransaction();
      // Defer swap/use/restore as one task so every step uses the captured aim and world.
      InventorySwapQueue.Mode effective=effectiveMode(mode);
      return restoreTicks<=0 ? InventorySwapQueue.submit(owner,name,effective,0,null,transaction::use)
         : InventorySwapQueue.submit(owner,name,effective,restoreTicks,transaction::restore,transaction::use,transaction::restore);
   }

   public static InventorySwapQueue.Mode effectiveMode(InventorySwapQueue.Mode mode) {
      return mode == InventorySwapQueue.Mode.RAGE && isGrimMode() ? InventorySwapQueue.Mode.LEGIT : mode;
   }
   public static void resetWorld() {
      InventorySwapQueue.clear();grimQueue.clear();deferred.clear();grimStall=grimApplied=grimFlushing=internalClick=testMasked=false;
   }

   public static void swapToOffhand(int containerSlot) {
      if (mc.player == null || containerSlot == -1) return;

      InventoryUtility.sendClick(containerSlot, 40, ContainerInput.SWAP);
   }

   public static void swapInventoryToHotbar(int inventorySlot, int hotbarSlot) {
      if (mc.player == null) return;

      InventoryUtility.sendClick(inventorySlot, hotbarSlot, ContainerInput.SWAP);
   }

   public static void selectSlot(int hotbarSlot) {
      if (mc.player == null) return;

      if (!isHvH() && mc.player.getInventory().getSelectedSlot() != hotbarSlot) {
         setSlotVisual(hotbarSlot);
      } else {
         mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
      }
   }

   public static void setSlotVisual(int hotbarSlot) {
      if (mc.player == null) return;

      mc.player.getInventory().setSelectedSlot(hotbarSlot);
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
      if (mc.gameMode != null) {
         ((MultiPlayerGameModeAccessor)mc.gameMode).setCarriedIndex(hotbarSlot);
      }
   }

   public static void schedule(int ticks, Runnable action) {
      deferred.add(new DeferredAction(ticks, action));
   }

   public static void tick() {
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      InventorySwapQueue.tick();
      if (!deferred.isEmpty()) {
         Iterator<DeferredAction> it = deferred.iterator();

         while (it.hasNext()) {
            DeferredAction da = it.next();
            if (da.ticks > 0) {
               da.ticks--;
            } else {
               it.remove();
               da.action.run();
            }
         }
      }
   }

   public static void finishPendingRestores() {
      InventorySwapQueue.clear();
      grimQueue.clear();
      grimStall = false;
      grimApplied = false;
      grimFlushing = false;
      internalClick = false;
      testMaskAfter();
      List<DeferredAction> restoring = new ArrayList<>(deferred);
      deferred.clear();
      for (DeferredAction action : restoring) action.action.run();
   }

   private static void useHvH(int containerSlot, Float xRot) {
      boolean isHotbar = containerSlot >= 36 && containerSlot <= 44;
      if (isHotbar) {
         int hotbarId = containerSlot - 36;
         int current = mc.player.getInventory().getSelectedSlot();
         if (current != hotbarId) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbarId));
         }

         sendUse(xRot);
         if (current != hotbarId) {
            restoreHand(() -> mc.player.connection.send(new ServerboundSetCarriedItemPacket(current)));
         }
      } else {
         int currentx = mc.player.getInventory().getSelectedSlot();
         boolean wasSprint = maybeStopSprint();
         InventoryUtility.sendClick(containerSlot, currentx, ContainerInput.SWAP);
         sendUse(xRot);
         if (wasSprint) {
            restoreSprint();
         }

         restoreHand(() -> InventoryUtility.sendClick(containerSlot, currentx, ContainerInput.SWAP));
      }
   }

   private static void restoreHand(Runnable back) {
      if (isPureHvH()) {
         back.run();
      } else {
         deferred.add(new DeferredAction(1, () -> {
            if (mc.player != null) {
               back.run();
            }
         }));
      }
   }

   private static void useDefault(int containerSlot, Float xRot) {
      boolean isHotbar = containerSlot >= 36 && containerSlot <= 44;
      int delay = getDelayTicks();
      if (isHotbar) {
         int hotbarId = containerSlot - 36;
         int prev = mc.player.getInventory().getSelectedSlot();
         setSlotVisual(hotbarId);
         sendUse(xRot);
         if (prev != hotbarId) {
            deferred.add(new DeferredAction(delay, () -> {
               if (mc.player != null) {
                  setSlotVisual(prev);
               }
            }));
         }
      } else {
         int current = mc.player.getInventory().getSelectedSlot();
         boolean wasSprint = maybeStopSprint();
         InventoryUtility.sendClick(containerSlot, current, ContainerInput.SWAP);
         sendUse(xRot);
         if (wasSprint) {
            restoreSprint();
         }

         deferred.add(new DeferredAction(delay, () -> {
            if (mc.player != null) {
               InventoryUtility.sendClick(containerSlot, current, ContainerInput.SWAP);
            }
         }));
      }
   }

   private static void sendUse(Float xRotOverride) {
      if (mc.player == null || mc.level == null) return;

      float yaw = mc.player.getYRot();
      float xRot = mc.player.getXRot();
      RotationController controller = RotationController.INSTANCE;
      if (controller != null && controller.isActive()) {
         Angle serverAngle = controller.getRotation();
         yaw = serverAngle.getYaw();
         xRot = serverAngle.getPitch();
      }

      if (xRotOverride != null) {
         xRot = xRotOverride;
      }

      BlockStatePredictionHandler prediction = mc.level.getBlockStatePredictionHandler().startPredicting();

      try {
         mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, prediction.currentSequence(), yaw, xRot));
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
   }

   private static boolean isHvH() {
      SwapSettings s = SwapSettings.get();
      return s == null || s.swapMode.is("HvH") || s.swapMode.is("Grim") || s.swapMode.is("Test");
   }

   private static boolean isPureHvH() {
      SwapSettings s = SwapSettings.get();
      return s == null || s.swapMode.is("HvH");
   }

   private static boolean isGrimMode() {
      SwapSettings s = SwapSettings.get();
      return !StreamProofRenderer.isVanillaGameplayActive() && (s != null && s.swapMode.is("Grim") || pizza.dlc.client.modules.impl.movement.InventoryMove.grimBypass());
   }

   private static boolean isTestMode() {
      SwapSettings s = SwapSettings.get();
      return !StreamProofRenderer.isVanillaGameplayActive() && s != null && s.swapMode.is("Test");
   }

   public static void testMaskBefore() {
      if (testMasked || !isTestMode() || !grimMoving() || mc.player == null) return;

      LocalPlayer p = mc.player;
      p.connection.send(new ServerboundPlayerInputPacket(Input.EMPTY));
      if (p.isSprinting() || p.wasSprinting) {
         p.connection.send(new ServerboundPlayerCommandPacket(p, Action.STOP_SPRINTING));
      }

      testMasked = true;
   }

   public static void testMaskAfter() {
      if (testMasked && mc.player != null) {
         testMasked = false;
         mc.player.connection.send(new ServerboundPlayerInputPacket(mc.player.input.keyPresses));
      } else {
         testMasked = false;
      }
   }

   private static boolean grimMoving() {
      if (mc.player == null) {
         return false;
      }

      Input kp = mc.player.input.keyPresses;
      return kp.forward() || kp.backward() || kp.left() || kp.right() || mc.player.isSprinting();
   }

   public static boolean grimDefer(Runnable click) {
      if (StreamProofRenderer.isVanillaGameplayActive() || pizza.dlc.api.util.render.streamproof.CleanRenderScope.isActive()) return false;
      if (InventorySwapQueue.executing()) return false;
      if (InventorySwapQueue.configuredMode() != InventorySwapQueue.Mode.RAGE) {
         return InventorySwapQueue.submit(click, "click", InventorySwapQueue.configuredMode(), 0, null, click);
      }
      if (!grimFlushing && isGrimMode() && grimMoving()) {
         var player = mc.player; var level = mc.level; var menu = player.containerMenu;
         grimQueue.add(() -> { if (mc.player == player && mc.level == level && player.containerMenu == menu) click.run(); });
         grimStall = true;
         return true;
      }

      return false;
   }

   public static boolean isInternalClick() {
      return internalClick;
   }

   public static void setInternalClick(boolean v) {
      internalClick = v;
   }

   public static boolean grimStallInput() {
      if (InventorySwapQueue.blocksMovement()) return true;
      if (!grimStall) {
         return false;
      }

      grimApplied = true;
      return true;
   }

   public static void flushGrim() {
      if (!grimStall || !grimApplied) return;

      if (mc.player == null) {
         grimQueue.clear();
         grimStall = false;
         grimApplied = false;
      } else {
         LocalPlayer self = mc.player;
         if (self.isSprinting() != self.wasSprinting) {
            grimApplied = false;
         } else {
            grimStall = false;
            grimApplied = false;
            if (grimQueue.isEmpty()) {
               grimQueue.clear();
            } else {
               List<Runnable> drain = new ArrayList<>(grimQueue);
               grimQueue.clear();
               grimFlushing = true;

               try {
                  for (Runnable r : drain) {
                     r.run();
                  }
               } finally {
                  grimFlushing = false;
               }
            }
         }
      }
   }

   private static int getDelayTicks() {
      SwapSettings s = SwapSettings.get();
      return s == null ? 1 : Math.max(1, (int)(s.inventoryDelay.getValue() / 50.0F));
   }

   private static boolean maybeStopSprint() {
      if (isGrimMode()) {
         return false;
      }

      SwapSettings s = SwapSettings.get();
      if (s != null && s.resetSprint.isEnabled() && mc.player != null) {
         LocalPlayer p = mc.player;
         boolean serverSprinting = p.wasSprinting;
         if (!serverSprinting && !p.isSprinting()) {
            return false;
         }

         p.connection.send(new ServerboundPlayerInputPacket(Input.EMPTY));
         p.setSprinting(false);
         if (serverSprinting) {
            p.connection.send(new ServerboundPlayerCommandPacket(p, Action.STOP_SPRINTING));
            p.wasSprinting = false;
         }

         return true;
      }

      return false;
   }

   private static void restoreSprint() {
      if (mc.player == null) return;

      mc.player.connection.send(new ServerboundPlayerInputPacket(mc.player.input.keyPresses));
   }

   private static final class DeferredAction {
      int ticks;
      final Runnable action;

      DeferredAction(int ticks, Runnable action) {
         this.ticks = ticks;
         var player=mc.player; var level=mc.level;
         this.action = () -> { if (mc.player==player && mc.level==level && player!=null) action.run(); };
      }
   }
}
