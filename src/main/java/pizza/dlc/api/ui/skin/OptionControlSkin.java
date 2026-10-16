package pizza.dlc.api.ui.skin;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.client.modules.impl.visuals.hud.HudStyle;
import pizza.dlc.mixin.client.CycleButtonSkinAccessor;

/** Renders real Minecraft controls with client visuals while retaining option callbacks and narration. */
public final class OptionControlSkin {
   private static final Map<AbstractWidget, Boolean> ROWS = new WeakHashMap<>();
   private static final Map<AbstractWidget, Animation> ANIMATIONS = new WeakHashMap<>();

   private OptionControlSkin() { }

   public static void row(GuiGraphicsExtractor g, AbstractWidget widget, int x, int y, int width, int height,
      int mouseX, int mouseY) {
      ROWS.put(widget, true);
      OptionControlLayout.Row layout = OptionControlLayout.row(x, y, width, height);
      layout.place(widget);
      boolean hover = MenuKit.inside(mouseX, mouseY, x, y, width, height);
      Render2DUtil.roundedRect(g, x, y, width, height, 7, hover ? 0xFF1A1919 : MenuKit.MOD);
      Render2DUtil.roundedOutline(g, x, y, width, height, 7, .5F, hover ? 0x24FFFFFF : 0x0EFFFFFF);
      String label = widget instanceof CycleButton<?> cycle
         ? ((CycleButtonSkinAccessor)cycle).pizzadlc$getOptionName().getString() : OptionControlText.from(widget.getMessage()).label();
      text(g, label, layout.labelX(), y, layout.labelW(), height, 8, widget.active ? MenuKit.TEXT : MenuKit.TEXT_OFF, widget.getAlpha(), false);
   }

   public static boolean drawButton(GuiGraphicsExtractor g, AbstractWidget widget) {
      if (CleanRenderScope.isActive() || !ROWS.containsKey(widget)) return false;
      int x = widget.getX(), y = widget.getY(), w = widget.getWidth(), h = widget.getHeight();
      float a = widget.getAlpha() * (widget.active ? 1 : .5F);
      Animation anim = animate(widget, 0, false);
      if (widget instanceof CycleButton<?> cycle && cycle.getValue() instanceof Boolean enabled) {
         float target = enabled ? 1 : 0;
         if (!anim.toggleReady) { anim.toggle = target; anim.toggleReady = true; }
         anim.toggle = MenuKit.expLerp(anim.toggle, target, 16, anim.dt);
         float toggleW = 29, toggleH = 14;
         Accent.toggle(g, x + w - toggleW - 2, y + (h - toggleH) / 2, toggleW, toggleH,
            anim.toggle, MenuKit.fade(MenuKit.MODE_SEL, a), MenuKit.fade(0xFF929292, a));
         if (!widget.active) Render2DUtil.roundedRect(g, x + w - toggleW - 2, y + (h - toggleH) / 2,
            toggleW, toggleH, toggleH / 2, MenuKit.fade(0xA00F0F0F, widget.getAlpha()));
         String value = ((CycleButtonSkinAccessor)cycle).pizzadlc$getValueStringifier().apply(cycle.getValue()).getString();
         text(g, value, x + 2, y, Math.max(1, w - toggleW - 10), h, 7.5F, MenuKit.TEXT_OFF, a, false);
      } else {
         Render2DUtil.roundedRect(g, x, y + 2, w, h - 4, 4, MenuKit.fade(anim.hover > .05F ? 0xFF292727 : MenuKit.MODE_SEL, a));
         Render2DUtil.roundedOutline(g, x, y + 2, w, h - 4, 4, .5F,
            widget.isFocused() ? Accent.color(a * .75F) : MenuKit.fade(0x24FFFFFF, a));
         OptionControlText content = OptionControlText.from(widget.getMessage());
         String value = widget instanceof CycleButton<?> cycle
            ? ((CycleButtonSkinAccessor)cycle).pizzadlc$getValueStringifier().apply(cycle.getValue()).getString()
            : content.value().isEmpty() ? Translations.text("Открыть") : content.value();
         text(g, value, x + 6, y, w - 18, h, 7.5F, MenuKit.TEXT, a, true);
         chevron(g, x + w - 8, y + h / 2F, widget instanceof CycleButton<?>, MenuKit.fade(MenuKit.UID, a));
      }
      if (widget.isFocused()) Render2DUtil.roundedOutline(g, x, y, w, h, 4, .7F, Accent.color(a * .65F));
      if (widget.isHovered()) g.requestCursor(widget.active ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
      return true;
   }

   public static OptionControlLayout.Slider sliderLayout(AbstractWidget widget) {
      OptionControlText content = OptionControlText.from(widget.getMessage());
      String value = OptionControlText.display(content.value().isEmpty() ? content.label() : content.value());
      return OptionControlLayout.slider(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), MenuKit.bookW(value, 7.5F));
   }

