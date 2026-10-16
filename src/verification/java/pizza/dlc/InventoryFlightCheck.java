package pizza.dlc;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.*;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import pizza.dlc.api.events.impl.game.*;
import pizza.dlc.api.events.impl.combat.*;
import pizza.dlc.api.interfaces.ResolvedRotationAccess;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.util.combat.*;
import pizza.dlc.api.util.game.*;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.ModuleManager;
import pizza.dlc.client.modules.impl.combat.*;
import pizza.dlc.client.modules.impl.movement.*;
import pizza.dlc.client.modules.impl.player.*;

public final class InventoryFlightCheck {
   private static void check(boolean value,String text) { if (!value) throw new AssertionError(text); }
   private static Object allocate(Class<?> type) throws Exception {
      var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
      return unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),type);
   }
   private static void set(Class<?> type,Object target,String field,Object value) throws Exception {
      var f=type.getDeclaredField(field);f.setAccessible(true);f.set(target,value);
   }
   private static Object call(Object target,String method,Class<?>[] types,Object... args) throws Exception {
      var m=target.getClass().getDeclaredMethod(method,types);m.setAccessible(true);return m.invoke(target,args);
   }
   private static TestPlayer player(Vec3 position,int id) throws Exception {
      var player=(TestPlayer)allocate(TestPlayer.class);place(player,position,id);
      player.gear=new EntityEquipment();player.inv=new Inventory(player,player.gear);
      set(Player.class,player,"inventory",player.inv);set(Player.class,player,"inventoryMenu",new InventoryMenu(player.inv,false,player));
      player.containerMenu=player.inventoryMenu;player.input=new TestInput();player.velocity=Vec3.ZERO;player.cooldowns=new ItemCooldowns();return player;
   }
   private static void place(Entity entity,Vec3 position,int id) throws Exception {
      set(Entity.class,entity,"position",position);set(Entity.class,entity,"id",id);set(Entity.class,entity,"uuid",UUID.randomUUID());
      set(Entity.class,entity,"eyeHeight",1.6F);set(Entity.class,entity,"bb",new AABB(position.x-.3,position.y,position.z-.3,position.x+.3,position.y+1.8,position.z+.3));
   }
   public static void main(String[] args) throws Exception {
      var engine=new InventorySwapQueue.Engine();var owner=new Object();var log=new ArrayList<String>();boolean[] valid={true};
      check(engine.submit(owner,"swap",InventorySwapQueue.Mode.LEGIT,1,4,() -> valid[0],null,() -> log.add("use"),() -> log.add("restore")),"Legit enqueue failed");
      check(!engine.submit(owner,"swap",InventorySwapQueue.Mode.LEGIT,0,0,() -> true,null,() -> log.add("duplicate")),"Duplicate task accepted");
      check(engine.blocksMovement() && engine.blocksSprint(),"Legit input not held");
      engine.tick(true,true);check(log.isEmpty(),"Legit executed while moving");
      engine.tick(false,false);check(log.equals(List.of("use")),"First step missing");
      engine.tick(false,false);check(log.equals(List.of("use","restore")),"One-tick restore delay failed");
      for (int i=0;i<3;i++) engine.tick(false,false);check(engine.blocksMovement(),"Stop period ended early");
      engine.tick(false,false);check(engine.size()==0,"Stop period never ended");
      engine.submit(owner,"matrix",InventorySwapQueue.Mode.MATRIX,0,0,() -> true,null,() -> log.add("matrix"));
      engine.tick(true,true);check(!log.contains("matrix") && !engine.blocksMovement() && engine.blocksSprint(),"Matrix sprint gating failed");
      engine.tick(true,false);check(log.contains("matrix"),"Matrix incorrectly waited for walking to stop");
      engine.submit(owner,"rage",InventorySwapQueue.Mode.RAGE,0,0,() -> true,null,() -> log.add("rage"));check(log.contains("rage"),"Rage was not immediate");
      engine.submit(owner,"cancel",InventorySwapQueue.Mode.LEGIT,2,4,() -> valid[0],() -> log.add("cleanup"),() -> log.add("begin"),() -> log.add("finish"));
      engine.tick(false,false);valid[0]=false;engine.tick(false,false);
      check(log.contains("cleanup") && !log.contains("finish") && engine.size()==0,"Invalid context did not clean up a started task");

      net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
      for (var item:List.of(Items.ENDER_PEARL,Items.FIREWORK_ROCKET,Items.APPLE,Items.ELYTRA,Items.DIAMOND_CHESTPLATE)) {
         var components=DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE,item==Items.ELYTRA || item==Items.DIAMOND_CHESTPLATE ? 1 : 64)
            .set(DataComponents.ITEM_NAME,Component.literal(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath()));
         if (item==Items.DIAMOND_CHESTPLATE) components.set(DataComponents.EQUIPPABLE,net.minecraft.world.item.equipment.Equippable.builder(EquipmentSlot.CHEST).build());
         item.builtInRegistryHolder().bindComponents(components.build());
      }
      var mc=(Minecraft)allocate(Minecraft.class);set(Minecraft.class,null,"instance",mc);set(Minecraft.class,mc,"gameThread",Thread.currentThread());
      set(Minecraft.class,mc,"gui",allocate(Gui.class));
      var pizzadlc=new PizzaDLC();pizzadlc.friendManager=new pizza.dlc.api.filemanager.impl.FriendManager();set(PizzaDLC.class,null,"instance",pizzadlc);
      pizzadlc.getBus().registerLambdaFactory("pizza.dlc",(lookup,type) -> MethodHandles.privateLookupIn(type,MethodHandles.lookup()));
      var client=new PizzaDLCClient();set(PizzaDLCClient.class,null,"instance",client);var modules=new ModuleManager();set(PizzaDLCClient.class,client,"moduleManager",modules);
      var self=player(Vec3.ZERO,1);self.grounded=true;
      var connection=(TestConnection)allocate(TestConnection.class);connection.packets=new ArrayList<>();connection.player=self;set(LocalPlayer.class,self,"connection",connection);
      var level=(TestLevel)allocate(TestLevel.class);level.entities=new ArrayList<>();level.people=new ArrayList<>();level.prediction=new BlockStatePredictionHandler();level.people.add(self);
      var game=(TestGameMode)allocate(TestGameMode.class);mc.player=self;mc.level=level;mc.gameMode=game;set(CombatRuntime.class,null,"initialized",true);
      var swaps=new SwapSettings();var invMove=new InventoryMove();var helper=new ElytraHelper();var pearl=new TargetPearl();
      var aura=new AttackAura();var target=new ElytraTarget();var motion=new ElytraMotion();
      modules.registerModules(swaps,invMove,helper,pearl,aura,target,motion);
      for (var module:List.of(swaps,invMove,helper,pearl)) {var save=module.save();module.load(save);check(save.equals(module.save()),"Config round trip: "+module.getName());}
      helper.setEnabled(true);pearl.setEnabled(true);

      self.inv.setItem(0,new ItemStack(Items.APPLE,3));self.inv.setItem(9,new ItemStack(Items.ENDER_PEARL,4));
      check(SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(73,-28),pearl,"inventory",InventorySwapQueue.Mode.RAGE,null),"Inventory pearl not accepted");
      var use=connection.lastUse();check(use!=null && use.getHand()==InteractionHand.MAIN_HAND && use.getYRot()==73 && use.getXRot()==-28,"Aim lost in actual use packet");
      check(self.inv.getItem(0).is(Items.APPLE) && self.inv.getItem(0).getCount()==3 && self.inv.getItem(9).getCount()==3,"Inventory or selected item not restored");
      check(connection.held==0 && game.clicks==2,"Inventory transaction is not swap/use/restore");
      check(self.getYRot()==0 && self.getXRot()==0,"Tracked throw changed the camera");
      self.gear.set(EquipmentSlot.OFFHAND,new ItemStack(Items.ENDER_PEARL,2));
      SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(-90,15),pearl,"offhand",InventorySwapQueue.Mode.RAGE,null);
      check(connection.lastUse().getHand()==InteractionHand.OFF_HAND && self.getOffhandItem().getCount()==1,"Offhand pearl not used");self.gear.set(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
      self.inv.setItem(2,new ItemStack(Items.FIREWORK_ROCKET,2));int clicks=game.clicks;
      SwapSystem.useItemTracked(Items.FIREWORK_ROCKET,null,helper,"hotbar",InventorySwapQueue.Mode.RAGE,null);
      check(game.clicks==clicks && self.inv.getItem(2).getCount()==1 && connection.held==0,"Hotbar firework restore failed");
      self.inv.setItem(2,new ItemStack(Items.FIREWORK_ROCKET,4));
      SwapSystem.useItemTracked(Items.FIREWORK_ROCKET,null,helper,"delayed-hotbar",InventorySwapQueue.Mode.RAGE,1,null);
      SwapSystem.tick();check(connection.held==2,"Legit firework did not hold its hotbar for a tick");
      SwapSystem.tick();check(connection.held==0 && self.inv.getItem(2).getCount()==3,"Delayed hotbar did not restore");
      self.inv.setItem(2,ItemStack.EMPTY);self.inv.setItem(11,new ItemStack(Items.FIREWORK_ROCKET,4));
      SwapSystem.useItemTracked(Items.FIREWORK_ROCKET,null,helper,"delayed-inventory",InventorySwapQueue.Mode.LEGIT,1,null);
      SwapSystem.tick();check(connection.held==1,"Delayed inventory use did not select spare hotbar");
      helper.setEnabled(false);SwapSystem.tick();check(connection.held==0 && self.inv.getItem(11).is(Items.FIREWORK_ROCKET),"Disabling mid-use lost the restore");helper.setEnabled(true);
      int before=connection.packets.size();swaps.actionMode.setCurrentMode("Легитный");((TestInput)self.input).moving(true);
      SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(45,-30),pearl,"legit",InventorySwapQueue.Mode.LEGIT,null);
      SwapSystem.tick();check(connection.packets.size()==before && SwapSystem.grimStallInput(),"Legit did not wait for stopped input");
      ((TestInput)self.input).moving(false);SwapSystem.tick();check(connection.lastUse().getYRot()==45,"Deferred angle changed");
      for (int i=0;i<4;i++) SwapSystem.tick();check(!InventorySwapQueue.blocksMovement(),"Legit movement remained blocked");
      try (var scope=new pizza.dlc.api.util.render.streamproof.CleanRenderScope(null,null)) {
         check(!SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(40,20),pearl,"clean",InventorySwapQueue.Mode.LEGIT,null),"Clean capture queued a custom use");
         check(!SwapSystem.grimDefer(() -> { throw new AssertionError("Clean capture executed a queued click"); }),"Clean capture deferred a vanilla click");
      }
      before=connection.packets.size();SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(80,10),pearl,"stale",InventorySwapQueue.Mode.LEGIT,null);
      self.inv.setItem(9,new ItemStack(Items.APPLE));SwapSystem.tick();check(connection.packets.size()==before,"Deferred throw used changed inventory");SwapSystem.resetWorld();
      self.inv.setItem(9,new ItemStack(Items.ENDER_PEARL,8));
      before=connection.packets.size();SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(80,10),pearl,"disabled",InventorySwapQueue.Mode.LEGIT,null);
      pearl.setEnabled(false);SwapSystem.tick();check(connection.packets.size()==before,"Deferred throw ran after module disable");pearl.setEnabled(true);
      SwapSystem.useItemTracked(Items.ENDER_PEARL,new Angle(80,10),pearl,"world",InventorySwapQueue.Mode.LEGIT,null);
      mc.level=(TestLevel)allocate(TestLevel.class);SwapSystem.tick();check(connection.packets.size()==before,"Deferred throw ran in a different world");mc.level=level;SwapSystem.resetWorld();swaps.actionMode.setCurrentMode("Рейджовый");

      self.inv.setItem(10,new ItemStack(Items.ELYTRA));self.gear.set(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
      helper.autoTakeoff.setEnabled(true);check(helper.handleSwap(),"Elytra swap failed");
      check(self.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && self.inv.getItem(10).is(Items.DIAMOND_CHESTPLATE),"Elytra/chestplate/source slot not restored");
      helper.onUpdate(new EventUpdate());check(self.jumps==1,"Auto takeoff did not jump");self.grounded=false;self.velocity=new Vec3(0,-.1,0);
      helper.onUpdate(new EventUpdate());check(self.flying && connection.packets.stream().anyMatch(p -> p instanceof ServerboundPlayerCommandPacket command && command.getAction()==ServerboundPlayerCommandPacket.Action.START_FALL_FLYING),"Auto takeoff did not start elytra flight");
      check(helper.handleSwap() && self.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE),"Chestplate did not swap back");

      var enemy=player(new Vec3(10,0,0),2);var incoming=(TestPearl)allocate(TestPearl.class);place(incoming,new Vec3(11,1.5,0),3);incoming.velocity=new Vec3(1.2,.15,0);
      level.people.add(enemy);level.entities.add(enemy);level.entities.add(incoming);
      set(Entity.class,enemy,"level",level);set(Entity.class,self,"level",level);set(Entity.class,incoming,"level",level);
      check(call(pearl,"estimateOwner",new Class<?>[]{ThrownEnderpearl.class},incoming)==enemy,"Unknown pearl owner not inferred");
      check((boolean)call(pearl,"validPearl",new Class<?>[]{ThrownEnderpearl.class},incoming),"Unknown enemy pearl rejected");
      pearl.jumpCompensation.setEnabled(false);self.flying=false;self.grounded=true;self.velocity=Vec3.ZERO;
      before=connection.packets.size();pearl.onUpdate(new EventUpdate());
      check(connection.packets.size()>before && connection.lastUse()!=null,"TargetPearl did not throw for an incoming pearl");
      before=connection.packets.size();pearl.onUpdate(new EventUpdate());check(connection.packets.size()==before,"One enemy pearl caused duplicate throws");

      self.flying=true;enemy.flying=true;place(enemy,new Vec3(30,5,0),2);
      var ground=player(new Vec3(3,0,0),4);ground.grounded=true;
      var farGround=player(new Vec3(20,0,0),5);farGround.grounded=true;
      check(TargetSelection.groundReplacement(enemy,enemy,List.of(ground,farGround),self,12)==ground,"Escaped flying target not replaced with a near ground target");
      check(TargetSelection.groundReplacement(enemy,enemy,List.of(farGround),self,12)==enemy,"Distant ground target incorrectly selected");
      place(enemy,new Vec3(7,0,0),2);check(TargetSelection.groundReplacement(enemy,enemy,List.of(ground),self,12)==enemy,"Nearby flying target swapped prematurely");
      place(enemy,new Vec3(30,0,0),2);self.flying=false;check(TargetSelection.groundReplacement(enemy,enemy,List.of(ground),self,12)==enemy,"Retarget ran without own elytra flight");self.flying=true;
      aura.setEnabled(true);aura.setTarget(ground);motion.setEnabled(true);
      set(ElytraMotion.class,motion,"savedVelocity",new Vec3(1,.1,0));self.velocity=Vec3.ZERO;motion.preserveFlightForTargetSwitch();
      check(self.velocity.equals(new Vec3(1,.1,0)) && self.flying,"Retarget did not restore own flight velocity");
      for (int i=0;i<12;i++) call(motion,"tick",new Class<?>[]{CombatTickEvent.class},new CombatTickEvent());
      var velocity=new GlidingVelocityEvent(self.velocity);call(motion,"onGlidingVelocity",new Class<?>[]{GlidingVelocityEvent.class},velocity);
      check(velocity.getVelocity().equals(self.velocity),"Ground replacement eventually stopped flight");
      motion.setEnabled(false);pearl.setEnabled(false);helper.setEnabled(false);SwapSystem.resetWorld();
      System.out.println("Inventory/flight: rage, legit, Matrix, ordering, hold, cleanup, captured yaw/pitch packets, hotbar/inventory/offhand use, restores, stale context, owner inference, automatic pearl throwing, elytra swap/takeoff and ground retarget preserving flight passed.");
   }
   private static final class TestInput extends ClientInput { TestInput() { moveVector=Vec2.ZERO;keyPresses=Input.EMPTY; } void moving(boolean value) { moveVector=value ? new Vec2(0,1) : Vec2.ZERO;keyPresses=new Input(value,false,false,false,false,false,false); } }
   private static final class TestPlayer extends LocalPlayer implements ResolvedRotationAccess {
      Inventory inv;EntityEquipment gear;ItemCooldowns cooldowns;Vec3 velocity;boolean flying,grounded;int jumps;
      private TestPlayer() { super(null,null,null,null,null,null,false,null); }
      @Override public Inventory getInventory() { return inv; }
      @Override public ItemStack getOffhandItem() { return gear.get(EquipmentSlot.OFFHAND); }
      @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return gear.get(slot); }
      @Override public void setItemSlot(EquipmentSlot slot,ItemStack item) { gear.set(slot,item); }
      @Override public ItemCooldowns getCooldowns() { return cooldowns; }
      @Override public boolean isAlive() { return true; }
      @Override public boolean isSprinting() { return false; }
      @Override public boolean isFallFlying() { return flying; }
      @Override public boolean onGround() { return grounded; }
      @Override public Vec3 getDeltaMovement() { return velocity; }
      @Override public void setDeltaMovement(Vec3 value) { velocity=value; }
      @Override public void jumpFromGround() { jumps++; }
      @Override public void startFallFlying() { flying=true; }
      @Override public boolean isInWater() { return false; }
      @Override public boolean isUnderWater() { return false; }
      @Override public boolean isEyeInFluid(net.minecraft.tags.TagKey<Fluid> fluid) { return false; }
      @Override public Component getName() { return Component.literal("enemy"+getId()); }
      @Override public float getResolvedYaw() { return getYRot(); } @Override public float getResolvedPitch() { return getXRot(); }
      @Override public Vec3 getResolvedPos() { return position(); }
      @Override public void setResolvedYaw(float yaw) { setYRot(yaw); } @Override public void setResolvedPitch(float pitch) { setXRot(pitch); }
      @Override public void setResolvedPos(Vec3 position) {} @Override public Vec3 getResolvedForward() { return Vec3.directionFromRotation(getXRot(),getYRot()); }
   }
   private static final class TestPearl extends ThrownEnderpearl {
      Vec3 velocity;private TestPearl() { super(null,null); }
      @Override public Vec3 getDeltaMovement() { return velocity; }
      @Override public Entity getOwner() { return null; }
      @Override public boolean isInWater() { return false; }
   }
   private static final class TestLevel extends ClientLevel {
      List<Entity> entities;List<AbstractClientPlayer> people;BlockStatePredictionHandler prediction;
      private TestLevel() { super(null,null,null,null,0,0,null,false,0,0); }
      @Override public Iterable<Entity> entitiesForRendering() { return entities; }
      @Override public List<AbstractClientPlayer> players() { return people; }
      @Override public List<Entity> getEntities(Entity except,AABB box,Predicate<? super Entity> predicate) { return List.of(); }
      @Override public BlockStatePredictionHandler getBlockStatePredictionHandler() { return prediction; }
      @Override public FluidState getFluidState(BlockPos position) { return Fluids.EMPTY.defaultFluidState(); }
      @Override public BlockHitResult clip(ClipContext context) {
         Vec3 a=context.getFrom(),b=context.getTo();
         if (a.y>=0 && b.y<=0) { double ratio=a.y/(a.y-b.y);Vec3 point=a.lerp(b,ratio);return new BlockHitResult(point,Direction.UP,BlockPos.containing(point),false); }
         return BlockHitResult.miss(b,Direction.UP,BlockPos.containing(b));
      }
   }
   private static final class TestConnection extends ClientPacketListener {
      List<Packet<?>> packets;TestPlayer player;int held;
      private TestConnection() { super(null,null,null); }
      @Override public void send(Packet<?> packet) {
         packets.add(packet);
         if (packet instanceof ServerboundSetCarriedItemPacket slot) held=slot.getSlot();
         if (packet instanceof ServerboundUseItemPacket use) {
            var stack=use.getHand()==InteractionHand.OFF_HAND ? player.getOffhandItem() : player.inv.getItem(held);stack.shrink(1);
         }
      }
      ServerboundUseItemPacket lastUse() { return packets.stream().filter(p -> p instanceof ServerboundUseItemPacket).map(p -> (ServerboundUseItemPacket)p).reduce((a,b) -> b).orElse(null); }
   }
   private static final class TestGameMode extends MultiPlayerGameMode {
      int clicks;private TestGameMode() { super(null,null); }
      @Override public void handleContainerInput(int window,int slot,int button,ContainerInput input,Player player) {
         check(window==0 && input==ContainerInput.SWAP && button>=0 && button<9,"Invalid inventory swap");
         int index=slot>=36 ? slot-36 : slot;ItemStack source=slot==6 ? player.getItemBySlot(EquipmentSlot.CHEST) : player.getInventory().getItem(index);
         ItemStack hotbar=player.getInventory().getItem(button);player.getInventory().setItem(button,source);
         if (slot==6) player.setItemSlot(EquipmentSlot.CHEST,hotbar);else player.getInventory().setItem(index,hotbar);clicks++;
      }
   }
}
