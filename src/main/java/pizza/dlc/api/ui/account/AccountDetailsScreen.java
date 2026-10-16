package pizza.dlc.api.ui.account;

import pizza.dlc.api.translation.Translations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import pizza.dlc.api.account.ServerLoginStore;
import pizza.dlc.api.filemanager.impl.AltAccountManager;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.ui.MenuBackdropScreen;
import pizza.dlc.api.ui.TextEdit;
import pizza.dlc.api.ui.TypingScreen;
import pizza.dlc.api.ui.menu.MenuAnim;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.util.render.Render2DUtil;

public final class AccountDetailsScreen extends Screen implements MenuBackdropScreen, TypingScreen {
   private final Screen parent;
   private final String account;
   private final TextEdit address = new TextEdit(260, c -> !Character.isWhitespace(c));
   private final TextEdit password = new TextEdit(240, c -> !Character.isWhitespace(c) && c != 167);
   private final List<Hit> hits = new ArrayList<>();
   private final AnimatedAccountField addressField = new AnimatedAccountField();
   private final AnimatedAccountField passwordField = new AnimatedAccountField();
   private final Map<String, Float> hovers = new HashMap<>();
   private final Map<String, Float> selections = new HashMap<>();
   private long openedAt;
   private long closedAt;
   private long statusAt;
   private long lastFrame;
   private float dt = 0.016F;
   private float alpha;
   private float scale = 1;
   private float slide;
   private float centerX;
   private float centerY;
   private int focused = -1;
   private int scroll;
   private String selectedServer;
   private String command = "login";
   private boolean enabled = true;
   private String status = "";
   private boolean error;
   private boolean useSharedPassword;

   public AccountDetailsScreen(Screen parent, AltAccountManager.OfflineAlt alt) {
      super(Component.literal(Translations.text("Серверы аккаунта")));
      this.parent = parent;
      this.account = alt.name();
   }

   @Override
   protected void init() {
      if (openedAt == 0) openedAt = System.currentTimeMillis();
   }

