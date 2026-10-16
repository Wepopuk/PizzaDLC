package pizza.dlc.client.modules.impl.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.settings.impl.StringSetting;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ItemFinder",
   description = "Ищет предмет в сундуках в радиусе и забирает его. Если инвентарь полон — складывает в шалкер.",
   category = ModuleCategory.PLAYER
)
public class ItemFinder extends Module {
   private final SliderSetting radius = new SliderSetting("Радиус", this, 10.0F, 1.0F, 32.0F, 1.0F);
   private final BooleanSetting useShulker = new BooleanSetting("Использовать шалкер", this);

   public ItemFinder() {
      this.useShulker.setEnabled(true);
   }
   private final StringSetting itemName = new StringSetting("Предмет", "Название предмета (id), например gunpowder", "", this);

   private static final int REACH = 4;
   private static final long ACT_DELAY = 160L;

   private enum State {
      IDLE,
      GOTO,
      WAIT_OPEN,
      LOOT,
      SHULKER_PLACE,
      SHULKER_OPEN,
      SHULKER_DUMP,
      DONE
   }

   private Item targetItem;
   private final List<BlockPos> queue = new ArrayList<>();
   private final Set<BlockPos> visited = new HashSet<>();
   private BlockPos currentChest;
   private BlockPos shulkerPos;
   private int shulkerSlot = -1;

   private State state = State.IDLE;
   private long lastAction = 0L;
   private int expectKind = 0;
   private BlockPos expectPos;
   private boolean giveUpChest = false;
   private int pendingDumpBefore = -1;
   private boolean roomMade = false;
   private BlockPos returnPos;
   private boolean teleported = false;

   public void startSearch(String name) {
      Item item = resolveItem(name);
      if (item == null) {
         ChatUtility.sendError("Предмет не найден: " + name);
         return;
      }
      this.targetItem = item;
      this.itemName.setText(name);
      this.setEnabled(true);
   }

   public void stopSearch() {
      this.setEnabled(false);
   }

   public void setRadius(int value) {
      this.radius.setValue(value);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.queue.clear();
      this.visited.clear();
      this.currentChest = null;
      this.shulkerPos = null;
      this.shulkerSlot = -1;
      this.giveUpChest = false;
      this.pendingDumpBefore = -1;
      this.roomMade = false;
      this.state = State.IDLE;
      this.lastAction = 0L;

      if (this.targetItem == null) {
         ChatUtility.sendError("Укажи предмет: .itemfinder search <название>");
         this.setEnabled(false);
         return;
      }
      this.scan();
      if (this.queue.isEmpty()) {
         ChatUtility.send("Сундуки с предметом не найдены в радиусе " + (int) this.radius.getValue());
         this.setEnabled(false);
         return;
      }
      ChatUtility.send("Найдено сундуков: " + this.queue.size() + ". Начинаю сбор «" + this.itemName.getText() + "».");
      this.currentChest = this.queue.remove(0);
      this.state = State.GOTO;
   }

   @Override
   public void onDisable() {
      if (mc.player != null && mc.player.containerMenu != null && mc.player.containerMenu != mc.player.inventoryMenu) {
         mc.player.closeContainer();
      }
      this.state = State.IDLE;
      super.onDisable();
   }

