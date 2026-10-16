package pizza.dlc.client.modules.impl.combat;

import java.util.Locale;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.combat.CombatTickEvent;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;

/** Native adaptation of the supplied AutoTotem and its six threat checks. */
public class AutoTotem extends Module {
   public final MultiModeSetting checks = CombatSettings.multi("Проверки", this,
      new CombatSettings.Option("Золотые сердца"), new CombatSettings.Option("Кристаллы"),
      new CombatSettings.Option("Якорь"), new CombatSettings.Option("Падение"),
      new CombatSettings.Option("Динамит"), new CombatSettings.Option("Броня"));
   public final SliderSetting health = new SliderSetting("Здоровье", this, 5, 1, 20, 1);
   public final SliderSetting returnHealth = new SliderSetting("Здоровье обратного свапа", this, 2, 1, 20, 1);
   public final SliderSetting elytraHealth = new SliderSetting("Здоровье на элитре", this, 5, 1, 20, 1);
   public final SliderSetting crystalRange = new SliderSetting("Дистанция до кристалла", this, 5, 1, 10, .1F);
   public final SliderSetting anchorRange = new SliderSetting("Дистанция до якоря", this, 5, 1, 10, .1F);
   public final SliderSetting fallDistance = new SliderSetting("Дистанция падения", this, 20, 1, 50, 1);
   public final SliderSetting tntRange = new SliderSetting("Дистанция до динамита", this, 5, 1, 10, .1F);
   public final SliderSetting swapDelay = new SliderSetting("Тики задержки свапа", this, 4, 1, 20, 1);
   public final BooleanSetting saveEnchanted = CombatSettings.bool("Сохранять зачарованный тотем", this, false);
   public final BooleanSetting saveDragonite = CombatSettings.bool("Не свапать драконитки", this, true);
   public final BooleanSetting keepSphere = CombatSettings.bool("Не свапать шар", this, true);
   public final BooleanSetting customSphereHealth = CombatSettings.bool("Кастомное здоровье с шаром", this, true);
   public final SliderSetting sphereHealth = new SliderSetting("Здоровье с шаром", this, 5, 1, 20, 1);
   public final SliderSetting crystalHold = new SliderSetting("Задержка свапа на кристал", this, 4, 0, 60, 1);
   public final SliderSetting anchorHold = new SliderSetting("Задержка свапа на якорь", this, 4, 0, 60, 1);
   public final ModeSetting swapMode = new ModeSetting("Режим свапов", this, "Общий", "Рейджовый", "Легитный");
   public final SliderSetting stopTicks = new SliderSetting("Тики остановки", this, 4, 0, 20, 1);

   private int ticks, lastSwapTick;
   private int crystalSeenTick, anchorSeenTick;
   private boolean crystalSeen, anchorSeen, popped, equipping, returning;
   private int originalSlot = -1;
   private ItemStack originalItem = ItemStack.EMPTY;
   private PendingSwap pending;
   private boolean swapQueued;
   private record PendingSwap(int slot, ItemStack expected, ItemStack offhand, boolean equip,
                              boolean legit, int requestedTick, int performedTick) { }

   public AutoTotem() {
      super("AutoTotem", "Автоматически берет тотем при различных факторах", ModuleCategory.COMBAT);
      crystalRange.setVisible(() -> check("Кристаллы"));
      anchorRange.setVisible(() -> check("Якорь"));
      fallDistance.setVisible(() -> check("Падение"));
      tntRange.setVisible(() -> check("Динамит"));
      customSphereHealth.setVisible(keepSphere::isEnabled);
      sphereHealth.setVisible(() -> keepSphere.isEnabled() && customSphereHealth.isEnabled());
      crystalHold.setVisible(() -> check("Кристаллы"));
      anchorHold.setVisible(() -> check("Якорь"));
      stopTicks.setVisible(() -> swapMode.is("Легитный"));
   }

   private boolean check(String name) { return checks.get(name).isEnabled(); }

   public float effectiveHealth() {
      float hp = mc.player.getHealth();
      return check("Золотые сердца") && mc.player.hasEffect(MobEffects.ABSORPTION)
         ? hp + mc.player.getAbsorptionAmount() : hp;
   }

   public boolean needsTotem() {
      float threshold = mc.player.isFallFlying() ? elytraHealth.getValue() : health.getValue();
      return effectiveHealth() <= threshold || fallingDanger() || crystalDanger() || tntDanger()
         || armorDanger() || anchorDanger();
   }

