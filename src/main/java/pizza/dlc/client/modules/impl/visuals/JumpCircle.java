package pizza.dlc.client.modules.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.AdditiveTextureType;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "JumpCircle",
   description = "Светящийся круг на земле при прыжке",
   category = ModuleCategory.VISUALS
)
public class JumpCircle extends Module {
   private static final String TYPE_1 = "Тип 1";
   private static final String TYPE_2 = "Тип 2";
   private static final String STYLE_PULSE = "Импульс";
   private static final String STYLE_WAVE = "Волна";
   private static final float APPEAR_FRAC = 0.18F;
   private static final float LAYER_TWIST = 37.0F;
   private static final float FADE_END = 0.85F;
   public final ModeSetting style = new ModeSetting("Стиль", this, STYLE_PULSE, STYLE_WAVE);
   public final ModeSetting mode = new ModeSetting("Текстура", this, TYPE_1, TYPE_2);
   public final SliderSetting duration = new SliderSetting("Длительность (мс)", this, 1000.0F, 100.0F, 3000.0F, 50.0F);
   public final SliderSetting maxRadius = new SliderSetting("Радиус", this, 2.5F, 0.5F, 5.0F, 0.1F);
   public final SliderSetting colorRotateSpeed = new SliderSetting("Скорость вращения", this, 90.0F, 0.0F, 720.0F, 5.0F);
   public final SliderSetting glowLayers = new SliderSetting("Слои свечения", this, 2.0F, 1.0F, 3.0F, 1.0F);
   public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", "Выключено — круг лежит на полу и прячется за блоками", this);
   private final Identifier circleTexture1 = Identifier.fromNamespaceAndPath("pizzadlc", "textures/images/circle1.png");
   private final Identifier circleTexture2 = Identifier.fromNamespaceAndPath("pizzadlc", "textures/images/circle2.png");
   private final List<Circle> circles = new ArrayList<>();
   private boolean wasOnGround = true;
   private double lastGroundY = 0.0;
   private int nextColorShift = 0;

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;

      boolean onGround = mc.player.onGround();
      if (onGround) {
         this.lastGroundY = mc.player.getY();
      }

      boolean isJumping = mc.options.keyJump.isDown();
      if (this.wasOnGround && !onGround && (isJumping || mc.player.getDeltaMovement().y > 0.0)) {
         double y = this.lastGroundY + 0.001;
         this.circles.add(new Circle(new Vec3(mc.player.getX(), y, mc.player.getZ()), this.nextColorShift));
         this.nextColorShift += 200;
      }

      this.wasOnGround = onGround;
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (this.circles.isEmpty()) return;

      long now = System.currentTimeMillis();
      long maxTime = (long)this.duration.getValue();
      this.circles.removeIf(circle -> now - circle.startTime > maxTime);
      if (!this.circles.isEmpty()) {
         Identifier textureToDraw = this.mode.is(TYPE_2) ? this.circleTexture2 : this.circleTexture1;
         RenderType type = this.throughWalls.isEnabled() ? AdditiveTextureType.image(textureToDraw) : AdditiveTextureType.imageDepth(textureToDraw);
         int accent = ThemeManager.accent();
         int secondary = ThemeManager.secondary();
         float rotSpeed = this.colorRotateSpeed.getValue();
         float maxR = this.maxRadius.getValue();
         int layers = Math.max(1, Math.round(this.glowLayers.getValue()));
         boolean pulse = this.style.is(STYLE_PULSE);
         Render3DUtil.drawTexturedGrounds(e, type, w -> {
            for (int i = 0; i < this.circles.size(); i++) {
               Circle circle = this.circles.get(i);
               long timeExisted = now - circle.startTime;
               float progress = (float)timeExisted / (float)maxTime;
               float rotation = (float)timeExisted * rotSpeed / 1000.0F;
               float radius;
               float visible;
               if (pulse) {
                  float appear = clamp01(progress / APPEAR_FRAC);
                  float decay = clamp01((progress - APPEAR_FRAC) / (1.0F - APPEAR_FRAC));
                  float scale = easeOutCubic(appear) * (1.0F - easeOutCubic(decay));
                  if (scale <= 0.01F) {
                     continue;
                  }

                  radius = maxR * scale;
                  visible = 1.0F;
               } else {
                  float inv = 1.0F - progress;
                  radius = maxR * (1.0F - inv * inv);
                  visible = 1.0F - progress / FADE_END;
                  if (visible <= 0.02F) {
                     continue;
                  }
               }

               int alphaBase = Math.min(255, (int)(255.0F * visible));
               int alphaExtra = (int)(alphaBase * visible * visible);
               int rgb1 = Render2DUtil.lerpColor(accent, secondary, circle.mix[0]) & 0xFFFFFF;
               int rgb2 = Render2DUtil.lerpColor(accent, secondary, circle.mix[1]) & 0xFFFFFF;
               int rgb3 = Render2DUtil.lerpColor(accent, secondary, circle.mix[2]) & 0xFFFFFF;
               int rgb4 = Render2DUtil.lerpColor(accent, secondary, circle.mix[3]) & 0xFFFFFF;

               for (int layer = 0; layer < layers; layer++) {
                  int a = layer == 0 ? alphaBase : alphaExtra;
                  if (a > 4) {
                     int shifted = a << 24;
                     w.quad(circle.pos, radius, rotation + layer * LAYER_TWIST, rgb1 | shifted, rgb2 | shifted, rgb3 | shifted, rgb4 | shifted);
                  }
               }
            }
         });
      }
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
   }

   private static float easeOutCubic(float x) {
      float inv = 1.0F - x;
      return 1.0F - inv * inv * inv;
   }

   private static class Circle {
      public final Vec3 pos;
      public final long startTime;
      public final float[] mix = new float[4];

      public Circle(Vec3 pos, int colorShift) {
         this.pos = pos;
         this.startTime = System.currentTimeMillis();

         for (int i = 0; i < 4; i++) {
            this.mix[i] = (float)(0.5 + 0.5 * Math.sin(Math.toRadians(i * 90 + colorShift)));
         }
      }
   }
}
