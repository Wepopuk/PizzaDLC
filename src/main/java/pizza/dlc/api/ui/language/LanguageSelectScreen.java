package pizza.dlc.api.ui.language;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.translation.ClientLanguage;
import pizza.dlc.api.translation.TranslationManager;
import pizza.dlc.api.translation.MinecraftLanguageSync;
import pizza.dlc.api.ui.MenuBackdropScreen;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.util.render.Render2DUtil;

public final class LanguageSelectScreen extends Screen implements MenuBackdropScreen {
   private final Screen parent;
   private final boolean firstLaunch;
   private final TranslationManager manager = TranslationManager.getInstance();
   private final float[] hover = new float[5], selection = new float[5];
   private ClientLanguage chosen = manager.language();
   private long openedAt, lastFrame;
   private float buttonHover;
   private boolean saveFailed;

   public LanguageSelectScreen(Screen parent, boolean firstLaunch) {
      super(Component.literal("Language / Язык"));
      this.parent = parent;
      this.firstLaunch = firstLaunch;
   }

   @Override protected void init() { if (openedAt == 0) openedAt = System.nanoTime(); }
   @Override public boolean isPauseScreen() { return false; }
   @Override public boolean shouldCloseOnEsc() { return !firstLaunch; }
   @Override public void onClose() { if (!firstLaunch) minecraft.setScreenAndShow(parent); }

   private float progress() { return Math.min(1, Math.max(0, (System.nanoTime() - openedAt) / 340_000_000F)); }
   private LanguagePickerLayout layout() {
      float p = progress(), eased = 1 - (1 - p) * (1 - p) * (1 - p);
      return LanguagePickerLayout.fit(width, height, .94F + .06F * eased);
   }
   private String tr(String key) { return manager.translate(chosen, key); }

   public static void flag(GuiGraphicsExtractor ctx, ClientLanguage language, float x, float y, float w, float h, float alpha) {
      Render2DUtil.roundedTexture(ctx, Identifier.fromNamespaceAndPath("pizzadlc", "textures/gui/languages/" + language.flag() + ".png"),
         x, y, w, h, 3, true, MenuKit.fade(0xFFFFFFFF, alpha));
      Render2DUtil.roundedOutline(ctx, Math.round(x), Math.round(y), Math.round(w), Math.round(h), 3, .6F, MenuKit.fade(0x35FFFFFF, alpha));
   }