   private boolean fallingDanger() {
      return check("Падение") && (mc.player.fallDistance >= fallDistance.getValue()
         || mc.player.getDeltaMovement().y <= -fallDistance.getValue() / 5);
   }

   private boolean crystalDanger() {
      if (!check("Кристаллы")) return false;
      boolean protectedSphere = keepSphere.isEnabled() && mc.player.getOffhandItem().is(Items.PLAYER_HEAD)
         && (!customSphereHealth.isEnabled() || effectiveHealth() > sphereHealth.getValue());
      if (!protectedSphere) {
         for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal && entity.distanceTo(mc.player) <= crystalRange.getValue()) {
               crystalSeenTick = ticks; crystalSeen = true;
               return true;
            }
         }
      }
      return crystalSeen && ticks - crystalSeenTick < (int)crystalHold.getValue();
   }

   private boolean tntDanger() {
      if (!check("Динамит")) return false;
      for (var entity : mc.level.entitiesForRendering()) {
         if ((entity instanceof PrimedTnt || entity instanceof MinecartTNT)
            && entity.distanceTo(mc.player) <= tntRange.getValue()) return true;
      }
      return false;
   }

   private boolean armorDanger() {
      if (!check("Броня")) return false;
      for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         if (mc.player.getItemBySlot(slot).isEmpty()) return true;
      }
      return false;
   }

   private boolean anchorDanger() {
      if (!check("Якорь")) return false;
      int radius = Math.round(anchorRange.getValue());
      BlockPos center = mc.player.getBlockPosBelowThatAffectsMyMovement();
      var position = new BlockPos.MutableBlockPos();
      for (int x = -radius; x < radius; x++) {
         for (int y = -radius; y < radius; y++) {
            for (int z = -radius; z < radius; z++) {
               position.set(center.getX() + x, center.getY() + y, center.getZ() + z);
               if (mc.level.getBlockState(position).is(Blocks.RESPAWN_ANCHOR)) {
                  anchorSeenTick = ticks; anchorSeen = true;
                  return true;
               }
            }
         }
      }
      return anchorSeen && ticks - anchorSeenTick < (int)anchorHold.getValue();
   }

   public int findTotemSlot() {
      int enchanted = -1;
      for (int slot = 0; slot < mc.player.getInventory().getNonEquipmentItems().size(); slot++) {
         ItemStack stack = mc.player.getInventory().getNonEquipmentItems().get(slot);
         if (!stack.is(Items.TOTEM_OF_UNDYING)) continue;
         String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
         if (saveDragonite.isEnabled() && (name.contains("драконид") || name.contains("драконит"))) continue;
         int containerSlot = slot < 9 ? slot + 36 : slot;
         if (!saveEnchanted.isEnabled() || !stack.hasFoil() && !stack.isEnchanted()) return containerSlot;
         if (enchanted == -1) enchanted = containerSlot;
      }
      return enchanted;
   }

   private boolean available() {
      return isEnabled() && CombatRuntime.gameplay() && mc.gameMode != null && mc.player.isAlive()
         && mc.player.containerMenu == mc.player.inventoryMenu && mc.player.inventoryMenu.getCarried().isEmpty();
   }

   @EventHandler
   private void onTick(CombatTickEvent event) {
      ticks++;
      if (!available()) { resetState(); return; }
      if (pending != null) { advanceSwap(); return; }
      ItemStack offhand = mc.player.getOffhandItem();
      if (ticks - lastSwapTick >= (int)swapDelay.getValue()) {
         if (offhand.is(Items.TOTEM_OF_UNDYING)) equipping = false;
         if (!offhand.isEmpty() && !offhand.is(Items.TOTEM_OF_UNDYING)) returning = false;
      }
      if (needsTotem()) {
         if (offhand.is(Items.TOTEM_OF_UNDYING) || equipping) return;
         int slot = findTotemSlot();
         if (slot == -1) return;
         if (originalSlot == -1) {
            originalSlot = slot;
            originalItem = offhand.copy();
         }
         requestSwap(slot, true);
      } else if (!returning && originalSlot != -1
         && (popped ? offhand.isEmpty() : effectiveHealth() > health.getValue() + returnHealth.getValue())) {
         // The offhand may have been changed manually while a totem was equipped.
         if (!offhand.isEmpty() && !offhand.is(Items.TOTEM_OF_UNDYING)) { resetState(); return; }
         int slot = findOriginalSlot();
         if (slot != -1) requestSwap(slot, false);
         else resetState();
      }
   }

   private int findOriginalSlot() {
      if (originalSlot < 9 || originalSlot >= 45) return -1;
      if (ItemStack.matches(mc.player.inventoryMenu.getSlot(originalSlot).getItem(), originalItem)) return originalSlot;
      if (originalItem.isEmpty()) return -1;
      for (int slot = 9; slot < 45; slot++) {
         if (ItemStack.matches(mc.player.inventoryMenu.getSlot(slot).getItem(), originalItem)) return slot;
      }
      return -1;
   }

   private void requestSwap(int slot, boolean equip) {
      lastSwapTick = ticks; equipping = equip; returning = !equip; popped = false;
      pending = new PendingSwap(slot, mc.player.inventoryMenu.getSlot(slot).getItem().copy(),
         mc.player.getOffhandItem().copy(), equip, swapMode.is("Легитный") || swapMode.is("Общий") && pizza.dlc.api.util.game.InventorySwapQueue.configuredMode() == pizza.dlc.api.util.game.InventorySwapQueue.Mode.LEGIT, ticks, -1);
      if (!pending.legit()) advanceSwap();
   }

   private void advanceSwap() {
      PendingSwap action = pending;
      if (action.performedTick() >= 0) {
         if (ticks - action.performedTick() >= (int)(swapMode.is("Общий") && pizza.dlc.client.modules.impl.player.SwapSettings.get()!=null ? pizza.dlc.client.modules.impl.player.SwapSettings.get().stopTicks.getValue() : stopTicks.getValue())) pending = null;
         return;
      }
      if (action.legit()) {
         if (ticks == action.requestedTick() || mc.player.input.getMoveVector().lengthSquared() != 0) return;
      }
      if (!ItemStack.matches(mc.player.inventoryMenu.getSlot(action.slot()).getItem(), action.expected())
         || !ItemStack.matches(mc.player.getOffhandItem(), action.offhand())) { resetState(); return; }
      if (swapQueued) return;
      var player = mc.player;
      var level = mc.level;
      Runnable click = () -> {
         if (pending != action || !available() || mc.player != player || mc.level != level) return;
         if (!ItemStack.matches(player.inventoryMenu.getSlot(action.slot()).getItem(), action.expected())
            || !ItemStack.matches(player.getOffhandItem(), action.offhand())) { resetState(); return; }
         performSwap(action);
      };
      swapQueued = true;
      if (!SwapSystem.grimDefer(click)) click.run();
   }

   private void performSwap(PendingSwap action) {
      // Container slot 45 is offhand; SWAP's button 40 selects it, with no cursor pickups.
      boolean internal = SwapSystem.isInternalClick();
      SwapSystem.setInternalClick(true);
      try { mc.gameMode.handleContainerInput(0, action.slot(), 40, ContainerInput.SWAP, mc.player); }
      finally { SwapSystem.setInternalClick(internal); }
      swapQueued = false;
      if (!action.equip()) { originalSlot = -1; originalItem = ItemStack.EMPTY; }
      if (mc.gui != null && mc.gui.screen() == null && mc.player.connection != null)
         mc.player.connection.send(new ServerboundContainerClosePacket(0));
      pending = action.legit() ? new PendingSwap(action.slot(), action.expected(), action.offhand(),
         action.equip(), true, action.requestedTick(), ticks) : null;
   }

   @EventHandler(priority = -100)
   private void stopMovement(EventInputMove event) {
      if (pending == null || !pending.legit() || !available()) return;
      event.setForward(0); event.setStrafe(0); event.setJump(false); event.setSneaking(false); event.setSprint(false);
   }

   @EventHandler
   private void onPacket(PacketEvent.Receive event) {
      if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet) || packet.getEventId() != 35) return;
      var player = mc.player; var level = mc.level;
      if (player == null || level == null) return;
      mc.execute(() -> {
         if (available() && mc.player == player && mc.level == level && packet.getEntity(level) == player) {
            popped = true; equipping = returning = false;
            if (pending != null && pending.performedTick() >= 0) pending = null;
         }
      });
   }

   @EventHandler private void onWorldLoad(EventWorldLoad event) { resetState(); }
   @Override public void onDisable() { resetState(); super.onDisable(); }

   public void resetState() {
      pending = null; swapQueued = false; originalSlot = -1; originalItem = ItemStack.EMPTY;
      popped = equipping = returning = crystalSeen = anchorSeen = false;
      lastSwapTick = ticks;
   }
}
