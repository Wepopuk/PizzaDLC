package pizza.dlc.client.modules.impl.visuals;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ShaderESP",
   description = "Шейдерная glow-обводка целей",
   category = ModuleCategory.VISUALS
)
public class ShaderESP extends Module {
   public final MultiModeSetting targets = new MultiModeSetting("Цели", this, "Игроки", "Мобы", "Монстры", "Себя");
   public final SliderSetting animSpeed = new SliderSetting("Скорость", this, 1.0F, 0.0F, 4.0F, 0.1F);
   public final SliderSetting radius = new SliderSetting("Радиус", this, 4.0F, 1.0F, 8.0F, 1.0F);
   public final SliderSetting intensity = new SliderSetting("Интенсивность", this, 1.0F, 0.2F, 1.0F, 0.05F);
   public final SliderSetting whiteness = new SliderSetting("Белизна", this, 0.2F, 0.0F, 1.0F, 0.05F);
   public final ModeSetting direction = new ModeSetting("Направление", this, "Наружу", "Внутрь", "Внутрь + Наружу");
   public final BooleanSetting contour = new BooleanSetting("Контур", "Резкий край силуэта поверх свечения", this);

   public ShaderESP() {
      this.targets.get("Игроки").setEnabled(true);
      this.targets.get("Монстры").setEnabled(true);
      this.contour.setEnabled(true);
   }

   public boolean accept(Entity ent) {
      if (ent != mc.player) {
         if (!(ent instanceof LivingEntity)) {
            return false;
         }

         String key = ent instanceof Player ? "Игроки" : (ent instanceof Enemy ? "Монстры" : "Мобы");
         return this.targets.get(key).isEnabled();
      }

      return !mc.options.getCameraType().isFirstPerson() && this.targets.get("Себя").isEnabled();
   }

   public int colorNow() {
      float secs = (float)(System.nanoTime() / 1.0E9);
      float phase = secs * this.animSpeed.getValue() * 0.25F % 1.0F;
      return animColor(phase, ThemeManager.accent(), ThemeManager.secondary());
   }

   public int coloredForOutline() {
      int c = this.colorNow();
      float in = this.intensity.getValue();
      float w = this.whiteness.getValue();
      int r = c >> 16 & 0xFF;
      int g = c >> 8 & 0xFF;
      int b = c & 0xFF;
      r = Math.round(r + (255 - r) * w);
      g = Math.round(g + (255 - g) * w);
      b = Math.round(b + (255 - b) * w);
      r = Math.min(255, Math.round(r * in));
      g = Math.min(255, Math.round(g * in));
      b = Math.min(255, Math.round(b * in));
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   public static int animColor(float phase, int c0, int c1) {
      float p = phase - (float)Math.floor(phase);
      float tri = p < 0.5F ? p * 2.0F : (1.0F - p) * 2.0F;
      return Render2DUtil.lerpColor(c0, c1, tri);
   }
}
