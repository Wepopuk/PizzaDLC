package pizza.dlc.client.modules.impl.visuals.animatedhands.lua;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiRuntime;

public final class LuaItem {
   private static final TagKey<Item> C_RODS = conventional("rods");
   private static final TagKey<Item> C_TOOLS = conventional("tools");
   private static final TagKey<Item> C_MACE_TOOLS = conventional("tools/mace");

   private static TagKey<Item> conventional(String path) {
      return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
   }

   public float getAttackDamage(ItemStack stack) {
      ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) return 0.0F;
      float damage = 0.0F;
      for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
         if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
            damage += (float)entry.modifier().amount();
         }
      }
      return damage;
   }

   public boolean isOf(ItemStack stack, Item item) {
      return item != null && stack.is(item);
   }

   public boolean isIn(ItemStack stack, TagKey<Item> tag) {
      return tag != null && stack.is(tag);
   }

   public boolean isEmpty(ItemStack stack) {
      return stack.isEmpty();
   }

   public String getUseAction(ItemStack stack) {
      return stack.getUseAnimation().getSerializedName();
   }

   public String getName(ItemStack stack) {
      return stack.getItem().toString();
   }

   public String getActualName(ItemStack stack) {
      return stack.getCustomName() != null ? stack.getCustomName().getString() : stack.getHoverName().getString();
   }

   public boolean isChargedCrossbow(ItemStack stack) {
      return CrossbowItem.isCharged(stack);
   }

   public ItemStack getDefaultStack(Item item) {
      return item.getDefaultInstance();
   }

   public boolean isBlock(ItemStack stack) {
      return Block.byItem(stack.getItem()) != Blocks.AIR;
   }

   public boolean shouldTranslateItem(ItemStack stack) {
      ItemUseAnimation use = stack.getUseAnimation();
      boolean weaponLike = stack.getItem() instanceof FishingRodItem
         || stack.is(C_RODS)
         || stack.is(C_TOOLS)
         || stack.is(ItemTags.SWORDS)
         || stack.is(C_MACE_TOOLS)
         || use == ItemUseAnimation.BLOCK
         || this.getAttackDamage(stack) > 0.0F;
      return !weaponLike || use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK || use == ItemUseAnimation.SPYGLASS;
   }

   public boolean isCustomTranslate(ItemStack stack) {
      return HmiRuntime.flag(HmiRuntime.TRANSLATE_ITEM, this.getName(stack), false);
   }

   public void setTranslate(ItemStack stack, boolean value) {
   }

   public void setRenderAsBlock(ItemStack stack, boolean value) {
      if (this.isBlock(stack)) HmiRuntime.RENDER_AS_BLOCK.put(this.getName(stack), value);
   }

   public boolean shouldRenderAsBlock(ItemStack stack) {
      return this.isBlock(stack) && HmiRuntime.flag(HmiRuntime.RENDER_AS_BLOCK, this.getName(stack), true);
   }

   public boolean isLantern(ItemStack stack) {
      return Block.byItem(stack.getItem()) instanceof LanternBlock;
   }

   public boolean isThrowable(ItemStack stack) {
      return stack.getItem() instanceof SplashPotionItem || stack.getItem() instanceof ProjectileItem;
   }

   public void setSwingSpeed(ItemStack stack, double ticks) {
      HmiRuntime.ITEM_SWING_SPEED.put(this.getName(stack), (int)ticks);
   }

   public boolean isEnchanted(ItemStack stack) {
      return stack.hasFoil();
   }

   public void setMainStack(Item item) {
      HmiRuntime.mainHandOverride = item.getDefaultInstance();
   }

   public void setOffStack(Item item) {
      HmiRuntime.offHandOverride = item.getDefaultInstance();
   }

   public void setChestOpen(double progress) {
   }

   public void setShulkerOpen(double progress) {
   }

   public SpearData getSpearData(ItemStack stack) {
      LocalPlayer player = Minecraft.getInstance().player;
      KineticWeapon weapon = stack.get(DataComponents.KINETIC_WEAPON);
      if (player == null || weapon == null) return new SpearData(false, false, false, false);
      int used = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() + 1);
      int delay = weapon.delayTicks();
      boolean dismount = used < weapon.dismountConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + delay;
      boolean knockback = used < weapon.knockbackConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + delay;
      boolean damage = used < weapon.damageConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + delay;
      boolean impact = player.getTicksSinceLastKineticHitFeedback(0.0F) == 1.0F;
      return new SpearData(damage, knockback, dismount, impact);
   }
}
