package pizza.dlc.client.modules.impl.misc;

import net.minecraft.sounds.SoundEvent;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.sound.PizzaDLCSounds;

@ModuleInfo(
   name = "Sounds",
   description = "Звук при переключении модулей",
   category = ModuleCategory.OTHER
)
public class Sounds extends Module {
   public final ModeSetting mode = new ModeSetting("Звук", this, "Toggle", "Chaos", "Peanut", "S9", "Tonos", "Haze", "Hint");
   public final SliderSetting volume = new SliderSetting("Громкость", this, 1.0F, 0.0F, 1.0F, 0.01F);
   public final SliderSetting pitchOn = new SliderSetting("Питч вкл", this, 1.0F, 0.5F, 2.0F, 0.01F);
   public final SliderSetting pitchOff = new SliderSetting("Питч выкл", this, 0.8F, 0.5F, 2.0F, 0.01F);

   public SoundEvent selected() {
      return switch (this.mode.get()) {
         case "Chaos" -> PizzaDLCSounds.CHAOS;
         case "Peanut" -> PizzaDLCSounds.PEANUT;
         case "S9" -> PizzaDLCSounds.S9;
         case "Tonos" -> PizzaDLCSounds.TONOS;
         case "Haze" -> PizzaDLCSounds.HAZE;
         case "Hint" -> PizzaDLCSounds.HINT;
         default -> PizzaDLCSounds.TOGGLE;
      };
   }
}
