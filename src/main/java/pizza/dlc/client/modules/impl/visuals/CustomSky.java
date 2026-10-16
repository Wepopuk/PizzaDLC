package pizza.dlc.client.modules.impl.visuals;

import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(name = "CustomSky", description = "Красивое небо из Phantom (аврора/сакура/плазма)", category = ModuleCategory.VISUALS)
public class CustomSky extends Module {
   public final ModeSetting mode = new ModeSetting(
      "Режим", this, "Аврора", "Сакура", "Плазма", "Плазма 2", "Северное сияние", "Ночной", "Летнее небо", "Каустик"
   );
   public final ModeSetting summerTime = new ModeSetting("Время суток", this, "Дневное", "Ночное");
   public final SliderSetting speed = new SliderSetting("Скорость", this, 1.0F, 0.1F, 5.0F, 0.01F);
   public final SliderSetting scale = new SliderSetting("Размер", this, 5.0F, 1.0F, 20.0F, 0.01F);
   public final SliderSetting intensity = new SliderSetting("Интенсивность", this, 0.1F, 0.1F, 1.0F, 0.01F);
   public final SliderSetting alpha = new SliderSetting("Прозрачность", this, 1.0F, 0.3F, 1.0F, 0.05F);
   public final BooleanSetting useThemeColor = new BooleanSetting("В цвет темы", this);
   private long startMillis = -1L;

   public CustomSky() {
      this.summerTime.setVisible(() -> this.mode.is("Летнее небо"));
   }

   @Override
   public void onEnable() {
      this.startMillis = System.currentTimeMillis();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.startMillis = -1L;
      super.onDisable();
   }

   public long getStartMillis() {
      if (this.startMillis < 0L) this.startMillis = System.currentTimeMillis();
      return this.startMillis;
   }

   public ModeSetting getMode() {
      return this.mode;
   }

   public ModeSetting getSummerTime() {
      return this.summerTime;
   }

   public SliderSetting getSpeed() {
      return this.speed;
   }

   public SliderSetting getScale() {
      return this.scale;
   }

   public SliderSetting getIntensity() {
      return this.intensity;
   }

   public SliderSetting getAlpha() {
      return this.alpha;
   }

   public BooleanSetting getUseThemeColor() {
      return this.useThemeColor;
   }
}
