package pizza.dlc.api.theme;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.util.render.Render2DUtil;

public final class Accent {
   private Accent() {
   }

   public static int color(float alpha) {
      return withAlpha(ThemeManager.accent(), alpha);
   }

   public static int secondary(float alpha) {
      return withAlpha(ThemeManager.secondary(), alpha);
   }

   public static int mix(float t, float alpha) {
      return withAlpha(Render2DUtil.lerpColor(ThemeManager.accent(), ThemeManager.secondary(), Math.max(0.0F, Math.min(1.0F, t))), alpha);
   }

   public static void fill(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float alpha) {
      if (alpha <= 0.01F || w <= 0.5F || h <= 0.5F) return;
      Render2DUtil.gradientRectFull(ctx, Math.round(x), Math.round(y), Math.round(w), Math.round(h), color(alpha), secondary(alpha), radius);
   }

   public static void glow(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float alpha, int spread) {
      if (alpha <= 0.01F) return;
      Render2DUtil.glow(ctx, Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(radius), color(alpha * 0.45F), spread);
   }

   public static void toggle(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float on, int offColor, int knobOff) {
      on = Math.max(0.0F, Math.min(1.0F, on));
      float r = h / 2.0F;
      float knob = h - 4.0F;
      float stretch = 4.0F * on * (1.0F - on);
      float knobW = knob * (1.0F + 0.55F * stretch);
      float kx = x + 2.0F + on * (w - knobW - 4.0F);
      Render2DUtil.roundedRect(ctx, x, y, w, h, r, offColor);
      if (on > 0.01F) {
         float fillW = Math.min(w, kx + knobW + 2.0F - x);
         glow(ctx, x, y, fillW, h, r, on, 5);
         Render2DUtil.gradientRectFull(ctx, Math.round(x), Math.round(y), Math.round(fillW), Math.round(h), color(on), secondary(on), r);
      }
      float squash = 1.0F - 0.12F * stretch;
      float kh = knob * squash;
      Render2DUtil.roundedRect(ctx, kx, y + 2.0F + (knob - kh) / 2.0F, knobW, kh, kh / 2.0F, Render2DUtil.lerpColor(knobOff, 0xFFFFFFFF, on));
   }

   private static int withAlpha(int argb, float alpha) {
      int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0.0F, Math.min(1.0F, alpha)));
      return argb & 0xFFFFFF | a << 24;
   }
}
