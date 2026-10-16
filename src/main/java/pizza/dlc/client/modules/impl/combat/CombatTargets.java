package pizza.dlc.client.modules.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.settings.impl.MultiModeSetting;

public final class CombatTargets {
   public static final String PLAYERS = "Игроки";
   public static final String NAKED = "Голые";
   public static final String MOBS = "Мобы";
   public static final String ANIMALS = "Животные";
   public static final String FRIENDS = "Друзья";
   private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

   private CombatTargets() {
   }

   public static boolean accepts(MultiModeSetting types, LivingEntity entity) {
      Minecraft mc = Minecraft.getInstance();
      if (entity == null || mc.player == null || entity == mc.player) return false;
      if (!entity.isAlive() || entity.isRemoved() || entity.getHealth() <= 0.0F) return false;
      if (AntiBot.isBot(entity)) return false;
      if (entity instanceof Player player) {
         if (player.isSpectator() || player.isCreative()) return false;
         if (isFriend(player)) return enabled(types, FRIENDS);
         return enabled(types, PLAYERS) && (!isNaked(player) || enabled(types, NAKED));
      }
      if (entity instanceof Animal) return enabled(types, ANIMALS);
      return entity instanceof Mob ? enabled(types, MOBS) : !(entity instanceof ArmorStand);
   }

   public static boolean isNaked(LivingEntity entity) {
      for (EquipmentSlot slot : ARMOR) {
         if (!entity.getItemBySlot(slot).isEmpty()) return false;
      }
      return true;
   }

   private static boolean isFriend(Player player) {
      return PizzaDLC.getInstance() != null
         && PizzaDLC.getInstance().getFriendManager() != null
         && PizzaDLC.getInstance().getFriendManager().isFriend(player.getName().getString());
   }

   private static boolean enabled(MultiModeSetting types, String name) {
      var setting = types.get(name);
      return setting != null && setting.isEnabled();
   }
}
