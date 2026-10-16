package pizza.dlc.api.ui.account;

import pizza.dlc.api.translation.Translations;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.ui.TextEdit;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.account.AccountSwitcher;
import pizza.dlc.api.account.MicrosoftAccountStore;
import pizza.dlc.api.account.MicrosoftAuth;
import pizza.dlc.api.account.ServerLoginStore;
import pizza.dlc.api.account.SkinHeads;
import pizza.dlc.api.filemanager.impl.AltAccountManager;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.ui.MenuBackdropScreen;
import pizza.dlc.api.ui.TypingScreen;
import pizza.dlc.api.ui.menu.MenuAnim;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.util.render.Render2DUtil;

public class AccountSwitcherScreen extends Screen implements MenuBackdropScreen, TypingScreen {
   private static final int SIDE_W = 160;
   private static final int ROW_H = 44;
   private static final int ROW_GAP = 4;
   private static final int HEAD = 24;
   private static final int CURRENT_H = 64;
   private static final int FIELD_H = 18;
   private static final int BACK_H = 24;
   private static final float FADE_H = 22.0F;
   private static final int CTRL_MOD = 2;
   private static final int KEY_F = 70;
   private static final int KEY_C = 67;
   private static final int KEY_R = 82;
   private static final long ROLL_MS = 320L;
   private static final String ROLL_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
   private static final int MS_H = 20;
   private static final int MODAL_W = 252;
   private static final int MODAL_H = 158;
   private static final int BAD = 0xFFFF6B6B;
   private static final int GOOD = 0xFF6FDC8C;
   private final Screen parent;
   private final Map<String, Float> rowHover = new HashMap<>();
   private final Map<String, Float> rowActive = new HashMap<>();
   private final Map<String, Float> modalHover = new HashMap<>();
   private final List<Hit> hits = new ArrayList<>();
   private float msHover;
   private MsStep msStep = MsStep.NONE;
   private long msStepMs;
   private float msOpen;
   private MicrosoftAuth.DeviceCode msCode;
   private String msError;
   private long msCopiedMs;
   private volatile int msFlow;
   private volatile boolean msCancelled;
   private String pendingLogin;
   private float scrollY;
   private float targetScrollY;
   private float maxScroll;
   private float frameDt;
   private long animLastNanos;
   private long openMs;
   private static final float SWITCH_TIME = 0.75F;
   private static final float ROW_FLASH_TIME = 0.6F;
   private String input = "";
   private final TextEdit inputEdit = new TextEdit(16, c -> validChar((char)c));
   private final TextEdit searchEdit = new TextEdit(256, c -> true);
   private boolean inputFocused;
   private String search = "";
   private String shownUser;
   private Identifier shownSkin;
   private String prevUser;
   private Identifier prevSkin;
   private float switchAge = 10.0F;
   private final Map<String, Float> rowSlot = new HashMap<>();
   private final Map<String, Float> rowAge = new HashMap<>();
   private final Map<String, Float> rowFlash = new HashMap<>();
   private String lastActiveName;
   private boolean searchFocused;
   private float searchFocus;
   private boolean caretVisible = true;
   private long caretToggleMs;
   private float backHover;
   private float addHover;
   private float diceHover;
   private long rollAt;
   private String rolledName;
   private float fieldFocus;
   private final TextEdit noteEdit = new TextEdit(256, c -> true);
   private final Map<String, AnimatedAccountField> noteFields = new HashMap<>();
   private String editingNote;
   private final TextEdit sharedPasswordEdit = new TextEdit(120, c -> !Character.isWhitespace(c) && !Character.isISOControl(c) && c != 167);
   private final Map<String, AnimatedAccountField> passwordFields = new HashMap<>();
   private String editingPassword;

   public AccountSwitcherScreen(Screen parent) {
      super(Component.literal(Translations.text("Alt Manager")));
      this.parent = parent;
   }

   @Override
   public boolean isTyping() {
      return this.inputFocused || this.searchFocused || this.editingNote != null || this.editingPassword != null;
   }

   private AltAccountManager alts() {
      return PizzaDLC.getInstance().getAltAccountManager();
   }

