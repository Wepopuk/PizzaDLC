package pizza.dlc.api.ui.skin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.render.msdf.ReplaceSymbols;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.modules.impl.visuals.hud.HudStyle;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

public final class UiSkin {
   private static final int BUTTON = 1;
   private static final int BUTTON_HOVER = 2;
   private static final int BUTTON_DISABLED = 3;
   private static final int SLIDER = 4;
   private static final int SLIDER_HOVER = 5;
   private static final int HANDLE = 6;
   private static final int HANDLE_HOVER = 7;
   private static final int CHECKBOX = 8;
   private static final int CHECKBOX_HOVER = 9;
   private static final int CHECKBOX_ON = 10;
   private static final int CHECKBOX_ON_HOVER = 11;
   private static final int FIELD = 12;
   private static final int FIELD_FOCUS = 13;
   private static final int SCROLL_TRACK = 14;
   private static final int SCROLL_THUMB = 15;
   private static final int TAB = 16;
   private static final int TAB_HOVER = 17;
   private static final int TAB_SELECTED = 18;
   private static final int TAB_SELECTED_HOVER = 19;
   private static final int TEX_MENU_BG = 1;
   private static final int TEX_INWORLD_BG = 2;
   private static final int TEX_LIST_BG = 3;
   private static final int TEX_HEADER_SEP = 4;
   private static final int TEX_FOOTER_SEP = 5;
   private static final int TEX_TAB_HEADER = 6;
   private static final Map<Identifier, Integer> SPRITES = new HashMap<>();
   private static final Map<Identifier, Integer> TEXTURES = new HashMap<>();
   private static final int FILL_IDLE = 0xF0141313;
   private static final int FILL_HOVER = 0xF5222020;
   private static final int FILL_DISABLED = 0xC0121111;
   private static final int OUTLINE_IDLE = 0x1CFFFFFF;
   private static final int OUTLINE_HOVER = 0x4DFFFFFF;
   private static final int OUTLINE_DISABLED = 0x0DFFFFFF;
   private static final int SLIDER_FILL = 0xFF2B2929;
   private static final int FIELD_FILL = 0xF00C0B0B;
   private static final int FIELD_OUTLINE = 0x26FFFFFF;
   private static final int MENU_DIM = 0xA6060606;
   private static final int INWORLD_DIM = 0x80050505;
   private static final int LIST_DIM = 0x2E000000;
   private static final int SEPARATOR = 0x1FFFFFFF;
   private static final int TAB_HEADER = 0xB8090909;
   private static final float RADIUS = 5.0F;
   private static final float LABEL_FS = 8.5F;
   private static final float STRING_FS = 9.0F;
   private static final float LABEL_FS_SMALL = 7.5F;
   private static final float LIST_FS = 8.5F;
   private static final float MARQUEE_GAP = 24.0F;
   private static final float MARQUEE_SPEED = 22.0F;
   private static final float HOVER_SPEED = 14.0F;
   private static final Map<AbstractWidget, float[]> HOVER = new WeakHashMap<>();
   private static GuiGraphicsExtractor graphics;
   private static AbstractWidget widget;
   private static int sliderX;
   private static int sliderY;
   private static int sliderH = -1;
   private static int sliderW;
   private static long lastNanos;
   private static float frameDt;

