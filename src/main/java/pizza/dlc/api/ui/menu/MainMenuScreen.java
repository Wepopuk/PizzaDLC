package pizza.dlc.api.ui.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.sounds.SoundEvents;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.account.SkinHeads;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.ui.account.AccountSwitcherScreen;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.mixin.client.LoadingOverlayAccessor;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

public class MainMenuScreen extends TitleScreen {
   private static final String COPYRIGHT = "Copyright Mojang AB. Do not distribute!";
   private static final int MAX_SERVERS = 6;
   private static final int CARD_H = 32;
   private static final int TILE_H = 54;
   private static final int SERVER_H = 26;
   private static final float FADE_IN = 0.45F;
   private static final float RETURN_SPEED = 1.7F;
   private static final float INTRO_SCALE = 0.88F;
   private static final float INTRO_SCALE_TIME = 0.6F;
   private static final float INTRO_BRAND_TIME = 1.1F;
   private static final long OVERLAY_OPAQUE_MS = 1000L;
   private long openedAt = System.currentTimeMillis();
   private float animSpeed = 1.0F;
   private boolean shownOnce;
   private boolean intro;
   private final List<ServerData> servers = new ArrayList<>();
   private final List<Hit> hits = new ArrayList<>();
   private final float[] navHover = new float[Nav.values().length];
   private final float[] tileHover = new float[2];
   private float[] serverHover = new float[0];
   private float cardHover;
   private float allHover;
   private int serverTotal;
   private long lastNs;
   private float dt;

   public MainMenuScreen() {
      super(false);
   }

   @Override
   public void added() {
      super.added();
      this.animSpeed = this.shownOnce ? RETURN_SPEED : 1.0F;
      this.intro = !this.shownOnce;
      this.shownOnce = true;
      this.openedAt = System.currentTimeMillis();
      this.lastNs = 0L;
      this.loadServers();
   }

   @Override
   protected void init() {
      super.init();
      if (this.servers.isEmpty()) {
         this.loadServers();
      }
   }

