package pizza.dlc.api.util.combat;
import pizza.dlc.api.util.combat.CombatRuntime;

import lombok.Generated;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.state.BlockState;

public final class CombatInventory implements pizza.dlc.api.interfaces.QuickImports {

   public static void m_7064(Item item) {
      pizza.dlc.api.util.game.SwapSystem.useItemTracked(item,null,CombatInventory.class,"use:"+item,
         pizza.dlc.api.util.game.InventorySwapQueue.configuredMode(),null);
   }

   public static void m_7065(int from, int to) {
      pizza.dlc.api.util.game.InventoryUtility.swap(m_7069(from),m_7069(to));
   }

   public static void m_7066(int slot, int button) {
      if (slot != -1 && button != -1) pizza.dlc.api.util.game.InventoryUtility.sendClick(slot,button,ContainerInput.SWAP);
   }

   public static boolean m_7067(Item var0) {
      return mc.player == null ? false : mc.player.getMainHandItem().is(var0) || mc.player.getOffhandItem().is(var0);
   }

   public static int m_7068(Item var0) {
      return m_7077(var0, 0, 35);
   }

   @Generated
   private CombatInventory() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static int m_7069(int var0) {
      return var0 >= 0 && var0 <= 8 ? 36 + var0 : var0;
   }

   public static int m_7070() {
      int var0 = -1;
      int var1 = -1;

      for (int var2 = 0; var2 < mc.player.getInventory().getNonEquipmentItems().size(); var2++) {
         ItemStack var3 = (ItemStack)mc.player.getInventory().getNonEquipmentItems().get(var2);
         if (!var3.isEmpty() && !var3.is(Items.ELYTRA)) {
            Equippable var4 = (Equippable)var3.get(DataComponents.EQUIPPABLE);
            if (var4 != null && var4.slot() == EquipmentSlot.CHEST) {
               int var5 = m_7076(var3);
               if (var5 > var1) {
                  var1 = var5;
                  var0 = var2;
               }
            }
         }
      }

      return var0 >= 0 && var0 < 9 ? var0 + 36 : var0;
   }

   public static void m_7071(CombatInventory.ClickAction var0, int var1, int var2) {
      if (var1 != -1 && var2 != -1 && var1 != CombatRuntime.sentSlot()) {
         switch (var0) {
            case Normal:
               mc.player.getInventory().setSelectedSlot(var1);
               ((pizza.dlc.mixin.client.MultiPlayerGameModeAccessor)mc.gameMode).invokerEnsureHasSentCarriedItem();
               break;
            case Silent:
               mc.getConnection().send(new ServerboundSetCarriedItemPacket(var1));
               break;
            case Alternative:
               m_7075(CombatInventory.InventoryScope.Swap, var1, var2);
         }
      }
   }

   public static void m_7072(CombatInventory.WeaponType var0) {
      switch (var0) {
         case MainHand:
            mc.player.swing(InteractionHand.MAIN_HAND);
            break;
         case OffHand:
            mc.player.swing(InteractionHand.OFF_HAND);
            break;
         case Packet:
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
      }
   }