   @Override
   protected void init() {
      this.openMs = System.currentTimeMillis();
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      MenuKit.background(ctx, this.width, this.height);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private Layout layout() {
      int winW = Math.min(this.width - 24, 560);
      int winH = Math.min(this.height - 36, 300);
      int winX = (this.width - winW) / 2;
      int winY = (this.height - winH) / 2;
      int sideX = winX + 13;
      int sideW = Math.min(SIDE_W, winW / 3);
      int listX = sideX + sideW + 14;
      int listW = winX + winW - 14 - listX;
      int listTop = winY + 40;
      int listBottom = winY + winH - 12;
      return new Layout(winX, winY, winW, winH, sideX, sideW, listX, listW, listTop, listBottom);
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      long nowNs = System.nanoTime();
      this.frameDt = this.animLastNanos == 0L ? 0.016F : Math.min(0.1F, (float)(nowNs - this.animLastNanos) / 1.0E9F);
      this.animLastNanos = nowNs;
      float t = clamp01((System.currentTimeMillis() - this.openMs) / 450.0F);
      float a = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
      Layout l = this.layout();
      this.hits.clear();
      List<AltAccountManager.OfflineAlt> list = this.filtered();
      AltAccountManager.OfflineAlt active = this.alts().getSelected();
      boolean modal = this.msStep != MsStep.NONE;
      this.msOpen = MenuKit.expLerp(this.msOpen, modal ? 1.0F : 0.0F, 14.0F, this.frameDt);
      int umx = this.msOpen > 0.01F ? -10000 : mouseX;
      int umy = this.msOpen > 0.01F ? -10000 : mouseY;
      float scale = t >= 1.0F ? 1.0F : 0.95F + 0.05F * MenuAnim.easeOutBack(t);
      float cx = l.winX + l.winW / 2.0F;
      float cy = l.winY + l.winH / 2.0F;
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().scale(scale, scale);
      ctx.pose().translate(-cx, -cy);
      MenuKit.window(ctx, l.winX, l.winY, l.winW, l.winH, a);
      MenuKit.brand(ctx, l.winX, l.winY, Translations.text("Аккаунты"), a);
      this.drawSide(ctx, l, list, active, umx, umy, a);
      Render2DUtil.rect(ctx, l.sideX + l.sideW + 6, l.winY + 12, 1, l.winH - 24, MenuKit.fade(MenuKit.DIVIDER, a));
      this.drawList(ctx, l, list, active, umx, umy, a);
      ctx.pose().popMatrix();
      if (this.msOpen > 0.01F) {
         this.hits.clear();
         this.drawModal(ctx, l, mouseX, mouseY, a);
      }
   }

   private void drawSide(GuiGraphicsExtractor ctx, Layout l, List<AltAccountManager.OfflineAlt> list, AltAccountManager.OfflineAlt active, int mx, int my, float a) {
      int x = l.sideX;
      int w = l.sideW;
      int y = l.winY + MenuKit.HEADER_H;
      Minecraft mc = Minecraft.getInstance();
      String current = mc.getUser().getName();
      Identifier skin = SkinHeads.current();
      if (this.shownUser == null) {
         this.shownUser = current;
         this.shownSkin = skin;
      } else if (!this.shownUser.equals(current)) {
         this.prevUser = this.shownUser;
         this.prevSkin = this.shownSkin;
         this.shownUser = current;
         this.switchAge = 0.0F;
      }
      this.shownSkin = skin;
      this.switchAge = Math.min(10.0F, this.switchAge + this.frameDt);
      float sw = clamp01(this.switchAge / SWITCH_TIME);
      boolean switching = sw < 1.0F && this.prevUser != null;
      Render2DUtil.softShadow(ctx, x, y, w, CURRENT_H, 10.0F, 4.0F, a);
      Render2DUtil.roundedRect(ctx, x, y, w, CURRENT_H, 10.0F, MenuKit.fade(MenuKit.USERCARD, a));
      if (switching) {
         float flash = 1.0F - sw;
         Render2DUtil.gradientRectFull(ctx, x, y, w, CURRENT_H, Accent.color(0.22F * flash * a), Accent.secondary(0.04F * flash * a), 10.0F);
         int bandHalf = Math.max(1, Math.round(w * 0.2F));
         int bandX = Math.round(x - bandHalf * 2 + (w + bandHalf * 2) * easeOutCubic(sw));
         int shine = Render2DUtil.lerpColor(0xFFFFFFFF, Accent.color(1.0F), 0.4F) & 0xFFFFFF;
         int peak = shine | Math.round(40.0F * flash * a) << 24;
         ctx.enableScissor(x, y, x + w, y + CURRENT_H);
         Render2DUtil.gradientRectFull(ctx, bandX, y, bandHalf, CURRENT_H, shine, peak, 0.0F);
         Render2DUtil.gradientRectFull(ctx, bandX + bandHalf, y, bandHalf, CURRENT_H, peak, shine, 0.0F);
         ctx.disableScissor();
      }
      Render2DUtil.roundedOutline(ctx, x, y, w, CURRENT_H, 10.0F, 0.6F,
         MenuKit.fade(switching ? Render2DUtil.lerpColor(0x24FFFFFF, Accent.color(0.8F), 1.0F - sw) : 0x24FFFFFF, a));
      int head = 34;
      int hx = x + 8;
      int hy = y + (CURRENT_H - head) / 2;
      float hcx = hx + head / 2.0F;
      float hcy = hy + head / 2.0F;
      if (switching && sw < 0.6F) {
         float p = sw / 0.6F;
         float grow = easeOutCubic(p) * 10.0F;
         int ringSize = Math.round(head + grow * 2.0F);
         Render2DUtil.roundedOutline(ctx, Math.round(hx - grow), Math.round(hy - grow), ringSize, ringSize, 6.0F + grow, 1.2F,
            Accent.color((1.0F - p) * (1.0F - p) * a));
      }
      if (this.pendingLogin != null) {
         float spin = (System.nanoTime() / 1.0E9F * 1.4F) % 1.0F;
         Render2DUtil.arcGradient(ctx, hx - 5, hy - 5, head + 10, head + 10, 0.43F, 0.05F, 110.0F, Accent.color(a), Accent.secondary(0.0F), spin);
      }
      float flip = switching ? (float)Math.cos(Math.min(1.0F, sw / 0.55F) * Math.PI) : 1.0F;
      boolean showPrev = switching && flip > 0.0F && sw < 0.275F;
      Identifier faceSkin = showPrev && this.prevSkin != null ? this.prevSkin : skin;
      float flipScale = Math.max(0.04F, Math.abs(flip));
      float pop = switching && !showPrev ? 1.0F + 0.12F * (float)Math.sin(clamp01((sw - 0.275F) / 0.45F) * Math.PI) : 1.0F;
      ctx.pose().pushMatrix();
      ctx.pose().translate(hcx, hcy);
      ctx.pose().scale(flipScale * pop, pop);
      ctx.pose().translate(-hcx, -hcy);
      Render2DUtil.roundedFace(ctx, faceSkin, hx, hy, head, 6, 8, 8, MenuKit.fade(-1, a));
      Render2DUtil.roundedFace(ctx, faceSkin, hx, hy, head, 6, 40, 8, MenuKit.fade(-1, a));
      ctx.pose().popMatrix();
      float tx = hx + head + 8;
      float maxW = x + w - 8 - tx;
      MenuKit.regText(ctx, Translations.text("ТЕКУЩИЙ АККАУНТ"), tx, y + 14, 6.0F, MenuKit.fade(MenuKit.TEXT_OFF, a));
      if (switching) {
         float outT = clamp01(sw / 0.45F);
         float inT = easeOutCubic(clamp01((sw - 0.2F) / 0.6F));
         ctx.enableScissor(Math.round(tx), y, x + w - 4, y + CURRENT_H);
         MenuKit.regText(ctx, MenuKit.truncate(true, this.prevUser, 11.0F, maxW), tx - outT * 24.0F, y + 25, 11.0F, MenuKit.fade(MenuKit.TEXT, a * (1.0F - outT)));
         MenuKit.regText(ctx, MenuKit.truncate(true, current, 11.0F, maxW), tx + (1.0F - inT) * 24.0F, y + 25, 11.0F, MenuKit.fade(MenuKit.TEXT, a * inT));
         ctx.disableScissor();
      } else {
         MenuKit.regText(ctx, MenuKit.truncate(true, current, 11.0F, maxW), tx, y + 25, 11.0F, MenuKit.fade(MenuKit.TEXT, a));
      }
      boolean premium = !mc.getUser().getProfileId().equals(UUIDUtil.createOfflinePlayerUUID(current));
      float badgeIn = switching ? easeOutCubic(clamp01((sw - 0.35F) / 0.5F)) : 1.0F;
      this.badge(ctx, tx, y + 41 + (1.0F - badgeIn) * 4.0F, this.pendingLogin != null ? Translations.text("Вход…") : (premium ? Translations.text("Лицензия") : Translations.text("Оффлайн")),
         this.pendingLogin != null ? Accent.color(1.0F) : (premium ? GOOD : MenuKit.UID), a * badgeIn);
      int fy = y + CURRENT_H + 16;
      MenuKit.section(ctx, Translations.text("ДОБАВИТЬ"), x, fy, w, a);
      int fieldY = fy + 10;
      int backY = l.winY + l.winH - 7 - BACK_H - 4;
      int msY = fieldY + FIELD_H + 18;
      boolean roomy = msY + MS_H <= backY - 6;
      int addW = 22;
      int fieldW = w - (addW + 4) * 2 - (roomy ? 0 : addW + 4);
      this.drawInput(ctx, x, fieldY, fieldW, mx, my, a);
      int diceX = x + fieldW + 4;
      boolean diceHov = MenuKit.inside(mx, my, diceX, fieldY, addW, FIELD_H);
      this.diceHover = MenuKit.expLerp(this.diceHover, diceHov ? 1.0F : 0.0F, 13.0F, this.frameDt);
      MenuKit.pill(ctx, diceX, fieldY, addW, FIELD_H, "", this.diceHover, false, a);
      this.drawDice(ctx, diceX + addW / 2.0F, fieldY + FIELD_H / 2.0F, a);
      this.hits.add(new Hit(diceX, fieldY, addW, FIELD_H, this::rollNick));
      int addX = diceX + addW + 4;
      boolean addHov = MenuKit.inside(mx, my, addX, fieldY, addW, FIELD_H);
      this.addHover = MenuKit.expLerp(this.addHover, addHov ? 1.0F : 0.0F, 13.0F, this.frameDt);
      MenuKit.pill(ctx, addX, fieldY, addW, FIELD_H, "+", this.addHover, true, a);
      this.hits.add(new Hit(addX, fieldY, addW, FIELD_H, this::addFromInput));
      MenuKit.bookText(ctx, MenuKit.truncate(false, Translations.text("3–16 символов: A–Z, 0–9, _ · кубик или Ctrl+R — случайный акк"), 6.5F, w - 2), x + 1, fieldY + FIELD_H + 6, 6.5F, MenuKit.fade(MenuKit.UID, a));
      int msX = roomy ? x : addX + addW + 4;
      int msBY = roomy ? msY : fieldY;
      int msW = roomy ? w : addW;
      int msH = roomy ? MS_H : FIELD_H;
      boolean msHov = MenuKit.inside(mx, my, msX, msBY, msW, msH);
      this.msHover = MenuKit.expLerp(this.msHover, msHov ? 1.0F : 0.0F, 13.0F, this.frameDt);
      this.drawMsButton(ctx, msX, msBY, msW, msH, this.msHover, a);
      this.hits.add(new Hit(msX, msBY, msW, msH, this::startMicrosoft));
      if (roomy && msBY + msH + 18 < backY) {
         MenuKit.bookText(ctx, MenuKit.truncate(false, Translations.text("Пароль у ника — общий для серверов"), 6.5F, w - 2), x + 1, msBY + msH + 9, 6.5F, MenuKit.fade(MenuKit.UID, a));
         if (msBY + msH + 29 < backY) {
            MenuKit.bookText(ctx, MenuKit.truncate(false, Translations.text("Под ником — заметка · … — серверы"), 6.5F, w - 2), x + 1, msBY + msH + 20, 6.5F, MenuKit.fade(MenuKit.UID, a));
         }
      }
      boolean backHov = MenuKit.inside(mx, my, x, backY, w, BACK_H);
      this.backHover = MenuKit.expLerp(this.backHover, backHov ? 1.0F : 0.0F, 13.0F, this.frameDt);
      MenuKit.sidebarRow(ctx, x, backY, w, BACK_H, MenuKit.Icon.HOME, Translations.text("В меню"), 0.0F, this.backHover, a);
      this.hits.add(new Hit(x, backY, w, BACK_H, this::onClose));
   }

   private void drawMsButton(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float hover, float a) {
      Render2DUtil.roundedRect(ctx, x, y, w, h, 4.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.USERCARD, MenuKit.MODE_SEL, hover), a));
      Render2DUtil.roundedOutline(ctx, x, y, w, h, 4.0F, 0.6F, MenuKit.fade(Render2DUtil.lerpColor(0x24FFFFFF, 0x5CFFFFFF, hover), a));
      float logo = 8.0F;
      String label = MenuKit.bookW(Translations.text("Войти через Microsoft"), 7.5F) + logo + 20.0F <= w ? Translations.text("Войти через Microsoft") : (w >= 70 ? "Microsoft" : null);
      if (label == null) {
         msLogo(ctx, x + (w - logo) / 2.0F, y + (h - logo) / 2.0F, logo, a);
      } else {
         float lw = MenuKit.bookW(label, 7.5F);
         float lx = x + (w - (logo + 6.0F + lw)) / 2.0F;
         msLogo(ctx, lx, y + (h - logo) / 2.0F, logo, a);
         MenuKit.bookText(ctx, label, lx + logo + 6.0F, y + (h - 5.25F) / 2.0F, 7.5F, MenuKit.fade(MenuKit.TEXT, a));
      }
   }

   private static void msLogo(GuiGraphicsExtractor ctx, float x, float y, float s, float a) {
      float gap = Math.max(0.6F, s * 0.08F);
      float q = (s - gap) / 2.0F;
      Render2DUtil.roundedRect(ctx, x, y, q, q, 0.4F, MenuKit.fade(0xFFF25022, a));
      Render2DUtil.roundedRect(ctx, x + q + gap, y, q, q, 0.4F, MenuKit.fade(0xFF7FBA00, a));
      Render2DUtil.roundedRect(ctx, x, y + q + gap, q, q, 0.4F, MenuKit.fade(0xFF00A4EF, a));
      Render2DUtil.roundedRect(ctx, x + q + gap, y + q + gap, q, q, 0.4F, MenuKit.fade(0xFFFFB900, a));
   }

   private void drawModal(GuiGraphicsExtractor ctx, Layout l, int mx, int my, float a) {
      float o = this.msOpen;
      float ease = MenuAnim.easeOutCubic(clamp01(o));
      boolean live = this.msStep != MsStep.NONE;
      Render2DUtil.roundedRect(ctx, l.winX, l.winY, l.winW, l.winH, MenuKit.WIN_RADIUS, MenuKit.fade(0xA6050505, a * o));
      int mw = Math.min(MODAL_W, l.winW - 30);
      int mh = MODAL_H;
      float cx = l.winX + l.winW / 2.0F;
      float cy = l.winY + l.winH / 2.0F;
      int x = Math.round(cx - mw / 2.0F);
      int y = Math.round(cy - mh / 2.0F);
      float s = 0.93F + 0.07F * ease;
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy + (1.0F - ease) * 6.0F);
      ctx.pose().scale(s, s);
      ctx.pose().translate(-cx, -cy);
      float ma = a * o;
      MenuKit.window(ctx, x, y, mw, mh, ma);
      msLogo(ctx, x + 14, y + 13, 10.0F, ma);
      MenuKit.regText(ctx, Translations.text("Вход через Microsoft"), x + 30, y + 14.5F, 10.0F, MenuKit.fade(MenuKit.TEXT, ma));
      int closeX = x + mw - 24;
      boolean closeHov = live && MenuKit.inside(mx, my, closeX, y + 8, 16, 16);
      cross(ctx, closeX + 8.0F, y + 16.0F, 7.0F, MenuKit.fade(closeHov ? -1 : MenuKit.TEXT_OFF, ma));
      if (live) {
         this.hits.add(new Hit(closeX, y + 8, 16, 16, this::closeModal));
      }

      Render2DUtil.rect(ctx, x + 12, y + 32, mw - 24, 1, MenuKit.fade(MenuKit.DIVIDER, ma));
      float ca = ma * clamp01((System.currentTimeMillis() - this.msStepMs) / 200.0F);
      int bx = x + 14;
      int bw = mw - 28;
      int btnY = y + mh - 34;
      switch (this.msStep) {
         case REQUEST:
            dots(ctx, cx, y + 72, ca);
            this.centered(ctx, Translations.text("Получаем код входа…"), cx, y + 86, 7.5F, MenuKit.TEXT_OFF, ca);
            this.modalButton(ctx, "cancel", bx, btnY, bw, 20, Translations.text("Отмена"), false, mx, my, ca, this::closeModal);
            break;
         case WAIT: {
            MicrosoftAuth.DeviceCode code = this.msCode;
            this.centered(ctx, Translations.text("Открой microsoft.com/link и введи код:"), cx, y + 42, 7.5F, MenuKit.TEXT_OFF, ca);
            int boxY = y + 54;
            int boxH = 30;
            boolean boxHov = MenuKit.inside(mx, my, bx, boxY, bw, boxH);
            float bh = MenuKit.expLerp(this.modalHover.getOrDefault("code", 0.0F), boxHov ? 1.0F : 0.0F, 13.0F, this.frameDt);
            this.modalHover.put("code", bh);
            Render2DUtil.roundedRect(ctx, bx, boxY, bw, boxH, 6.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.USERCARD, MenuKit.MOD, bh), ca));
            Render2DUtil.roundedOutline(ctx, bx, boxY, bw, boxH, 6.0F, 0.6F, MenuKit.fade(Render2DUtil.lerpColor(0x24FFFFFF, 0x6EFFFFFF, bh), ca));
            String text = code == null ? "…" : code.userCode();
            float spacing = 2.5F;
            float size = 15.0F;
            float tw = 0.0F;

            for (int i = 0; i < text.length(); i++) {
               tw += MenuKit.regW(String.valueOf(text.charAt(i)), size) + (i > 0 ? spacing : 0.0F);
            }

            float gx = cx - tw / 2.0F;

            for (int i = 0; i < text.length(); i++) {
               String ch = String.valueOf(text.charAt(i));
               MenuKit.regText(ctx, ch, gx, boxY + (boxH - size * 0.7F) / 2.0F, size, MenuKit.fade(MenuKit.TEXT, ca));
               gx += MenuKit.regW(ch, size) + spacing;
            }

            this.hits.add(new Hit(bx, boxY, bw, boxH, this::copyCode));
            boolean copied = System.currentTimeMillis() - this.msCopiedMs < 1800L;
            this.centered(ctx, copied ? Translations.text("Код скопирован") : Translations.text("Нажми на код, чтобы скопировать"), cx, boxY + boxH + 6, 6.5F, copied ? GOOD : MenuKit.UID, ca);
            String left = "";
            if (code != null) {
               long sec = Math.max(0L, (code.expiresAtMs() - System.currentTimeMillis()) / 1000L);
               left = " · " + sec / 60L + ":" + String.format("%02d", sec % 60L);
            }

            String wait = Translations.text("Ждём подтверждения") + left;
            float ww = MenuKit.bookW(wait, 7.0F);
            float gx0 = cx - (ww + 16.0F) / 2.0F;
            dots(ctx, gx0 + 6.0F, y + 106.45F, ca * 0.9F, 0.55F);
            MenuKit.bookText(ctx, wait, gx0 + 16.0F, y + 104, 7.0F, MenuKit.fade(MenuKit.TEXT_OFF, ca));
            int half = (bw - 6) / 2;
            this.modalButton(ctx, "open", bx, btnY, half, 20, Translations.text("Открыть страницу"), true, mx, my, ca, this::openMsPage);
            this.modalButton(ctx, "cancel", bx + half + 6, btnY, bw - half - 6, 20, Translations.text("Отмена"), false, mx, my, ca, this::closeModal);
            break;
         }
         case LOGIN:
            dots(ctx, cx, y + 70, ca);
            this.centered(ctx, Translations.text("Входим в Minecraft…"), cx, y + 84, 8.0F, MenuKit.TEXT, ca);
            this.centered(ctx, "Microsoft → Xbox Live → Minecraft", cx, y + 97, 6.5F, MenuKit.UID, ca);
            this.modalButton(ctx, "cancel", bx, btnY, bw, 20, Translations.text("Скрыть"), false, mx, my, ca, this::closeModal);
            break;
         case ERROR: {
            Render2DUtil.roundedRect(ctx, cx - 11.0F, y + 44, 22.0F, 22.0F, 11.0F, MenuKit.fade(0x24FF6B6B, ca));
            MenuKit.regText(ctx, "!", cx - MenuKit.regW("!", 11.0F) / 2.0F, y + 51.2F, 11.0F, MenuKit.fade(BAD, ca));
            List<String> lines = wrap(this.msError == null ? Translations.text("Не удалось войти") : this.msError, 8.0F, bw);
            float ly = y + 76;

            for (int i = 0; i < Math.min(3, lines.size()); i++) {
               this.centered(ctx, lines.get(i), cx, ly, 8.0F, MenuKit.TEXT, ca);
               ly += 11.0F;
            }

            int half = (bw - 6) / 2;
            this.modalButton(ctx, "retry", bx, btnY, half, 20, Translations.text("Повторить"), true, mx, my, ca, this::startMicrosoft);
            this.modalButton(ctx, "cancel", bx + half + 6, btnY, bw - half - 6, 20, Translations.text("Закрыть"), false, mx, my, ca, this::closeModal);
            break;
         }
         default:
            break;
      }

      ctx.pose().popMatrix();
   }

   private void modalButton(GuiGraphicsExtractor ctx, String id, int x, int y, int w, int h, String text, boolean strong, int mx, int my, float a, Runnable action) {
      boolean live = this.msStep != MsStep.NONE;
      boolean hov = live && MenuKit.inside(mx, my, x, y, w, h);
      float hv = MenuKit.expLerp(this.modalHover.getOrDefault(id, 0.0F), hov ? 1.0F : 0.0F, 13.0F, this.frameDt);
      this.modalHover.put(id, hv);
      MenuKit.pill(ctx, x, y, w, h, MenuKit.truncate(false, text, 7.5F, w - 8), strong ? 0.8F + 0.2F * hv : hv, strong, a);
      if (live) {
         this.hits.add(new Hit(x, y, w, h, action));
      }
   }

   private void centered(GuiGraphicsExtractor ctx, String text, float cx, float y, float size, int color, float a) {
      float available = Math.max(1, Math.min(this.width - 24, MODAL_W) - 28);
      size = Math.min(size, size * available / Math.max(1, MenuKit.bookW(text, size)));
      MenuKit.bookText(ctx, text, cx - MenuKit.bookW(text, size) / 2.0F, y, size, MenuKit.fade(color, a));
   }

   private static void dots(GuiGraphicsExtractor ctx, float cx, float cy, float a) {
      dots(ctx, cx, cy, a, 1.0F);
   }

   private static void dots(GuiGraphicsExtractor ctx, float cx, float cy, float a, float k) {
      double t = System.currentTimeMillis() / 160.0;

      for (int i = 0; i < 3; i++) {
         float ph = (float)Math.sin(t - i * 0.9) * 0.5F + 0.5F;
         float r = (2.0F + ph * 0.8F) * k;
         float dx = (i - 1) * 9.0F * k;
         Render2DUtil.roundedRect(ctx, cx + dx - r, cy - r, r * 2.0F, r * 2.0F, r, MenuKit.fade(MenuKit.TEXT, a * (0.35F + 0.65F * ph)));
      }
   }

   private static List<String> wrap(String text, float size, float maxW) {
      List<String> out = new ArrayList<>();
      StringBuilder line = new StringBuilder();

      for (String word : text.split(" ")) {
         String next = line.isEmpty() ? word : line + " " + word;
         if (MenuKit.bookW(next, size) > maxW && !line.isEmpty()) {
            out.add(line.toString());
            line = new StringBuilder(word);
         } else {
            line = new StringBuilder(next);
         }
      }

      if (!line.isEmpty()) {
         out.add(line.toString());
      }

      return out;
   }

   private void setMsStep(MsStep step) {
      this.msStep = step;
      this.msStepMs = System.currentTimeMillis();
   }

   private void startMicrosoft() {
      int flow = ++this.msFlow;
      this.msCancelled = false;
      this.msCode = null;
      this.msError = null;
      this.setMsStep(MsStep.REQUEST);
      Minecraft mc = this.minecraft;
      CompletableFuture.supplyAsync(() -> {
         try {
            MicrosoftAuth.DeviceCode code = MicrosoftAuth.requestDeviceCode();
            mc.execute(() -> {
               if (flow == this.msFlow && this.msStep == MsStep.REQUEST) {
                  this.msCode = code;
                  this.setMsStep(MsStep.WAIT);
                  this.copyCode();
               }
            });
            MicrosoftAuth.MsaTokens tokens = MicrosoftAuth.pollDeviceCode(code, () -> this.msCancelled || flow != this.msFlow);
            mc.execute(() -> {
               if (flow == this.msFlow) {
                  this.setMsStep(MsStep.LOGIN);
               }
            });
            MicrosoftAuth.Session s = MicrosoftAuth.minecraftLogin(tokens);
            MicrosoftAccountStore.get().put(s);
            return s;
         } catch (MicrosoftAuth.AuthException e) {
            throw new CompletionException(e);
         }
      }, Util.nonCriticalIoPool()).thenCompose(AccountSwitcher::applyPremium).whenCompleteAsync((s, err) -> {
         if (s != null) {
            this.alts().addPremium(s.name());
            this.alts().selectAlt(s.name());
            NotificationManager.add(Translations.text("Alt Manager"), Translations.text("Лицензия: ") + s.name(), NotificationType.SUCCESS, 3000L);
         }

         if (flow == this.msFlow) {
            if (err == null) {
               this.setMsStep(MsStep.NONE);
            } else if (!this.msCancelled) {
               this.msError = AccountSwitcher.errorText(err);
               this.setMsStep(MsStep.ERROR);
            }
         }
      }, mc);
   }

   private void closeModal() {
      if (this.msStep == MsStep.REQUEST || this.msStep == MsStep.WAIT) {
         this.msCancelled = true;
      }

      this.msFlow++;
      this.setMsStep(MsStep.NONE);
   }

   private void copyCode() {
      MicrosoftAuth.DeviceCode code = this.msCode;
      if (code != null && this.minecraft != null) {
         this.minecraft.keyboardHandler.setClipboard(code.userCode());
         this.msCopiedMs = System.currentTimeMillis();
      }
   }

   private void openMsPage() {
      MicrosoftAuth.DeviceCode code = this.msCode;
      if (code != null) {
         this.copyCode();
         String uri = code.verificationUri();
         Util.getPlatform().openUri(uri + (uri.contains("?") ? "&" : "?") + "otc=" + code.userCode());
      }
   }

   @Override
   public void removed() {
      this.finishNote(true);
      this.finishPassword(true);
      if (this.msStep == MsStep.REQUEST || this.msStep == MsStep.WAIT) {
         this.msCancelled = true;
      }

      super.removed();
   }

   private void drawInput(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my, float a) {
      boolean hov = MenuKit.inside(mx, my, x, y, w, FIELD_H);
      this.fieldFocus = MenuKit.expLerp(this.fieldFocus, this.inputFocused ? 1.0F : (hov ? 0.4F : 0.0F), 13.0F, this.frameDt);
      Render2DUtil.roundedRect(ctx, x, y, w, FIELD_H, 4.0F, MenuKit.fade(0xFF0C0B0B, a));
      Render2DUtil.roundedOutline(ctx, x, y, w, FIELD_H, 4.0F, 0.6F, MenuKit.fade(Render2DUtil.lerpColor(0x24FFFFFF, Accent.color(0.85F), this.fieldFocus), a));
      boolean placeholder = this.input.isEmpty() && !this.inputFocused;
      String shown = placeholder ? Translations.text("Ник...") : this.input;
      float ty = y + (FIELD_H - 5.6F) / 2.0F;
      MenuKit.bookText(ctx, shown, x + 6, ty, 8.0F, MenuKit.fade(placeholder ? MenuKit.UID : MenuKit.TEXT, a));
      if (this.inputFocused) {
         long now = System.currentTimeMillis();
         if (now - this.caretToggleMs > 530L) {
            this.caretVisible = !this.caretVisible;
            this.caretToggleMs = now;
         }

         if (this.inputEdit.hasSelection()) {
            float s0 = x + 6 + MenuKit.bookW(this.input.substring(0, this.inputEdit.selStart()), 8.0F);
            float s1 = x + 6 + MenuKit.bookW(this.input.substring(0, this.inputEdit.selEnd()), 8.0F);
            Render2DUtil.roundedRect(ctx, s0 - 0.5F, y + 3.0F, s1 - s0 + 1.0F, FIELD_H - 6.0F, 1.5F, Accent.color(0.45F * a));
            MenuKit.bookText(ctx, shown, x + 6, (float)(y + (FIELD_H - 5.6F) / 2.0F), 8.0F, MenuKit.fade(MenuKit.TEXT, a));
         }
         if (this.caretVisible || this.inputEdit.sinceChange() < 0.5F) {
            float cx = x + 6 + MenuKit.bookW(this.input.substring(0, Math.min(this.inputEdit.caret(), this.input.length())), 8.0F) + 0.5F;
            Render2DUtil.roundedRect(ctx, cx, y + 4.0F, 1.0F, FIELD_H - 8.0F, 0.5F, MenuKit.fade(MenuKit.TEXT, a));
         }
      }

      this.hits.add(new Hit(x, y, w, FIELD_H, () -> {
         this.inputFocused = true;
         this.caretVisible = true;
         this.caretToggleMs = System.currentTimeMillis();
      }));
   }

   private void drawSearch(GuiGraphicsExtractor ctx, int x, int y, int w, int mx, int my, float a) {
      int h = 16;
      boolean hov = MenuKit.inside(mx, my, x, y, w, h);
      this.searchFocus = MenuKit.expLerp(this.searchFocus, this.searchFocused ? 1.0F : (hov ? 0.4F : 0.0F), 13.0F, this.frameDt);
      Render2DUtil.roundedRect(ctx, x, y, w, h, 5.0F, MenuKit.fade(0xFF0C0B0B, a));
      Render2DUtil.roundedOutline(ctx, x, y, w, h, 5.0F, 0.6F, MenuKit.fade(Render2DUtil.lerpColor(0x24FFFFFF, Accent.color(0.85F), this.searchFocus), a));
      MenuKit.Icon.texture("search").draw(ctx, x + 9.0F, y + h / 2.0F, 8.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.ICON_IDLE, Accent.color(1.0F), this.searchFocus), a));
      float tx = x + 18.0F;
      float maxW = x + w - 6 - tx;
      float ty = y + (h - 5.25F) / 2.0F;
      boolean placeholder = this.search.isEmpty() && !this.searchFocused;
      if (placeholder) {
         MenuKit.bookText(ctx, MenuKit.truncate(false, Translations.text("Поиск · Ctrl+F"), 7.5F, maxW), tx, ty, 7.5F, MenuKit.fade(MenuKit.UID, a));
      } else {
         ctx.enableScissor((int)tx, y, (int)(tx + maxW), y + h);
         float textW = MenuKit.bookW(this.search, 7.5F);
         float shift = Math.max(0.0F, textW + 2.0F - maxW);
         MenuKit.bookText(ctx, this.search, tx - shift, ty, 7.5F, MenuKit.fade(MenuKit.TEXT, a));
         long now = System.currentTimeMillis();
         if (this.searchFocused && now - this.caretToggleMs > 530L) {
            this.caretVisible = !this.caretVisible;
            this.caretToggleMs = now;
         }

         if (this.searchFocused && this.searchEdit.hasSelection()) {
            float s0 = tx - shift + MenuKit.bookW(this.search.substring(0, this.searchEdit.selStart()), 7.5F);
            float s1 = tx - shift + MenuKit.bookW(this.search.substring(0, this.searchEdit.selEnd()), 7.5F);
            Render2DUtil.roundedRect(ctx, s0 - 0.5F, y + 3.0F, s1 - s0 + 1.0F, h - 6.0F, 1.5F, Accent.color(0.45F * a));
         }
         if (this.searchFocused && (this.caretVisible || this.searchEdit.sinceChange() < 0.5F)) {
            float caretW = MenuKit.bookW(this.search.substring(0, Math.min(this.searchEdit.caret(), this.search.length())), 7.5F);
            Render2DUtil.roundedRect(ctx, tx - shift + caretW + 0.5F, y + 4.0F, 1.0F, h - 8.0F, 0.5F, MenuKit.fade(MenuKit.TEXT, a));
         }

         ctx.disableScissor();
      }

      this.hits.add(new Hit(x, y, w, h, () -> {
         this.searchFocused = true;
         this.caretVisible = true;
         this.caretToggleMs = System.currentTimeMillis();
      }));
   }

   private void advanceRows(List<AltAccountManager.OfflineAlt> list, AltAccountManager.OfflineAlt active) {
      java.util.Set<String> present = new java.util.HashSet<>();
      float k = 1.0F - (float)Math.exp(-14.0F * this.frameDt);
      for (int i = 0; i < list.size(); i++) {
         String name = list.get(i).name();
         present.add(name);
         Float slot = this.rowSlot.get(name);
         if (slot == null) {
            this.rowSlot.put(name, (float)i);
            this.rowAge.put(name, System.currentTimeMillis() - this.openMs < 400L ? 1.0F : 0.0F);
         } else {
            float next = slot + (i - slot) * k;
            this.rowSlot.put(name, Math.abs(next - i) < 0.002F ? i : next);
         }
      }
      this.rowSlot.keySet().retainAll(present);
      this.rowAge.keySet().retainAll(present);
      this.rowAge.replaceAll((n, v) -> Math.min(10.0F, v + this.frameDt));
      String activeName = active == null ? null : active.name();
      if (activeName != null && this.lastActiveName != null && !activeName.equalsIgnoreCase(this.lastActiveName)) {
         this.rowFlash.put(activeName, 1.0F);
      }
      this.lastActiveName = activeName;
      this.rowFlash.replaceAll((n, v) -> Math.max(0.0F, v - this.frameDt / ROW_FLASH_TIME));
      this.rowFlash.values().removeIf(v -> v <= 0.0F);
   }

   private static float easeOutCubic(float t) {
      float inv = 1.0F - clamp01(t);
      return 1.0F - inv * inv * inv;
   }

   private void setSearch(String value) {
      if (!value.equals(this.searchEdit.text())) {
         this.searchEdit.set(value);
      }
      if (value.equals(this.search)) return;

      this.search = value;
      this.targetScrollY = 0.0F;
   }

   private void drawList(GuiGraphicsExtractor ctx, Layout l, List<AltAccountManager.OfflineAlt> list, AltAccountManager.OfflineAlt active, int mx, int my, float a) {
      int x = l.listX;
      int w = l.listW;
      MenuKit.regText(ctx, Translations.text("Аккаунты"), x, l.winY + 15, 11.0F, MenuKit.fade(MenuKit.TEXT, a));
      int total = this.alts().getItems().size();
      String count = this.search.isEmpty() ? String.valueOf(list.size()) : list.size() + " / " + total;
      float cw = MenuKit.bookW(count, 7.5F) + 10.0F;
      MenuKit.pill(ctx, x + w - cw, l.winY + 13, cw, 14.0F, count, 0.0F, false, a);
      float searchX = x + MenuKit.regW(Translations.text("Аккаунты"), 11.0F) + 12.0F;
      float searchW = x + w - cw - 8.0F - searchX;
      if (searchW >= 60.0F) {
         this.drawSearch(ctx, Math.round(searchX), l.winY + 12, Math.round(searchW), mx, my, a);
      }
      Render2DUtil.rect(ctx, x, l.winY + 33, w, 1, MenuKit.fade(MenuKit.DIVIDER, a));
      int top = l.listTop;
      int lh = l.listBottom - top;
      float contentH = list.size() * (ROW_H + ROW_GAP) - (list.isEmpty() ? 0 : ROW_GAP);
      this.maxScroll = Math.max(0.0F, contentH - lh);
      this.targetScrollY = clamp(this.targetScrollY, -this.maxScroll, 0.0F);
      this.scrollY = MenuKit.expLerp(this.scrollY, this.targetScrollY, 16.0F, this.frameDt);
      if (list.isEmpty()) {
         String hint = this.search.isEmpty() ? Translations.text("Аккаунтов нет — впиши ник слева и нажми +") : Translations.tr("search.empty", this.search);
         MenuKit.bookText(ctx, MenuKit.truncate(false, hint, 8.0F, w), x + (w - Math.min(w, MenuKit.bookW(hint, 8.0F))) / 2.0F, top + 20, 8.0F, MenuKit.fade(MenuKit.UID, a));
      } else {
         ctx.enableScissor(x - 2, top, x + w + 2, top + lh);

         this.advanceRows(list, active);
         for (int i = 0; i < list.size(); i++) {
            float slot = this.rowSlot.getOrDefault(list.get(i).name(), (float)i);
            int ry = top + Math.round(this.scrollY + slot * (ROW_H + ROW_GAP));
            if (ry + ROW_H >= top && ry <= top + lh) {
               this.drawRow(ctx, list.get(i), i, x, ry, w, top, top + lh, mx, my, active, a);
            }
         }

         ctx.disableScissor();
         if (this.scrollY < -1.0F) {
            Render2DUtil.gradientRectVTop(ctx, x, top, w, Math.round(FADE_H), MenuKit.WINDOW, MenuKit.WINDOW & 0xFFFFFF, 0.0F);
         }

         if (this.scrollY > -this.maxScroll + 1.0F) {
            Render2DUtil.gradientRectV(ctx, x, top + lh - Math.round(FADE_H), w, Math.round(FADE_H), MenuKit.WINDOW & 0xFFFFFF, MenuKit.WINDOW, 0.0F);
         }

         if (this.maxScroll > 0.0F) {
            float sbH = Math.max(18.0F, lh * (lh / (lh + this.maxScroll)));
            float sbY = top + -this.scrollY / this.maxScroll * (lh - sbH);
            Render2DUtil.roundedRect(ctx, (float)(x + w + 4), (float)top, 2.0F, (float)lh, 1.0F, MenuKit.fade(0x14FFFFFF, a));
            Render2DUtil.gradientRectFull(ctx, x + w + 4, Math.round(sbY), 2, Math.round(sbH), Accent.color(a * 0.85F), Accent.secondary(a * 0.85F), 1.0F);
         }
      }
   }

   private void drawRow(
      GuiGraphicsExtractor ctx,
      AltAccountManager.OfflineAlt alt,
      int i,
      int x,
      int ry,
      int w,
      int clipTop,
      int clipBottom,
      int mx,
      int my,
      AltAccountManager.OfflineAlt active,
      float a
   ) {
      String key = alt.name();
      float et = clamp01((float)(System.currentTimeMillis() - this.openMs - i * 35L) / 220.0F);
      float appear = easeOutCubic(clamp01(this.rowAge.getOrDefault(key, 1.0F) / 0.35F));
      float enter = Math.min(1.0F - (1.0F - et) * (1.0F - et), appear);
      int y = ry + Math.round((1.0F - enter) * 8.0F);
      float ra = a * enter;
      x += Math.round((1.0F - appear) * 18.0F);
      boolean inClip = my >= clipTop && my <= clipBottom;
      boolean hov = inClip && MenuKit.inside(mx, my, x, y, w, ROW_H);
      boolean isActive = active != null && alt.name().equalsIgnoreCase(active.name());
      float h = MenuKit.expLerp(this.rowHover.getOrDefault(key, 0.0F), hov ? 1.0F : 0.0F, 13.0F, this.frameDt);
      float s = MenuKit.expLerp(this.rowActive.getOrDefault(key, isActive ? 1.0F : 0.0F), isActive ? 1.0F : 0.0F, 12.0F, this.frameDt);
      this.rowHover.put(key, h);
      this.rowActive.put(key, s);
      float lift = h * 1.5F;
      y -= Math.round(lift);
      Render2DUtil.softShadow(ctx, x, y, w, ROW_H, 8.0F, 2.0F + 3.0F * Math.max(s, h), ra);
      Render2DUtil.roundedRect(ctx, x, y, w, ROW_H, 8.0F, MenuKit.fade(Render2DUtil.lerpColor(MenuKit.MOD, MenuKit.MODE_SEL, Math.max(s, h * 0.45F)), ra));
      if (s > 0.01F) {
         Render2DUtil.gradientRectFull(ctx, x, y, w, ROW_H, Accent.color(ra * s * 0.14F), Accent.secondary(0.0F), 8.0F);
      }
      Render2DUtil.roundedOutline(ctx, x, y, w, ROW_H, 8.0F, 0.6F, MenuKit.fade(Render2DUtil.lerpColor(0x16FFFFFF, 0x50878787, h), ra));
      if (s > 0.01F) {
         Render2DUtil.roundedOutline(ctx, x, y, w, ROW_H, 8.0F, 0.8F, Accent.color(ra * s * 0.6F));
         Render2DUtil.gradientRectVTop(ctx, x + 3, y + 9, 2, ROW_H - 18, Accent.color(ra * s), Accent.secondary(ra * s), 1.0F);
      }
      float flash = this.rowFlash.getOrDefault(key, 0.0F);
      if (flash > 0.01F) {
         float sweep = easeOutCubic(1.0F - flash);
         int half = Math.max(1, Math.round(w * 0.2F));
         int bandX = Math.round(x - half * 2 + (w + half * 2) * sweep);
         int shine = Render2DUtil.lerpColor(0xFFFFFFFF, Accent.color(1.0F), 0.4F) & 0xFFFFFF;
         int peak = shine | Math.round(34.0F * flash * ra) << 24;
         ctx.enableScissor(x, Math.max(y, clipTop), x + w, Math.min(y + ROW_H, clipBottom));
         Render2DUtil.gradientRectFull(ctx, bandX, y, half, ROW_H, shine, peak, 0.0F);
         Render2DUtil.gradientRectFull(ctx, bandX + half, y, half, ROW_H, peak, shine, 0.0F);
         ctx.disableScissor();
      }

      int hx = x + 9;
      int hy = y + (ROW_H - HEAD) / 2;
      Identifier skin = SkinHeads.of(alt.name(), alt.premium());
      Render2DUtil.roundedFace(ctx, skin, hx, hy, HEAD, 5, 8, 8, MenuKit.fade(-1, ra));
      Render2DUtil.roundedFace(ctx, skin, hx, hy, HEAD, 5, 40, 8, MenuKit.fade(-1, ra));
      float tx = hx + HEAD + 9;
      int starX = x + w - 22;
      int delX = x + w - 40;
      int editX = x + w - 61;
      float right = editX - 5;
      boolean compact = w < 270;
      int passwordW = compact ? 54 : 58;
      int passwordX = compact ? x + w - 9 - passwordW : editX - 6 - passwordW;
      int passwordY = y + (compact ? 23 : 6);
      boolean pending = alt.name().equalsIgnoreCase(this.pendingLogin);
      String badgeText = pending ? Translations.text("Вход…") : (isActive ? Translations.text("Активен") : (alt.premium() ? Translations.text("Лицензия") : Translations.text("Оффлайн")));
      float badgeW = MenuKit.bookW(badgeText, 6.5F) + 8.0F;
      float nameMax = Math.max(0.0F, (compact ? right : passwordX - 5) - badgeW - 6.0F - tx);
      String name = MenuKit.truncate(true, alt.name(), 9.0F, nameMax);
      float roll = alt.name().equals(this.rolledName) ? this.rollProgress() : 1.0F;
      if (roll < 1.0F) {
         MenuKit.regText(ctx, scramble(name, roll), tx, y + 9.0F, 9.0F, MenuKit.fade(Render2DUtil.lerpColor(Accent.color(1.0F), MenuKit.TEXT, roll), ra));
      } else {
         MenuKit.regText(ctx, name, tx, y + 9.0F, 9.0F, MenuKit.fade(MenuKit.TEXT, ra));
      }
      float nameW = MenuKit.regW(name, 9.0F);
      this.badge(ctx, tx + nameW + 6.0F, y + 7.5F, badgeText, pending || isActive ? Accent.color(1.0F) : (alt.premium() ? GOOD : MenuKit.UID), ra);
      boolean editing = alt.name().equals(this.editingNote);
      int noteX = Math.round(tx);
      int noteY = y + 23;
      int noteW = x + w - 9 - noteX;
      if (compact) noteW = Math.max(16, passwordX - 4 - noteX);
      AnimatedAccountField noteField = this.noteFields.computeIfAbsent(key, ignored -> new AnimatedAccountField());
      noteField.draw(ctx, editing ? this.noteEdit.text() : alt.note(), editing ? this.noteEdit : null,
         Translations.text("Добавить заметку…"), editing, noteX, noteY, noteW, 15, 6.5F,
         inClip ? mx : -10000, inClip ? my : -10000, ra, this.frameDt);
      boolean editingSecret = alt.name().equals(this.editingPassword);
      ServerLoginStore.Entry shared = ServerLoginStore.get().defaultForAccount(alt.name());
      String secret = editingSecret ? this.sharedPasswordEdit.text() : shared == null ? "" : shared.password();
      AnimatedAccountField passwordField = this.passwordFields.computeIfAbsent(key, ignored -> new AnimatedAccountField());
      passwordField.draw(ctx, "*".repeat(secret.length()), editingSecret ? this.sharedPasswordEdit : null,
         Translations.text("Пароль"), editingSecret, passwordX, passwordY, passwordW, 15, 6.5F,
         inClip ? mx : -10000, inClip ? my : -10000, ra, this.frameDt);
      boolean editHov = hov && MenuKit.inside(mx, my, editX, y, 18, 22);
      MenuKit.bookText(ctx, "…", editX + 4, y + 8, 9.0F, MenuKit.fade(editHov ? Accent.color(1.0F) : MenuKit.TEXT_OFF, ra));
      boolean starHov = hov && MenuKit.inside(mx, my, starX - 2, y, 20, 22);
      Render2DUtil.star(ctx, starX + 8.0F, y + 12, 10.0F, 5, 2.0F, 0.0F, MenuKit.fade(alt.favorite() ? Accent.color(1.0F) : (starHov ? 0x99FFFFFF : 0x40FFFFFF), ra));
      if (h > 0.05F) {
         boolean delHov = MenuKit.inside(mx, my, delX - 2, y, 18, 22);
         int col = MenuKit.fade(delHov ? 0xFFFF6B6B : 0xFF8C8E9C, ra * h);
         cross(ctx, delX + 7.0F, y + 12, 7.0F, col);
      }

      if (inClip) {
         this.hits.add(new Hit(passwordX, Math.max(passwordY, clipTop), passwordW, Math.max(0, Math.min(passwordY + 15, clipBottom) - Math.max(passwordY, clipTop)), () -> this.beginPassword(alt.name()), null, true));
         this.hits.add(new Hit(noteX, Math.max(noteY, clipTop), noteW, Math.max(0, Math.min(noteY + 15, clipBottom) - Math.max(noteY, clipTop)), () -> this.beginNote(alt.name()), null, true));
         this.hits.add(new Hit(editX, y, 18, 22, () -> this.minecraft.setScreenAndShow(new AccountDetailsScreen(this, alt))));
         this.hits.add(new Hit(starX - 2, y, 20, 22, () -> this.alts().toggleFavorite(alt.name())));
         if (h > 0.05F) {
            this.hits.add(new Hit(delX - 2, y, 18, 22, () -> this.removeAccount(alt)));
         }

         this.hits.add(new Hit(x, Math.max(y, clipTop), w, Math.min(y + ROW_H, clipBottom) - Math.max(y, clipTop), null, alt.name()));
      }
   }

   private void badge(GuiGraphicsExtractor ctx, float x, float y, String text, int color, float a) {
      float bw = MenuKit.bookW(text, 6.5F) + 8.0F;
      Render2DUtil.roundedRect(ctx, x, y, bw, 11.0F, 3.0F, MenuKit.fade(color & 0xFFFFFF | 0x24000000, a));
      MenuKit.bookText(ctx, text, x + 4.0F, y + (11.0F - 4.55F) / 2.0F, 6.5F, MenuKit.fade(color, a));
   }

   private static void cross(GuiGraphicsExtractor ctx, float cx, float cy, float len, int color) {
      for (float angle : new float[]{(float)(Math.PI / 4), (float)(-Math.PI / 4)}) {
         ctx.pose().pushMatrix();
         ctx.pose().translate(cx, cy);
         ctx.pose().rotate(angle);
         Render2DUtil.roundedRect(ctx, -len / 2.0F, -0.75F, len, 1.5F, 0.75F, color);
         ctx.pose().popMatrix();
      }
   }

   private List<AltAccountManager.OfflineAlt> sorted() {
      List<AltAccountManager.OfflineAlt> v = new ArrayList<>(
         this.alts().getItems().stream().filter(alt -> alt != null && alt.name() != null && !alt.name().isBlank()).toList()
      );
      v.sort(
         Comparator.<AltAccountManager.OfflineAlt>comparingInt(alt -> alt.favorite() ? 1 : 0)
            .reversed()
            .thenComparing(Comparator.comparingLong(AltAccountManager.OfflineAlt::createdMs).reversed())
            .thenComparing(AltAccountManager.OfflineAlt::name, String.CASE_INSENSITIVE_ORDER)
      );
      return v;
   }

   private List<AltAccountManager.OfflineAlt> filtered() {
      List<AltAccountManager.OfflineAlt> all = this.sorted();
      if (this.search.isEmpty()) {
         return all;
      }

      String q = this.search.toLowerCase(java.util.Locale.ROOT);
      return all.stream().filter(alt -> alt.name().toLowerCase(java.util.Locale.ROOT).contains(q)
         || alt.note().toLowerCase(java.util.Locale.ROOT).contains(q)).toList();
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
      if (e.button() == 0) {
         for (Hit h : this.hits) {
            if (!MenuKit.inside(e.x(), e.y(), h.x, h.y, h.w, h.h)) continue;

            // Clicking an inline editor again keeps its selection and caret.
            if (h.note) {
               h.action.run();
               return true;
            }
            this.finishNote(true);
            this.finishPassword(true);
            this.inputFocused = false;
            this.searchFocused = false;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (h.login != null) {
               this.login(h.login, dbl);
            } else {
               h.action.run();
            }

            return true;
         }

         this.finishNote(true);
         this.finishPassword(true);
         this.inputFocused = false;
         this.searchFocused = false;
      }

      return super.mouseClicked(e, dbl);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
      if (this.msStep != MsStep.NONE) {
         return true;
      }

      if (this.maxScroll > 0.0F) {
         this.finishNote(true);
         this.finishPassword(true);
         this.targetScrollY = clamp(this.targetScrollY + (float)(vertical * 24.0), -this.maxScroll, 0.0F);
         return true;
      }

      return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
   }

   @Override
   public boolean keyPressed(KeyEvent e) {
      int key = e.key();
      if (this.msStep != MsStep.NONE) {
         if (key == 256) {
            this.closeModal();
         } else if ((e.modifiers() & CTRL_MOD) != 0 && key == KEY_C) {
            this.copyCode();
         }

         return true;
      }

      if (this.editingPassword != null) {
         if (key == 256) {
            this.finishPassword(false);
         } else if (key == 257 || key == 335 || key == 258) {
            this.finishPassword(true);
         } else if ((e.modifiers() & CTRL_MOD) != 0 && key == KEY_F) {
            this.finishPassword(true);
            this.searchFocused = true;
         } else if (!e.isCopy() && !e.isCut()) {
            this.sharedPasswordEdit.key(e);
         }
         return true;
      }

      if (this.editingNote != null) {
         if (key == 256) {
            this.finishNote(false);
         } else if (key == 257 || key == 335 || key == 258) {
            this.finishNote(true);
         } else if ((e.modifiers() & CTRL_MOD) != 0 && key == KEY_F) {
            this.finishNote(true);
            this.searchFocused = true;
         } else {
            this.noteEdit.key(e);
         }
         return true;
      }

      if ((e.modifiers() & CTRL_MOD) != 0 && key == KEY_F) {
         this.inputFocused = false;
         this.searchFocused = true;
         this.caretVisible = true;
         this.caretToggleMs = System.currentTimeMillis();
         return true;
      }

      if (this.searchFocused) {
         if (key == 257 || key == 335) {
            List<AltAccountManager.OfflineAlt> found = this.filtered();
            if (!found.isEmpty()) {
               this.login(found.get(0).name(), true);
            }
         } else if (key == 256) {
            if (this.search.isEmpty()) {
               this.searchFocused = false;
            } else {
               this.setSearch("");
            }
         } else if (this.searchEdit.key(e)) {
            this.setSearch(this.searchEdit.text());
         }

         return true;
      }

      if (this.inputFocused) {
         int k = e.key();
         if ((e.modifiers() & CTRL_MOD) != 0 && k == KEY_R) {
            this.rollNick();
         } else if (k == 257 || k == 335) {
            this.addFromInput();
         } else if (k == 256) {
            this.inputFocused = false;
            this.inputEdit.clearSelection();
         } else if (this.inputEdit.key(e)) {
            this.input = this.inputEdit.text();
         }

         return true;
      }

      return super.keyPressed(e);
   }

   @Override
   public boolean charTyped(CharacterEvent e) {
      if (this.msStep != MsStep.NONE) {
         return true;
      }

      if (this.editingPassword != null) {
         this.sharedPasswordEdit.insert(e.codepointAsString());
         return true;
      }

      if (this.editingNote != null) {
         this.noteEdit.insert(e.codepointAsString());
         return true;
      }

      if (this.searchFocused) {
         this.searchEdit.insert(e.codepointAsString());
         this.setSearch(this.searchEdit.text());
         return true;
      }

      if (!this.inputFocused) {
         return super.charTyped(e);
      }

      this.inputEdit.insert(e.codepointAsString());
      this.input = this.inputEdit.text();

      return true;
   }

   private void login(String name, boolean dbl) {
      AltAccountManager.OfflineAlt alt = this.alts().getItems().stream().filter(a -> a.name().equalsIgnoreCase(name)).findFirst().orElse(null);
      if (alt != null && alt.premium()) {
         this.loginPremium(alt.name());
      } else {
         this.alts().selectAlt(name);
         AccountSwitcher.applyOffline(name);
         if (dbl) {
            NotificationManager.add(Translations.text("Alt Manager"), Translations.text("Аккаунт: ") + name + Translations.text(" (со следующего захода)"), NotificationType.SUCCESS, 2500L);
         }
      }
   }

   private void beginNote(String name) {
      this.finishPassword(true);
      if (name.equals(this.editingNote)) return;
      this.finishNote(true);
      AltAccountManager.OfflineAlt alt = this.alts().getItems().stream().filter(a -> a.name().equals(name)).findFirst().orElse(null);
      if (alt == null) return;
      this.editingNote = name;
      this.noteEdit.set(alt.note());
      this.inputFocused = false;
      this.searchFocused = false;
   }

   private void finishNote(boolean save) {
      if (this.editingNote == null) return;
      String name = this.editingNote;
      this.editingNote = null;
      if (save) {
         this.alts().setNote(name, this.noteEdit.text());
         AnimatedAccountField field = this.noteFields.get(name);
         if (field != null) field.confirm();
      }
      this.noteEdit.set("");
   }

   private void beginPassword(String name) {
      if (name.equals(this.editingPassword)) return;
      this.finishNote(true);
      this.finishPassword(true);
      ServerLoginStore.Entry shared = ServerLoginStore.get().defaultForAccount(name);
      this.editingPassword = name;
      this.sharedPasswordEdit.set(shared == null ? "" : shared.password());
      this.inputFocused = false;
      this.searchFocused = false;
   }

   private void finishPassword(boolean save) {
      if (this.editingPassword == null) return;
      String name = this.editingPassword;
      this.editingPassword = null;
      if (save) {
         ServerLoginStore.Entry old = ServerLoginStore.get().defaultForAccount(name);
         ServerLoginStore.get().putDefault(name, this.sharedPasswordEdit.text(), old == null || old.enabled());
         AnimatedAccountField field = this.passwordFields.get(name);
         if (field != null) field.confirm();
      }
      this.sharedPasswordEdit.set("");
   }

   private void loginPremium(String name) {
      Minecraft mc = this.minecraft;
      String current = mc.getUser().getName();
      if (current.equalsIgnoreCase(name) && !mc.getUser().getProfileId().equals(UUIDUtil.createOfflinePlayerUUID(current))) {
         this.alts().selectAlt(name);
      } else if (this.pendingLogin == null) {
         this.pendingLogin = name;
         AccountSwitcher.loginPremium(name).whenCompleteAsync((s, err) -> {
            this.pendingLogin = null;
            if (s != null) {
               this.alts().rename(name, s.name());
               this.alts().selectAlt(s.name());
               NotificationManager.add(Translations.text("Alt Manager"), Translations.text("Лицензия: ") + s.name(), NotificationType.SUCCESS, 2500L);
            } else {
               NotificationManager.add(Translations.text("Alt Manager"), AccountSwitcher.errorText(err), NotificationType.ERROR, 4000L);
            }
         }, mc);
      }
   }

   private void removeAccount(AltAccountManager.OfflineAlt alt) {
      this.alts().removeAlt(alt.name());
      if (alt.premium()) {
         MicrosoftAccountStore.get().remove(alt.name());
      }
   }

   private void rollNick() {
      String name = NickGenerator.generate(candidate -> this.alts().getItems().stream().anyMatch(alt -> alt.name().equalsIgnoreCase(candidate)));
      this.alts().addAlt(name);
      this.rolledName = name;
      this.rollAt = System.currentTimeMillis();
      this.targetScrollY = 0.0F;
      if (!this.search.isEmpty() && !name.toLowerCase(java.util.Locale.ROOT).contains(this.search.toLowerCase(java.util.Locale.ROOT))) {
         this.setSearch("");
      }
   }

   private float rollProgress() {
      return clamp01((System.currentTimeMillis() - this.rollAt) / (float)ROLL_MS);
   }

   private static String scramble(String target, float progress) {
      int settled = Math.round(target.length() * easeOutCubic(progress));
      StringBuilder out = new StringBuilder(target.length());
      out.append(target, 0, settled);
      ThreadLocalRandom random = ThreadLocalRandom.current();
      for (int i = settled; i < target.length(); i++) {
         out.append(ROLL_CHARS.charAt(random.nextInt(ROLL_CHARS.length())));
      }

      return out.toString();
   }

   private void drawDice(GuiGraphicsExtractor ctx, float cx, float cy, float a) {
      float roll = this.rollProgress();
      float spin = roll < 1.0F ? (1.0F - easeOutCubic(roll)) * (float)(Math.PI * 2.0) : 0.0F;
      float size = 8.5F + this.diceHover * 0.6F;
      float half = size / 2.0F;
      int color = MenuKit.fade(Render2DUtil.lerpColor(MenuKit.TEXT, Accent.color(1.0F), Math.max(this.diceHover, roll < 1.0F ? 1.0F - roll : 0.0F)), a);
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().rotate(spin);
      Render2DUtil.roundedRect(ctx, -half, -half, size, size, 2.2F, color);
      int pipColor = MenuKit.fade(0xFF151414, a);
      float pip = 1.7F;
      float off = size * 0.26F;
      Render2DUtil.roundedRect(ctx, -off - pip / 2.0F, -off - pip / 2.0F, pip, pip, pip / 2.0F, pipColor);
      Render2DUtil.roundedRect(ctx, -pip / 2.0F, -pip / 2.0F, pip, pip, pip / 2.0F, pipColor);
      Render2DUtil.roundedRect(ctx, off - pip / 2.0F, off - pip / 2.0F, pip, pip, pip / 2.0F, pipColor);
      ctx.pose().popMatrix();
   }

   private void addFromInput() {
      String name = sanitize(this.input);
      if (name != null) {
         this.alts().addAlt(name);
         this.input = "";
         this.inputEdit.set("");
      }
   }


   private static boolean validChar(char c) {
      return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
   }

   private static String sanitize(String s) {
      if (s == null) {
         return null;
      }

      s = s.trim().replaceAll("[^A-Za-z0-9_]", "");
      if (s.length() < 3) {
         return null;
      }

      if (s.length() > 16) {
         s = s.substring(0, 16);
      }

      return s;
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   @Override
   public void onClose() {
      this.finishNote(true);
      this.finishPassword(true);
      Minecraft.getInstance().setScreenAndShow(this.parent);
   }

   private enum MsStep {
      NONE,
      REQUEST,
      WAIT,
      LOGIN,
      ERROR;
   }

   private record Layout(int winX, int winY, int winW, int winH, int sideX, int sideW, int listX, int listW, int listTop, int listBottom) {
   }

   private record Hit(float x, float y, float w, float h, Runnable action, String login, boolean note) {
      Hit(float x, float y, float w, float h, Runnable action) {
         this(x, y, w, h, action, null, false);
      }

      Hit(float x, float y, float w, float h, Runnable action, String login) {
         this(x, y, w, h, action, login, false);
      }
   }
}