   private void loadServers() {
      this.servers.clear();

      try {
         ServerList list = new ServerList(this.minecraft);
         list.load();
         this.serverTotal = list.size();

         for (int i = 0; i < Math.min(list.size(), MAX_SERVERS); i++) {
            this.servers.add(list.get(i));
         }
      } catch (Throwable ignored) {
         this.serverTotal = 0;
      }

      this.serverHover = new float[this.servers.size()];
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      if (CleanRenderScope.isActive()) super.extractBackground(g, mouseX, mouseY, partialTick);
      else MenuKit.background(g, this.width, this.height);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
      if (CleanRenderScope.isActive()) {
         super.extractRenderState(g, mouseX, mouseY, partialTick);
         return;
      }
      long now = System.nanoTime();
      this.dt = this.lastNs == 0L ? 0.016F : Math.min(0.1F, (float)(now - this.lastNs) / 1.0E9F);
      this.lastNs = now;
      if (this.loadingInProgress()) {
         this.openedAt = System.currentTimeMillis();
      }

      float t = Math.min(1.0F, this.age() / FADE_IN);
      float a = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
      Layout l = this.layout();
      this.hits.clear();
      float cx = l.winX + l.winW / 2.0F;
      float cy = l.winY + l.winH / 2.0F;
      float scale;
      if (this.intro) {
         float st = Math.min(1.0F, this.age() / INTRO_SCALE_TIME);
         scale = INTRO_SCALE + (1.0F - INTRO_SCALE) * MenuAnim.easeOutBack(st);
      } else {
         scale = 0.97F + 0.03F * a;
      }

      g.pose().pushMatrix();
      g.pose().translate(cx, cy);
      g.pose().scale(scale, scale);
      g.pose().translate(-cx, -cy);
      MenuKit.window(g, l.winX, l.winY, l.winW, l.winH, a);
      MenuKit.brandIntro(g, l.winX, l.winY, "v1.0", a, this.intro ? Math.min(1.0F, this.age() / INTRO_BRAND_TIME) : 1.0F);

      for (int i = 0; i < l.rows.size(); i++) {
         Nav n = l.rows.get(i);
         int ry = l.rowsY + i * l.rowStep;
         float e = this.stagger(0.08F + i * 0.045F);
         boolean hov = MenuKit.inside(mouseX, mouseY, l.sideX, ry, l.sideW, l.rowH);
         this.navHover[n.ordinal()] = MenuKit.expLerp(this.navHover[n.ordinal()], hov ? 1.0F : 0.0F, 13.0F, this.dt);
         g.pose().pushMatrix();
         g.pose().translate((1.0F - e) * -12.0F, 0.0F);
         MenuKit.sidebarRow(g, l.sideX, ry, l.sideW, l.rowH, this.icon(n), Translations.text(n.label), n == Nav.HOME ? 1.0F : 0.0F, this.navHover[n.ordinal()], a * e);
         g.pose().popMatrix();
         this.hits.add(new Hit(l.sideX, ry, l.sideW, l.rowH, () -> this.navigate(n)));
      }

      boolean cardHov = MenuKit.inside(mouseX, mouseY, l.sideX, l.cardY, l.sideW, CARD_H);
      this.cardHover = MenuKit.expLerp(this.cardHover, cardHov ? 1.0F : 0.0F, 13.0F, this.dt);
      float ce = this.stagger(0.3F);
      g.pose().pushMatrix();
      g.pose().translate(0.0F, (1.0F - ce) * 8.0F);
      MenuKit.profileCard(g, l.sideX, l.cardY, l.sideW, CARD_H, this.skin(), this.nick(), Translations.text("Сменить аккаунт"), this.cardHover * 0.65F, a * ce);
      g.pose().popMatrix();
      this.hits.add(new Hit(l.sideX, l.cardY, l.sideW, CARD_H, () -> this.navigate(Nav.ACCOUNTS)));
      if (!l.compact) {
         Render2DUtil.rect(g, l.sideX + l.sideW + 6, l.winY + 12, 1, l.winH - 24, MenuKit.fade(MenuKit.DIVIDER, a));
         this.drawDashboard(g, l, mouseX, mouseY, a);
      }

      g.pose().popMatrix();
      float footY = this.height - 6.0F - 4.9F;
      MenuKit.bookText(g, "Minecraft " + SharedConstants.getCurrentVersion().name(), 6.0F, footY, 7.0F, MenuKit.fade(MenuKit.TEXT_OFF, a));
      float copyW = MenuKit.bookW(COPYRIGHT, 7.0F);
      MenuKit.bookText(g, COPYRIGHT, this.width - 6.0F - copyW, footY, 7.0F, MenuKit.fade(MenuKit.TEXT_OFF, a));
   }

