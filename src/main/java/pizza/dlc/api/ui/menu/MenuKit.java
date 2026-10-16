package pizza.dlc.api.ui.menu;

import pizza.dlc.api.translation.Translations;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.util.render.Render2DUtil;

public final class MenuKit {
   public static final int WINDOW = 0xFF0F0F0F;
   public static final int MOD = 0xFF151414;
   public static final int MODE_SEL = 0xFF252323;
   public static final int USERCARD = 0xFF0C0B0B;
   public static final int TEXT = 0xFFE9EAF2;
   public static final int TEXT_OFF = 0xFF8C8E9C;
   public static final int UID = 0xFF8E8E93;
   public static final int V1 = 0xFF878787;
   public static final int ICON_IDLE = 0xFF929292;
   public static final int SHADOW = 0x40000000;
   public static final int DIVIDER = 0x12FFFFFF;
   public static final float WIN_RADIUS = 12.0F;
   public static final float ROW_RADIUS = 11.0F;
   public static final int HEADER_H = 44;
   private static final int PARTICLES_MAX = 90;

   private MenuKit() {
   }

   public static Identifier figma(String name) {
      return Identifier.fromNamespaceAndPath("pizzadlc", "textures/gui/figma/" + name + ".png");
   }

   public static MsdfFont reg() {
      return PizzaDLCFonts.nexaRegular();
   }

   public static MsdfFont book() {
      return PizzaDLCFonts.nexaBook();
   }

   public static void regText(GuiGraphicsExtractor g, String t, float x, float y, float s, int c) {
      Render2DUtil.msdfText(g, reg(), t, x, y, s, c);
   }

   public static void bookText(GuiGraphicsExtractor g, String t, float x, float y, float s, int c) {
      Render2DUtil.msdfText(g, book(), t, x, y, s, c);
   }

   public static float regW(String t, float s) {
      return reg().width(t, s);
   }

   public static float bookW(String t, float s) {
      return book().width(t, s);
   }

   public static int fade(int argb, float f) {
      f = Math.max(0.0F, Math.min(1.0F, f));
      return argb & 0xFFFFFF | Math.round((argb >>> 24 & 0xFF) * f) << 24;
   }

   public static float expLerp(float cur, float target, float speed, float dt) {
      return cur + (target - cur) * (1.0F - (float)Math.exp(-speed * dt));
   }