   private static void m_7073(int var0, int var1) {
      if (mc.player != null) {
         if (mc.player.isUsingItem() && !mc.player.isBlocking()) {
            m_7065(var0, 45);
            mc.getConnection()
               .send(
                  new ServerboundUseItemPacket(
                     InteractionHand.OFF_HAND,
                     mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(),
                     mc.player.getYRot(),
                     mc.player.getXRot()
                  )
               );
            m_7065(var0, 45);
         } else {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(var0));
            mc.getConnection()
               .send(
                  new ServerboundUseItemPacket(
                     InteractionHand.MAIN_HAND,
                     mc.level.getBlockStatePredictionHandler().startPredicting().currentSequence(),
                     mc.player.getYRot(),
                     mc.player.getXRot()
                  )
               );
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(var1));
         }
      }
   }

   public static int m_7074(Item var0) {
      if (mc != null && mc.player != null) {
         for (int var1 = 0; var1 < 36; var1++) {
            ItemStack var2 = mc.player.getInventory().getItem(var1);
            if (!var2.isEmpty() && var2.getItem() == var0) {
               return var1;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static void m_7075(CombatInventory.InventoryScope scope, int from, int to) {
      if (from == -1 || to == -1) return;
      if (scope == InventoryScope.Pickup) pizza.dlc.api.util.game.InventoryUtility.swap(m_7069(from),m_7069(to));
      else pizza.dlc.api.util.game.InventoryUtility.sendClick(from,to,ContainerInput.SWAP);
   }

   private static int m_7076(ItemStack var0) {
      if (var0.is(Items.NETHERITE_CHESTPLATE)) {
         return 5;
      } else if (var0.is(Items.DIAMOND_CHESTPLATE)) {
         return 4;
      } else if (var0.is(Items.IRON_CHESTPLATE)) {
         return 3;
      } else if (var0.is(Items.CHAINMAIL_CHESTPLATE)) {
         return 2;
      } else if (var0.is(Items.GOLDEN_CHESTPLATE)) {
         return 1;
      } else {
         return var0.is(Items.LEATHER_CHESTPLATE) ? 0 : -1;
      }
   }

   public static int m_7077(Item var0, int var1, int var2) {
      for (int var3 = var2; var3 >= var1; var3--) {
         if (mc.player.getInventory().getItem(var3).getItem() == var0) {
            return var3;
         }
      }

      return -1;
   }

   public static int m_7078(Item var0) {
      if (mc.player == null) {
         return 0;
      }

      int var1 = 0;

      for (ItemStack var3 : mc.player.getInventory().getNonEquipmentItems()) {
         if (!var3.isEmpty() && var3.getItem().equals(var0)) {
            var1 += var3.getCount();
         }
      }

      return var1;
   }

   public static int m_7079() {
      for (int var0 = 0; var0 < 45; var0++) {
         ItemStack var1 = mc.player.getInventory().getItem(var0);
         if (!var1.isEmpty() && var1.getItem() instanceof AxeItem) {
            return var0;
         }
      }

      return -1;
   }

   public static void m_7080(CombatInventory.ClickAction var0, int var1, int var2) {
      if (var1 != -1 && var2 != -1 && var1 != CombatRuntime.sentSlot()) {
         switch (var0) {
            case Normal:
               mc.player.getInventory().setSelectedSlot(var2);
               ((pizza.dlc.mixin.client.MultiPlayerGameModeAccessor)mc.gameMode).invokerEnsureHasSentCarriedItem();
               break;
            case Silent:
               mc.getConnection().send(new ServerboundSetCarriedItemPacket(var2));
               break;
            case Alternative:
               m_7075(CombatInventory.InventoryScope.Swap, var1, var2);
         }
      }
   }

   public static int m_7081(BlockState var0, boolean var1) {
      if (mc.player == null) {
         return -1;
      }

      int var2 = -1;
      float var3 = 1.0F;
      int var4 = var1 ? 9 : 36;

      for (int var5 = 0; var5 < var4; var5++) {
         ItemStack var6 = mc.player.getInventory().getItem(var5);
         if (!var6.isEmpty()) {
            float var7 = var6.getDestroySpeed(var0);
            if (var7 > var3) {
               var3 = var7;
               var2 = var5;
            }
         }
      }

      return var2;
   }

   public static void m_7082(int var0) {
      if (var0 != -1 && var0 != CombatRuntime.sentSlot()) {
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(var0));
      }
   }

   public enum InventoryScope {
      Pickup,
      Swap;
   }

   public enum ClickAction {
      Normal("settings.normal"),
      Silent("settings.switch.silent"),
      Alternative("settings.switch.alternative"),
      None("settings.none");

      public final String f_23146;

      public String getLabel() {
         return this.f_23146;
      }

      @Generated
      ClickAction(String var3) {
         this.f_23146 = var3;
      }
   }

   public enum WeaponType {
      MainHand("settings.swing.mainhand"),
      OffHand("settings.swing.offhand"),
      Packet("settings.packet"),
      None("settings.none");

      public final String f_23147;

      public String getLabel() {
         return this.f_23147;
      }

      @Generated
      WeaponType(String var3) {
         this.f_23147 = var3;
      }
   }
}
