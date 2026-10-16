package pizza.dlc.client.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.game.EventMotion;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ElytraJump",
   description = "Взлёт+буст на элитре с фикс-углом; авто-свап элитры",
   category = ModuleCategory.MOVEMENT
)
public class ElytraJump extends Module {
   private static final String M_BRAVO = "BravoFFA";
   private static final String M_REALLY = "ReallyWorld";
   private static final String M_LONY = "LonyGrief";
   private static final int ARMOR_CHEST_SLOT = 6;
   private final ModeSetting mode = new ModeSetting("Режим", this, M_BRAVO, M_REALLY, M_LONY);
   private final BooleanSetting autoSwap = new BooleanSetting("Свапать элитру", this);
   private static final int SWAP_COOLDOWN_TICKS = 4;
   private boolean lockingRotation;
   private float lockedYaw;
   private int swapCooldown;
   private static final Item[] CHESTPLATES = new Item[]{
      Items.NETHERITE_CHESTPLATE,
      Items.DIAMOND_CHESTPLATE,
      Items.IRON_CHESTPLATE,
      Items.GOLDEN_CHESTPLATE,
      Items.CHAINMAIL_CHESTPLATE,
      Items.LEATHER_CHESTPLATE
   };

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;
      if (this.swapCooldown > 0) this.swapCooldown--;
      ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
      boolean wearingElytra = chest.getItem() == Items.ELYTRA;
      if (this.autoSwap.isEnabled() && mc.player.hurtTime > 0) {
         if (wearingElytra) this.swapToChestplate();
         return;
      }

      if (!wearingElytra) {
         if (this.autoSwap.isEnabled()) {
            this.swapToElytra();
         } else {
            mc.options.keyJump.setDown(false);
            this.lockingRotation = false;
         }
         return;
      }

      if (!mc.player.getAbilities().flying && !mc.player.onGround() && !mc.player.isInWater() && !mc.player.isFallFlying() && this.elytraUsable(chest)) {
         mc.player.tryToStartFallFlying();
         mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, Action.START_FALL_FLYING));
      }

      mc.options.keyJump.setDown(true);
      if (mc.player.onGround() || mc.player.isInWater() || mc.player.isInLava()) {
         this.lockingRotation = false;
      }

      if (mc.player.isFallFlying()) {
         Vec3 m = mc.player.getDeltaMovement();
         double add = this.mode.is(M_REALLY) ? 0.02658 : 0.06 + Math.random() * 0.001;
         mc.player.setDeltaMovement(m.x, m.y + add, m.z);
      }
   }

   @EventHandler
   public void onMotion(EventMotion e) {
      if (mc.player != null && mc.player.isFallFlying()) {
         if (!this.lockingRotation) {
            this.lockingRotation = true;
            this.lockedYaw = mc.player.getYRot();
         }

         float fixedPitch = 10.0F;
         e.setYaw(this.lockedYaw);
         e.setPitch(fixedPitch);
         mc.player.setYRot(this.lockedYaw);
         mc.player.setYHeadRot(this.lockedYaw);
         mc.player.setYBodyRot(this.lockedYaw);
         mc.player.setXRot(fixedPitch);
      } else {
         this.lockingRotation = false;
      }
   }

   private void swapToElytra() {
      this.swapToChest(InventoryUtility.find(Items.ELYTRA));
   }

   private void swapToChestplate() {
      this.swapToChest(this.findChestplate());
   }

   private void swapToChest(int slot) {
      if (slot == -1 || this.swapCooldown > 0) return;
      InventoryUtility.swap(slot, ARMOR_CHEST_SLOT);
      this.swapCooldown = SWAP_COOLDOWN_TICKS;
   }

   private int findChestplate() {
      for (Item cp : CHESTPLATES) {
         int slot = InventoryUtility.find(cp);
         if (slot != -1) {
            return slot;
         }
      }

      return -1;
   }

   private boolean elytraUsable(ItemStack s) {
      return s.getMaxDamage() <= 0 || s.getDamageValue() < s.getMaxDamage() - 1;
   }

   @Override
   public void onEnable() {
      this.swapCooldown = 0;
      if (this.autoSwap.isEnabled() && mc.player != null) {
         this.swapToElytra();
      }

      super.onEnable();
   }

   @Override
   public void onDisable() {
      if (mc.options != null) {
         mc.options.keyJump.setDown(false);
      }

      if (this.autoSwap.isEnabled() && mc.player != null && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
         this.swapCooldown = 0;
         this.swapToChestplate();
      }

      super.onDisable();
   }
}