   public static boolean inside(double mx, double my, float x, float y, float w, float h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public static String truncate(boolean regular, String s, float size, float maxW) {
      MsdfFont font = regular ? reg() : book();
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

   public static void background(GuiGraphicsExtractor g, int w, int h) {
      background(g, w, h, 1.0F);
   }

   public static void background(GuiGraphicsExtractor g, int w, int h, float a) {
      if (w > 0 && h > 0 && !(a <= 0.004F)) {
         double t = System.nanoTime() / 1.0E9;
         Render2DUtil.gradientRectV(g, 0, 0, w, h, fade(0xFF0E0E10, a), fade(0xFF060607, a), 0.0F);
         int big = (int)(Math.max(w, h) * 0.75F);
         blob(g, w * 0.28F + (float)Math.sin(t * 0.13) * w * 0.08F, h * 0.32F + (float)Math.cos(t * 0.11) * h * 0.1F, big, fade(0x14FFFFFF, a));
         int accent = ThemeManager.accent() & 0xFFFFFF;
         blob(g, w * 0.76F + (float)Math.cos(t * 0.09) * w * 0.07F, h * 0.7F + (float)Math.sin(t * 0.12) * h * 0.08F, (int)(big * 0.85F), fade(0x16000000 | accent, a));
         int count = Math.max(30, Math.min(PARTICLES_MAX, w * h / 9000));

         for (int i = 0; i < count; i++) {
            float r1 = hash(i * 3 + 1);
            float r2 = hash(i * 7 + 2);
            float r3 = hash(i * 11 + 3);
            float r4 = hash(i * 13 + 4);
            float speed = 4.0F + r1 * 10.0F;
            float travel = h + 12.0F;
            float y = h + 6.0F - (float)((r2 * travel + t * speed) % travel);
            float x = r3 * w + (float)Math.sin(t * 0.3 + i) * 6.0F;
            float size = 1.0F + r4 * 1.4F;
            float twinkle = 0.5F + 0.5F * (float)Math.sin(t * (0.6 + r1) + i * 1.7);
            int alpha = Math.round(255.0F * (0.06F + 0.24F * twinkle) * a);
            Render2DUtil.roundedRect(g, x, y, size, size, size / 2.0F, alpha << 24 | 0xFFFFFF);
         }

         int vig = Math.max(1, h / 4);
         Render2DUtil.gradientRectV(g, 0, 0, w, vig, fade(0x8C000000, a), 0x00000000, 0.0F);
         Render2DUtil.gradientRectV(g, 0, h - vig, w, vig, 0x00000000, fade(0xA0000000, a), 0.0F);
      }
   }

   public static void loadingLogo(GuiGraphicsExtractor g, float cx, float cy, float h, float a) {
      float gem = h * 0.62F;
      float fs = h * 0.42F;
      float gap = h * 0.16F;
      String name = "PizzaDLC";
      float total = gem + gap + regW(name, fs);
      float x = cx - total / 2.0F;
      float pulse = 0.5F + 0.5F * (float)Math.sin(System.nanoTime() / 1.0E9 * 2.2);
      Render2DUtil.crystal(g, x + gem / 2.0F, cy, gem, 0.0F, fade(gemTop(0xFF4A4A4E), a), fade(gemBottom(0xFF2C2C2F), a));
      regText(g, name, x + gem + gap, cy - 0.35F * fs, fs, fade(TEXT, a));
   }

   public static void loadingBar(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float progress, float a) {
      float w = x1 - x0;
      float cy = (y0 + y1) / 2.0F;
      float h = 3.0F;
      Render2DUtil.roundedRect(g, (float)x0, cy - h / 2.0F, w, h, h / 2.0F, fade(MODE_SEL, a));
      float fw = w * Math.max(0.0F, Math.min(1.0F, progress));
      if (fw >= h) {
         Accent.fill(g, x0, cy - h / 2.0F, fw, h, h / 2.0F, a);
      }

      float textY = cy - h / 2.0F - 6.0F - 4.9F;
      bookText(g, Translations.text("Загрузка ресурсов"), x0, textY, 7.0F, fade(UID, a));
      String pct = Math.round(Math.max(0.0F, Math.min(1.0F, progress)) * 100.0F) + "%";
      regText(g, pct, x1 - regW(pct, 7.0F), textY, 7.0F, fade(TEXT, a));
   }

   private static void blob(GuiGraphicsExtractor g, float cx, float cy, int size, int inner) {
      int x = Math.round(cx - size / 2.0F);
      int y = Math.round(cy - size / 2.0F);
      Render2DUtil.roundedRadial(g, x, y, size, size, inner, inner & 0xFFFFFF, size / 2.0F);
   }

   private static float hash(int n) {
      n = n << 13 ^ n;
      int v = n * (n * n * 15731 + 789221) + 1376312589 & Integer.MAX_VALUE;
      return v / 2.14748365E9F;
   }

   public static void window(GuiGraphicsExtractor g, float x, float y, float w, float h, float a) {
      int ix = Math.round(x);
      int iy = Math.round(y);
      int iw = Math.round(w);
      int ih = Math.round(h);
      Render2DUtil.softShadow(g, ix, iy, iw, ih, WIN_RADIUS, 16.0F, a * 1.4F);
      Render2DUtil.roundedRect(g, (float)ix, (float)iy, (float)iw, (float)ih, WIN_RADIUS, fade(WINDOW, a));
      Render2DUtil.roundedOutline(g, ix, iy, iw, ih, WIN_RADIUS, 0.6F, fade(0x1EFFFFFF, a));
   }

   public static void brand(GuiGraphicsExtractor g, float winX, float winY, String subtitle, float a) {
      float hTop = winY + 2.0F;
      Render2DUtil.crystal(g, winX + 7.0F + 8.0F + 10.0F, hTop + 17.0F, 18.0F, 0.0F, fade(gemTop(0xFF3A3A3C), a), fade(gemBottom(0xFF2A2A2C), a));
      float tx = winX + 7.0F + 8.0F + 24.0F;
      regText(g, "PizzaDLC", tx, hTop + 10.2F, 11.0F, fade(TEXT, a));
      regText(g, subtitle, tx, hTop + 21.0F, 6.5F, fade(V1, a));
   }

   public static void brandIntro(GuiGraphicsExtractor g, float winX, float winY, String subtitle, float a, float intro) {
      if (intro >= 1.0F) {
         brand(g, winX, winY, subtitle, a);
      } else {
         float hTop = winY + 2.0F;
         float gemE = Math.max(0.0F, Math.min(1.0F, intro * 1.6F));
         float gemScale = 0.4F + 0.6F * gemE;
         float gcx = winX + 7.0F + 8.0F + 10.0F;
         float gcy = hTop + 17.0F;
         g.pose().pushMatrix();
         g.pose().translate(gcx, gcy);
         g.pose().rotate((1.0F - gemE) * -1.4F);
         g.pose().scale(gemScale, gemScale);
         Render2DUtil.crystal(g, 0.0F, 0.0F, 18.0F, 0.0F, fade(gemTop(0xFF3A3A3C), a * gemE), fade(gemBottom(0xFF2A2A2C), a * gemE));
         g.pose().popMatrix();
         float tx = winX + 7.0F + 8.0F + 24.0F;
         String name = "PizzaDLC";
         float cx = tx;

         for (int i = 0; i < name.length(); i++) {
            String ch = String.valueOf(name.charAt(i));
            float le = Math.max(0.0F, Math.min(1.0F, (intro - 0.15F - i * 0.07F) / 0.3F));
            float ease = 1.0F - (1.0F - le) * (1.0F - le);
            regText(g, ch, cx, hTop + 10.2F + (1.0F - ease) * 4.0F, 11.0F, fade(TEXT, a * ease));
            cx += regW(ch, 11.0F);
         }

         float se = Math.max(0.0F, Math.min(1.0F, (intro - 0.6F) / 0.4F));
         regText(g, subtitle, tx, hTop + 21.0F, 6.5F, fade(V1, a * se));
      }
   }

   public static void sidebarRow(GuiGraphicsExtractor g, int x, int y, int w, int h, Icon icon, String label, float sel, float hover, float a) {
      sidebarRow(g, x, y, w, h, icon, label, sel, hover, a, true);
   }

   public static void sidebarHighlight(GuiGraphicsExtractor g, float x, float y, int w, int h, float a) {
      int ix = Math.round(x);
      int iy = Math.round(y);
      Render2DUtil.softShadow(g, ix, iy, w, h, ROW_RADIUS, 5.0F, a);
      Render2DUtil.roundedRect(g, x, y, (float)w, (float)h, ROW_RADIUS, fade(MODE_SEL, a));
      Render2DUtil.roundedOutline(g, ix, iy, w, h, ROW_RADIUS, 0.6F, fade(0x50878787, a));
   }

   private static int gemTop(int base) {
      return Render2DUtil.lerpColor(base, Accent.color(1.0F), 0.55F);
   }

   private static int gemBottom(int base) {
      return Render2DUtil.lerpColor(base, Accent.secondary(1.0F), 0.4F);
   }

   public static void sidebarRow(GuiGraphicsExtractor g, int x, int y, int w, int h, Icon icon, String label, float sel, float hover, float a, boolean selBackground) {
      float bgSel = selBackground ? sel : 0.0F;
      float bgA = Math.max(bgSel, hover * 0.45F) * a;
      if (bgA > 0.004F) {
         Render2DUtil.softShadow(g, x, y, w, h, ROW_RADIUS, 2.0F + 3.0F * Math.max(bgSel, hover), bgA);
         Render2DUtil.roundedRect(g, x, y, w, h, ROW_RADIUS, fade(Render2DUtil.lerpColor(WINDOW, MODE_SEL, Math.max(bgSel, hover * 0.5F)), bgA));
         Render2DUtil.roundedOutline(g, x, y, w, h, ROW_RADIUS, 0.6F, fade(0x50878787, bgA));
      }
      int tint = fade(Render2DUtil.lerpColor(ICON_IDLE, -1, Math.max(sel, hover * 0.75F)), a);
      icon.draw(g, x + 15.0F, y + h / 2.0F, 12.0F, tint);
      float fs = h >= 20 ? 10.0F : 9.0F;
      float maxW = w - 27 - (sel > 0.01F ? 16 : 6);
      String text = truncate(true, label, fs, maxW);
      regText(g, text, x + 27 + hover, y + (h - 0.7F * fs) / 2.0F, fs, fade(Render2DUtil.lerpColor(TEXT_OFF, TEXT, Math.max(sel, hover * 0.65F)), a));
      if (sel > 0.01F) {
         float cx = x + w - 10 + (1.0F - sel) * 3.0F;
         float cy = y + h / 2.0F;
         Render2DUtil.chevron(g, Math.round(cx - 4.0F), Math.round(cy - 3.0F), 6, 6, false, 1.2F, fade(-1, sel * a));
      }
   }

   public static void section(GuiGraphicsExtractor g, String text, float x, float y, float w, float a) {
      float tw = regW(text, 6.5F);
      regText(g, text, x, y, 6.5F, fade(TEXT_OFF, a));
      int lineX = Math.round(x + tw + 5.0F);
      int lineW = Math.round(x + w) - lineX;
      if (lineW > 0) {
         Render2DUtil.rect(g, lineX, Math.round(y + 2.5F), lineW, 1, fade(0x23FFFFFF, a));
      }
   }

   public static void profileCard(GuiGraphicsExtractor g, int x, int y, int w, int h, Identifier skin, String nick, String role, float highlight, float a) {
      Render2DUtil.softShadow(g, x, y, w, h, 8.0F, 3.0F + 3.0F * highlight, a);
      Render2DUtil.roundedRect(g, x, y, w, h, 8.0F, fade(Render2DUtil.lerpColor(USERCARD, MODE_SEL, highlight), a));
      Render2DUtil.roundedOutline(g, x, y, w, h, 8.0F, 0.6F, fade(Render2DUtil.lerpColor(0x20404040, 0x708A8A8A, highlight), a));
      int head = Math.min(22, h - 10);
      int hx = x + 6;
      int hy = y + (h - head) / 2;
      if (skin != null) {
         int tint = fade(-1, a);
         Render2DUtil.roundedFace(g, skin, hx, hy, head, 5, 8, 8, tint);
         Render2DUtil.roundedFace(g, skin, hx, hy, head, 5, 40, 8, tint);
      }

      int nickX = hx + head + 7;
      float maxW = Math.max(0, x + w - 6 - nickX);
      bookText(g, truncate(false, nick, 8.0F, maxW), nickX, y + h / 2.0F - 7.0F, 8.0F, fade(TEXT, a));
      regText(g, truncate(true, role, 6.5F, maxW), nickX, y + h / 2.0F + 2.0F, 6.5F, fade(UID, a));
   }

   public static void pill(GuiGraphicsExtractor g, float x, float y, float w, float h, String text, float hover, boolean strong, float a) {
      int bg = strong ? MODE_SEL : Render2DUtil.lerpColor(MODE_SEL, 0xFF353333, hover);
      int fg = strong ? Render2DUtil.lerpColor(TEXT, 0xFFFFFFFF, hover) : TEXT;
      Render2DUtil.softShadow(g, x, y, w, h, 3.0F, 1.5F + 2.5F * hover, a * 0.8F);
      Render2DUtil.roundedRect(g, x, y, w, h, 3.0F, fade(bg, a));
      if (strong && hover > 0.01F) {
         Accent.fill(g, x, y, w, h, 3.0F, a * hover);
      }
      Render2DUtil.roundedOutline(g, Math.round(x), Math.round(y), Math.round(w), Math.round(h), 3.0F, 0.5F, fade(0x24FFFFFF, a));
      float textSize = Math.min(7.5F, 7.5F * Math.max(1, w - 12) / Math.max(1, bookW(text, 7.5F)));
      float tw = bookW(text, textSize);
      bookText(g, text, x + (w - tw) / 2.0F, y + (h - .7F * textSize) / 2.0F, textSize, fade(fg, a));
   }

   public interface Icon {
      void draw(GuiGraphicsExtractor g, float cx, float cy, float size, int color);

      static Icon texture(String figmaName) {
         Identifier id = figma(figmaName);
         return (g, cx, cy, size, color) -> Render2DUtil.roundedTexture(g, id, cx - size / 2.0F, cy - size / 2.0F, size, size, 0.0F, true, color);
      }

      static Icon face(Identifier skin) {
         return (g, cx, cy, size, color) -> {
            int s = Math.round(size);
            int x = Math.round(cx - size / 2.0F);
            int y = Math.round(cy - size / 2.0F);
            Render2DUtil.roundedFace(g, skin, x, y, s, 3, 8, 8, color | 0xFFFFFF);
            Render2DUtil.roundedFace(g, skin, x, y, s, 3, 40, 8, color | 0xFFFFFF);
         };
      }

      static Icon text(String glyph) {
         return (g, cx, cy, size, color) -> {
            float fs = size * 0.8F;
            float tw = regW(glyph, fs);
            regText(g, glyph, cx - tw / 2.0F, cy - 0.35F * fs, fs, color);
         };
      }

      Icon HOME = (g, cx, cy, size, color) -> {
         float s = size;
         Render2DUtil.chevron(g, Math.round(cx - s * 0.5F), Math.round(cy - s * 0.5F), Math.round(s), Math.round(s * 0.45F), true, 1.4F, color);
         Render2DUtil.roundedOutline(g, Math.round(cx - s * 0.32F), Math.round(cy - s * 0.08F), Math.round(s * 0.64F), Math.round(s * 0.52F), 1.5F, 1.2F, color);
      };
      Icon EXIT = (g, cx, cy, size, color) -> {
         float s = size;
         float t = 1.4F;
         float bx = cx - s * 0.46F;
         float by = cy - s * 0.45F;
         float bh = s * 0.9F;
         float bw = s * 0.36F;
         Render2DUtil.roundedRect(g, bx, by, t, bh, t / 2.0F, color);
         Render2DUtil.roundedRect(g, bx, by, bw, t, t / 2.0F, color);
         Render2DUtil.roundedRect(g, bx, by + bh - t, bw, t, t / 2.0F, color);
         float ax = cx - s * 0.12F;
         float tip = cx + s * 0.46F;
         Render2DUtil.roundedRect(g, ax, cy - t / 2.0F, tip - ax, t, t / 2.0F, color);
         float head = s * 0.28F;

         for (float angle : new float[]{(float)(Math.PI * 1.25), (float)(Math.PI * 0.75)}) {
            g.pose().pushMatrix();
            g.pose().translate(tip, cy);
            g.pose().rotate(angle);
            Render2DUtil.roundedRect(g, -t / 2.0F, -t / 2.0F, head, t, t / 2.0F, color);
            g.pose().popMatrix();
         }
      };
      Icon KEY = (g, cx, cy, size, color) -> {
         float s = size * 0.8F;
         Render2DUtil.roundedOutline(g, Math.round(cx - s / 2.0F), Math.round(cy - s / 2.0F), Math.round(s), Math.round(s), 2.0F, 1.2F, color);
         Render2DUtil.roundedRect(g, cx - s * 0.25F, cy + s * 0.12F, s * 0.5F, 1.4F, 0.7F, color);
      };
      Icon CHAT = (g, cx, cy, size, color) -> {
         float s = size;
         Render2DUtil.roundedOutline(g, Math.round(cx - s / 2.0F), Math.round(cy - s * 0.38F), Math.round(s), Math.round(s * 0.68F), 3.0F, 1.2F, color);
         Render2DUtil.roundedRect(g, cx - s * 0.25F, cy - s * 0.08F, s * 0.5F, 1.2F, 0.6F, color);
      };
      Icon PACKS = (g, cx, cy, size, color) -> {
         float s = size * 0.62F;
         Render2DUtil.roundedOutline(g, Math.round(cx - s * 0.75F), Math.round(cy - s * 0.75F), Math.round(s), Math.round(s), 2.0F, 1.2F, color);
         Render2DUtil.roundedRect(g, cx - s * 0.25F, cy - s * 0.25F, s, s, 2.0F, color);
      };
      Icon SOUND = (g, cx, cy, size, color) -> {
         float bw = Math.max(1.6F, size * 0.16F);
         float gap = size * 0.12F;
         float[] heights = new float[]{0.45F, 0.85F, 0.6F, 0.95F};
         float total = heights.length * bw + (heights.length - 1) * gap;
         float x = cx - total / 2.0F;

         for (float hk : heights) {
            float bh = size * hk;
            Render2DUtil.roundedRect(g, x, cy + size * 0.5F - bh, bw, bh, bw / 2.0F, color);
            x += bw + gap;
         }
      };
      Icon MOUSE = (g, cx, cy, size, color) -> {
         float w = size * 0.6F;
         float h = size * 0.9F;
         Render2DUtil.roundedOutline(g, Math.round(cx - w / 2.0F), Math.round(cy - h / 2.0F), Math.round(w), Math.round(h), w / 2.0F, 1.2F, color);
         Render2DUtil.roundedRect(g, cx - 0.6F, cy - h * 0.32F, 1.2F, h * 0.22F, 0.6F, color);
      };
   }
}