   static {
      sprite("widget/button", BUTTON);
      sprite("widget/button_highlighted", BUTTON_HOVER);
      sprite("widget/button_disabled", BUTTON_DISABLED);
      sprite("widget/slider", SLIDER);
      sprite("widget/slider_highlighted", SLIDER_HOVER);
      sprite("widget/slider_handle", HANDLE);
      sprite("widget/slider_handle_highlighted", HANDLE_HOVER);
      sprite("widget/checkbox", CHECKBOX);
      sprite("widget/checkbox_highlighted", CHECKBOX_HOVER);
      sprite("widget/checkbox_selected", CHECKBOX_ON);
      sprite("widget/checkbox_selected_highlighted", CHECKBOX_ON_HOVER);
      sprite("widget/text_field", FIELD);
      sprite("widget/text_field_highlighted", FIELD_FOCUS);
      sprite("widget/scroller_background", SCROLL_TRACK);
      sprite("widget/scroller", SCROLL_THUMB);
      sprite("widget/tab", TAB);
      sprite("widget/tab_highlighted", TAB_HOVER);
      sprite("widget/tab_selected", TAB_SELECTED);
      sprite("widget/tab_selected_highlighted", TAB_SELECTED_HOVER);
      texture("textures/gui/menu_background.png", TEX_MENU_BG);
      texture("textures/gui/inworld_menu_background.png", TEX_INWORLD_BG);
      texture("textures/gui/menu_list_background.png", TEX_LIST_BG);
      texture("textures/gui/inworld_menu_list_background.png", TEX_LIST_BG);
      texture("textures/gui/header_separator.png", TEX_HEADER_SEP);
      texture("textures/gui/inworld_header_separator.png", TEX_HEADER_SEP);
      texture("textures/gui/footer_separator.png", TEX_FOOTER_SEP);
      texture("textures/gui/inworld_footer_separator.png", TEX_FOOTER_SEP);
      texture("textures/gui/tab_header_background.png", TEX_TAB_HEADER);
   }

   private UiSkin() {
   }

   private static void sprite(String path, int kind) {
      SPRITES.put(Identifier.withDefaultNamespace(path), kind);
   }

   private static void texture(String path, int kind) {
      TEXTURES.put(Identifier.withDefaultNamespace(path), kind);
   }

   public static void begin(GuiGraphicsExtractor g, AbstractWidget w) {
      if (CleanRenderScope.isActive()) return;
      graphics = g;
      widget = w;
      long now = System.nanoTime();
      if (now - lastNanos > 2_000_000L) {
         frameDt = lastNanos == 0L ? 0.016F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
         lastNanos = now;
      }
   }