   @Override public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
      MenuKit.background(ctx, width, height);
   }

   @Override public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
      long now = System.nanoTime();
      float dt = lastFrame == 0 ? .016F : Math.min(.1F, (now - lastFrame) / 1_000_000_000F);
      lastFrame = now;
      LanguagePickerLayout l = layout();
      float x = l.localX(mx), y = l.localY(my);
      float p = progress(), alpha = 1 - (1 - p) * (1 - p) * (1 - p);
      ctx.pose().pushMatrix();
      ctx.pose().translate(l.centerX(), l.centerY());
      ctx.pose().scale(l.scale(), l.scale());
      ctx.pose().translate(-LanguagePickerLayout.WIDTH / 2F, -LanguagePickerLayout.HEIGHT / 2F);
      MenuKit.window(ctx, 0, 0, LanguagePickerLayout.WIDTH, LanguagePickerLayout.HEIGHT, alpha);
      MenuKit.brand(ctx, 0, 0, firstLaunch ? tr("language.welcome") : tr("language.title"), alpha);
      MenuKit.regText(ctx, tr("language.choose"), 22, 53, 15, MenuKit.fade(MenuKit.TEXT, alpha));
      MenuKit.bookText(ctx, tr("language.hint"), 22, 76, 7.5F, MenuKit.fade(MenuKit.UID, alpha));
      for (ClientLanguage language : ClientLanguage.values()) {
         int index = language.ordinal(), rowY = LanguagePickerLayout.rowY(index);
         hover[index] = MenuKit.expLerp(hover[index], LanguagePickerLayout.rowAt(x, y) == index ? 1 : 0, 14, dt);
         selection[index] = MenuKit.expLerp(selection[index], chosen == language ? 1 : 0, 14, dt);
         float reveal = Math.min(1, Math.max(0, (p - index * .045F) * 2.2F));
         float a = alpha * reveal;
         int bg = Render2DUtil.lerpColor(MenuKit.MOD, MenuKit.MODE_SEL, hover[index]);
         Render2DUtil.roundedRect(ctx, 22, rowY, 348, 38, 8, MenuKit.fade(bg, a));
         Render2DUtil.roundedRect(ctx, 22, rowY, 348, 38, 8, Accent.color(selection[index] * a * .1F));
         Render2DUtil.roundedOutline(ctx, 22, rowY, 348, 38, 8, .8F,
            MenuKit.fade(Render2DUtil.lerpColor(0x20FFFFFF, Accent.color(1), selection[index]), a));
         flag(ctx, language, 33, rowY + 8, 33, 22, a);
         MenuKit.regText(ctx, language.nativeName(), 78 + hover[index], rowY + 8, 10, MenuKit.fade(MenuKit.TEXT, a));
         MenuKit.bookText(ctx, language.englishName() + "  ·  " + language.code().toUpperCase(java.util.Locale.ROOT),
            78 + hover[index], rowY + 24, 6.5F, MenuKit.fade(MenuKit.UID, a));
         float badge = selection[index] * a;
         if (badge > .01F) {
            String selected = tr("language.selected");
            float badgeW = MenuKit.bookW(selected, 7) + 16;
            Render2DUtil.roundedRect(ctx, 357 - badgeW, rowY + 11, badgeW, 16, 5, Accent.color(badge * .18F));
            MenuKit.bookText(ctx, selected, 365 - badgeW, rowY + 16, 7, Accent.color(badge));
         }
      }
      boolean over = MenuKit.inside(x, y, 22, 324, 348, 27);
      buttonHover = MenuKit.expLerp(buttonHover, over ? 1 : 0, 14, dt);
      MenuKit.pill(ctx, 22, 324, 348, 27, tr(firstLaunch ? "language.continue" : "language.apply"), buttonHover, true, alpha);
      String footer = saveFailed ? tr("language.save_error") : firstLaunch ? "English · Русский · Українська · Polski · Deutsch" : tr("language.back") + " · Esc";
      float size = saveFailed ? 6.5F : 6;
      MenuKit.bookText(ctx, MenuKit.truncate(false, footer, size, 348), 22, 360, size,
         MenuKit.fade(saveFailed ? 0xFFFF7878 : MenuKit.UID, alpha));
      ctx.pose().popMatrix();
   }

   private void apply() {
      if (manager.setLanguage(chosen.code())) {
         MinecraftLanguageSync.applySavedLanguage();
         minecraft.setScreenAndShow(parent);
      }
      else saveFailed = true;
   }

   @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() != 0) return super.mouseClicked(event, doubleClick);
      LanguagePickerLayout l = layout();
      float x = l.localX(event.x()), y = l.localY(event.y());
      int row = LanguagePickerLayout.rowAt(x, y);
      if (row >= 0) { chosen = ClientLanguage.values()[row]; saveFailed = false; return true; }
      if (MenuKit.inside(x, y, 22, 324, 348, 27)) { apply(); return true; }
      return super.mouseClicked(event, doubleClick);
   }

   @Override public boolean keyPressed(KeyEvent event) {
      int key = event.key();
      if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_LEFT) {
         int direction = key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_TAB && event.hasShiftDown() ? -1 : 1;
         chosen = ClientLanguage.values()[Math.floorMod(chosen.ordinal() + direction, 5)];
         saveFailed = false;
         return true;
      }
      if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { apply(); return true; }
      if (key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
      return super.keyPressed(event);
   }
}
