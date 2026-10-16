package pizza.dlc.api.util.combat;
import pizza.dlc.api.util.combat.CombatRuntime;

import lombok.Generated;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;

public final class CombatHealth implements pizza.dlc.api.interfaces.QuickImports {

   @Generated
   private CombatHealth() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static float m_6757(LivingEntity var0) {
      float var1 = var0.getHealth();
      ScoreHolder var2 = ScoreHolder.forNameOnly(var0.getScoreboardName());
      if (mc.level.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null && var0 instanceof Player var3) {
         ScoreAccess var4 = mc.level
            .getScoreboard()
            .getOrCreatePlayerScore(var2, mc.level.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME));
         if (mc.getCurrentServer() != null && CombatRuntime.moduleEnabled("FixHP")) {
            var1 = var4.get();
         }
      }

      return var1;
   }
}