   public static boolean drawSprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h, int color) {
      if (CleanRenderScope.isActive()) return false;
      Integer kind = SPRITES.get(sprite);
      if (kind == null) {
         return false;
      }

      float a = (color >>> 24 & 0xFF) / 255.0F;
      if (a <= 0.004F) {
         return true;
      }

      switch (kind) {
         case BUTTON, BUTTON_HOVER, BUTTON_DISABLED -> button(g, x, y, w, h, kind == BUTTON_HOVER, kind == BUTTON_DISABLED, a);
         case SLIDER, SLIDER_HOVER -> {
            button(g, x, y, w, h, kind == SLIDER_HOVER, false, a);
            sliderX = x;
            sliderY = y;
            sliderH = h;
            sliderW = w;
         }
         case HANDLE, HANDLE_HOVER -> handle(g, x, y, w, h, kind == HANDLE_HOVER, a);
         case CHECKBOX, CHECKBOX_HOVER, CHECKBOX_ON, CHECKBOX_ON_HOVER -> checkbox(
            g, x, y, w, h, kind == CHECKBOX_ON || kind == CHECKBOX_ON_HOVER, kind == CHECKBOX_HOVER || kind == CHECKBOX_ON_HOVER, a
         );
         case FIELD, FIELD_FOCUS -> field(g, x, y, w, h, kind == FIELD_FOCUS, a);
         case SCROLL_TRACK -> Render2DUtil.roundedRect(g, x + w / 2.0F - 1.0F, (float)y, 2.0F, (float)h, 1.0F, HudStyle.fade(0x14FFFFFF, a));
         case SCROLL_THUMB -> {
            Render2DUtil.gradientRectFull(g, Math.round(x + w / 2.0F - 1.5F), y, 3, h, Accent.color(a * 0.85F), Accent.secondary(a * 0.85F), 1.5F);
         }
         default -> tab(g, x, y, w, h, kind == TAB_SELECTED || kind == TAB_SELECTED_HOVER, kind == TAB_HOVER || kind == TAB_SELECTED_HOVER, a);
      }

      return true;
   }

   public static boolean drawTexture(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h, int color) {
      if (CleanRenderScope.isActive()) return false;
      Integer kind = TEXTURES.get(texture);
      if (kind == null) {
         return false;
      }

      float a = (color >>> 24 & 0xFF) / 255.0F;
      switch (kind) {
         case TEX_MENU_BG -> g.fill(x, y, x + w, y + h, HudStyle.fade(MENU_DIM, a));
         case TEX_INWORLD_BG -> g.fill(x, y, x + w, y + h, HudStyle.fade(INWORLD_DIM, a));
         case TEX_LIST_BG -> g.fill(x, y, x + w, y + h, HudStyle.fade(LIST_DIM, a));
         case TEX_HEADER_SEP -> g.fill(x, y + h - 1, x + w, y + h, HudStyle.fade(SEPARATOR, a));
         case TEX_FOOTER_SEP -> g.fill(x, y, x + w, y + 1, HudStyle.fade(SEPARATOR, a));
         default -> {
            g.fill(x, y, x + w, y + h, HudStyle.fade(TAB_HEADER, a));
            g.fill(x, y + h - 1, x + w, y + h, HudStyle.fade(SEPARATOR, a));
         }
      }

      return true;
   }

   public static void selection(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean focused) {
      Render2DUtil.roundedRect(g, x, y, w, h, 4.0F, focused ? 0x1EFFFFFF : 0x12FFFFFF);
      Render2DUtil.roundedOutline(g, x, y, w, h, 4.0F, 0.6F, focused ? Accent.color(0.7F) : 0x30FFFFFF);
   }

   public static boolean drawLabel(AbstractWidget w, Component message, int margin, float alpha, boolean active) {
      if (CleanRenderScope.isActive()) return false;
      GuiGraphicsExtractor g = graphics;
      if (g != null && message != null && w.visible && isLabelled(w)) {
         MsdfFont font = HudStyle.title();
         String plain = UiText.display(ReplaceSymbols.normalize(message.getString()));
         if (plain.isEmpty()) {
            return true;
         }

         if (!supported(font, plain)) {
            return false;
         }

         float avail = w.getWidth() - margin * 2;
         float fs = LABEL_FS;
         float textW = font.width(plain, fs);
         if (textW > avail) {
            fs = LABEL_FS_SMALL;
            textW = font.width(plain, fs);
         }

         float a = Math.max(0.0F, Math.min(1.0F, alpha));
         int base = active ? HudStyle.TEXT : HudStyle.TEXT_OFF;
         float ty = HudStyle.textY(w.getY(), w.getHeight(), fs);
         if (textW <= avail) {
            drawComponent(g, font, message, w.getX() + (w.getWidth() - textW) / 2.0F, ty, fs, base, active, a);
         } else {
            int left = w.getX() + margin;
            int right = w.getX() + w.getWidth() - margin;
            float period = textW + MARQUEE_GAP;
            float off = (float)(System.nanoTime() / 1.0E9 * MARQUEE_SPEED % period);
            g.enableScissor(left, w.getY(), right, w.getY() + w.getHeight());
            drawComponent(g, font, message, left - off, ty, fs, base, active, a);
            drawComponent(g, font, message, left - off + period, ty, fs, base, active, a);
            g.disableScissor();
         }

         return true;
      }

      return false;
   }

   private static void drawComponent(GuiGraphicsExtractor g, MsdfFont font, Component message, float x, float y, float fs, int base, boolean styled, float a) {
      float[] cx = new float[]{x};
      message.visit((style, str) -> {
         String text = UiText.display(ReplaceSymbols.normalize(str));
         int color = styled && style.getColor() != null ? 0xFF000000 | style.getColor().getValue() : base;
         Render2DUtil.msdfText(g, font, text, cx[0], y, fs, HudStyle.fade(color, a));
         cx[0] += font.width(text, fs);
         return Optional.empty();
      }, Style.EMPTY);
   }

   public static boolean drawCentered(GuiGraphicsExtractor g, Component message, float centerX, float centerY, int color) {
      if (CleanRenderScope.isActive()) return false;
      MsdfFont font = HudStyle.title();
      String plain = UiText.display(ReplaceSymbols.normalize(message.getString()));
      if (plain.isEmpty() || !supported(font, plain)) {
         return false;
      }

      float w = font.width(plain, LIST_FS);
      drawComponent(g, font, message, centerX - w / 2.0F, centerY - 0.35F * LIST_FS, LIST_FS, color == -1 ? HudStyle.TEXT : color, true, 1.0F);
      return true;
   }

   public static boolean drawString(GuiGraphicsExtractor g, AbstractWidget w, Component message) {
      if (CleanRenderScope.isActive()) return false;
      if (w instanceof StringWidget && message != null && w.visible) {
         MsdfFont font = HudStyle.title();
         String plain = UiText.display(ReplaceSymbols.normalize(message.getString()));
         if (plain.isEmpty() || !supported(font, plain)) {
            return false;
         }

         float size = Math.min(STRING_FS, (w.getWidth() + 12) / Math.max(1, font.width(plain, 1)));
         float textW = font.width(plain, size);

         float[] cx = new float[]{w.getX() + (w.getWidth() - textW) / 2.0F};
         float ty = HudStyle.textY(w.getY(), w.getHeight(), size);
         message.visit((style, str) -> {
            String text = UiText.display(ReplaceSymbols.normalize(str));
            int color = style.getColor() != null ? 0xFF000000 | style.getColor().getValue() : HudStyle.TEXT;
            Render2DUtil.msdfText(g, font, text, cx[0], ty, size, color);
            cx[0] += font.width(text, size);
            return Optional.empty();
         }, Style.EMPTY);
         return true;
      }

      return false;
   }

   private static boolean isLabelled(AbstractWidget w) {
      return w instanceof AbstractSliderButton || w instanceof AbstractButton && !(w instanceof PlainTextButton);
   }

   public static boolean drawMenuText(GuiGraphicsExtractor g, Component message, int x, int y, int color) {
      if (CleanRenderScope.isActive()) return false;
      MsdfFont font = HudStyle.title();
      String plain = UiText.display(ReplaceSymbols.normalize(message.getString()));
      if (plain.isEmpty() || !supported(font, plain)) return false;
      drawComponent(g, font, message, x, y, STRING_FS, color, true, 1);
      return true;
   }

   public static Component component(FormattedCharSequence sequence) {
      MutableComponent result = Component.empty();
      StringBuilder segment = new StringBuilder();
      Style[] current = {Style.EMPTY};
      sequence.accept((index, style, codepoint) -> {
         if (!style.equals(current[0])) {
            if (!segment.isEmpty()) result.append(Component.literal(segment.toString()).withStyle(current[0]));
            segment.setLength(0);
            current[0] = style;
         }
         segment.appendCodePoint(codepoint);
         return true;
      });
      if (!segment.isEmpty()) result.append(Component.literal(segment.toString()).withStyle(current[0]));
      return result;
   }

   private static boolean supported(MsdfFont font, String text) {
      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c != ' ' && font.glyph(c) == null) {
            return false;
         }
      }

      return true;
   }

   private static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hover, boolean disabled, float a) {
      float t = hoverAnim(x, y, w, h, hover && !disabled);
      int fill = disabled ? FILL_DISABLED : Render2DUtil.lerpColor(FILL_IDLE, FILL_HOVER, t);
      int outline = disabled ? OUTLINE_DISABLED : Render2DUtil.lerpColor(OUTLINE_IDLE, OUTLINE_HOVER, t);
      float r = Math.min(RADIUS, h / 2.0F);
      if (!disabled) {
         Render2DUtil.softShadow(g, x, y, w, h, r, 2.0F + 3.0F * t, a * 0.9F);
      }
      Render2DUtil.roundedRect(g, x, y, w, h, r, HudStyle.fade(fill, a));
      Render2DUtil.roundedRect(g, x + r * 0.5F, (float)y, w - r, 1.0F, 0.5F, HudStyle.fade(0x0EFFFFFF + ((int)(t * 10) << 24), a));
      Render2DUtil.roundedOutline(g, x, y, w, h, r, 0.6F, HudStyle.fade(outline, a));
      if (t > 0.01F) {
         Render2DUtil.roundedOutline(g, x, y, w, h, r, 0.8F, Accent.color(a * t * 0.6F));
         float barW = (w - r * 4.0F) * t;
         Render2DUtil.gradientRectFull(g, Math.round(x + (w - barW) / 2.0F), y + h - 2, Math.round(barW), 1, Accent.color(a * t), Accent.secondary(a * t), 0.5F);
      }
   }

   private static void handle(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hover, float a) {
      float cx = x + w / 2.0F;
      if (sliderH == h && sliderY == y) {
         float t = hoverAnim(sliderX, y, sliderW, h, hover);
         float r = Math.min(RADIUS, h / 2.0F);
         float fillW = Math.max(r * 2.0F, cx - sliderX);
         Render2DUtil.roundedRect(g, (float)sliderX, (float)y, fillW, (float)h, r, HudStyle.fade(SLIDER_FILL, a));
         float lineX = sliderX + 4.0F;
         float lineW = Math.max(0.0F, cx - lineX);
         float lineH = 1.5F + 0.5F * t;
         float lineY = y + h - 3.0F - lineH / 2.0F;
         if (lineW >= 1.0F) {
            Accent.glow(g, lineX, lineY, lineW, lineH, lineH / 2.0F, a * (0.6F + 0.4F * t), 3);
            Accent.fill(g, lineX, lineY, lineW, lineH, lineH / 2.0F, a);
         }

         float dot = 3.0F + t;
         Render2DUtil.roundedRect(g, cx - dot / 2.0F, lineY + lineH / 2.0F - dot / 2.0F, dot, dot, dot / 2.0F, HudStyle.fade(0xFFFFFFFF, a));
      }
   }

   private static void checkbox(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean on, boolean hover, float a) {
      float r = Math.min(4.0F, w / 3.0F);
      Render2DUtil.softShadow(g, x, y, w, h, r, on ? 3.0F : 2.0F, a * 0.8F);
      if (on) {
         Accent.glow(g, x, y, w, h, r, a, 4);
         Accent.fill(g, x, y, w, h, r, a);
         int pad = Math.max(2, w / 5);
         Render2DUtil.check(g, x + pad, y + pad, w - pad * 2, h - pad * 2, 1.8F, HudStyle.fade(0xFFFFFFFF, a));
      } else {
         Render2DUtil.roundedRect(g, x, y, w, h, r, HudStyle.fade(hover ? 0xFF2E2C2C : HudStyle.PILL, a));
      }
      Render2DUtil.roundedOutline(g, x, y, w, h, r, 0.6F, HudStyle.fade(hover ? 0x66FFFFFF : 0x24FFFFFF, a));
   }

   private static void field(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean focus, float a) {
      float r = Math.min(4.0F, h / 2.0F);
      Render2DUtil.roundedRect(g, x, y, w, h, r, HudStyle.fade(FIELD_FILL, a));
      Render2DUtil.roundedOutline(g, x, y, w, h, r, 0.6F, HudStyle.fade(FIELD_OUTLINE, a));
      if (focus) {
         Render2DUtil.roundedOutline(g, x, y, w, h, r, 0.8F, Accent.color(a * 0.8F));
      }
   }

   private static void tab(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected, boolean hover, float a) {
      float t = hoverAnim(x, y, w, h, hover || selected);
      int ix = x + 2;
      int iy = y + 3;
      int iw = w - 4;
      int ih = h - 6;
      if (selected) {
         Render2DUtil.softShadow(g, ix, iy, iw, ih, RADIUS, 3.0F, a);
         Render2DUtil.roundedRect(g, ix, iy, iw, ih, RADIUS, HudStyle.fade(HudStyle.PILL, a));
         Render2DUtil.roundedOutline(g, ix, iy, iw, ih, RADIUS, 0.6F, HudStyle.fade(0x40FFFFFF, a));
         float barW = Math.min(iw - 12.0F, 24.0F + 8.0F * t);
         Accent.fill(g, ix + (iw - barW) / 2.0F, iy + ih - 3.0F, barW, 2.0F, 1.0F, a);
      } else if (t > 0.01F) {
         Render2DUtil.roundedRect(g, ix, iy, iw, ih, RADIUS, HudStyle.fade(0x16FFFFFF, a * t));
      }
   }

   private static float hoverAnim(int x, int y, int w, int h, boolean target) {
      AbstractWidget wd = widget;
      if (wd != null && wd.getX() == x && wd.getY() == y && wd.getWidth() == w && wd.getHeight() == h) {
         float[] v = HOVER.computeIfAbsent(wd, k -> new float[]{target ? 1.0F : 0.0F});
         float k = 1.0F - (float)Math.exp(-HOVER_SPEED * frameDt);
         v[0] += ((target ? 1.0F : 0.0F) - v[0]) * k;
         return v[0];
      }

      return target ? 1.0F : 0.0F;
   }
}
