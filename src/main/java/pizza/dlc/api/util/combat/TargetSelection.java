package pizza.dlc.api.util.combat;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.combat.CombatHealth;
import pizza.dlc.api.util.combat.TargetNameFilter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Generated;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public final class TargetSelection implements pizza.dlc.api.interfaces.QuickImports {

   private static boolean m_7569(LivingEntity var0, float var1) {
      if (!pizza.dlc.api.neuro.NeuroSystem.isBot(var0) && var0 instanceof Player var2 && pizza.dlc.client.modules.impl.combat.AntiBot.isBot(var2)) {
         return false;
      } else {
         return !var0.isAlive() ? false : var0.distanceTo(mc.player) <= var1;
      }
   }

   @Generated
   private TargetSelection() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static List<LivingEntity> m_7570(float var0, boolean var1, boolean var2, boolean var3, boolean var4) {
      return new ArrayList<>(m_7571(var0, var1, var2, var3, var4));
   }

   private static List<LivingEntity> m_7571(float var0, boolean var1, boolean var2, boolean var3, boolean var4) {
      ArrayList var5 = new ArrayList();

      for (Entity var7 : mc.level.entitiesForRendering()) {
         if (var7 != mc.player && !(var7 instanceof LivingEntity var8 && !m_7569(var8, var0))) {
            if (pizza.dlc.api.neuro.NeuroSystem.isBot(var7) && (var1 || var2)) { var5.add(var7); continue; }
            if (TargetNameFilter.m_5567()) {
               if (var7 instanceof Player var10 && TargetNameFilter.m_5565(var10)) {
                  var5.add(var10);
               }
            } else if (var7 instanceof Player var11) {
               if (pizza.dlc.PizzaDLC.getInstance().getFriendManager().isFriend(var11.getGameProfile().name())) {
                  if (var3) {
                     var5.add(var11);
                  }
               } else if (var11.getArmorValue() == 0) {
                  if (var2) {
                     var5.add(var11);
                  }
               } else if (var1) {
                  var5.add(var11);
               }
            } else if (var7 instanceof Mob var9 && var4) {
               var5.add(var9);
            }
         }
      }

      return var5;
   }

   public static LivingEntity groundReplacement(LivingEntity escaped, LivingEntity selected, List<LivingEntity> candidates,
                                                LivingEntity player, float escapeDistance) {
      if (escaped == null || !player.isFallFlying() || !escaped.isFallFlying() || escaped.distanceTo(player)<escapeDistance) return selected;
      return candidates.stream().filter(entity -> entity != escaped && entity.onGround() && !entity.isFallFlying()
         && entity.distanceTo(player)<=escapeDistance)
         .min(Comparator.comparingDouble((LivingEntity entity) -> entity.distanceToSqr(player)).thenComparingInt(Entity::getId)).orElse(selected);
   }

   public static LivingEntity m_7574(LivingEntity var0, float var1, boolean var2, boolean var3, boolean var4, boolean var5, ModeSetting var6) {
      return var6.is("Умная")
         ? m_7571(var1, var2, var3, var4, var5)
            .stream()
            .min(
               Comparator.<LivingEntity, Boolean>comparing(var1x -> var1x != var0)
                  .thenComparingDouble(LivingEntity::getArmorValue)
                  .thenComparingDouble(CombatHealth::m_6757)
                  .thenComparingDouble(var0x -> var0x.distanceTo(mc.player))
                  .thenComparingInt(Entity::getId)
            )
            .orElse(null)
         : m_7571(var1, var2, var3, var4, var5)
            .stream()
            .min(
               Comparator.<LivingEntity, Boolean>comparing(var1x -> var1x != var0)
                  .thenComparingDouble(
                     var1x -> var6.is("Дистанция")
                        ? var1x.distanceTo(mc.player)
                        : (var6.is("Броня") ? var1x.getArmorValue() : CombatHealth.m_6757(var1x))
                  )
                  .thenComparingInt(Entity::getId)
            )
            .orElse(null);
   }
}
