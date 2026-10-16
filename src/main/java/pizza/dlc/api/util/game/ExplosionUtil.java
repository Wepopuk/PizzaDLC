package pizza.dlc.api.util.game;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class ExplosionUtil {
   public static final float CRYSTAL_POWER = 6.0F;

   private ExplosionUtil() {
   }

   public static float seenPercent(Vec3 center, Entity entity) {
      AABB bb = entity.getBoundingBox();
      double xs = 1.0 / ((bb.maxX - bb.minX) * 2.0 + 1.0);
      double ys = 1.0 / ((bb.maxY - bb.minY) * 2.0 + 1.0);
      double zs = 1.0 / ((bb.maxZ - bb.minZ) * 2.0 + 1.0);
      if (!(xs < 0.0) && !(ys < 0.0) && !(zs < 0.0)) {
         double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
         double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
         int hits = 0;
         int count = 0;

         for (double xx = 0.0; xx <= 1.0; xx += xs) {
            for (double yy = 0.0; yy <= 1.0; yy += ys) {
               for (double zz = 0.0; zz <= 1.0; zz += zs) {
                  Vec3 from = new Vec3(Mth.lerp(xx, bb.minX, bb.maxX) + xOffset, Mth.lerp(yy, bb.minY, bb.maxY), Mth.lerp(zz, bb.minZ, bb.maxZ) + zOffset);
                  if (entity.level().clip(new ClipContext(from, center, Block.COLLIDER, Fluid.NONE, entity)).getType() == Type.MISS) {
                     hits++;
                  }

                  count++;
               }
            }
         }

         return count == 0 ? 0.0F : (float)hits / count;
      }

      return 0.0F;
   }

   public static float rawDamage(Vec3 center, Entity entity, float power) {
      double doubleRadius = power * 2.0;
      double dist = Math.sqrt(entity.distanceToSqr(center)) / doubleRadius;
      if (dist > 1.0) {
         return 0.0F;
      }

      double pow = (1.0 - dist) * seenPercent(center, entity);
      return (float)((pow * pow + pow) / 2.0 * 7.0 * doubleRadius + 1.0);
   }

   public static float crystalDamage(Vec3 center, LivingEntity entity) {
      return damage(center, entity, 6.0F);
   }

   public static float damage(Vec3 center, LivingEntity entity, float power) {
      float raw = rawDamage(center, entity, power);
      if (raw <= 0.0F) {
         return 0.0F;
      }

      if (entity instanceof Player) {
         Difficulty difficulty = entity.level().getDifficulty();
         if (difficulty == Difficulty.HARD) {
            raw *= 1.5F;
         } else if (difficulty == Difficulty.EASY) {
            raw = Math.min(raw / 2.0F + 1.0F, raw);
         } else if (difficulty == Difficulty.PEACEFUL) {
            return 0.0F;
         }
      }
      float armor = entity.getArmorValue();
      float toughness = (float)entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      float realArmor = Mth.clamp(armor - raw / (2.0F + toughness / 4.0F), armor * 0.2F, 20.0F);
      raw *= 1.0F - realArmor / 25.0F;
      float epf = 0.0F;

      for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack piece = entity.getItemBySlot(slot);
         if (piece != null && !piece.isEmpty()) {
            ItemEnchantments ench = (ItemEnchantments)piece.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

            for (Holder<Enchantment> h : ench.keySet()) {
               if (h.is(Enchantments.PROTECTION)) {
                  epf += ench.getLevel(h);
               } else if (h.is(Enchantments.BLAST_PROTECTION)) {
                  epf += ench.getLevel(h) * 2.0F;
               }
            }
         }
      }

      raw *= 1.0F - Mth.clamp(epf, 0.0F, 20.0F) / 25.0F;
      MobEffectInstance resistance = entity.getEffect(MobEffects.RESISTANCE);
      if (resistance != null) {
         raw *= Math.max(0.0F, 1.0F - (resistance.getAmplifier() + 1) * 0.2F);
      }

      return Math.max(0.0F, raw);
   }
}
