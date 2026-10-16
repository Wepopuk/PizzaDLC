package pizza.dlc.client.modules.impl.visuals.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.util.render.Render2DUtil;

public final class HudStyle {
   public static final String CLIENT_NAME = "PizzaDLC";
   public static final int WINDOW = 0xFF0F0F0F;
   public static final int CARD = 0xFF151414;
   public static final int PILL = 0xFF252323;
   public static final int TEXT = 0xFFE9EAF2;
   public static final int TEXT_DIM = 0xFF8E8E93;
   public static final int TEXT_OFF = 0xFF8C8E9C;
   public static final int GEM_TOP = 0xFF3A3A3C;
   public static final int GEM_BOTTOM = 0xFF2A2A2C;
   public static final int GOOD = 0xFF6FDC8C;
   public static final int WARN = 0xFFFFB454;
   public static final int BAD = 0xFFFF6B6B;
   public static final int TRACK = 0xFF252323;
   public static final int DIVIDER = 0x12FFFFFF;
   public static final int PANEL_BG = 0xEB0F0F0F;
   private static final int FLAT_BG = 0xD90F0F0F;
   private static final int OUTLINE = 0x1CFFFFFF;
   private static final int PILL_OUTLINE = 0x14FFFFFF;
   private static final int SHADOW_SPREAD = 7;
   private static final int FLAT_SHADOW_SPREAD = 4;
   public static final float RADIUS = 6.0F;
   public static final float PILL_RADIUS = 3.0F;
   public static final int PAD = 6;
   public static final int HEADER_H = 17;
   public static final int ROW_H = 12;
   public static final int ROWS_TOP = 3;
   public static final int ROWS_BOTTOM = 4;
   public static final int PILL_H = 10;
   public static final int PILL_PAD = 3;
   public static final float TITLE_FS = 8.5F;
   public static final float ROW_FS = 7.5F;
   public static final float PILL_FS = 7.0F;
   public static final float SLIDE = 8.0F;

   private HudStyle() {
   }

   public static MsdfFont title() {
      return PizzaDLCFonts.nexaRegular();
   }

   public static MsdfFont body() {
      return PizzaDLCFonts.nexaBook();
   }

   public static MsdfFont mono() {
      return PizzaDLCFonts.sfpro();
   }

   public static float textY(float boxY, float boxH, float size) {
      return boxY + (boxH - 0.7F * size) / 2.0F;
   }

   public static float listPanelH(float contentH) {
      return HEADER_H + ROWS_TOP + contentH + ROWS_BOTTOM;
   }

   public static void panel(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float a) {
      panel(ctx, x, y, w, h, radius, a, true, PANEL_BG);
   }

   public static void panelFlat(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float a) {
      panel(ctx, x, y, w, h, radius, a, false, FLAT_BG);
   }

   private static void panel(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float a, boolean glass, int bg) {
      if (a <= 0.01F || w <= 0.0F || h <= 0.0F) return;

      int ix = (int)Math.floor(x);
      int iy = (int)Math.floor(y);
      int iw = Math.round(w);
      int ih = Math.round(h);
      ctx.pose().pushMatrix();
      ctx.pose().translate(x - ix, y - iy);
      Render2DUtil.softShadow(ctx, ix, iy, iw, ih, radius, glass ? SHADOW_SPREAD : FLAT_SHADOW_SPREAD, a);
      if (glass && a >= 0.99F) {
         Render2DUtil.glassRect(ctx, ix, iy, iw, ih, radius, bg);
      } else {
         Render2DUtil.roundedRect(ctx, (float)ix, (float)iy, (float)iw, (float)ih, radius, fade(bg, a));
      }

      Render2DUtil.roundedOutline(ctx, ix, iy, iw, ih, radius, 0.5F, fade(OUTLINE, a));
      ctx.pose().popMatrix();
   }

   public static int healthColor(float frac) {
      frac = Math.max(0.0F, Math.min(1.0F, frac));
      return frac > 0.5F ? Render2DUtil.lerpColor(WARN, GOOD, (frac - 0.5F) * 2.0F) : Render2DUtil.lerpColor(BAD, WARN, frac * 2.0F);
   }

   public static void panelBg(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float rtl, float rtr, float rbr, float rbl, float a) {
      if (a <= 0.01F || w <= 0.0F || h <= 0.0F) return;

      if (a >= 0.99F) {
         Render2DUtil.glassRect(ctx, x, y, w, h, rtl, rtr, rbr, rbl, PANEL_BG);
      } else {
         int ix = (int)Math.floor(x);
         int iy = (int)Math.floor(y);
         ctx.pose().pushMatrix();
         ctx.pose().translate(x - ix, y - iy);
         Render2DUtil.roundedRect(ctx, ix, iy, Math.round(w), Math.round(h), rtl, rtr, rbr, rbl, fade(PANEL_BG, a));
         ctx.pose().popMatrix();
      }
   }

