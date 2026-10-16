package pizza.dlc.client.modules.impl.misc;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "FixHP",
   description = "Реальное HP игроков из скорборда (скорборд-HP сервера)",
   category = ModuleCategory.OTHER
)
public class FixHP extends Module {
   public static FixHP INSTANCE;
   private static final Set<String> SCOREBOARD_HP_SERVERS = Set.of(
      "funtime", "cuteworld", "mineblaze", "reallyworld", "playrw", "saturn-x", "skytime", "space-times", "legendsgrief"
   );
   private static final String BELOW_NAME = "BelowName";
   private static final String ALL_OBJECTIVES = "AllObjectives";
   private final ModeSetting mode = new ModeSetting("Режим", this, BELOW_NAME, ALL_OBJECTIVES);
   private final BooleanSetting addAbsorption = new BooleanSetting("Учитывать абсорбцию", this);

   public FixHP() {
      INSTANCE = this;
      this.addAbsorption.setVisible(() -> this.mode.is(ALL_OBJECTIVES));
   }

   public static float getHealth(LivingEntity entity) {
      if (entity == null) {
         return 0.0F;
      }

      if (INSTANCE != null && INSTANCE.isEnabled() && entity instanceof Player) {
         int scoreHp = INSTANCE.mode.is(BELOW_NAME) ? readBelowName(entity) : readAllObjectives(entity);
         if (scoreHp >= 0) {
            return scoreHp;
         }
      }

      return entity.getHealth();
   }

   public static float getMaxHealth(LivingEntity entity) {
      if (entity == null) {
         return 20.0F;
      }

      return INSTANCE != null && INSTANCE.isEnabled() && entity instanceof Player && isScoreboardHpServer() ? 20.0F : entity.getMaxHealth();
   }

   private static int readBelowName(LivingEntity entity) {
      if (!isScoreboardHpServer()) {
         return -1;
      }

      Scoreboard sb = scoreboard();
      if (sb == null) {
         return -1;
      }

      Objective obj = sb.getDisplayObjective(DisplaySlot.BELOW_NAME);
      if (obj == null) {
         return -1;
      }

      ReadOnlyScoreInfo score = sb.getPlayerScoreInfo(entity, obj);
      return score == null ? -1 : score.value();
   }

   private static int readAllObjectives(LivingEntity entity) {
      Scoreboard sb = scoreboard();
      if (sb == null) {
         return -1;
      }

      for (Objective obj : sb.getObjectives()) {
         ReadOnlyScoreInfo score = sb.getPlayerScoreInfo(entity, obj);
         if (score != null) {
            int hp = score.value();
            if (INSTANCE.addAbsorption.isEnabled()) {
               hp += (int)entity.getAbsorptionAmount();
            }

            return Math.max(hp, 1);
         }
      }

      return -1;
   }

   private static Scoreboard scoreboard() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level != null ? mc.level.getScoreboard() : null;
   }

   private static boolean isScoreboardHpServer() {
      ServerData data = Minecraft.getInstance().getCurrentServer();
      if (data != null && data.ip != null && !data.ip.isEmpty()) {
         String lower = data.ip.toLowerCase();

         for (String server : SCOREBOARD_HP_SERVERS) {
            if (!lower.contains(server)) continue;

            return true;
         }

         return false;
      }

      return false;
   }
}
