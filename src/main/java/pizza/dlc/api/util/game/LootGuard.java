package pizza.dlc.api.util.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LootGuard {
   public static final String VALUABLE = "Ценные";
   public static final String ALL = "Все";
   public static final String OFF = "Выкл";
   private static final float DAMAGE_MARGIN = 1.0F;
   private static final Set<Item> VALUABLE_ITEMS = Set.of(
      Items.TOTEM_OF_UNDYING,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.GOLDEN_APPLE,
      Items.ENDER_PEARL,
      Items.END_CRYSTAL,
      Items.RESPAWN_ANCHOR,
      Items.EXPERIENCE_BOTTLE,
      Items.ENCHANTED_BOOK,
      Items.POTION,
      Items.SPLASH_POTION,
      Items.LINGERING_POTION,
      Items.NETHERITE_INGOT,
      Items.NETHERITE_SCRAP,
      Items.NETHERITE_BLOCK,
      Items.DIAMOND,
      Items.DIAMOND_BLOCK,
      Items.EMERALD_BLOCK,
      Items.NETHER_STAR,
      Items.BEACON
   );
   private static final Map<Integer, Integer> HEALTH = new HashMap<>();
   private static Level trackedLevel;

   private LootGuard() {
   }

   public static boolean threatened(Level level, Vec3 center, float power, String mode) {
      if (level == null || OFF.equals(mode)) return false;

      DamageSource blast = level.damageSources().explosion(null, null);
      for (ItemEntity item : itemsAround(level, center, power)) {
         ItemStack stack = item.getItem();
         if (VALUABLE.equals(mode) && !isValuable(stack) || !stack.canBeHurtBy(blast)) continue;

         float damage = ExplosionUtil.rawDamage(center, item, power);
         if (damage > 0.0F && damage + DAMAGE_MARGIN > health(level, item) - 1) return true;
      }

      return false;
   }

   public static void onExplosion(Level level, Vec3 center, float power) {
      if (level == null) return;

      sync(level);
      HEALTH.keySet().removeIf(id -> level.getEntity(id) == null);
      DamageSource blast = level.damageSources().explosion(null, null);
      for (ItemEntity item : itemsAround(level, center, power)) {
         if (!item.getItem().canBeHurtBy(blast)) continue;

         float damage = ExplosionUtil.rawDamage(center, item, power);
         if (damage > 0.0F) HEALTH.put(item.getId(), (int)(health(level, item) - damage));
      }
   }

   public static boolean isValuable(ItemStack stack) {
      if (stack.isEmpty()) return false;
      if (stack.isDamageableItem() || stack.isEnchanted() || VALUABLE_ITEMS.contains(stack.getItem())) return true;

      return stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock;
   }

   private static Iterable<ItemEntity> itemsAround(Level level, Vec3 center, float power) {
      double reach = power * 2.0;
      return level.getEntitiesOfClass(ItemEntity.class, AABB.ofSize(center, reach * 2.0, reach * 2.0, reach * 2.0), Entity::isAlive);
   }

   private static int health(Level level, ItemEntity item) {
      sync(level);
      return HEALTH.getOrDefault(item.getId(), ItemEntity.DEFAULT_HEALTH);
   }

   private static void sync(Level level) {
      if (level == trackedLevel) return;

      HEALTH.clear();
      trackedLevel = level;
   }
}