   public static boolean drawSlider(GuiGraphicsExtractor g, AbstractSliderButton widget, double value, boolean dragging) {
      if (CleanRenderScope.isActive()) return false;
      float a = widget.getAlpha();
      if (a <= .004F) return true;
      OptionControlLayout.Slider layout = sliderLayout(widget);
      Animation anim = animate(widget, (float)value, dragging);
      float trackAlpha = a * (widget.active ? 1 : .35F);
      Render2DUtil.roundedRect(g, layout.trackX() - 2, layout.trackY(), layout.trackW() + 4, layout.trackH(), 2,
         MenuKit.fade(MenuKit.MODE_SEL, trackAlpha));
      float knobX = layout.knobX(dragging ? value : anim.value);
      float fill = knobX - layout.trackX();
      if (fill > .5F) {
         if (widget.active) Accent.glow(g, layout.trackX() - 2, layout.trackY(), fill + 2, layout.trackH(), 2,
            trackAlpha * (.65F + .25F * anim.hover), 3);
         Accent.fill(g, layout.trackX() - 2, layout.trackY(), fill + 2, layout.trackH(), 2, trackAlpha);
      }
      float knob = 6 + 2 * anim.hover;
      if (widget.active && anim.hover > .01F) {
         float ring = knob + 4 * anim.hover;
         Render2DUtil.roundedRect(g, knobX - ring / 2, layout.trackY() + 2 - ring / 2, ring, ring, ring / 2, Accent.color(trackAlpha * .2F * anim.hover));
      }
      Render2DUtil.roundedRect(g, knobX - knob / 2, layout.trackY() + 2 - knob / 2, knob, knob, knob / 2, MenuKit.fade(0xFFFFFFFF, trackAlpha));
      Render2DUtil.roundedRect(g, layout.valueX(), layout.valueY(), layout.valueW(), layout.valueH(), 3,
         MenuKit.fade(0xFF1C1C1C, a));
      Render2DUtil.roundedOutline(g, layout.valueX(), layout.valueY(), Math.round(layout.valueW()), layout.valueH(), 3, .5F,
         widget.isFocused() ? Accent.color(trackAlpha * .75F) : MenuKit.fade(0x24FFFFFF, a));
      OptionControlText content = OptionControlText.from(widget.getMessage());
      text(g, content.value().isEmpty() ? content.label() : content.value(), layout.valueX() + 3, layout.valueY(),
         layout.valueW() - 6, layout.valueH(), 7.5F, MenuKit.TEXT, trackAlpha, true);
      if (!ROWS.containsKey(widget) && !content.value().isEmpty()) {
         // Sliders outside the options list also receive the same control and a compact caption.
         text(g, content.label(), widget.getX() + 6, widget.getY() - 1, Math.max(1, layout.valueX() - widget.getX() - 16),
            7, 6, MenuKit.TEXT, trackAlpha, false);
      }
      if (dragging) g.requestCursor(CursorTypes.RESIZE_EW);
      else if (widget.isHovered()) g.requestCursor(widget.active ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
      return true;
   }

   private static Animation animate(AbstractWidget widget, float value, boolean dragging) {
      Animation anim = ANIMATIONS.computeIfAbsent(widget, key -> new Animation(value));
      long now = System.nanoTime();
      anim.dt = anim.lastNs == 0 ? .016F : Math.min(.1F, Math.max(0, (now - anim.lastNs) / 1.0E9F));
      anim.lastNs = now;
      anim.hover = MenuKit.expLerp(anim.hover, widget.active && (dragging || widget.isHoveredOrFocused()) ? 1 : 0, 16, anim.dt);
      anim.value = dragging ? value : MenuKit.expLerp(anim.value, value, 18, anim.dt);
      return anim;
   }

   private static void chevron(GuiGraphicsExtractor g, float x, float y, boolean down, int color) {
      // Draw geometry so arrows remain visible with every font and language.
      if (down) {
         Render2DUtil.line(g, x - 2, y - 1, x, y + 1, .8F, color);
         Render2DUtil.line(g, x, y + 1, x + 2, y - 1, .8F, color);
      } else {
         Render2DUtil.line(g, x - 1, y - 2, x + 1, y, .8F, color);
         Render2DUtil.line(g, x + 1, y, x - 1, y + 2, .8F, color);
      }
   }

   private static void text(GuiGraphicsExtractor g, String text, float x, float y, float width, float height,
      float size, int color, float alpha, boolean centered) {
      if (width <= 0 || text.isEmpty()) return;
      text = OptionControlText.display(text);
      MsdfFont font = MenuKit.book();
      boolean supported = text.codePoints().allMatch(cp -> cp == ' ' || font.glyph(cp) != null);
      if (!supported) {
         var vanilla = Minecraft.getInstance().font;
         String shown = vanilla.plainSubstrByWidth(text, Math.max(1, (int)width));
         g.text(vanilla, shown, (int)(centered ? x + (width - vanilla.width(shown)) / 2 : x), (int)(y + (height - 9) / 2), MenuKit.fade(color, alpha), false);
         return;
      }
      String shown = MenuKit.truncate(false, text, size, width);
      float textX = centered ? x + (width - font.width(shown, size)) / 2 : x;
      MenuKit.bookText(g, shown, textX, HudStyle.textY(y, height, size), size, MenuKit.fade(color, alpha));
   }

   private static final class Animation {
      float value, hover, dt, toggle;
      long lastNs;
      boolean toggleReady;
      Animation(float value) { this.value = value; }
   }
}
