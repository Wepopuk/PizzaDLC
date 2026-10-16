package pizza.dlc.client.modules.impl.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.combat.CombatInventory;
import pizza.dlc.api.util.game.InventoryNotify;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.api.util.game.InventorySwapQueue;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(name="ElytraHelper", description="Свап элитры, авто взлет и фейерверк по бинду", category=ModuleCategory.PLAYER)
public class ElytraHelper extends Module {
   public final BindSetting swapBind=new BindSetting("Свап элитры",this,-1);
   public final BindSetting fireworkBind=new BindSetting("Фейерверк",this,-1);
   public final ModeSetting fireworkMode=new ModeSetting("Мод запуска фейверка",this,"Общий","Рейджовый","Легитный");
   public final BooleanSetting autoTakeoff=new BooleanSetting("Авто взлет",this);
   private boolean takingOff;
   private int takeoffTicks;

   @EventHandler public void onKey(EventKey event) {
      if (event.getAction()!=1 || mc.player==null || mc.level==null || mc.gui.screen()!=null) return;
      if (event.getKey()==swapBind.getKey()) { handleSwap(); event.setCancelled(true); }
      else if (event.getKey()==fireworkBind.getKey() && mc.player.isFallFlying()) {
         int slot=InventoryUtility.find(Items.FIREWORK_ROCKET);
         boolean hotbar=slot>=36 || mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET);
         InventorySwapQueue.Mode mode=fireworkMode.is("Общий") ? InventorySwapQueue.configuredMode()
            : fireworkMode.is("Легитный") && !hotbar ? InventorySwapQueue.Mode.LEGIT : InventorySwapQueue.Mode.RAGE;
         SwapSystem.useItemTracked(Items.FIREWORK_ROCKET,null,this,"firework",mode,fireworkMode.is("Легитный") ? 1 : 0,null);
         event.setCancelled(true);
      }
   }
   public boolean handleSwap() {
      if (mc.player==null || mc.gameMode==null || mc.level==null) return false;
      takingOff=false;
      boolean chestplate=mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
      int slot=chestplate ? CombatInventory.m_7070() : InventoryUtility.find(Items.ELYTRA);
      if (slot<0) { if (chestplate) InventoryNotify.missing("Нагрудника"); else InventoryNotify.missing(Items.ELYTRA); return false; }
      var player=mc.player;
      ItemStack expected=player.inventoryMenu.getSlot(slot).getItem().copy();
      ItemStack chest=player.getItemBySlot(EquipmentSlot.CHEST).copy();
      return InventorySwapQueue.submit(this,"chest",SwapSystem.effectiveMode(InventorySwapQueue.configuredMode()),0,null,() -> {
         if (!ItemStack.matches(expected,player.inventoryMenu.getSlot(slot).getItem()) || !ItemStack.matches(chest,player.getItemBySlot(EquipmentSlot.CHEST))) return;
         int hotbar=slot>=36 ? slot-36 : (player.getInventory().getSelectedSlot()+1)%9;
         if (slot<36) InventoryUtility.sendClick(slot,hotbar,ContainerInput.SWAP);
         InventoryUtility.sendClick(6,hotbar,ContainerInput.SWAP);
         if (slot<36) InventoryUtility.sendClick(slot,hotbar,ContainerInput.SWAP);
         if (mc.gui.screen()==null) player.connection.send(new ServerboundContainerClosePacket(0));
         takingOff=!chestplate && autoTakeoff.isEnabled(); takeoffTicks=0;
      });
   }
   @EventHandler public void onUpdate(EventUpdate event) {
      if (!takingOff) return;
      if (mc.player==null || mc.level==null || !autoTakeoff.isEnabled() || !mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
          || mc.player.isFallFlying() || ++takeoffTicks>20 || mc.player.isInWater() || mc.player.isPassenger()) { takingOff=false; return; }
      if (InventorySwapQueue.blocksMovement()) return;
      if (mc.player.onGround()) mc.player.jumpFromGround();
      else if (mc.player.getDeltaMovement().y<0) {
         mc.player.startFallFlying();
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player,ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
      }
   }
   private void resetState() { takingOff=false; takeoffTicks=0; InventorySwapQueue.cancel(this); }
   @EventHandler private void world(EventWorldLoad event) { resetState(); }
   @Override public void onDisable() { resetState(); super.onDisable(); }
}