   private void drawDashboard(GuiGraphicsExtractor g, Layout l, int mx, int my, float a) {
      int x = l.contentX;
      int w = l.contentW;
      int y = l.winY + 14;
      float we = this.stagger(0.1F);
      float wa = a * we;
      float wy = (1.0F - we) * 6.0F;
      MenuKit.regText(g, Translations.text("Добро пожаловать,"), x, y + wy, 8.0F, MenuKit.fade(MenuKit.UID, wa));
      MenuKit.regText(g, MenuKit.truncate(true, this.nick(), 16.0F, w), x, y + 12 + wy, 16.0F, MenuKit.fade(MenuKit.TEXT, wa));
      String sub = "Minecraft " + SharedConstants.getCurrentVersion().name() + "  ·  PizzaDLC v1.0";
      MenuKit.bookText(g, MenuKit.truncate(false, sub, 7.0F, w), x, y + 33 + wy, 7.0F, MenuKit.fade(MenuKit.UID, wa));
      int ty = y + 48;
      int tw = (w - 8) / 2;
      String serversDesc = this.serverTotal == 0 ? Translations.text("Сервера и быстрый вход") : Translations.tr("menu.servers_saved", this.serverTotal);
      this.tile(g, 0, x, ty, tw, MenuKit.Icon.texture("player"), Translations.text("Одиночная игра"), Translations.text("Миры и сохранения"), mx, my, a, () -> this.navigate(Nav.SINGLE));
      this.tile(g, 1, x + tw + 8, ty, w - tw - 8, MenuKit.Icon.texture("combat"), Translations.text("Сетевая игра"), serversDesc, mx, my, a, () -> this.navigate(Nav.MULTI));
      int sy = ty + TILE_H + 14;
      float se = this.stagger(0.28F);
      String all = Translations.text("Все сервера");
      float allW = MenuKit.regW(all, 7.0F);
      boolean allHov = MenuKit.inside(mx, my, x + w - allW - 2, sy - 3, allW + 4, 11.0F);
      this.allHover = MenuKit.expLerp(this.allHover, allHov ? 1.0F : 0.0F, 13.0F, this.dt);
      MenuKit.section(g, Translations.text("СЕРВЕРЫ"), x, sy, w - allW - 10, a * se);
      MenuKit.regText(g, all, x + w - allW, sy - 0.5F, 7.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.TEXT_OFF, MenuKit.TEXT, this.allHover), a * se));
      this.hits.add(new Hit(x + w - allW - 2, sy - 3, allW + 4, 11.0F, () -> this.navigate(Nav.MULTI)));
      int rowY = sy + 11;
      int bottom = l.winY + l.winH - 12;
      if (this.servers.isEmpty()) {
         MenuKit.bookText(g, Translations.text("Серверов пока нет — добавь их в сетевой игре"), x, rowY + 6, 7.5F, MenuKit.fade(MenuKit.UID, a * se));
      } else {
         for (int i = 0; i < this.servers.size() && rowY + SERVER_H <= bottom; i++) {
            float re = this.stagger(0.32F + i * 0.05F);
            g.pose().pushMatrix();
            g.pose().translate(0.0F, (1.0F - re) * 8.0F);
            this.serverRow(g, i, this.servers.get(i), x, rowY, w, mx, my, a * re);
            g.pose().popMatrix();
            rowY += SERVER_H + 4;
         }
      }
   }

   private boolean loadingInProgress() {
      if (!(this.minecraft.gui.overlay() instanceof LoadingOverlay overlay)) return false;
      long fadeOutStart = ((LoadingOverlayAccessor)overlay).pizzadlc$getFadeOutStart();
      return fadeOutStart == -1L || Util.getMillis() - fadeOutStart < OVERLAY_OPAQUE_MS;
   }

   private float age() {
      return (System.currentTimeMillis() - this.openedAt) / 1000.0F * this.animSpeed;
   }

   private float stagger(float delay) {
      float t = (this.age() - delay) / 0.35F;
      t = Math.max(0.0F, Math.min(1.0F, t));
      return MenuAnim.easeOutCubic(t);
   }

   private void tile(GuiGraphicsExtractor g, int idx, int x, int y, int w, MenuKit.Icon icon, String title, String desc, int mx, int my, float a, Runnable action) {
      boolean hov = MenuKit.inside(mx, my, x, y, w, TILE_H);
      this.tileHover[idx] = MenuKit.expLerp(this.tileHover[idx], hov ? 1.0F : 0.0F, 13.0F, this.dt);
      float t = this.tileHover[idx];
      float e = this.stagger(0.18F + idx * 0.07F);
      a *= e;
      this.hits.add(new Hit(x, y, w, TILE_H, action));
      y = Math.round(y + (1.0F - e) * 10.0F - t * 1.5F);
      Render2DUtil.softShadow(g, x, y, w, TILE_H, 10.0F, 3.0F + 5.0F * t, a);
      Render2DUtil.roundedRect(g, x, y, w, TILE_H, 10.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.MOD, 0xFF1D1C1C, t), a));
      Render2DUtil.roundedOutline(g, x, y, w, TILE_H, 10.0F, 0.6F, MenuKit.fade(Render2DUtil.lerpColor(0x16FFFFFF, 0x30FFFFFF, t), a));
      if (t > 0.01F) {
         Render2DUtil.roundedOutline(g, x, y, w, TILE_H, 10.0F, 0.8F, Accent.color(a * t * 0.55F));
      }
      int box = 28;
      int bx = x + 12;
      int by = y + (TILE_H - box) / 2;
      Render2DUtil.roundedRect(g, bx, by, box, box, 8.0F, MenuKit.fade(MenuKit.MODE_SEL, a));
      Render2DUtil.gradientRectDiag(g, bx, by, box, box, Accent.color(a * (0.22F + 0.6F * t)), Accent.secondary(a * (0.12F + 0.5F * t)), 8.0F);
      icon.draw(g, bx + box / 2.0F, by + box / 2.0F, 14.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.ICON_IDLE, 0xFFFFFFFF, Math.max(0.35F, t)), a));
      float tx = bx + box + 10;
      float maxW = x + w - 22 - tx;
      MenuKit.regText(g, MenuKit.truncate(true, title, 9.5F, maxW), tx, y + TILE_H / 2.0F - 8.0F, 9.5F, MenuKit.fade(MenuKit.TEXT, a));
      MenuKit.bookText(g, MenuKit.truncate(false, desc, 7.0F, maxW), tx, y + TILE_H / 2.0F + 3.0F, 7.0F, MenuKit.fade(MenuKit.UID, a));
      if (t > 0.01F) {
         g.pose().pushMatrix();
         g.pose().translate(x + w - 13 + (1.0F - t) * -3.0F, y + TILE_H / 2.0F);
         g.pose().rotate((float)(-Math.PI / 2));
         Render2DUtil.chevron(g, -3, -2, 6, 4, false, 1.3F, Accent.color(t * a));
         g.pose().popMatrix();
      }
   }

   private void serverRow(GuiGraphicsExtractor g, int i, ServerData s, int x, int y, int w, int mx, int my, float a) {
      boolean hov = MenuKit.inside(mx, my, x, y, w, SERVER_H);
      this.serverHover[i] = MenuKit.expLerp(this.serverHover[i], hov ? 1.0F : 0.0F, 13.0F, this.dt);
      float t = this.serverHover[i];
      Render2DUtil.softShadow(g, x, y, w, SERVER_H, 6.0F, 2.0F + 3.0F * t, a * 0.85F);
      Render2DUtil.roundedRect(g, x, y, w, SERVER_H, 6.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.MOD, 0xFF1D1C1C, t), a));
      Render2DUtil.roundedOutline(g, x, y, w, SERVER_H, 6.0F, 0.5F, MenuKit.fade(Render2DUtil.lerpColor(0x12FFFFFF, 0x40FFFFFF, t), a));
      int av = 16;
      int ax = x + 6;
      int ay = y + (SERVER_H - av) / 2;
      Render2DUtil.gradientRectDiag(g, ax, ay, av, av, MenuKit.fade(Render2DUtil.lerpColor(0xFF3A3A3C, Accent.color(1.0F), 0.25F + 0.5F * t), a), MenuKit.fade(Render2DUtil.lerpColor(0xFF2A2A2C, Accent.secondary(1.0F), 0.15F + 0.4F * t), a), av / 2.0F);
      String name = s.name == null || s.name.isBlank() ? s.ip : s.name;
      String ini = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
      float iniW = MenuKit.regW(ini, 7.0F);
      MenuKit.regText(g, ini, ax + (av - iniW) / 2.0F, ay + (av - 4.9F) / 2.0F, 7.0F, MenuKit.fade(MenuKit.TEXT, a));
      float pillW = 42.0F;
      float pillX = x + w - 6 - pillW;
      float textX = ax + av + 7;
      float maxW = pillX - 6 - textX;
      MenuKit.regText(g, MenuKit.truncate(true, name == null ? "" : name, 8.5F, maxW), textX, y + 5.5F, 8.5F, MenuKit.fade(MenuKit.TEXT, a));
      MenuKit.bookText(g, MenuKit.truncate(false, s.ip == null ? "" : s.ip, 6.5F, maxW), textX, y + 15.5F, 6.5F, MenuKit.fade(MenuKit.UID, a));
      MenuKit.pill(g, pillX, y + (SERVER_H - 14) / 2.0F, pillW, 14.0F, Translations.text("Играть"), t, true, a);
      this.hits.add(new Hit(x, y, w, SERVER_H, () -> this.join(s)));
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
      if (StreamProofRenderer.isVanillaGameplayActive()) return super.mouseClicked(e, doubleClick);
      if (e.button() == 0) {
         for (Hit h : this.hits) {
            if (!MenuKit.inside(e.x(), e.y(), h.x, h.y, h.w, h.h)) continue;

            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            h.action.run();
            return true;
         }
      }

      return super.mouseClicked(e, doubleClick);
   }

   private void navigate(Nav n) {
      switch (n) {
         case SINGLE -> this.minecraft.gui.setScreen(new SelectWorldScreen(this));
         case MULTI -> {
            if (this.multiplayerAllowed()) {
               this.minecraft.gui.setScreen(this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this));
            }
         }
         case ACCOUNTS -> this.minecraft.gui.setScreen(new AccountSwitcherScreen(this));
         case SETTINGS -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options, false));
         case QUIT -> this.minecraft.stop();
         default -> {
         }
      }
   }

   private void join(ServerData s) {
      if (!this.multiplayerAllowed() || s.ip == null) return;

      ConnectScreen.startConnecting(new JoinMultiplayerScreen(this), this.minecraft, ServerAddress.parseString(s.ip), s, false, null);
   }

   private boolean multiplayerAllowed() {
      if (this.minecraft.allowsMultiplayer() && !this.minecraft.isNameBanned()) {
         return true;
      }

      NotificationManager.add(Translations.text("Сетевая игра"), Translations.text("Недоступна для этого аккаунта"), NotificationType.ERROR, 2500L);
      return false;
   }

   private MenuKit.Icon icon(Nav n) {
      return switch (n) {
         case HOME -> MenuKit.Icon.HOME;
         case SINGLE -> MenuKit.Icon.texture("player");
         case MULTI -> MenuKit.Icon.texture("combat");
         case ACCOUNTS -> MenuKit.Icon.face(this.skin());
         case SETTINGS -> MenuKit.Icon.texture("other");
         case QUIT -> MenuKit.Icon.EXIT;
      };
   }

   private String nick() {
      return this.minecraft.getUser().getName();
   }

   private Identifier skin() {
      return SkinHeads.current();
   }

   private Layout layout() {
      int winW = Math.min(this.width - 24, 560);
      int winH = Math.min(this.height - 36, 290);
      int winX = (this.width - winW) / 2;
      int winY = (this.height - winH) / 2 - 4;
      boolean compact = winW < 400;
      int sideX = winX + 13;
      int sideW = compact ? winW - 26 : 128;
      int cardY = winY + winH - 7 - CARD_H - 4;
      int rowsY = winY + MenuKit.HEADER_H;
      List<Nav> rows = new ArrayList<>(List.of(Nav.values()));
      int avail = cardY - 6 - rowsY;
      if (compact || avail / rows.size() < 22) {
         rows.remove(Nav.HOME);
      }

      int step = Math.max(14, Math.min(28, avail / rows.size()));
      int rowH = Math.min(24, step - 3);
      int contentX = sideX + sideW + 14;
      int contentW = winX + winW - 14 - contentX;
      return new Layout(winX, winY, winW, winH, compact, sideX, sideW, rowsY, step, rowH, cardY, contentX, contentW, rows);
   }

   private record Layout(
      int winX,
      int winY,
      int winW,
      int winH,
      boolean compact,
      int sideX,
      int sideW,
      int rowsY,
      int rowStep,
      int rowH,
      int cardY,
      int contentX,
      int contentW,
      List<Nav> rows
   ) {
   }

   private record Hit(float x, float y, float w, float h, Runnable action) {
   }

   private static enum Nav {
      HOME("Главная"),
      SINGLE("Одиночная игра"),
      MULTI("Сетевая игра"),
      ACCOUNTS("Аккаунты"),
      SETTINGS("Настройки"),
      QUIT("Выход");

      final String label;

      private Nav(String label) {
         this.label = label;
      }
   }
}