   public static void header(GuiGraphicsExtractor ctx, float x, float y, float w, String title, String badge, float a) {
      title = pizza.dlc.api.translation.Translations.text(title);
      Render2DUtil.msdfText(ctx, title(), title, x + PAD, textY(y, HEADER_H, TITLE_FS), TITLE_FS, fade(TEXT, a));
      if (badge != null && !badge.isEmpty()) {
         float bw = pillWidth(badge);
         pill(ctx, x + w - PAD - bw, y + (HEADER_H - PILL_H) / 2.0F, bw, badge, TEXT_DIM, a);
      }

      Render2DUtil.rect(ctx, Math.round(x + PAD), Math.round(y + HEADER_H - 1), Math.max(1, Math.round(w - PAD * 2)), 1, fade(DIVIDER, a));
   }

   public static float headerWidth(String title, String badge) {
      title = pizza.dlc.api.translation.Translations.text(title);
      float w = title().width(title, TITLE_FS);
      if (badge != null && !badge.isEmpty()) {
         w += 10.0F + pillWidth(badge);
      }

      return PAD + w + PAD;
   }

   public static float pillWidth(String text) {
      return mono().width(text, PILL_FS) + PILL_PAD * 2;
   }

   public static void pill(GuiGraphicsExtractor ctx, float x, float y, float w, String text, int textColor, float a) {
      int ix = (int)Math.floor(x);
      int iy = (int)Math.floor(y);
      int iw = Math.round(w);
      ctx.pose().pushMatrix();
      ctx.pose().translate(x - ix, y - iy);
      Render2DUtil.roundedRect(ctx, (float)ix, (float)iy, (float)iw, (float)PILL_H, PILL_RADIUS, fade(PILL, a));
      Render2DUtil.roundedOutline(ctx, ix, iy, iw, PILL_H, PILL_RADIUS, 0.5F, fade(PILL_OUTLINE, a));
      float tw = mono().width(text, PILL_FS);
      Render2DUtil.msdfText(ctx, mono(), text, ix + (iw - tw) / 2.0F, textY(iy, PILL_H, PILL_FS), PILL_FS, fade(textColor, a));
      ctx.pose().popMatrix();
   }

   public static void bar(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float frac, int fill, float a) {
      if (w <= 0.0F) return;

      frac = Math.max(0.0F, Math.min(1.0F, frac));
      Render2DUtil.roundedRect(ctx, x, y, w, h, h / 2.0F, fade(TRACK, a));
      float fw = w * frac;
      if (fw >= h) {
         Render2DUtil.roundedRect(ctx, x, y, fw, h, h / 2.0F, fade(fill, a));
      }
   }

   public static void gem(GuiGraphicsExtractor ctx, float cx, float cy, float size, float a) {
      Accent.glow(ctx, cx - size * 0.3F, cy - size * 0.3F, size * 0.6F, size * 0.6F, size * 0.3F, a * 0.8F, Math.max(3, Math.round(size * 0.45F)));
      int top = Render2DUtil.lerpColor(GEM_TOP, Accent.color(1.0F), 0.55F);
      int bottom = Render2DUtil.lerpColor(GEM_BOTTOM, Accent.secondary(1.0F), 0.4F);
      Render2DUtil.crystal(ctx, cx, cy, size, 0.0F, fade(top, a), fade(bottom, a));
   }

   public static void accentBar(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float frac, float a) {
      if (w <= 0.0F) return;
      frac = Math.max(0.0F, Math.min(1.0F, frac));
      Render2DUtil.roundedRect(ctx, x, y, w, h, h / 2.0F, fade(TRACK, a));
      float fw = w * frac;
      if (fw >= h) {
         Accent.glow(ctx, x, y, fw, h, h / 2.0F, a, 3);
         Accent.fill(ctx, x, y, fw, h, h / 2.0F, a);
      }
   }

   public static String truncate(MsdfFont font, String s, float size, float maxW) {
      if (maxW <= 0.0F) {
         return "";
      }

      if (font.width(s, size) <= maxW) {
         return s;
      }

      String ell = "…";
      float ellW = font.width(ell, size);
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < s.length() && !(font.width(sb.toString() + s.charAt(i), size) + ellW > maxW); i++) {
         sb.append(s.charAt(i));
      }

      return sb + ell;
   }

   public static int fade(int argb, float f) {
      f = Math.max(0.0F, Math.min(1.0F, f));
      int alpha = argb >>> 24 & 0xFF;
      return argb & 0xFFFFFF | Math.round(alpha * f) << 24;
   }

   public static float expLerp(float cur, float target, float speed, float dt) {
      float t = 1.0F - (float)Math.exp(-speed * dt);
      return cur + (target - cur) * t;
   }
}
