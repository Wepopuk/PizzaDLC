package pizza.dlc;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.combat.CombatTickEvent;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.ModuleManager;
import pizza.dlc.client.modules.impl.combat.AutoTotem;

public final class AutoTotemCheck {
   private static void check(boolean condition, String text) { if (!condition) throw new AssertionError(text); }
   private static Object allocate(Class<?> type) throws Exception {
      var unsafe = Class.forName("sun.misc.Unsafe"); var field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
      return unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
   }
   private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
      Field field = owner.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
   }
   private static void tick(PizzaDLC pizzadlc) { pizzadlc.getBus().post(new CombatTickEvent()); }
   private static void place(Entity entity, Vec3 position) throws Exception { set(Entity.class, entity, "position", position); }

   public static void main(String[] args) throws Exception {
      net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap();
      for (var item : List.of(Items.SHIELD, Items.TOTEM_OF_UNDYING, Items.DIAMOND_CHESTPLATE, Items.PLAYER_HEAD, Items.APPLE)) {
         item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder()
            .set(DataComponents.MAX_STACK_SIZE, item == Items.PLAYER_HEAD || item == Items.APPLE ? 64 : 1)
            .set(DataComponents.ITEM_NAME, Component.literal(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath())).build());
      }

      var mc = (Minecraft)allocate(Minecraft.class); set(Minecraft.class, null, "instance", mc);
      set(Minecraft.class, mc, "gameThread", Thread.currentThread());
      var pizzadlc = new PizzaDLC(); set(PizzaDLC.class, null, "instance", pizzadlc);
      pizzadlc.getBus().registerLambdaFactory("pizza.dlc", (lookup, type) -> MethodHandles.privateLookupIn(type, MethodHandles.lookup()));
      var client = new PizzaDLCClient(); set(PizzaDLCClient.class, null, "instance", client);
      var manager = new ModuleManager(); set(PizzaDLCClient.class, client, "moduleManager", manager);
      var player = (TotemPlayer)allocate(TotemPlayer.class); place(player, Vec3.ZERO); set(Entity.class, player, "id", 1);
      player.gear = new EntityEquipment(); player.inventory = new Inventory(player, player.gear);
      set(Player.class, player, "inventory", player.inventory);
      set(Player.class, player, "inventoryMenu", new InventoryMenu(player.inventory, false, player)); player.containerMenu = player.inventoryMenu;
      player.input = new MovingInput(); player.hp = 20; player.velocity = Vec3.ZERO;
      var level = (TotemLevel)allocate(TotemLevel.class); level.entities = new ArrayList<>(); level.player = player;
      var mode = (TotemGameMode)allocate(TotemGameMode.class);
      mc.player = player; mc.level = level; mc.gameMode = mode;
      set(CombatRuntime.class, null, "initialized", true);
      var totem = new AutoTotem(); manager.registerModules(totem); totem.setEnabled(true);
      check(totem.getName().equals("AutoTotem"), "Native module identity lost");
      check(totem.checks.getBoolSettings().size() == 6 && totem.getSettings().size() == 18, "Source settings missing");
      var config = totem.save(); totem.load(config); check(config.equals(totem.save()), "AutoTotem settings round trip changed");

      player.gear.set(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
      player.inventory.setItem(9, new ItemStack(Items.TOTEM_OF_UNDYING)); player.hp = 4;
      tick(pizzadlc);
      check(player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) && player.inventory.getItem(9).is(Items.SHIELD), "Low health swap failed");
      check(mode.clicks == 1, "Swap was not one offhand SWAP click");
      tick(pizzadlc); check(mode.clicks == 1, "Holding a totem caused repeated swaps");
      player.hp = 7; tick(pizzadlc); check(player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "Return health margin is not strict");
      player.hp = 8; tick(pizzadlc);
      check(player.getOffhandItem().is(Items.SHIELD) && player.inventory.getItem(9).is(Items.TOTEM_OF_UNDYING), "Original offhand item not restored");

      player.hp = 4; tick(pizzadlc);
      player.gear.set(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
      pizzadlc.getBus().post(new PacketEvent.Receive(new ClientboundEntityEventPacket(player, (byte)35)));
      check((boolean)get(totem, "popped"), "Totem-pop packet not recognized");
      player.inventory.setItem(10, new ItemStack(Items.TOTEM_OF_UNDYING)); tick(pizzadlc);
      check(player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "Totem not replenished after pop");
      player.hp = 20; tick(pizzadlc); check(player.getOffhandItem().is(Items.SHIELD), "Pop lost the original offhand slot");

      totem.resetState(); player.inventory.setItem(0, new ItemStack(Items.TOTEM_OF_UNDYING));
      check(totem.findTotemSlot() == 36, "Hotbar slot did not convert to container slot 36");
      var enchanted = new ItemStack(Items.TOTEM_OF_UNDYING); enchanted.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      player.inventory.setItem(0, enchanted); player.inventory.setItem(9, ItemStack.EMPTY); player.inventory.setItem(10, new ItemStack(Items.TOTEM_OF_UNDYING)); totem.saveEnchanted.setEnabled(true);
      check(totem.findTotemSlot() == 10, "Enchanted totem was not preserved while a normal one exists");
      player.inventory.setItem(10, ItemStack.EMPTY); check(totem.findTotemSlot() == 36, "Enchanted fallback lost");
      var dragonite = new ItemStack(Items.TOTEM_OF_UNDYING); dragonite.set(DataComponents.CUSTOM_NAME, Component.literal("Драконид"));
      player.inventory.setItem(0, dragonite); check(totem.findTotemSlot() == -1, "Protected Dragonite selected");
      totem.saveDragonite.setEnabled(false); check(totem.findTotemSlot() == 36, "Dragonite setting ignored");
      player.inventory.setItem(0, ItemStack.EMPTY); player.inventory.setItem(9, new ItemStack(Items.TOTEM_OF_UNDYING));

      player.hp = 4; player.absorption = 10; totem.checks.get("Золотые сердца").setEnabled(true);
      check(!totem.needsTotem(), "Absorption hearts were not counted");
      player.absorption = 0; player.hp = 10; player.flying = true; totem.elytraHealth.setValue(12);
      check(totem.needsTotem(), "Elytra health threshold ignored"); player.flying = false;
      totem.checks.get("Падение").setEnabled(true); player.fallDistance = 20;
      check(totem.needsTotem(), "Fall distance threat ignored"); player.fallDistance = 0; player.velocity = new Vec3(0,-4,0);
      check(totem.needsTotem(), "Falling speed threat ignored"); player.velocity = Vec3.ZERO; totem.checks.get("Падение").setEnabled(false);
      player.hp = 20; totem.checks.get("Броня").setEnabled(true);
      check(totem.needsTotem(), "Missing armor threat ignored");
      for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) player.gear.set(slot, new ItemStack(Items.DIAMOND_CHESTPLATE));
      check(!totem.needsTotem(), "Full armor still counted as missing"); totem.checks.get("Броня").setEnabled(false);
      var crystal = (EndCrystal)allocate(EndCrystal.class); place(crystal, new Vec3(0,0,2)); level.entities.add(crystal);
      totem.checks.get("Кристаллы").setEnabled(true); check(totem.needsTotem(), "Crystal threat ignored");
      level.entities.clear(); check(totem.needsTotem(), "Crystal threat hold lost"); totem.resetState();
      player.gear.set(EquipmentSlot.OFFHAND, new ItemStack(Items.PLAYER_HEAD)); level.entities.add(crystal);
      check(!totem.needsTotem(), "Sphere protection ignored"); totem.keepSphere.setEnabled(false);
      check(totem.needsTotem(), "Disabled sphere protection still applies");
      level.entities.clear(); totem.checks.get("Кристаллы").setEnabled(false); totem.resetState();
      var tnt = (PrimedTnt)allocate(PrimedTnt.class); place(tnt, new Vec3(0,0,2)); level.entities.add(tnt);
      totem.checks.get("Динамит").setEnabled(true); check(totem.needsTotem(), "TNT threat ignored");
      level.entities.clear(); totem.checks.get("Динамит").setEnabled(false);
      totem.checks.get("Якорь").setEnabled(true); level.anchor = true; check(totem.needsTotem(), "Anchor threat ignored");
      level.anchor = false; check(totem.needsTotem(), "Anchor hold lost");
      totem.checks.get("Якорь").setEnabled(false); totem.resetState();

      player.gear.set(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD)); player.hp = 4;
      totem.swapMode.setCurrentMode("Легитный"); int before = mode.clicks; tick(pizzadlc);
      check(mode.clicks == before, "Legit swap clicked before stopping movement");
      var input = new EventInputMove(1,1,true,true,true); pizzadlc.getBus().post(input);
      check(!input.isMoving() && !input.isJump() && !input.isSneaking() && !input.isSprint(), "Legit swap input not stopped");
      tick(pizzadlc); check(mode.clicks == before+1 && player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "Legit swap did not execute after stop");
      for (int i=0;i<4;i++) tick(pizzadlc);
      input = new EventInputMove(1,0,false,false,true); pizzadlc.getBus().post(input); check(input.isMoving(), "Legit swap never released movement");

      totem.resetState(); player.gear.set(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD)); tick(pizzadlc);
      before = mode.clicks; player.inventory.setItem(9, new ItemStack(Items.APPLE)); tick(pizzadlc);
      check(mode.clicks == before && player.getOffhandItem().is(Items.SHIELD), "Stale inventory slot was swapped");
      player.inventory.setItem(9, new ItemStack(Items.TOTEM_OF_UNDYING)); tick(pizzadlc);
      pizzadlc.getBus().post(new EventWorldLoad()); input = new EventInputMove(1,0,false,false,true); pizzadlc.getBus().post(input);
      check(input.isMoving() && get(totem,"pending") == null, "World change retained a pending swap");
      try (var clean = new CleanRenderScope(null,null)) { before = mode.clicks; tick(pizzadlc); check(mode.clicks == before, "Clean capture swapped inventory"); }
      player.hp = 4; tick(pizzadlc); totem.setEnabled(false);
      check(get(totem,"pending") == null, "Disable retained a pending swap");
      var swaps = new pizza.dlc.client.modules.impl.player.SwapSettings();
      swaps.swapMode.setCurrentMode("Grim"); totem.swapMode.setCurrentMode("Рейджовый");
      player.gear.set(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
      player.inventory.setItem(9, new ItemStack(Items.TOTEM_OF_UNDYING));
      player.input.keyPresses = new net.minecraft.world.entity.player.Input(true,false,false,false,false,false,false);
      totem.setEnabled(true); before = mode.clicks; tick(pizzadlc);
      check(mode.clicks == before && (boolean)get(totem,"swapQueued"), "Native Grim mode did not defer the swap");
      totem.setEnabled(false);
      pizza.dlc.api.util.game.SwapSystem.grimStallInput(); pizza.dlc.api.util.game.SwapSystem.flushGrim();
      check(mode.clicks == before, "Deferred Grim swap executed after disable");
      totem.setEnabled(true); tick(pizzadlc); player.inventory.setItem(9, new ItemStack(Items.APPLE));
      pizza.dlc.api.util.game.SwapSystem.grimStallInput(); pizza.dlc.api.util.game.SwapSystem.flushGrim();
      check(mode.clicks == before, "Deferred Grim swap ignored changed inventory");
      player.inventory.setItem(9, new ItemStack(Items.TOTEM_OF_UNDYING)); tick(pizzadlc);
      pizza.dlc.api.util.game.SwapSystem.grimStallInput(); pizza.dlc.api.util.game.SwapSystem.flushGrim();
      check(mode.clicks == before + 1 && player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "Native Grim swap failed after the input pause");
      totem.resetState(); player.gear.set(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
      player.inventory.setItem(9, new ItemStack(Items.TOTEM_OF_UNDYING)); swaps.swapMode.setCurrentMode("HvH");
      before = mode.clicks; player.inventoryMenu.setCarried(new ItemStack(Items.APPLE)); tick(pizzadlc);
      check(mode.clicks == before, "Swap ran while carrying a cursor item"); player.inventoryMenu.setCarried(ItemStack.EMPTY);
      player.containerMenu = new InventoryMenu(player.inventory, false, player); tick(pizzadlc);
      check(mode.clicks == before, "Swap ran in a different container"); player.containerMenu = player.inventoryMenu;
      totem.setEnabled(false);

      System.out.println("AutoTotem: native config, six threats, hotbar mapping, absorption, elytra, original item return, pop refill, legit pause, stale slots, world reset native Grim deferral, container/cursor guards and clean capture passed.");
   }
   private static Object get(Object target, String name) throws Exception { var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
   private static final class MovingInput extends ClientInput { MovingInput() { moveVector = Vec2.ZERO; } }
   private static final class TotemPlayer extends LocalPlayer {
      Inventory inventory; EntityEquipment gear; float hp, absorption; boolean flying; Vec3 velocity;
      private TotemPlayer() { super(null,null,null,null,null,null,false,null); }
      @Override public Inventory getInventory() { return inventory; }
      @Override public ItemStack getOffhandItem() { return gear.get(EquipmentSlot.OFFHAND); }
      @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return gear.get(slot); }
      @Override public void setItemSlot(EquipmentSlot slot, ItemStack item) { gear.set(slot,item); }
      @Override public float getHealth() { return hp; }
      @Override public float getAbsorptionAmount() { return absorption; }
      @Override public boolean hasEffect(Holder<MobEffect> effect) { return effect.equals(MobEffects.ABSORPTION) && absorption>0; }
      @Override public boolean isAlive() { return hp>0; }
      @Override public boolean isSprinting() { return false; }
      @Override public boolean isFallFlying() { return flying; }
      @Override public Vec3 getDeltaMovement() { return velocity; }
      @Override public BlockPos getBlockPosBelowThatAffectsMyMovement() { return BlockPos.ZERO; }
   }
   private static final class TotemLevel extends ClientLevel {
      List<Entity> entities; Player player; boolean anchor;
      private TotemLevel() { super(null,null,null,null,0,0,null,false,0,0); }
      @Override public Iterable<Entity> entitiesForRendering() { return entities; }
      @Override public BlockState getBlockState(BlockPos position) { return anchor && position.equals(BlockPos.ZERO) ? Blocks.RESPAWN_ANCHOR.defaultBlockState() : Blocks.AIR.defaultBlockState(); }
      @Override public Entity getEntity(int id) { return id == player.getId() ? player : null; }
   }
   private static final class TotemGameMode extends MultiPlayerGameMode {
      int clicks;
      private TotemGameMode() { super(null,null); }
      @Override public void handleContainerInput(int window, int slot, int button, ContainerInput input, Player player) {
         check(window == 0 && slot>=9 && slot<45 && button==40 && input == ContainerInput.SWAP, "Invalid 26.2 offhand click");
         int inventorySlot = slot>=36 ? slot-36 : slot;
         var item = player.getInventory().getItem(inventorySlot);
         player.getInventory().setItem(inventorySlot, player.getOffhandItem()); player.setItemSlot(EquipmentSlot.OFFHAND,item); clicks++;
      }
   }
}