   @Override
   public boolean isTyping() {
      return focused >= 0 && closedAt == 0;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
      MenuKit.background(ctx, width, height);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
      long now = System.nanoTime();
      dt = lastFrame == 0 ? 0.016F : Math.min(0.1F, (now - lastFrame) / 1000000000.0F);
      lastFrame = now;
      float open = clamp((System.currentTimeMillis() - openedAt) / 340.0F);
      float close = closedAt == 0 ? 0 : MenuAnim.easeOutCubic(clamp((System.currentTimeMillis() - closedAt) / 180.0F));
      alpha = MenuAnim.easeOutCubic(open) * (1 - close);
      scale = (0.95F + 0.05F * MenuAnim.easeOutBack(open)) * (1 - 0.018F * close);
      slide = (1 - MenuAnim.easeOutCubic(open)) * 9 + close * 7;
      centerX = width / 2.0F;
      centerY = height / 2.0F;
      mx = Math.round(centerX + (mx - centerX) / scale);
      my = Math.round(centerY + (my - centerY - slide) / scale);
      ctx.pose().pushMatrix();
      ctx.pose().translate(centerX, centerY + slide);
      ctx.pose().scale(scale, scale);
      ctx.pose().translate(-centerX, -centerY);
      hits.clear();
      int w = Math.min(width - 24, 440);
      int h = Math.min(height - 24, 280);
      int x = (width - w) / 2;
      int y = (height - h) / 2;
      int left = Math.min(140, w / 3);
      int rx = x + left + 24;
      int rw = w - left - 38;
      int footer = y + h - 30;
      MenuKit.window(ctx, x, y, w, h, alpha);
      MenuKit.regText(ctx, MenuKit.truncate(true, Translations.text("Серверы · ") + account, 11.0F, w - 28), x + 14, y + 14, 11.0F, MenuKit.fade(MenuKit.TEXT, alpha));
      MenuKit.bookText(ctx, Translations.text("Пароли и автоматический вход"), x + 14, y + 39, 7.5F, MenuKit.fade(MenuKit.UID, alpha * stage(60)));
      ServerLoginStore.Entry shared = ServerLoginStore.get().defaultForAccount(account);
      if (shared != null) {
         button(ctx, x + 14, y + 51, w - 28, 16, Translations.text("Общий пароль: ") + (shared.enabled() ? Translations.text("вкл") : Translations.text("выкл")) + Translations.text(" · подтверждение новых серверов"), mx, my,
            () -> ServerLoginStore.get().putDefault(account, shared.password(), !shared.enabled()));
      }
      label(ctx, Translations.text("Серверы"), x + 14, y + 72);
      Render2DUtil.rect(ctx, rx - 7, y + 70, 1, h - 104, MenuKit.fade(MenuKit.DIVIDER, alpha * stage(90)));
      List<ServerLoginStore.Entry> entries = ServerLoginStore.get().forAccount(account);
      int slots = Math.max(1, (footer - 28 - (y + 84)) / 22);
      scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - slots)));
      if (entries.isEmpty()) {
         MenuKit.bookText(ctx, Translations.text("Добавь адрес справа"), x + 14, y + 88, 6.5F, MenuKit.fade(MenuKit.UID, alpha * stage(100)));
      }
      for (int i = scroll; i < Math.min(entries.size(), scroll + slots); i++) {
         ServerLoginStore.Entry entry = entries.get(i);
         float row = stage(90 + (i - scroll) * 25);
         int rowY = y + 84 + (i - scroll) * 22 + Math.round((1 - row) * 5);
         boolean selected = entry.server().equals(selectedServer);
         float selection = MenuKit.expLerp(selections.getOrDefault(entry.server(), 0.0F), selected ? 1 : 0, 13, dt);
         selections.put(entry.server(), selection);
         float hover = hover("server:" + entry.server(), MenuKit.inside(mx, my, x + 14, rowY, left - 18, 18));
         String name = entry.server().endsWith(":25565") ? entry.server().substring(0, entry.server().length() - 6) : entry.server();
         MenuKit.pill(ctx, x + 14, rowY, left - 18, 18, "", hover, false, alpha * row);
         Render2DUtil.roundedOutline(ctx, x + 14, rowY, left - 18, 18, 4, 0.7F, Accent.color(alpha * row * selection * 0.7F));
         MenuKit.bookText(ctx, MenuKit.truncate(false, name, 6.5F, left - 30), x + 19 + selection, rowY + 6, 6.5F, MenuKit.fade(entry.enabled() ? MenuKit.TEXT : MenuKit.UID, alpha * row));
         hits.add(new Hit(x + 14, rowY, left - 18, 18, () -> select(entry)));
         button(ctx, x + left - 2, rowY, 14, 18, "×", mx, my, () -> {
            ServerLoginStore.get().remove(account, entry.server());
            if (entry.server().equals(selectedServer)) clearServer();
         });
      }
      button(ctx, x + 14, footer - 24, left - 2, 18, Translations.text("+ Сервер"), mx, my, this::clearServer);
      label(ctx, Translations.text("Адрес сервера (можно с портом)"), rx, y + 72);
      field(ctx, address, 0, rx, y + 82, rw, "play.example.net:25565", false, mx, my);
      label(ctx, useSharedPassword ? Translations.text("Общий пароль · ввод создаст отдельный") : Translations.text("Пароль на этом сервере"), rx, y + 103);
      field(ctx, password, 1, rx, y + 113, rw, Translations.text("Пароль"), true, mx, my);
      button(ctx, rx, y + 135, rw, 18, Translations.text("Команда: /") + command, mx, my, () -> command = command.equals("login") ? "l" : "login");
      button(ctx, rx, y + 155, rw, 18, Translations.text("Автовход и регистрация: ") + (enabled ? Translations.text("вкл") : Translations.text("выкл")), mx, my, () -> enabled = !enabled);
      if (!status.isEmpty()) {
         float show = MenuAnim.easeOutCubic(clamp((System.currentTimeMillis() - statusAt) / 220.0F));
         MenuKit.bookText(ctx, MenuKit.truncate(false, status, 6.5F, rw), rx, footer - 12 + (1 - show) * 2, 6.5F, MenuKit.fade(error ? 0xFFFF6B6B : 0xFF6FDC8C, alpha * show));
      }
      boolean saved = !status.isEmpty() && !error && System.currentTimeMillis() - statusAt < 1100;
      button(ctx, rx, footer, rw, 20, saved ? Translations.text("Сохранено ✓") : Translations.text("Сохранить сервер"), mx, my, this::saveServer);
      button(ctx, x + 14, footer, left - 2, 20, Translations.text("Готово"), mx, my, this::onClose);
      ctx.pose().popMatrix();
      if (closedAt != 0) hits.clear();
   }

   private float stage(int delay) {
      return MenuAnim.easeOutCubic(clamp((System.currentTimeMillis() - openedAt - delay) / 240.0F));
   }

   private void label(GuiGraphicsExtractor ctx, String value, int x, int y) {
      float available = (width + Math.min(width - 24, 440)) / 2F - x - 14;
      MenuKit.bookText(ctx, MenuKit.truncate(false, value, 6.5F, available), x, y, 6.5F, MenuKit.fade(MenuKit.TEXT_OFF, alpha * stage(80)));
   }

   private void field(GuiGraphicsExtractor ctx, TextEdit edit, int id, int x, int y, int w, String hint, boolean secret, int mx, int my) {
      AnimatedAccountField animation = secret ? passwordField : addressField;
      animation.draw(ctx, secret ? "*".repeat(edit.text().length()) : edit.text(), edit, hint, focused == id,
         x, y, w, 18, 7.5F, mx, my, alpha * stage(100 + id * 40), dt);
      hits.add(new Hit(x, y, w, 18, () -> focused = id));
   }

   private float hover(String key, boolean active) {
      float value = MenuKit.expLerp(hovers.getOrDefault(key, 0.0F), active ? 1 : 0, 12, dt);
      hovers.put(key, value);
      return value;
   }

   private void button(GuiGraphicsExtractor ctx, int x, int y, int w, int h, String label, int mx, int my, Runnable action) {
      float hover = hover(x + ":" + y, MenuKit.inside(mx, my, x, y, w, h));
      MenuKit.pill(ctx, x, y - hover * 0.8F, w, h, label, hover, false, alpha * stage(120));
      hits.add(new Hit(x, y, w, h, action));
   }

   private void select(ServerLoginStore.Entry entry) {
      selectedServer = entry.server();
      address.set(entry.server());
      ServerLoginStore.Entry credential = ServerLoginStore.get().resolveCredential(entry);
      password.set(credential == null ? "" : credential.password());
      useSharedPassword = entry.useSharedPassword();
      command = entry.command();
      enabled = entry.enabled();
      status = "";
      focused = -1;
   }

   private void clearServer() {
      selectedServer = null;
      address.set("");
      password.set("");
      command = "login";
      enabled = true;
      useSharedPassword = false;
      status = "";
      focused = 0;
   }

   private void saveServer() {
      try {
         if (useSharedPassword) {
            ServerLoginStore.get().putSharedServer(account, address.text(), command, enabled);
         } else {
            ServerLoginStore.get().put(account, address.text(), password.text(), command, enabled);
         }
         String saved = ServerLoginStore.normalizeAddress(address.text());
         if (selectedServer != null && !selectedServer.equals(saved)) ServerLoginStore.get().remove(account, selectedServer);
         selectedServer = saved;
         address.set(saved);
         addressField.confirm();
         passwordField.confirm();
         status = Translations.text("Сохранено · вход при запросе /") + command;
         error = false;
      } catch (IllegalArgumentException ex) {
         status = ex.getMessage();
         error = true;
      }
      statusAt = System.currentTimeMillis();
   }

   private TextEdit focusedEdit() {
      return switch (focused) {
         case 0 -> address;
         case 1 -> password;
         default -> null;
      };
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
      if (closedAt != 0) return true;
      if (e.button() == 0) {
         focused = -1;
         float mx = centerX + (float)(e.x() - centerX) / scale;
         float my = centerY + (float)(e.y() - centerY - slide) / scale;
         for (Hit hit : hits) {
            if (MenuKit.inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) {
               hit.action().run();
               return true;
            }
         }
      }
      return super.mouseClicked(e, dbl);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
      if (closedAt == 0 && mx < width / 2.0) scroll += vertical < 0 ? 1 : vertical > 0 ? -1 : 0;
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent e) {
      if (closedAt != 0) return true;
      if (e.key() == 256) {
         onClose();
      } else if (e.key() == 258) {
         focused = focused < 0 ? (e.hasShiftDown() ? 1 : 0) : Math.floorMod(focused + (e.hasShiftDown() ? -1 : 1), 2);
      } else if (e.key() == 257 || e.key() == 335) {
         saveServer();
      } else {
         TextEdit edit = focusedEdit();
         // Pasting works; copying a masked password never exposes it to the clipboard.
         if (edit != null && !(focused == 1 && (e.isCopy() || e.isCut()))) {
            String before = edit.text();
            edit.key(e);
            if (focused == 1 && !before.equals(edit.text())) useSharedPassword = false;
         }
      }
      return true;
   }

   @Override
   public boolean charTyped(CharacterEvent e) {
      if (closedAt != 0) return true;
      TextEdit edit = focusedEdit();
      if (edit != null && edit.insert(e.codepointAsString()) && focused == 1) useSharedPassword = false;
      return true;
   }

   @Override
   public void removed() {
      password.set("");
      super.removed();
   }

   @Override
   public void tick() {
      super.tick();
      if (closedAt != 0 && System.currentTimeMillis() - closedAt >= 180) Minecraft.getInstance().setScreenAndShow(parent);
   }

   @Override
   public void onClose() {
      if (closedAt == 0) {
         closedAt = System.currentTimeMillis();
         focused = -1;
      }
   }

   private static float clamp(float value) {
      return Math.max(0, Math.min(1, value));
   }

   private record Hit(int x, int y, int w, int h, Runnable action) {}
}