   @EventHandler
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.level == null || mc.gameMode == null) {
         return;
      }
      switch (this.state) {
         case IDLE:
         case DONE:
            this.setState(State.DONE);
            this.finish();
            break;
         case GOTO:
            this.tickGoto();
            break;
         case WAIT_OPEN:
            this.tickWaitOpen();
            break;
         case LOOT:
            this.tickLoot();
            break;
         case SHULKER_PLACE:
            this.tickShulkerPlace();
            break;
         case SHULKER_OPEN:
            this.tickShulkerOpen();
            break;
         case SHULKER_DUMP:
            this.tickShulkerDump();
            break;
      }
   }

   private void finish() {
      if (this.state != State.DONE || !this.queue.isEmpty()) return;

      ChatUtility.send("Готово. Все сундуки в радиусе проверены.");
      this.setEnabled(false);
   }

   private void tickGoto() {
      if (this.currentChest == null) {
         if (this.queue.isEmpty()) {
            this.setState(State.DONE);
            return;
         }
         this.currentChest = this.queue.remove(0);
      }
      if (this.queue.isEmpty() && this.currentChest == null) {
         this.setState(State.DONE);
         return;
      }
      if (this.isMenuOpen()) {
         this.closeMenu();
      }
      BlockPos c = this.currentChest;
      double dx = (c.getX() + 0.5D) - mc.player.getX();
      double dy = (c.getY() + 0.5D) - mc.player.getY();
      double dz = (c.getZ() + 0.5D) - mc.player.getZ();
      double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (d <= REACH) {
         this.openBlock(c, 1);
      } else {
         this.returnPos = mc.player.blockPosition();
         this.sendTeleport(c.getX() + 0.5D, c.getY() + 1.0D, c.getZ() + 0.5D);
         this.teleported = true;
         this.openBlock(c, 1);
      }
   }

   private void sendTeleport(double x, double y, double z) {
      mc.player.setPos(x, y, z);
      mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, true, true));
   }

   private void restorePosition() {
      if (!this.teleported || this.returnPos == null) {
         return;
      }
      this.sendTeleport(this.returnPos.getX() + 0.5D, this.returnPos.getY() + 0.5D, this.returnPos.getZ() + 0.5D);
      this.teleported = false;
   }

   private void tickWaitOpen() {
      if (!canAct()) {
         return;
      }
      if (this.isMenuOpen()) {
         this.lastAction = System.currentTimeMillis();
         if (this.expectKind == 1) {
            this.setState(State.LOOT);
         } else {
            this.setState(State.SHULKER_DUMP);
         }
         return;
      }
      if (System.currentTimeMillis() - this.lastAction > 1200L) {
         if (this.expectKind == 1) {
            ChatUtility.sendError("Не удалось открыть сундук, пропускаю.");
            this.visited.add(this.expectPos);
            this.currentChest = null;
            this.restorePosition();
            this.setState(State.GOTO);
         } else {
            ChatUtility.sendError("Не удалось открыть шалкер.");
            this.shulkerPos = null;
            this.setState(State.LOOT);
         }
      }
   }

   private void tickLoot() {
      if (!this.isMenuOpen()) {
         this.setState(State.GOTO);
         return;
      }
      if (!canAct()) {
         return;
      }
      AbstractContainerMenu menu = mc.player.containerMenu;
      Slot target = this.findContainerSlot(menu, this.targetItem);
      if (target == null) {
         this.visited.add(this.currentChest);
         this.currentChest = null;
         this.giveUpChest = false;
         this.closeMenu();
         this.restorePosition();
         this.setState(State.GOTO);
         return;
      }
      if (this.hasRoom(target.getItem())) {
         InventoryUtility.sendClick(target.index, 0, ContainerInput.QUICK_MOVE);
         this.lastAction = System.currentTimeMillis();
         return;
      }
      if (!this.useShulker.isEnabled()) {
         ChatUtility.sendError("Инвентарь полон, шалкер отключён. Пропускаю сундук.");
         this.visited.add(this.currentChest);
         this.currentChest = null;
         this.giveUpChest = false;
         this.closeMenu();
         this.restorePosition();
         this.setState(State.GOTO);
         return;
      }
      this.shulkerSlot = this.findShulkerSlot();
      if (this.shulkerSlot == -1) {
         ChatUtility.sendError("Шалкер не найден в инвентаре. Пропускаю сундук.");
         this.visited.add(this.currentChest);
         this.currentChest = null;
         this.giveUpChest = false;
         this.closeMenu();
         this.restorePosition();
         this.setState(State.GOTO);
         return;
      }
      this.closeMenu();
      this.setState(this.shulkerPos == null ? State.SHULKER_PLACE : State.SHULKER_OPEN);
   }

   private void tickShulkerPlace() {
      if (!canAct()) {
         return;
      }
      int saved = mc.player.getInventory().getSelectedSlot();
      int putInto = this.prepareShulkerHand(saved);
      if (putInto == -1) {
         ChatUtility.sendError("Не удалось подготовить шалкер.");
         this.setState(State.LOOT);
         return;
      }
      BlockHitResult hr = this.findShulkerPlacement();
      if (hr == null) {
         ChatUtility.sendError("Нет места чтобы поставить шалкер рядом.");
         mc.player.connection.send(new ServerboundSetCarriedItemPacket(saved));
         this.setState(State.LOOT);
         return;
      }
      mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hr);
      this.shulkerPos = hr.getBlockPos().relative(hr.getDirection());
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(saved));
      this.lastAction = System.currentTimeMillis();
      this.setState(State.SHULKER_OPEN);
   }

   private void tickShulkerOpen() {
      if (!canAct()) {
         return;
      }
      if (this.shulkerPos == null) {
         this.setState(State.LOOT);
         return;
      }
      this.openBlock(this.shulkerPos, 2);
   }

   private void tickShulkerDump() {
      if (!this.isMenuOpen()) {
         this.setState(State.SHULKER_OPEN);
         return;
      }
      if (pendingDumpBefore != -1) {
         int after = this.countPlayerTarget();
         if (after >= pendingDumpBefore) {
            this.roomMade = false;
            this.pendingDumpBefore = -1;
            this.closeMenu();
            if (this.giveUpChest) {
               this.visited.add(this.currentChest);
               this.currentChest = null;
               this.giveUpChest = false;
            }
            this.restorePosition();
            this.setState(State.GOTO);
            return;
         }
         this.roomMade = true;
         this.pendingDumpBefore = -1;
      }
      if (!canAct()) {
         return;
      }
      Slot s = this.findPlayerSlot(mc.player.containerMenu, this.targetItem);
      if (s == null) {
         this.closeMenu();
         this.restorePosition();
         this.setState(State.GOTO);
         return;
      }
      int before = this.countPlayerTarget();
      InventoryUtility.sendClick(s.index, 0, ContainerInput.QUICK_MOVE);
      this.pendingDumpBefore = before;
      this.lastAction = System.currentTimeMillis();
   }

   private void scan() {
      BlockPos center = mc.player.blockPosition();
      int r = (int) this.radius.getValue();
      for (int x = -r; x <= r; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos p = center.offset(x, y, z);
               if (!(mc.level.getBlockState(p).getBlock() instanceof AbstractChestBlock)) {
                  continue;
               }
               if (this.visited.contains(p)) {
                  continue;
               }
               if (this.isChest(p.west()) || this.isChest(p.south())) {
                  continue;
               }
               this.queue.add(p);
               this.visited.add(p);
            }
         }
      }
   }

   private boolean isChest(BlockPos p) {
      return mc.level.getBlockState(p).getBlock() instanceof AbstractChestBlock;
   }

   private void openBlock(BlockPos pos, int kind) {
      ensureSafeHand();
      Vec3 hit = new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
      BlockHitResult hr = new BlockHitResult(hit, Direction.UP, pos, false);
      mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hr);
      this.expectKind = kind;
      this.expectPos = pos;
      this.lastAction = System.currentTimeMillis();
      this.setState(State.WAIT_OPEN);
   }

   private boolean isMenuOpen() {
      return mc.player.containerMenu != null && mc.player.containerMenu != mc.player.inventoryMenu;
   }

   private void closeMenu() {
      if (!this.isMenuOpen()) return;

      mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
      mc.player.closeContainer();
   }

   private boolean canAct() {
      return System.currentTimeMillis() - this.lastAction >= ACT_DELAY;
   }

   private Slot findContainerSlot(AbstractContainerMenu menu, Item item) {
      for (Slot slot : menu.slots) {
         if (slot.container == mc.player.getInventory() || !slot.hasItem() || slot.getItem().getItem() != item) continue;

         return slot;
      }
      return null;
   }

   private Slot findPlayerSlot(AbstractContainerMenu menu, Item item) {
      for (Slot slot : menu.slots) {
         if (slot.container != mc.player.getInventory() || !slot.hasItem() || slot.getItem().getItem() != item) continue;

         return slot;
      }
      return null;
   }

   private int countPlayerTarget() {
      int n = 0;
      for (Slot slot : mc.player.inventoryMenu.slots) {
         if (slot.container != mc.player.getInventory() || !slot.hasItem() || slot.getItem().getItem() != this.targetItem) continue;

         n += slot.getItem().getCount();
      }
      return n;
   }

   private boolean hasRoom(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }
      if (mc.player.getInventory().getFreeSlot() != -1) {
         return true;
      }
      return mc.player.getInventory().getSlotWithRemainingSpace(stack) != -1;
   }

   private int findShulkerSlot() {
      for (int i = 0; i <= 44; i++) {
         ItemStack s = mc.player.getInventory().getItem(i);
         if (isShulker(s)) {
            return i;
         }
      }
      return -1;
   }

   private boolean isShulker(ItemStack s) {
      return !s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
   }

   private int prepareShulkerHand(int savedSelected) {
      int slot = this.shulkerSlot;
      if (slot >= 36 && slot <= 44) {
         int hb = slot - 36;
         if (hb != savedSelected) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(hb));
         }
         return hb;
      }
      InventoryUtility.swap(slot, 36 + savedSelected);
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(savedSelected));
      return savedSelected;
   }

   private BlockHitResult findShulkerPlacement() {
      BlockPos pp = mc.player.blockPosition();
      for (Direction d : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
         BlockPos air = pp.relative(d);
         BlockState airState = mc.level.getBlockState(air);
         BlockState floor = mc.level.getBlockState(air.below());
         if (airState.canBeReplaced() && floor.isSolid()) {
            Vec3 center = Vec3.atCenterOf(air.below());
            return new BlockHitResult(center, Direction.UP, air.below(), false);
         }
      }
      return null;
   }

   private void ensureSafeHand() {
      ItemStack held = mc.player.getInventory().getSelectedItem();
      if (held.isEmpty()) {
         return;
      }
      if (!(held.getItem() instanceof net.minecraft.world.item.BlockItem)) {
         return;
      }
      int selected = mc.player.getInventory().getSelectedSlot();
      for (int i = 0; i < 9; i++) {
         if (i == selected) {
            continue;
         }
         ItemStack s = mc.player.getInventory().getItem(i);
         if (s.isEmpty() || !(s.getItem() instanceof net.minecraft.world.item.BlockItem)) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(i));
            return;
         }
      }
   }

   private void setState(State s) {
      this.state = s;
   }

   private Item resolveItem(String name) {
      if (name == null || name.isBlank()) {
         return null;
      }
      Identifier id = Identifier.tryParse(name.trim());
      if (id == null && !name.contains(":")) {
         id = Identifier.withDefaultNamespace(name.trim());
      }
      if (id == null) {
         return null;
      }
      Item item = BuiltInRegistries.ITEM.getValue(id);
      return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
   }
}
