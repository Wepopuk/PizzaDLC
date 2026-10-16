package pizza.dlc.api.ui.account;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.ui.TextEdit;
import pizza.dlc.api.ui.menu.MenuAnim;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.util.render.Render2DUtil;

/** Visual animation only: edits, selection and clipboard operations stay immediate. */
final class AnimatedAccountField {
   private final long born = System.nanoTime();
   private long changedAt = born;
   private long savedAt;
   private String previous = "";
   private int animatedFrom;
   private float focus;
   private float hover;
   private float caretX;
   private float scrollX;
   private boolean initialized;

   void confirm() {
      savedAt = System.nanoTime();
   }

   void draw(GuiGraphicsExtractor ctx, String value, TextEdit edit, String hint, boolean active,
      int x, int y, int w, int h, float fontSize, int mx, int my, float alpha, float dt) {
      long now = System.nanoTime();
      if (!value.equals(previous)) {
         int prefix = 0;
         while (prefix < value.length() && prefix < previous.length()
            && value.codePointAt(prefix) == previous.codePointAt(prefix)) {
            prefix += Character.charCount(value.codePointAt(prefix));
         }
         animatedFrom = prefix;
         previous = value;
         changedAt = now;
      }
      float appear = MenuAnim.easeOutCubic(clamp((now - born) / 260000000.0F));
      float a = alpha * appear;
      focus = MenuKit.expLerp(focus, active ? 1 : 0, 14, dt);
      hover = MenuKit.expLerp(hover, MenuKit.inside(mx, my, x, y, w, h) ? 1 : 0, 12, dt);
      float saved = savedAt == 0 ? 0 : 1 - clamp((now - savedAt) / 650000000.0F);
      float glow = Math.max(focus, saved);
      Render2DUtil.roundedRect(ctx, x, y, w, h, 4, MenuKit.fade(Render2DUtil.lerpColor(0xFF0C0B0B, MenuKit.MODE_SEL, hover * 0.25F + focus * 0.3F), a));
      if (glow > 0.01F) {
         Render2DUtil.gradientRectFull(ctx, x, y, w, h, Accent.color(a * glow * 0.13F), Accent.secondary(0), 4);
      }
      Render2DUtil.roundedOutline(ctx, x, y, w, h, 4, 0.6F,
         MenuKit.fade(Render2DUtil.lerpColor(0x18FFFFFF, Accent.color(0.8F), Math.max(glow, hover * 0.35F)), a));
      if (glow > 0.01F) {
         float lineW = (w - 10) * glow;
         Render2DUtil.gradientRectFull(ctx, Math.round(x + (w - lineW) / 2), y + h - 1, Math.round(lineW), 1,
            Accent.color(a * glow * 0.7F), Accent.secondary(a * glow * 0.2F), 0.5F);
      }
      int caret = edit == null ? 0 : edit.caret();
      float targetCaret = MenuKit.bookW(value.substring(0, Math.min(caret, value.length())), fontSize);
      float targetScroll = active ? Math.max(0, targetCaret - (w - 16)) : 0;
      if (!initialized) {
         caretX = targetCaret;
         scrollX = targetScroll;
         initialized = true;
      }
      caretX = MenuKit.expLerp(caretX, targetCaret, 24, dt);
      scrollX = MenuKit.expLerp(scrollX, targetScroll, 18, dt);
      float tx = x + 6 - scrollX;
      float ty = y + (h - fontSize * 0.7F) / 2;
      ctx.enableScissor(x + 4, y, x + w - 4, y + h);
      if (active && edit != null && edit.hasSelection()) {
         float start = MenuKit.bookW(value.substring(0, edit.selStart()), fontSize);
         float end = MenuKit.bookW(value.substring(0, edit.selEnd()), fontSize);
         Render2DUtil.roundedRect(ctx, tx + start, y + 3, end - start, h - 6, 1.5F, Accent.color(a * 0.35F));
      }
      float age = (now - changedAt) / 1000000.0F;
      if (value.isEmpty()) {
         MenuKit.bookText(ctx, hint, x + 6 + (1 - appear) * 5, ty, fontSize, MenuKit.fade(MenuKit.UID, a * (1 - focus * 0.3F)));
      } else if (age >= 340 || animatedFrom == value.length()) {
         MenuKit.bookText(ctx, value, tx, ty, fontSize, MenuKit.fade(MenuKit.TEXT, a));
      } else {
         MenuKit.bookText(ctx, value.substring(0, animatedFrom), tx, ty, fontSize, MenuKit.fade(MenuKit.TEXT, a));
         int index = 0;
         for (int i = animatedFrom; i < value.length();) {
            int next = i + Character.charCount(value.codePointAt(i));
            float p = clamp((age - Math.min(150, index * 18)) / 180);
            float ease = MenuAnim.easeOutCubic(p);
            float gx = tx + MenuKit.bookW(value.substring(0, i), fontSize);
            MenuKit.bookText(ctx, value.substring(i, next), gx, ty + (1 - ease) * 3, fontSize,
               MenuKit.fade(Render2DUtil.lerpColor(Accent.color(1), MenuKit.TEXT, ease), a * ease));
            i = next;
            index++;
         }
      }
      if (focus > 0.01F) {
         float blink = edit != null && edit.sinceChange() < 0.55F ? 1 : 0.25F + 0.75F * (0.5F + 0.5F * (float)Math.sin(now / 1000000000.0 * 5.9));
         Render2DUtil.roundedRect(ctx, tx + caretX + 0.5F, y + 3, 1, h - 6, 0.5F, Accent.color(a * focus * blink));
      }
      if (saved > 0.01F) {
         float bandX = x - 30 + (w + 60) * MenuAnim.easeOutCubic(1 - saved);
         Render2DUtil.gradientRectFull(ctx, Math.round(bandX), y + 1, 22, h - 2, 0x00FFFFFF, MenuKit.fade(0x24FFFFFF, a * saved), 0);
         Render2DUtil.gradientRectFull(ctx, Math.round(bandX + 22), y + 1, 22, h - 2, MenuKit.fade(0x24FFFFFF, a * saved), 0x00FFFFFF, 0);
      }
      ctx.disableScissor();
   }

   private static float clamp(float value) {
      return Math.max(0, Math.min(1, value));
   }
}
