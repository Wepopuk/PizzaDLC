package pizza.dlc.api.util.game;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ToolSpeed {
   private ToolSpeed() {
   }

   public static float speed(ItemStack tool, BlockState state, Level level) {
      float speed = tool.isEmpty() ? 1.0F : tool.getDestroySpeed(state);
      if (speed > 1.0F) {
         int efficiency = enchantLevel(Enchantments.EFFICIENCY, tool, level);
         if (efficiency > 0) speed += efficiency * efficiency + 1;
      }
      return speed;
   }

   public static float progressPerTick(ItemStack tool, BlockState state, Level level) {
      boolean correct = !state.requiresCorrectToolForDrops() || !tool.isEmpty() && tool.isCorrectToolForDrops(state);
      return speed(tool, state, level) / (correct ? 30.0F : 100.0F);
   }

   public static int enchantLevel(ResourceKey<Enchantment> key, ItemStack stack, Level level) {
      if (stack == null || stack.isEmpty() || level == null) return 0;
      try {
         return EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), stack);
      } catch (RuntimeException ignored) {
         return 0;
      }
   }
}
