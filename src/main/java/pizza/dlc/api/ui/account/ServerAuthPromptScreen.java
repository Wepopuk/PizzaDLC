package pizza.dlc.api.ui.account;

import pizza.dlc.api.translation.Translations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import pizza.dlc.api.ui.menu.MenuAnim;
import pizza.dlc.api.ui.menu.MenuKit;

/** Shows the exact destination and account. No password is rendered or placed in chat. */
public final class ServerAuthPromptScreen extends Screen {
   private final String account;
   private final String server;
   private final String title;
   private final String detail;
   private final String acceptLabel;
   private Runnable accept;
   private Runnable decline;
   private final long openedAt = System.currentTimeMillis();
   private int buttonX;
   private int buttonY;
   private int buttonW;
   private float yesHover;
   private float noHover;
   private float scale = 1;
   private float centerX;
   private float centerY;
   private long lastFrame;

   public ServerAuthPromptScreen(String account, String server, String title, String detail,
      String acceptLabel, Runnable accept, Runnable decline) {
      super(Component.literal(title));
      this.account = account;
      this.server = server;
      this.title = title;
      this.detail = detail;
      this.acceptLabel = acceptLabel;
      this.accept = accept;
      this.decline = decline;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
      long now = System.nanoTime();
      float dt = lastFrame == 0 ? 0.016F : Math.min(0.1F, (now - lastFrame) / 1000000000.0F);
      lastFrame = now;
      float p = Math.min(1, (System.currentTimeMillis() - openedAt) / 280.0F);
      float a = MenuAnim.easeOutCubic(p);
      scale = 0.95F + 0.05F * MenuAnim.easeOutBack(p);
      centerX = width / 2.0F;
      centerY = height / 2.0F;
      mx = Math.round(centerX + (mx - centerX) / scale);
      my = Math.round(centerY + (my - centerY) / scale);
      int w = Math.min(width - 24, 330);
      int h = 144;
      int x = (width - w) / 2;
      int y = (height - h) / 2;
      ctx.pose().pushMatrix();
      ctx.pose().translate(centerX, centerY);
      ctx.pose().scale(scale, scale);
      ctx.pose().translate(-centerX, -centerY);
      MenuKit.window(ctx, x, y, w, h, a);
      MenuKit.regText(ctx, Translations.text(title), x + 14, y + 15, 10.5F, MenuKit.fade(MenuKit.TEXT, a));
      MenuKit.bookText(ctx, MenuKit.truncate(false, server, 8, w - 28), x + 14, y + 37, 8, MenuKit.fade(MenuKit.TEXT, a));
      MenuKit.bookText(ctx, Translations.text("Аккаунт: ") + account, x + 14, y + 54, 7.5F, MenuKit.fade(MenuKit.UID, a));
      MenuKit.bookText(ctx, MenuKit.truncate(false, Translations.text(detail), 7, w - 28), x + 14, y + 73, 7, MenuKit.fade(MenuKit.TEXT_OFF, a));
      MenuKit.bookText(ctx, Translations.text("Пароль скрыт · только для этого сервера"), x + 14, y + 87, 6.5F, MenuKit.fade(MenuKit.UID, a));
      buttonX = x + 14;
      buttonY = y + h - 34;
      buttonW = (w - 34) / 2;
      yesHover = MenuKit.expLerp(yesHover, MenuKit.inside(mx, my, buttonX, buttonY, buttonW, 20) ? 1 : 0, 14, dt);
      noHover = MenuKit.expLerp(noHover, MenuKit.inside(mx, my, buttonX + buttonW + 6, buttonY, buttonW, 20) ? 1 : 0, 14, dt);
      MenuKit.pill(ctx, buttonX, buttonY, buttonW, 20, Translations.text(acceptLabel), yesHover, true, a);
      MenuKit.pill(ctx, buttonX + buttonW + 6, buttonY, buttonW, 20, Translations.text("Не сейчас"), noHover, false, a);
      ctx.pose().popMatrix();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
      if (e.button() == 0) {
         float mx = centerX + (float)(e.x() - centerX) / scale;
         float my = centerY + (float)(e.y() - centerY) / scale;
         if (MenuKit.inside(mx, my, buttonX, buttonY, buttonW, 20)) { answer(true); return true; }
         if (MenuKit.inside(mx, my, buttonX + buttonW + 6, buttonY, buttonW, 20)) { answer(false); return true; }
      }
      return super.mouseClicked(e, dbl);
   }

   @Override
   public boolean keyPressed(KeyEvent e) {
      if (e.key() == 256) answer(false);
      // Enter is intentionally not an implicit approval of a password destination.
      return true;
   }

   private void answer(boolean yes) {
      Runnable action = yes ? accept : decline;
      accept = null;
      decline = null;
      Minecraft.getInstance().setScreenAndShow(null);
      if (action != null) action.run();
   }

   @Override
   public void removed() {
      Runnable action = decline;
      accept = null;
      decline = null;
      if (action != null) action.run();
      super.removed();
   }

   @Override
   public void onClose() {
      answer(false);
   }
}
