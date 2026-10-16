package pizza.dlc.client.modules.impl.player;

import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "SwapSettings",
   category = ModuleCategory.PLAYER,
   permanent = true
)
public class SwapSettings extends Module {
   private static SwapSettings INSTANCE;
   public final ModeSetting swapMode = new ModeSetting("Режим", this, "HvH", "Default", "Grim", "Test");
   public final ModeSetting actionMode = new ModeSetting("Режим свапов", this, "Рейджовый", "Легитный", "Matrix");
   public final SliderSetting stopTicks = new SliderSetting("Тики остановки", this, 4, 0, 20, 1);
   public final SliderSetting inventoryDelay = new SliderSetting("Задержка", this, 50.0F, 0.0F, 500.0F, 10.0F);
   public final BooleanSetting resetSprint = new BooleanSetting("Сброс спринта", this);

   public SwapSettings() {
      INSTANCE = this;
      stopTicks.setVisible(() -> actionMode.is("Легитный"));
      this.resetSprint.setEnabled(true);
      this.setEnabled(true);
   }

   @meteordevelopment.orbit.EventHandler private void world(pizza.dlc.api.events.impl.game.EventWorldLoad event) { pizza.dlc.api.util.game.SwapSystem.resetWorld(); }

   public static SwapSettings get() {
      return INSTANCE;
   }
}
