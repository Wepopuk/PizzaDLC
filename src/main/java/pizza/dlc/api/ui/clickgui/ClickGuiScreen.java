package pizza.dlc.api.ui.clickgui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.translation.Translations;
import pizza.dlc.api.translation.TranslationManager;
import pizza.dlc.api.ui.language.LanguageSelectScreen;
import pizza.dlc.api.ui.TextEdit;
import pizza.dlc.api.ui.TypingScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.BindSetting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ButtonSetting;
import pizza.dlc.api.settings.impl.ColorSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.settings.impl.StringSetting;
import pizza.dlc.api.theme.Theme;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.input.KeyNameUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.ModuleManager;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.impl.other.ClickGui;

public class ClickGuiScreen extends Screen implements TypingScreen {
   private static final Identifier SEARCH_TEXTURE = categoryTexture("search");
   private static final Map<ModuleCategory, Identifier> CATEGORY_TEXTURES = Map.of(
      ModuleCategory.COMBAT, categoryTexture("combat"),
      ModuleCategory.MOVEMENT, categoryTexture("movement"),
      ModuleCategory.VISUALS, categoryTexture("visuals"),
      ModuleCategory.PLAYER, categoryTexture("player"),
      ModuleCategory.OTHER, categoryTexture("other")
   );

   private static Identifier categoryTexture(String name) {
      return Identifier.fromNamespaceAndPath("pizzadlc", "textures/gui/figma/" + name + ".png");
   }
   private static final int C_WINDOW = 0xFF0F0F0F;
   private static final int C_MOD = 0xFF151414;
   private static final int C_MODE_SEL = 0xFF252323;
   private static final int C_BIND = 0xFF252323;
   private static final int C_V1 = 0xFF878787;
   private static final int C_UID = 0xFF8E8E93;
   private static final int C_TOGGLE_OFF = 0xFF534E4E;
   private static final int C_USERCARD = 0xFF0C0B0B;
   private static int C_CARD_ON = 0x20EE5577;
   private static int C_ACCENT = 0xFFEE5577;
   private static int C_ACCENT_FILL = 0xFF832E41;
   private static final int C_TEXT = 0xFFE9EAF2;
   private static final int C_TEXT_OFF = 0xFF8C8E9C;
   private static final int WIN_PAD = 7;
   private static final int DROP_ITEM_H = 12;
   private static ModuleCategory selectedCategory;
   private static Module selectedModule;
   private String searchQuery = "";
   private final TextEdit searchEdit = new TextEdit(32, c -> true);
   private final TextEdit stringEdit = new TextEdit(256, c -> true);
   private final TextEdit sliderEdit = new TextEdit(10, c -> c >= '0' && c <= '9' || c == '.' || c == '-');
   private float searchResultsShown;
   private float noResultsAnim;
   private boolean searchFocused;
   private boolean profileSelected;
   private float profileAge;
   private float profileCardHover;
   private float profileCardSelected;
   private float profileThemesHover;
   private float profileLanguageHover;
   private float searchFocusAnim;
   private String animatedSearchQuery = "";
   private final List<Float> searchGlyphAges = new ArrayList<>();
   private float searchCaretX;
   private float searchCaretAge;
   private float searchTypingPulse;
   private float searchTextScroll;
   private final Map<ModuleCategory, Float> tabHoverAnim = new HashMap<>();
   private static final String PROFILE_USER = "h0td04ek";
   private static final String PROFILE_ROLE = "ADMIN";
   private static final String PROFILE_EXPIRY = "06.07.2067";
   private boolean searchOpen = false;
   private float searchAnim = 0.0F;
   private static final int ROW_HOVER_PAD_X = 6;
   private static final int ROW_HOVER_PAD_Y = 3;
   private static final float OPEN_TIME = 0.34F;
   private static final float CLOSE_TIME = 0.17F;
   private static final float CARD_ENTER_TIME = 0.42F;
   private static final float CARD_STAGGER = 0.035F;
   private static final int CARD_STAGGER_MAX = 12;
   private static final float RIPPLE_TIME = 0.55F;
   private static final float PILL_STIFFNESS = 340.0F;
   private static final float PILL_DAMPING = 24.0F;
   private float windowAnim = 0.0F;
   private float windowScale = 0.94F;
   private float openProgress;
   private boolean closing;
   private boolean closeQueued;
   private final Map<Object, Float> cardAge = new HashMap<>();
   private final Map<Module, Boolean> cardWasOn = new HashMap<>();
   private final Map<Module, Float> cardFlash = new HashMap<>();
   private final Map<SliderSetting, Float> sliderShown = new HashMap<>();
   private final Map<SliderSetting, Float> knobAnim = new HashMap<>();
   private final List<float[]> ripples = new ArrayList<>();
   private final Map<Object, Float> rowHoverAnim = new HashMap<>();
   private final Map<Object, float[]> cardSlide = new HashMap<>();
   private List<W> lastLayout = List.of();
   private float tabPillY = Float.NaN;
   private float tabPillVel;
   private float tabPillAlpha;
   private final Map<Module, Float> cardEnter = new HashMap<>();
   private final Map<Module, Float> cardHover = new HashMap<>();
   private int mxCache;
   private int myCache;
   private final Map<Module, Float> moduleSelAnim = new HashMap<>();
   private final Map<Module, Float> moduleAnim = new HashMap<>();
   private final Map<Setting, Float> dropAnim = new HashMap<>();
   private Setting openDrop;
   private int dropScroll;
   private final Map<String, Float> dropItemSlide = new HashMap<>();
   private final Map<BooleanSetting, Float> boolAnim = new HashMap<>();
   private SliderSetting draggingSlider;
   private boolean draggingScrollbar;
   private boolean autoScroll;
   private int autoScrollAnchorX;
   private int autoScrollAnchorY;
   private int sliderTrackX;
   private int sliderTrackW;
   private final Map<SliderSetting, Float> sliderAnim = new HashMap<>();
   private float frameDt = 0.0F;
   private long animLastNanos = 0L;
   private SliderSetting editingSlider;
   private String sliderBuf = "";
   private Object listeningBind;
   private StringSetting editingString;
   private Theme editTheme;
   private boolean editSecondary;
   private String editText = "";
   private boolean editSelectAll;
   private final ColorPicker colorPicker = new ColorPicker();
   private final Map<Module, Float> cardOnAnim = new HashMap<>();

   @Override
   public boolean isTyping() {
      return this.searchFocused || this.editingSlider != null || this.editingString != null || this.editTheme != null || this.listeningBind != null;
   }
   private final BindPopup bindPopup = new BindPopup();
   private float scrollOffset = 0.0F;
   private float scrollTarget = 0.0F;
   private int contentH = 0;
   private float topFadeAnim = 0.0F;
   private float bottomFadeAnim = 0.0F;
   private float categoryAnim = 1.0F;
   private float categorySlideDir = 0.0F;
   private final Map<ModuleCategory, Float> tabSelAnim = new HashMap<>();
   private float glowPhase = 0.0F;
   private float glowPulse = 1.0F;
   private float detailScroll = 0.0F;
   private float detailScrollTarget = 0.0F;
   private int detailContentH = 0;
   private long openedAtMillis = 0L;
   private boolean bindCloseArmed = false;
   private static final int PIZZA_SIDEBAR_W = 104;
   private static final int PIZZA_GAP = 8;
   private static final int PIZZA_MASTER_W = 184;
   private static final float WINDOW_RADIUS_PIZZA = 12.0F;
   private static final int K_TAB = 0;
   private static final int K_LABEL = 1;
   private static final int K_BOOL = 2;
   private static final int K_SLIDER = 3;
   private static final int K_BIND = 4;
   private static final int K_STRING = 5;
   private static final int K_THEME = 6;
   private static final int K_THEME_ADD = 7;
   private static final int K_BUTTON = 8;
   private static final int K_DROP_HEAD = 9;
   private static final int K_MODULE_ROW = 10;
   private static final int K_COLOR = 11;
   private static final int K_SECTION = 12;
   private static final int K_CARD = 13;

   private static float outlineThickness() {
      return 0.2F;
   }

   private static int outlineColor() {
      return 0x40FFFFFF;
   }

   private static int hairline() {
      int a = Math.max(1, Math.round((outlineColor() >>> 24 & 0xFF) * outlineThickness()));
      return a << 24 | outlineColor() & 0xFFFFFF;
   }

   public ClickGuiScreen() {
      super(Component.literal("PizzaDLC"));
      this.openedAtMillis = System.currentTimeMillis();
      this.bindCloseArmed = false;
   }

   private ModuleManager mm() {
      return PizzaDLCClient.getInstance().getModuleManager();
   }

   private int winX() {
      return (this.width - this.winW()) / 2;
   }

   private int winY() {
      return Math.max(36, (this.height - this.winH()) / 2 + 14);
   }

   private int winW() {
      return Math.max(1, Math.min(560, this.width - 16));
   }

   private int winH() {
      return Math.max(1, Math.min(260, this.height - 76));
   }

   private int sidebarX() { return this.winX() + WIN_PAD + 6; }
   private int sidebarW() { return Math.min(PIZZA_SIDEBAR_W, Math.max(76, this.winW() / 5)); }
   private int masterX2() { return this.sidebarX() + this.sidebarW() + PIZZA_GAP; }
   private int masterW2() { return PIZZA_MASTER_W; }
   private int pizzaDetailX() { return this.masterX2() + this.masterW2() + PIZZA_GAP; }
   private int pizzaDetailW() { return this.winX() + this.winW() - WIN_PAD - this.pizzaDetailX(); }
   private int contentX() { return this.masterX2(); }
   private int contentW() { return this.winX() + this.winW() - WIN_PAD - this.contentX(); }
   private int floatingSearchW() { return Math.min(360, this.winW()); }
   private int floatingSearchH() { return 22; }
   private int[] floatingSearchRect() {
      int w = floatingSearchW();
      int h = floatingSearchH();
      int x = (this.width - w) / 2;
      int y = this.winY() - h - 10;
      if (y < 6) y = 6;
      return new int[]{x,y,w,h};
   }
   private static String pizzaCatName(ModuleCategory c) {
      if (c == ModuleCategory.VISUALS) return Translations.text("Render");
      if (c == ModuleCategory.OTHER) return Translations.text("Misc");
      if (c == ModuleCategory.ENVIRONMENT) return Translations.text("World");
      return Translations.text(c.getDisplayName());
   }
   private static String pizzaCatIcon(ModuleCategory c) {
      if (c == ModuleCategory.COMBAT) return "a";
      if (c == ModuleCategory.MOVEMENT) return "e";
      if (c == ModuleCategory.VISUALS) return "l";
      if (c == ModuleCategory.PLAYER) return "q";
      if (c == ModuleCategory.OTHER) return "r";
      if (c == ModuleCategory.ENVIRONMENT) return "s";
      if (c == ModuleCategory.THEMES) return "t";
      return "u";
   }

   private int contentTop() {
      return this.winY() + 2 + 34;
   }

   private int masterListTop() {
      return this.contentTop() + 12;
   }

   private int listTop() {
      return this.isMasterDetail() ? this.masterListTop() : this.contentTop() + 10;
   }

   private int viewportH() {
      return this.winY() + this.winH() - 7 - this.listTop();
   }

   private boolean isMasterDetail() {
      return false;
   }

   private int detailX() {
      return this.pizzaDetailX();
   }

   private int detailW() {
      return this.pizzaDetailW();
   }

   private int detailHeaderBottom() {
      Module m = selectedModule;
      if (m == null) {
         return this.contentTop() + 10;
      }

      int y = this.contentTop() + 10 + Math.round(9.099999F) + 3;
      y += wrapText(Translations.text(m.getDescription()), 7.5F, this.detailW() - 20).size() * 9;
      return y + 9;
   }

   private int detailSettingsTop() {
      return this.detailHeaderBottom();
   }

   private int detailSettingsBot() {
      return this.winY() + this.winH() - 7 - 10;
   }

   private int detailViewportH() {
      return this.detailSettingsBot() - this.detailSettingsTop();
   }

   private float clampDetailScroll(float v) {
      float max = Math.max(0.0F, (float)(this.detailContentH - this.detailViewportH()));
      return Math.max(0.0F, Math.min(max, v));
   }

   private boolean matchesSearch(Module m) {
      return ModuleSearch.matches(m, this.searchQuery, TranslationManager.getInstance());
   }

   private static int[] modulePillRect(W w, Module m) {
      int selX = w.x;
      int selW = w.w;
      int pad = 8;
      int tx = selX + pad;
      String bTxt = m.getKey() == -1 ? "—" : keyName(m.getKey());
      int bW = Math.min(Math.max(18, w.w / 4), (int)Math.ceil(sfW(bTxt, 8.0F)) + 6);
      int bH = 16;
      boolean perm = m.isPermanent();
      int rightX = perm
         ? selX + selW - pad - ((int)Math.ceil(bookW(Translations.text("ВСЕГДА"), 8.0F)) + 6)
         : selX + selW - pad - 28;
      float maxNameW = Math.max(0.0F, (float)(rightX - 6 - (bW + 8) - tx));
      String nameTxt = Translations.moduleName(m.getName());
      if (bookW(nameTxt, 9.0F) > maxNameW) {
         nameTxt = truncateToWidth(nameTxt, 9.0F, maxNameW);
      }

      float nameW = bookW(nameTxt, 9.0F);
      int bX = rightX - 6 - bW;
      int bY = w.y + (22 - bH) / 2;
      return new int[]{bX, bY, bW, bH};
   }

   private float clampScroll(float v) {
      float max = Math.max(0.0F, (float)(this.contentH - this.viewportH()));
      return Math.max(0.0F, Math.min(max, v));
   }

   protected void init() {
      if (selectedCategory == null) {
         for (ModuleCategory c : ModuleCategory.values()) {
            if (c == ModuleCategory.THEMES || !this.hasModules(c)) continue;

            selectedCategory = c;
            break;
         }
      }
      this.windowAnim = 0.0F;
      this.categoryAnim = 0.0F;
      this.profileAge = 0.0F;
      this.windowScale = 0.92F;
      this.searchAnim = this.searchOpen ? 1.0F : 0.0F;
      if (this.openedAtMillis == 0L) {
         this.openedAtMillis = System.currentTimeMillis();
         this.openProgress = 0.0F;
         this.closing = false;
         this.cardAge.clear();
         this.tabPillY = Float.NaN;
      }
   }

   private boolean hasModules(ModuleCategory c) {
      if (c == ModuleCategory.THEMES) return true;
      for (Module m : this.mm().getModules()) {
         if (m.isHidden() || m.getCategory() != c) continue;

         return true;
      }

      return false;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private List<W> buildLayout() {
      List<W> out = new ArrayList<>();
      if (this.profileSelected) {
         this.contentH = 200;
      } else if (selectedCategory == ModuleCategory.THEMES && this.searchQuery.isEmpty()) {
         int areaX = this.contentX();
         int areaTop = this.listTop() - Math.round(this.scrollOffset);
         int areaRight = this.winX() + this.winW() - 7;
         int columns = Math.max(1, (areaRight - areaX + 6) / 157);
         int maxBottom = areaTop;
         List<Theme> themeList = ThemeManager.themes();
         int total = themeList.size() + 1;

         for (int i = 0; i < total; i++) {
            int cardX = areaX + i % columns * 157;
            int cardY = areaTop + i / columns * 91;
            W tw = i < themeList.size() ? new W(K_THEME, themeList.get(i), cardX, cardY, 151, 85) : new W(K_THEME_ADD, null, cardX, cardY, 151, 85);
            tw.owner = i < themeList.size() ? themeList.get(i) : K_THEME_ADD;
            tw.order = i;
            out.add(tw);

            maxBottom = Math.max(maxBottom, cardY + 85 + 6);
         }

         this.contentH = maxBottom - areaTop;
      } else if (selectedCategory != null) {
         int areaX = this.contentX();
         int areaW = this.contentW();
         int areaTop = this.listTop() - Math.round(this.scrollOffset);
         boolean searching = !this.searchQuery.isEmpty();
         int gap = 10;
         int cols = 3;
         int cardW = (areaW - gap * (cols - 1)) / cols;
         int headH = 22;

         List<Module> mods = new ArrayList<>();
         for (Module m : this.mm().getModules()) {
            if (m.isHidden() || !(searching || m.getCategory() == selectedCategory) || !this.matchesSearch(m)) continue;

            mods.add(m);
         }

         int[] colBottom = new int[cols];
         for (int i = 0; i < cols; i++) colBottom[i] = areaTop;

         for (int mi = 0; mi < mods.size(); mi++) {
            Module m = mods.get(mi);
            int c = 0;
            for (int i = 1; i < cols; i++) {
               if (colBottom[i] < colBottom[c]) c = i;
            }
            int cx = areaX + c * (cardW + gap);
            int cy = colBottom[c];
            int sy = cy + headH + 4;
            List<W> tmp = new ArrayList<>();
            int by = this.layoutModuleSettings(tmp, m, cx + 8, cardW - 16, sy);
            int cardBottom = by + 6;
            W card = new W(K_CARD, m, cx, cy, cardW, cardBottom - cy);
            W hdr = new W(K_MODULE_ROW, m, cx, cy, cardW, headH);
            out.add(card);
            out.add(hdr);
            out.addAll(tmp);
            for (W part : List.of(card, hdr)) {
               part.owner = m;
               part.order = mi;
            }
            for (W part : tmp) {
               part.owner = m;
               part.order = mi;
            }
            colBottom[c] = cardBottom + gap;
         }

         int maxBottom = areaTop;
         for (int cb : colBottom) maxBottom = Math.max(maxBottom, cb);
         this.contentH = Math.max(0, maxBottom - areaTop - gap);
      }

      List<ModuleCategory> cats = new ArrayList<>();

      for (ModuleCategory c : ModuleCategory.values()) {
         if (c == ModuleCategory.THEMES || !this.hasModules(c)) continue;

         cats.add(c);
      }

      for (int i = 0; i < cats.size(); i++) {
         int[] row = this.sidebarCategoryRect(i, cats.size());
         W w = new W(K_TAB, cats.get(i), row[0], row[1], row[2], row[3]);
         w.sel = !this.profileSelected && cats.get(i) == selectedCategory;
         out.add(w);
      }

      return out;
   }

   private static int fadeAlpha(int argb, float k) {
      int a = Math.round((argb >>> 24 & 0xFF) * clamp01(k));
      return a << 24 | argb & 0xFFFFFF;
   }

   private static float clamp01(float x) {
      return x < 0.0F ? 0.0F : Math.min(x, 1.0F);
   }

   private static List<String> wrapText(String text, float size, int maxW) {
      List<String> lines = new ArrayList<>();
      StringBuilder cur = new StringBuilder();

      for (String word : text.split(" ")) {
         String trial = cur.length() == 0 ? word : cur + " " + word;
         if (cur.length() > 0 && bookW(trial, size) > maxW) {
            lines.add(cur.toString());
            cur = new StringBuilder(word);
         } else {
            cur = new StringBuilder(trial);
         }
      }

      if (cur.length() > 0) {
         lines.add(cur.toString());
      }

      if (lines.isEmpty()) {
         lines.add("");
      }

      return lines;
   }

   private static void drawArrow(GuiGraphicsExtractor ctx, float cx, float cy, float rot, int color) {
      drawArrow(ctx, cx, cy, rot, color, 1.0F);
   }

   private static void drawArrow(GuiGraphicsExtractor ctx, float cx, float cy, float rot, int color, float scale) {
      int w = Math.max(1, Math.round(6.0F * scale));
      int h = Math.max(1, Math.round(6.0F * scale));
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().rotate(rot);
      Render2DUtil.chevron(ctx, -w / 2, -h / 2, w, h, false, 1.5F, color);
      ctx.pose().popMatrix();
   }

   private static int dropItemCount(Setting s) {
      if (s instanceof ModeSetting md) {
         return md.getModes().size();
      }

      return s instanceof MultiModeSetting mm ? mm.getBoolSettings().size() : 0;
   }

   private static String dropItemLabel(Setting s, int i) {
      return Translations.text(s instanceof ModeSetting md ? md.getModes().get(i) : ((MultiModeSetting)s).getBoolSettings().get(i).getName());
   }

   private static boolean dropItemSelected(Setting s, int i) {
      return s instanceof ModeSetting md ? md.is(md.getModes().get(i)) : ((MultiModeSetting)s).getBoolSettings().get(i).isEnabled();
   }

   private void applyDropItem(Setting s, int i) {
      if (s instanceof ModeSetting md) {
         md.setCurrentMode(md.getModes().get(i));
         this.openDrop = null;
      } else if (s instanceof MultiModeSetting mm) {
         mm.getBoolSettings().get(i).toggle();
      }
   }

   private int[] dropFieldGeom(W head) {
      int labelW = (int)Math.ceil(Math.min(bookW(head.text, 8.0F), (float)head.w * 0.55F));
      int avail = head.w - labelW - 6;
      String val = head.mode == null ? "" : head.mode;
      int desired = 7 + (int)Math.ceil(bookW(val, 8.0F)) + 6 + 12 + 2;
      int cap = Math.min(avail, Math.round(head.w * 0.7F));
      int fw = Math.min(Math.max(28, head.w - 30), Math.max(46, Math.min(desired, cap)));
      return new int[]{head.x + head.w - fw, fw};
   }

   private int[] popupBox(W head, int n) {
      int[] fg = this.dropFieldGeom(head);
      int fieldTop = head.y + (head.h - 14) / 2;
      int fullH = Math.min(n * 12, Math.max(12, (this.height - 12) / 12 * 12));
      int py = fieldTop + 14;
      int dir = 1;
      int contentBot = this.height - 6;
      int clipTop = 6;
      if (py + fullH > contentBot && fieldTop - fullH >= clipTop) {
         py = fieldTop - fullH;
         dir = -1;
      }

      py = Math.max(clipTop, Math.min(py, contentBot - fullH));
      return new int[]{fg[0], py, fg[1], fullH, dir};
   }

   private void renderPopup(GuiGraphicsExtractor ctx, List<W> layout) {
      int contentBot = this.height - 6;

      for (Entry<Setting, Float> e : this.dropAnim.entrySet()) {
         float anim = e.getValue();
         if (!(anim < 0.01F)) {
            Setting s = e.getKey();
            W head = null;

            for (W w : layout) {
               if (w.kind != K_DROP_HEAD || w.target != s) continue;

               head = w;
               break;
            }

            if (head != null) {
               int n = dropItemCount(s);
               if (n != 0) {
                  int[] b = this.popupBox(head, n);
                  int px = b[0];
                  int py = b[1];
                  int pw = b[2];
                  int fullH = b[3];
                  boolean up = b[4] < 0;
                  int clipTop = 6;
                  int animH = Math.max(1, Math.round(fullH * anim));
                  int top;
                  int bottom;
                  if (up) {
                     top = py + fullH - animH;
                     bottom = py + fullH;
                  } else {
                     top = py;
                     bottom = py + animH;
                  }

                  top = Math.max(top, clipTop);
                  bottom = Math.min(bottom, contentBot);
                  if (bottom > top) {
                     ctx.enableScissor(px, top, px + pw, bottom);
                     int drawH = bottom - py;
                     Render2DUtil.softShadow(ctx, px, py, pw, drawH, 4, 6.0F, 1.0F);
                     Render2DUtil.roundedRect(ctx, px, py, pw, drawH, 3.0F, C_MODE_SEL);

                     for (int i = 0; i < n; i++) {
                        int iy = py + (i - (s == this.openDrop ? this.dropScroll : 0)) * 12;
                        if (iy + 12 <= top || iy >= bottom) continue;
                        boolean sel = dropItemSelected(s, i);
                        String key = System.identityHashCode(s) + ":" + i;
                        float sl = expLerp(this.dropItemSlide.getOrDefault(key, sel ? 1.0F : 0.0F), sel ? 1.0F : 0.0F, 16.0F, this.frameDt);
                        this.dropItemSlide.put(key, sl);
                        int visIdx = i - (s == this.openDrop ? this.dropScroll : 0);
                        float itemE = easeOutCubic(clamp01((anim - Math.min(Math.max(0, visIdx), 8) * 0.04F) / 0.6F));
                        float textX = px + 3 + sl * 8.0F + (1.0F - itemE) * 8.0F;
                        if (sl > 0.02F) {
                           drawArrow(ctx, textX - 4.0F, iy + 6.0F, (float) (-Math.PI / 2), Math.round(255.0F * sl * itemE) << 24 | 0xFFFFFF, 0.65F);
                        }

                        regText(
                           ctx,
                           truncateToWidth(dropItemLabel(s, i), 8.0F, px + pw - 3 - textX),
                           textX,
                           iy + 3.2F,
                           8.0F,
                           fadeAlpha(Render2DUtil.lerpColor(C_UID, C_TEXT, sl), itemE)
                        );
                        if (i < n - 1) {
                           Render2DUtil.rect(ctx, px + 3, iy + 12, pw - 6, 1, hairline());
                        }
                     }

                     ctx.disableScissor();
                     int fieldTop = head.y + (head.h - 14) / 2;
                     int oTop = up ? Math.max(py, clipTop) : Math.max(fieldTop, clipTop);
                     int oBot = up ? Math.min(fieldTop + 14, contentBot) : bottom;
                     if (oBot > oTop) {
                        ctx.enableScissor(px, oTop, px + pw, oBot);
                        Render2DUtil.roundedOutline(
                           ctx, px, up ? py : fieldTop, pw, (up ? fieldTop + 14 : bottom) - (up ? py : fieldTop), 3.0F, outlineThickness(), outlineColor()
                        );
                        ctx.disableScissor();
                     }
                  }
               }
            }
         }
      }
   }

   private static String multiSummary(MultiModeSetting mm) {
      List<String> on = multiSelected(mm);
      if (on.isEmpty()) {
         return Translations.text("Не выбрано");
      }

      if (on.size() == mm.getBoolSettings().size()) {
         return Translations.text("Все");
      }

      return on.size() <= 2 ? String.join(", ", on) : on.get(0) + ", " + on.get(1);
   }

   private static List<String> multiSelected(MultiModeSetting mm) {
      List<String> on = new ArrayList<>();

      for (BooleanSetting b : mm.getBoolSettings()) {
         if (!b.isEnabled()) continue;

         on.add(Translations.text(b.getName()));
      }

      return on;
   }

   private static String truncateToWidth(String s, float fs, float maxW) {
      if (maxW <= 0.0F) {
         return "";
      }

      if (bookW(s, fs) <= maxW) {
         return s;
      }

      float ellW = bookW("...", fs);
      if (ellW > maxW) return "";
      int n = s.length();

      while (n > 0 && bookW(s.substring(0, n), fs) + ellW > maxW) {
         n--;
      }

      return s.substring(0, n) + "...";
   }

   private static W tagDetail(W w) {
      w.detail = false;
      return w;
   }

   private int layoutModuleSettings(List<W> out, Module m, int areaX, int areaW, int y) {
      List<Setting> vis = new ArrayList<>();

      for (Setting s : m.getSettings()) {
         Supplier<Boolean> hc = s.getHideCondition();
         if (hc == null || !Boolean.TRUE.equals(hc.get())) {
            vis.add(s);
         }
      }

      LinkedHashMap<String, List<Setting>> sections = new LinkedHashMap<>();

      for (Setting sx : vis) {
         sections.computeIfAbsent(sx.getGroup() == null ? "" : sx.getGroup(), k -> new ArrayList<>()).add(sx);
      }

      int gap = 7;
      boolean firstSection = true;

      for (Entry<String, List<Setting>> section : sections.entrySet()) {
         List<Setting> items = section.getValue();
         items.sort((a, b) -> Integer.compare(settingOrder(a), settingOrder(b)));
         if (!section.getKey().isEmpty()) {
            if (!firstSection) {
               y += 4;
            }

            W head = tagDetail(new W(K_SECTION, null, areaX, y, areaW, 11));
            head.text = Translations.text(section.getKey());
            out.add(head);
            y += 15;
         }

         firstSection = false;

         for (Setting sx : items) {
            int h = this.addDetailSetting(out, sx, areaX, y, areaW);
            y += h + gap;
         }
      }

      return y;
   }

   private static int settingOrder(Setting s) {
      if (s instanceof ModeSetting || s instanceof MultiModeSetting) {
         return 0;
      }

      if (s instanceof SliderSetting) {
         return 1;
      }

      return s instanceof BooleanSetting ? 2 : 3;
   }

   private int addDetailSetting(List<W> out, Setting s, int x, int y, int w) {
      if (s instanceof BooleanSetting b) {
         out.add(tagDetail(new W(K_BOOL, b, x, y, w, 14)));
         return 14;
      }

      if (s instanceof ButtonSetting bt) {
         out.add(tagDetail(new W(K_BUTTON, bt, x, y, w, 16)));
         return 16;
      }

      if (s instanceof ModeSetting md) {
         W head = tagDetail(new W(K_DROP_HEAD, md, x, y, w, 14));
         head.text = Translations.text(md.getName());
         head.mode = Translations.text(md.getCurrentMode());
         out.add(head);
         this.dropAnim.computeIfAbsent(md, k -> md == this.openDrop ? 1.0F : 0.0F);
         return 14;
      }

      if (s instanceof MultiModeSetting mmm) {
         W head = tagDetail(new W(K_DROP_HEAD, mmm, x, y, w, 14));
         head.text = Translations.text(mmm.getName());
         head.mode = multiSummary(mmm);
         out.add(head);
         this.dropAnim.computeIfAbsent(mmm, k -> mmm == this.openDrop ? 1.0F : 0.0F);
         return 14;
      }

      if (s instanceof BindSetting bs) {
         W lbl = tagDetail(new W(K_LABEL, bs, x, y, w, 14));
         lbl.text = Translations.text(bs.getName());
         out.add(lbl);
         int bw = bindW(bs.getKey());
         out.add(tagDetail(new W(K_BIND, bs, x + w - bw, y, bw, 14)));
         return 14;
      }

      if (s instanceof StringSetting ss) {
         out.add(tagDetail(new W(K_STRING, ss, x, y, w, 14)));
         return 14;
      }

      if (s instanceof SliderSetting sl) {
         W sw = tagDetail(new W(K_SLIDER, sl, x, y, w, 18));
         sw.trackX = x;
         sw.trackW = Math.max(4, w - this.sliderPillW(sl) - 0 - 3);
         out.add(sw);
         return 18;
      }

      if (s instanceof ColorSetting cs) {
         out.add(tagDetail(new W(K_COLOR, cs, x, y, w, 14)));
         return 14;
      }

      return 0;
   }

   private static String fmt(float v) {
      return String.format(Locale.US, "%.1f", v);
   }

   private static String keyName(int code) {
      return KeyNameUtil.displayName(code);
   }

   private static int bindW(int key) {
      return key == -1 ? 12 : (int)Math.ceil(bookW(keyName(key), 8.0F)) + 4;
   }

   private static void setKeyOn(Object target, int key) {
      if (target instanceof Module m) {
         m.setKey(key);
      } else if (target instanceof BindSetting b) {
         b.setKey(key);
      } else if (target instanceof BooleanSetting b) {
         b.setKey(key);
      }
   }

   private static int keyOf(Object target) {
      if (target instanceof Module m) {
         return m.getKey();
      }

      if (target instanceof BindSetting b) {
         return b.getKey();
      }

      return target instanceof BooleanSetting b ? b.getKey() : -1;
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      float prevOpacity = Render2DUtil.opacity();
      try {
         this.renderGui(ctx, mouseX, mouseY);
      } finally {
         Render2DUtil.setOpacity(prevOpacity);
      }
   }

   private void renderGui(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
      C_ACCENT = ThemeManager.accent();
      C_ACCENT_FILL = darkenColor(C_ACCENT, 0.55F);
      C_CARD_ON = ThemeManager.accentAlpha(32);
      this.mxCache = mouseX;
      this.myCache = mouseY;
      long now = System.nanoTime();
      this.frameDt = this.animLastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.animLastNanos) / 1.0E9F);
      this.animLastNanos = now;
      this.openProgress = clamp01(this.openProgress + (this.closing ? -this.frameDt / CLOSE_TIME : this.frameDt / OPEN_TIME));
      if (this.closing && this.openProgress <= 0.0F) {
         this.closeQueued = true;
         return;
      }
      this.profileAge = Math.min(1.0F, this.profileAge + this.frameDt);
      this.profileCardHover = expLerp(this.profileCardHover,
         inside(mouseX, mouseY, this.profileCardRect()) ? 1 : 0, 13, this.frameDt);
      this.profileCardSelected = expLerp(this.profileCardSelected, this.profileSelected ? 1 : 0, 11, this.frameDt);
      this.profileThemesHover = expLerp(this.profileThemesHover,
         this.profileSelected && inside(mouseX, mouseY, this.profileThemesRect()) ? 1 : 0, 13, this.frameDt);
      this.profileLanguageHover = expLerp(this.profileLanguageHover,
         this.profileSelected && inside(mouseX, mouseY, this.profileLanguageRect()) ? 1 : 0, 13, this.frameDt);
      this.searchFocusAnim = expLerp(this.searchFocusAnim, this.searchFocused ? 1 : 0, 12, this.frameDt);
      this.advanceSearchTyping();
      this.searchAnim = 1.0F;
      this.windowAnim = easeOutCubic(this.openProgress);
      this.windowScale = this.closing ? 0.94F + 0.06F * this.windowAnim : 0.9F + 0.1F * easeOutBack(this.openProgress);
      int dimAlpha = Math.round(120 * this.windowAnim);
      Render2DUtil.rect(ctx, 0, 0, this.width, this.height, (dimAlpha << 24) | 0x000000);
      Render2DUtil.setOpacity(this.windowAnim);
      float baseOpacity = this.windowAnim;
      float winCx = this.winX() + this.winW()/2.0F;
      float winCy = this.winY() + this.winH()/2.0F;
      ctx.pose().pushMatrix();
      ctx.pose().translate(0.0F, (1.0F - this.windowAnim) * 10.0F);
      ctx.pose().translate(winCx, winCy);
      ctx.pose().scale(this.windowScale, this.windowScale);
      ctx.pose().translate(-winCx, -winCy);
      Render2DUtil.softShadow(ctx, this.winX(), this.winY(), this.winW(), this.winH(), WINDOW_RADIUS_PIZZA, 18.0F, 1.4F * this.windowAnim);
      Render2DUtil.roundedRect(ctx, this.winX(), this.winY(), this.winW(), this.winH(), WINDOW_RADIUS_PIZZA, 0xFF0F0F0F);
      Render2DUtil.roundedOutline(ctx, this.winX(), this.winY(), this.winW(), this.winH(), WINDOW_RADIUS_PIZZA, 0.6F, 0x1EFFFFFF);
      this.glowPhase = this.glowPhase + this.frameDt * 3.2F;
      if (this.glowPhase > (float) (Math.PI * 2)) {
         this.glowPhase -= (float) (Math.PI * 2);
      }

      this.glowPulse = 0.7F + 0.3F * (0.5F + 0.5F * (float)Math.sin(this.glowPhase));

      for (Entry<SliderSetting, Float> e : this.sliderAnim.entrySet()) {
         e.setValue(expLerp(e.getValue(), this.sliderPillWTarget(e.getKey()), 14.0F, this.frameDt));
      }

      for (Entry<Setting, Float> e : this.dropAnim.entrySet()) {
         e.setValue(expLerp(e.getValue(), e.getKey() == this.openDrop ? 1.0F : 0.0F, 12.0F, this.frameDt));
      }

      for (Entry<BooleanSetting, Float> e : this.boolAnim.entrySet()) {
         e.setValue(expLerp(e.getValue(), e.getKey().isEnabled() ? 1.0F : 0.0F, 16.0F, this.frameDt));
      }

      for (Entry<Module, Float> e : this.moduleAnim.entrySet()) {
         e.setValue(expLerp(e.getValue(), e.getKey().isEnabled() ? 1.0F : 0.0F, 14.0F, this.frameDt));
      }

      for (ModuleCategory c : ModuleCategory.values()) {
         float tgt = !this.profileSelected && c == selectedCategory ? 1.0F : 0.0F;
         this.tabSelAnim.put(c, expLerp(this.tabSelAnim.getOrDefault(c, tgt), tgt, 12.0F, this.frameDt));
      }

      for (Entry<Module, Float> e : this.moduleSelAnim.entrySet()) {
         e.setValue(expLerp(e.getValue(), e.getKey() == selectedModule ? 1.0F : 0.0F, 14.0F, this.frameDt));
      }

      if (selectedModule != null) {
         this.moduleSelAnim.computeIfAbsent(selectedModule, k -> 0.0F);
      }
      for (Entry<Module, Float> e : this.cardEnter.entrySet()) {
         e.setValue(expLerp(e.getValue(), 1.0F, 10.0F, this.frameDt));
      }

      if (this.autoScroll) {
         if (this.scrollbar() == null) {
            this.autoScroll = false;
         } else {
            float d = mouseY - this.autoScrollAnchorY;
            if (Math.abs(d) > 6.0F) {
               this.scrollTarget = this.scrollTarget + Math.signum(d) * (Math.abs(d) - 6.0F) * 9.0F * this.frameDt;
            }
         }
      }

      this.scrollTarget = this.clampScroll(this.scrollTarget);
      this.scrollOffset = expLerp(this.scrollOffset, this.scrollTarget, 16.0F, this.frameDt);
      this.detailScrollTarget = this.clampDetailScroll(this.detailScrollTarget);
      this.detailScroll = expLerp(this.detailScroll, this.detailScrollTarget, 16.0F, this.frameDt);
      List<W> layout = this.buildLayout();
      float clampedScroll = this.clampScroll(this.scrollOffset);
      if (clampedScroll != this.scrollOffset) {
         this.scrollOffset = clampedScroll;
         this.scrollTarget = this.clampScroll(this.scrollTarget);
         layout = this.buildLayout();
      }
      this.lastLayout = layout;
      this.advanceCardAges(layout);
      int clipTop = this.listTop() - 1;
      int clipBot = this.winY() + this.winH() - 7;
      int gpad = 4;
      int esL = this.contentX();
      int esT = this.listTop();
      int esR = Math.min(this.winX() + this.winW() - 1, this.winX() + this.winW() - 7 + 2 + gpad);
      int esB = clipBot;
      ctx.enableScissor(esL, esT, esR, esB);
      this.categoryAnim = expLerp(this.categoryAnim, 1.0F, 10.0F, this.frameDt);
      float catSlide = 0.0F;
      ctx.pose().pushMatrix();
      ctx.pose().translate(catSlide, 0.0F);

      for (W w : layout) {
         if (w.kind == K_TAB || w.detail) continue;
         if (w.owner == null) {
            this.drawW(ctx, w);
            continue;
         }
         float enter = this.cardEnterEase(w.owner);
         float settle = 1.0F - easeOutBack(clamp01(this.cardAge.getOrDefault(w.owner, 1.0F) / CARD_ENTER_TIME));
         float lift = w.owner instanceof Module mod ? this.cardHover.getOrDefault(mod, 0.0F) : 0.0F;
         float[] slide = this.cardSlide.get(w.owner);
         float slideX = slide == null ? 0.0F : slide[0] - slide[2];
         float slideY = slide == null ? 0.0F : slide[1] - slide[3];
         Render2DUtil.setOpacity(baseOpacity * enter);
         ctx.pose().pushMatrix();
         ctx.pose().translate(this.categorySlideDir * settle * 16.0F + slideX, settle * 14.0F - lift * 1.5F + slideY);
         this.drawW(ctx, w);
         ctx.pose().popMatrix();
      }
      Render2DUtil.setOpacity(baseOpacity);
      this.drawRipples(ctx);
      if (this.profileSelected) this.renderProfile(ctx);

      ctx.pose().popMatrix();
      ctx.disableScissor();

      float maxScroll = Math.max(0.0F, (float)(this.contentH - this.viewportH()));
      this.topFadeAnim = expLerp(this.topFadeAnim, this.scrollOffset > 1.0F ? 1.0F : 0.0F, 12.0F, this.frameDt);
      this.bottomFadeAnim = expLerp(this.bottomFadeAnim, this.scrollOffset < maxScroll - 1.0F ? 1.0F : 0.0F, 12.0F, this.frameDt);
      int fadeMaxH = 36;
      int fadeW = this.isMasterDetail() ? this.masterX2() + this.masterW2() + gpad - esL : esR - esL;
      int bgTrans = 855309;
      int botH = Math.round(fadeMaxH * this.bottomFadeAnim);
      if (botH > 0) {
         Render2DUtil.gradientRectV(ctx, esL, esB - botH, fadeW, botH, bgTrans, C_WINDOW, 2.0F);
      }

      int topH = Math.round(fadeMaxH * this.topFadeAnim);
      if (topH > 0) {
         Render2DUtil.gradientRectVTop(ctx, esL, esT, fadeW, topH, C_WINDOW, bgTrans, 2.0F);
      }

      this.renderHeader(ctx);
      this.noResultsAnim = expLerp(this.noResultsAnim, !this.profileSelected && this.contentH == 0 ? 1.0F : 0.0F, 10.0F, this.frameDt);
      if (this.noResultsAnim > 0.01F) {
         String empty = this.searchQuery.isEmpty() ? Translations.text("Здесь пусто") : Translations.tr("search.empty", this.searchQuery);
         float ew = regW(empty, 9.0F);
         float ex = this.contentX() + (this.contentW() - ew) / 2.0F;
         float ey = this.listTop() + 30 + (1.0F - easeOutCubic(this.noResultsAnim)) * 8.0F;
         regText(ctx, empty, ex, ey, 9.0F, fadeAlpha(C_TEXT_OFF, this.noResultsAnim));
      }
      this.renderDetail(ctx, layout);
      int nvL = Integer.MAX_VALUE;
      int nvR = Integer.MIN_VALUE;
      int nvT = 0;
      int nvB = 0;

      for (W wx : layout) {
         if (wx.kind != K_TAB) continue;

         nvL = Math.min(nvL, wx.x);
         nvR = Math.max(nvR, wx.x + wx.w);
         nvT = wx.y;
         nvB = wx.y + wx.h;
      }

      if (nvR > nvL) {
         ctx.enableScissor(this.winX(), this.winY(), this.winX() + this.winW(), this.winY() + this.winH());
      }

      this.drawTabPill(ctx, layout);

      for (W wxx : layout) {
         if (wxx.kind != K_TAB) continue;

         this.drawW(ctx, wxx);
      }

      if (nvR > nvL) {
         ctx.disableScissor();
      }

      int[] sb = this.scrollbar();
      if (sb != null) {
         Render2DUtil.roundedRect(ctx, sb[0], sb[2], sb[1], sb[3], sb[1] / 2.0F, 0x14FFFFFF);
         Render2DUtil.roundedRect(ctx, sb[0], sb[4], sb[1], sb[5], sb[1] / 2.0F, 0x80FFFFFF);
      }

      if (this.autoScroll) {
         int cx = this.autoScrollAnchorX;
         int cy = this.autoScrollAnchorY;
         int r = 11;
         Render2DUtil.roundedRect(ctx, cx - r, cy - r, 2 * r, 2 * r, (float)r, 0x66000000);
         Render2DUtil.roundedOutline(ctx, cx - r, cy - r, 2 * r, 2 * r, r, 1.0F, 0xAAFFFFFF);
         Render2DUtil.chevron(ctx, cx - 4, cy - 7, 8, 4, true, 1.0F, 0xE6FFFFFF);
         Render2DUtil.chevron(ctx, cx - 4, cy + 3, 8, 4, false, 1.0F, 0xE6FFFFFF);
         Render2DUtil.roundedRect(ctx, cx - 1, cy - 1, 2, 2, 1.0F, 0xE6FFFFFF);
      }
      ctx.pose().popMatrix();
      this.renderFloatingSearch(ctx);
      this.renderPopup(ctx, layout);
      this.bindPopup.render(ctx, mouseX, mouseY);
      this.colorPicker.render(ctx, mouseX, mouseY);
   }

   private void renderFloatingSearch(GuiGraphicsExtractor ctx) {
      int[] r = floatingSearchRect();
      int x = r[0]; int y = r[1]; int w = r[2]; int h = r[3];
      float offY = 0.0F;
      int alpha = Math.round(255 * this.searchAnim);
      int bg = (Math.round(220 * this.searchAnim) << 24) | 0x0A0A0A;
      int tx = x;
      int ty = Math.round(y + offY);
      float focus = this.searchFocusAnim;
      Render2DUtil.glow(ctx, tx, ty, w, h, Math.round(WINDOW_RADIUS_PIZZA), (Math.round(90*this.searchAnim)<<24) | 0x000000, 8);
      if (focus > 0.01F) {
         Accent.glow(ctx, tx, ty, w, h, 8.0F, focus * 0.55F, 6);
      }
      Render2DUtil.roundedRect(ctx, tx, ty, w, h, 8.0F, bg);
      Render2DUtil.roundedOutline(ctx, tx, ty, w, h, 8.0F, 0.8F,
         Render2DUtil.lerpColor(0x40404040, Accent.color(0.85F), focus));
      float lineW = (w - 24) * focus;
      if (lineW > 1.0F) {
         Render2DUtil.gradientRectFull(ctx, Math.round(tx + (w - lineW) / 2.0F), ty + h - 2, Math.round(lineW), 1,
            Accent.color(0.0F), Accent.color(0.9F * focus), 0.5F);
      }
      float iconScale = 1.0F + 0.28F * this.searchTypingPulse;
      float iconSize = 10.0F * iconScale;
      Render2DUtil.roundedTexture(ctx, SEARCH_TEXTURE, tx + 13 - iconSize / 2.0F, ty + h / 2.0F - iconSize / 2.0F,
         iconSize, iconSize, 0, true, Render2DUtil.lerpColor(0xFF929292, Render2DUtil.lerpColor(C_TEXT, C_ACCENT, 0.55F),
            Math.max(focus, this.searchTypingPulse * 0.7F)));
      boolean hasQuery = !this.searchQuery.isEmpty();
      int results = hasQuery ? this.searchResultCount() : 0;
      this.searchResultsShown = expLerp(this.searchResultsShown, results, 14.0F, this.frameDt);
      String badge = hasQuery ? Translations.tr("search.results", Math.round(this.searchResultsShown)) : "";
      float badgeW = hasQuery ? bookW(badge, 7.0F) + 10.0F : 0.0F;
      int textX = tx + 25;
      int textRight = Math.round(tx + w - 10 - badgeW);
      float textY = ty + 7.0F;
      if (hasQuery) {
         int badgeColor = results == 0 ? 0xFFFF6B6B : C_UID;
         float bx = tx + w - 8 - badgeW + 6.0F;
         Render2DUtil.roundedRect(ctx, bx - 4.0F, ty + 4.0F, badgeW - 2.0F, h - 8.0F, 4.0F, results == 0 ? 0x22FF4040 : 0x14FFFFFF);
         bookText(ctx, badge, bx, ty + (h - 4.9F) / 2.0F, 7.0F, badgeColor);
      }
      ctx.enableScissor(textX, ty + 3, textRight, ty + h - 3);
      if (this.searchQuery.isEmpty()) {
         regText(ctx, Translations.text("Поиск модулей..."), textX + focus * 3, textY, 8,
            fadeAlpha(C_UID, 1 - focus * 0.6F));
      } else {
         float penX = textX - this.searchTextScroll;
         if (this.searchEdit.hasSelection()) {
            float sx0 = penX + regW(this.searchQuery.substring(0, this.searchEdit.selStart()), 8);
            float sx1 = penX + regW(this.searchQuery.substring(0, this.searchEdit.selEnd()), 8);
            Render2DUtil.roundedRect(ctx, sx0 - 0.5F, ty + 5, sx1 - sx0 + 1.0F, 12, 2.0F, Accent.color(0.45F));
         }
         int[] codepoints = this.searchQuery.codePoints().toArray();
         for (int i = 0; i < codepoints.length; i++) {
            String glyph = new String(Character.toChars(codepoints[i]));
            float age = i < this.searchGlyphAges.size() ? this.searchGlyphAges.get(i) : 1;
            float t = clamp01(age / 0.16F);
            float reveal = 1 - (1 - t) * (1 - t) * (1 - t);
            regText(ctx, glyph, penX, textY + (1 - reveal) * 3, 8, fadeAlpha(C_TEXT, reveal));
            penX += regW(glyph, 8);
         }
      }
      float blink = this.searchCaretAge < 0.65F ? 1 :
         0.25F + 0.75F * (0.5F + 0.5F * (float)Math.cos((this.searchCaretAge - 0.65F) * Math.PI * 2 / 1.1F));
      Render2DUtil.roundedRect(ctx, textX + this.searchCaretX - this.searchTextScroll + 1,
         ty + 6, 1, 10, 0.5F, fadeAlpha(C_TEXT, blink * this.searchFocusAnim));
      ctx.disableScissor();
   }

   private void advanceSearchTyping() {
      int[] current = this.searchQuery.codePoints().toArray();
      if (!this.searchQuery.equals(this.animatedSearchQuery)) {
         int[] previous = this.animatedSearchQuery.codePoints().toArray();
         int common = 0;
         while (common < current.length && common < previous.length && current[common] == previous[common]) common++;
         while (this.searchGlyphAges.size() > common) this.searchGlyphAges.remove(this.searchGlyphAges.size() - 1);
         for (int i = common; i < current.length; i++) this.searchGlyphAges.add(-(i - common) * 0.018F);
         this.animatedSearchQuery = this.searchQuery;
         this.searchCaretAge = 0;
         this.searchTypingPulse = 1;
      }
      for (int i = 0; i < this.searchGlyphAges.size(); i++) {
         this.searchGlyphAges.set(i, Math.min(1, this.searchGlyphAges.get(i) + this.frameDt));
      }
      this.searchCaretAge += this.frameDt;
      this.searchTypingPulse = expLerp(this.searchTypingPulse, 0, 10, this.frameDt);
      float textWidth = regW(this.searchQuery, 8);
      int caret = Math.min(this.searchEdit.caret(), this.searchQuery.length());
      float caretWidth = regW(this.searchQuery.substring(0, caret), 8);
      this.searchCaretX = expLerp(this.searchCaretX, caretWidth, 24, this.frameDt);
      float visible = this.floatingSearchW() - 35 - (this.searchQuery.isEmpty() ? 0 : 60);
      float overflow = Math.max(0, Math.min(textWidth + 4 - visible, caretWidth + 8 - visible));
      this.searchTextScroll = expLerp(this.searchTextScroll, Math.max(0, overflow), 20, this.frameDt);
   }

   private void renderHeader(GuiGraphicsExtractor ctx) {
      int hTop = this.winY() + 2;
      float hCy = hTop + 17.0F;
      Render2DUtil.crystal(ctx, this.winX() + WIN_PAD + 8 + 10, hCy, 18.0F, 0.0F, 0xFF3A3A3C, 0xFF2A2A2C);
      float tx = this.winX() + WIN_PAD + 8 + 24;
      regText(ctx, "PizzaDLC", tx, hTop + 10.2F, 11.0F, C_TEXT);
      regText(ctx, "v1.0", tx, hTop + 21.0F, 6.5F, C_V1);
      String nick = PROFILE_USER;
      String ini = nick.isEmpty() ? "?" : nick.substring(0, 1).toUpperCase(Locale.ROOT);
      float iniW = regW(ini, 7.0F);
      int sidebarX = this.sidebarX();
      int sbW = this.sidebarW();
      int cardY = this.winY() + this.winH() - WIN_PAD - 36;
      int cardH = 32;
      int lastRowBottom = this.sidebarRowsBottom();
      if (cardY < lastRowBottom + 4) {
         return;
      }

      float profileHighlight = Math.max(this.profileCardHover * 0.65F, this.profileCardSelected);
      Render2DUtil.roundedRect(ctx, sidebarX, cardY, sbW, cardH, 8.0F,
         Render2DUtil.lerpColor(C_USERCARD, C_MODE_SEL, profileHighlight));
      Render2DUtil.roundedOutline(ctx, sidebarX, cardY, sbW, cardH, 8.0F, 0.6F,
         Render2DUtil.lerpColor(0x20404040, 0x708A8A8A, profileHighlight));
      int av2 = 22;
      int av2X = sidebarX + 6;
      int av2Y = cardY + (cardH - av2)/2;
      Render2DUtil.gradientRectDiag(ctx, av2X, av2Y, av2, av2,
         Render2DUtil.lerpColor(0xFF3A3A3C, 0xFF555559, profileHighlight), 0xFF2A2A2C, av2/2.0F);
      regText(ctx, ini, av2X + (av2 - iniW)/2.0F, av2Y + (av2 - 4.9F)/2.0F, 7.0F, C_TEXT);
      int nickX = av2X + av2 + 7;
      float maxNickW = Math.max(0.0F, (float)(sidebarX + sbW - 6 - nickX));
      String nickTxt = nick;
      if (bookW(nickTxt, 8.0F) > maxNickW) {
         nickTxt = truncateToWidth(nickTxt, 8.0F, maxNickW);
      }

      bookText(ctx, nickTxt, nickX, cardY + 9, 8.0F, C_TEXT);
      regText(ctx, Translations.text(PROFILE_ROLE), nickX, cardY + 18, 6.5F, C_UID);
   }

   private int sidebarRowsBottom() {
      int count = 0;

      for (ModuleCategory c : ModuleCategory.values()) {
         if (c == ModuleCategory.THEMES || !this.hasModules(c)) continue;

         count++;
      }

      if (count == 0) return this.winY() + 44;
      int[] last = this.sidebarCategoryRect(count - 1, count);
      return last[1] + last[3];
   }

   private int[] sidebarCategoryRect(int index, int count) {
      int top = this.winY() + 44;
      int bottom = this.profileCardRect()[1] - 8;
      int rowHeight = 24;
      int span = Math.max(0, bottom - top - rowHeight);
      int offset = count > 1 ? span * index / (count - 1) : 0;
      return new int[]{this.sidebarX(), top + offset, this.sidebarW(), rowHeight};
   }

   private int[] profileCardRect() {
      return new int[]{this.sidebarX(), this.winY() + this.winH() - WIN_PAD - 36, this.sidebarW(), 32};
   }

   private int[] profileRect() {
      return new int[]{this.contentX(), this.listTop() - Math.round(this.scrollOffset), this.contentW(), 200};
   }

   private int[] profileThemesRect() {
      int[] r = this.profileRect();
      return new int[]{r[0] + 14, r[1] + r[3] - 38, r[2] - 28, 24};
   }

   private int[] profileLanguageRect() {
      int[] r = this.profileRect();
      return new int[]{r[0] + 14, r[1] + 116, r[2] - 28, 32};
   }

   private static boolean inside(double x, double y, int[] r) {
      return x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3];
   }

   private void renderProfile(GuiGraphicsExtractor ctx) {
      int[] r = this.profileRect();
      int x = r[0], y = r[1], w = r[2];
      float panel = this.profileReveal(0);
      Render2DUtil.softShadow(ctx, x, y, w, r[3], 10, 6.0F, panel);
      Render2DUtil.roundedRect(ctx, x, y, w, r[3], 10, fadeAlpha(C_MOD, panel));
      Render2DUtil.roundedOutline(ctx, x, y, w, r[3], 10, 0.7F, fadeAlpha(0x30FFFFFF, panel));
      regText(ctx, Translations.tr("profile.title"), x + 14, y + 16 + (1 - panel) * 6, 12, fadeAlpha(C_TEXT, panel));
      Render2DUtil.rect(ctx, x + 14, y + 39, Math.round((w - 28) * panel), 1, fadeAlpha(0x16FFFFFF, panel));
      String[] labels = {Translations.tr("profile.user"), Translations.tr("profile.role"), Translations.tr("profile.expiry")};
      String[] values = {PROFILE_USER, Translations.text(PROFILE_ROLE), PROFILE_EXPIRY};
      for (int i = 0; i < labels.length; i++) {
         int rowY = y + 51 + i * 21;
         float reveal = this.profileReveal(0.055F + i * 0.045F);
         float textY = rowY + (1 - reveal) * 5;
         bookText(ctx, labels[i], x + 14, textY, 9, fadeAlpha(C_UID, reveal));
         String value = values[i];
         bookText(ctx, value, x + w - 14 - bookW(value, 9), textY, 9, fadeAlpha(C_TEXT, reveal));
      }
      int[] languageButton = this.profileLanguageRect();
      float languageReveal = this.profileReveal(.17F);
      Render2DUtil.roundedRect(ctx, languageButton[0], languageButton[1], languageButton[2], languageButton[3], 6,
         fadeAlpha(Render2DUtil.lerpColor(C_MODE_SEL, 0xFF353535, this.profileLanguageHover), languageReveal));
      Render2DUtil.roundedOutline(ctx, languageButton[0], languageButton[1], languageButton[2], languageButton[3], 6, .7F,
         Accent.color(this.profileLanguageHover * languageReveal * .65F));
      var language = TranslationManager.getInstance().language();
      LanguageSelectScreen.flag(ctx, language, languageButton[0] + 8, languageButton[1] + 8, 24, 16, languageReveal);
      bookText(ctx, Translations.tr("language.title"), languageButton[0] + 41, languageButton[1] + 6, 8, fadeAlpha(C_TEXT, languageReveal));
      bookText(ctx, language.nativeName(), languageButton[0] + 41, languageButton[1] + 20, 6.5F, fadeAlpha(C_UID, languageReveal));
      Render2DUtil.chevron(ctx, languageButton[0] + languageButton[2] - 16, languageButton[1] + 13, 6, 6, false, 1, fadeAlpha(C_UID, languageReveal));
      int[] button = this.profileThemesRect();
      float buttonReveal = this.profileReveal(0.19F);
      Render2DUtil.roundedRect(ctx, button[0], button[1], button[2], button[3], 5,
         fadeAlpha(Render2DUtil.lerpColor(C_MODE_SEL, 0xFF353535, this.profileThemesHover), buttonReveal));
      Render2DUtil.roundedOutline(ctx, button[0], button[1], button[2], button[3], 5, 0.6F,
         fadeAlpha(0x70FFFFFF, this.profileThemesHover * buttonReveal));
      String themes = Translations.tr("profile.themes");
      bookText(ctx, themes, button[0] + (button[2] - bookW(themes, 9)) / 2,
         button[1] + 8, 9, fadeAlpha(C_TEXT, buttonReveal));
   }

   private float profileReveal(float delay) {
      float t = clamp01((this.profileAge - delay) / 0.28F);
      return 1 - (float)Math.pow(1 - t, 3);
   }

   private void advanceCardAges(List<W> layout) {
      Set<Object> present = new HashSet<>();
      int scroll = Math.round(this.scrollOffset);
      float k = 1.0F - (float)Math.exp(-13.0F * this.frameDt);
      for (W w : layout) {
         if (w.owner == null || !present.add(w.owner)) continue;

         this.cardAge.putIfAbsent(w.owner, -Math.min(w.order, CARD_STAGGER_MAX) * CARD_STAGGER);
         float tx = w.x;
         float ty = w.y + scroll;
         float[] slide = this.cardSlide.get(w.owner);
         if (slide == null) {
            this.cardSlide.put(w.owner, new float[]{tx, ty, tx, ty});
            continue;
         }
         slide[2] = tx;
         slide[3] = ty;
         slide[0] += (tx - slide[0]) * k;
         slide[1] += (ty - slide[1]) * k;
         if (Math.abs(tx - slide[0]) < 0.05F && Math.abs(ty - slide[1]) < 0.05F) {
            slide[0] = tx;
            slide[1] = ty;
         }
      }
      this.cardSlide.keySet().retainAll(present);
      this.cardAge.keySet().retainAll(present);
      for (Entry<Object, Float> e : this.cardAge.entrySet()) {
         e.setValue(Math.min(10.0F, e.getValue() + this.frameDt));
      }
   }

   private float cardEnterEase(Object owner) {
      return easeOutCubic(clamp01(this.cardAge.getOrDefault(owner, 1.0F) / CARD_ENTER_TIME));
   }

   private void drawTabPill(GuiGraphicsExtractor ctx, List<W> layout) {
      W selTab = null;
      W anyTab = null;
      for (W w : layout) {
         if (w.kind != K_TAB) continue;
         anyTab = w;
         if (w.sel) selTab = w;
      }
      this.tabPillAlpha = expLerp(this.tabPillAlpha, selTab != null ? 1.0F : 0.0F, 12.0F, this.frameDt);
      if (selTab != null) {
         if (Float.isNaN(this.tabPillY)) {
            this.tabPillY = selTab.y;
            this.tabPillVel = 0.0F;
         }
         int steps = Math.max(1, (int)Math.ceil(this.frameDt / 0.004F));
         float h = this.frameDt / steps;
         for (int i = 0; i < steps; i++) {
            float force = (selTab.y - this.tabPillY) * PILL_STIFFNESS - this.tabPillVel * PILL_DAMPING;
            this.tabPillVel += force * h;
            this.tabPillY += this.tabPillVel * h;
         }
      }
      if (anyTab == null || Float.isNaN(this.tabPillY) || this.tabPillAlpha < 0.01F) return;
      float a = this.tabPillAlpha;
      float py = this.tabPillY;
      Render2DUtil.softShadow(ctx, anyTab.x, py, anyTab.w, anyTab.h, 11, 5.0F, a);
      Render2DUtil.roundedRect(ctx, anyTab.x, py, anyTab.w, anyTab.h, 11, fadeAlpha(Render2DUtil.lerpColor(C_WINDOW, C_MODE_SEL, 0.35F), a));
      Render2DUtil.roundedOutline(ctx, anyTab.x, Math.round(py), anyTab.w, anyTab.h, 11, 0.6F, fadeAlpha(0x50878787, a));
   }

   private void drawRowHover(GuiGraphicsExtractor ctx, W w) {
      if (w.target == null || w.kind == K_TAB || w.kind == K_CARD || w.kind == K_MODULE_ROW || w.kind == K_THEME || w.kind == K_SECTION || w.kind == K_BIND) return;
      boolean over = this.mxCache >= w.x - ROW_HOVER_PAD_X && this.mxCache < w.x + w.w + ROW_HOVER_PAD_X
         && this.myCache >= w.y - ROW_HOVER_PAD_Y && this.myCache < w.y + w.h + ROW_HOVER_PAD_Y
         && this.myCache >= this.listTop() && this.myCache < this.listTop() + this.viewportH();
      float t = expLerp(this.rowHoverAnim.getOrDefault(w.target, 0.0F), over ? 1.0F : 0.0F, 14.0F, this.frameDt);
      this.rowHoverAnim.put(w.target, t);
      if (t < 0.01F) return;
      float grow = 1.0F - t;
      int hx = Math.round(w.x - ROW_HOVER_PAD_X + grow * 2.0F);
      int hy = Math.round(w.y - ROW_HOVER_PAD_Y + grow * 1.5F);
      int hw = Math.round(w.w + ROW_HOVER_PAD_X * 2 - grow * 4.0F);
      int hh = Math.round(w.h + ROW_HOVER_PAD_Y * 2 - grow * 3.0F);
      Render2DUtil.roundedRect(ctx, hx, hy, hw, hh, 5.0F, Math.round(14.0F * t) << 24 | 0xFFFFFF);
      Render2DUtil.roundedOutline(ctx, hx, hy, hw, hh, 5.0F, 0.6F, Math.round(34.0F * t) << 24 | 0xFFFFFF);
   }

   private float listenPulse() {
      return 0.55F + 0.45F * (float)Math.sin(System.nanoTime() / 1.0E9 * Math.PI * 2.4);
   }

   private void drawRipples(GuiGraphicsExtractor ctx) {
      Iterator<float[]> it = this.ripples.iterator();
      while (it.hasNext()) {
         float[] r = it.next();
         r[2] += this.frameDt;
         float t = r[2] / RIPPLE_TIME;
         if (t >= 1.0F) {
            it.remove();
            continue;
         }
         float spread = easeOutCubic(t);
         float maxR = (float)Math.hypot(Math.max(r[0] - r[3], r[3] + r[5] - r[0]), Math.max(r[1] - r[4], r[4] + r[6] - r[1]));
         float rad = Math.max(1.0F, maxR * spread);
         float fade = (1.0F - t) * (1.0F - t);
         ctx.enableScissor(Math.round(r[3]), Math.round(r[4]), Math.round(r[3] + r[5]), Math.round(r[4] + r[6]));
         Render2DUtil.roundedRect(ctx, r[0] - rad, r[1] - rad, rad * 2.0F, rad * 2.0F, rad, Render2DUtil.lerpColor(0xFFFFFFFF, C_ACCENT, 0.35F) & 0xFFFFFF | Math.round(34.0F * fade) << 24);
         ctx.disableScissor();
      }
   }

   private void spawnRipple(double mx, double my) {
      for (W w : this.lastLayout) {
         if (w.kind != K_CARD && w.kind != K_THEME && w.kind != K_THEME_ADD) continue;
         if (mx < w.x || mx >= w.x + w.w || my < w.y || my >= w.y + w.h) continue;
         if (my < this.listTop() || my >= this.listTop() + this.viewportH()) return;
         this.ripples.add(new float[]{(float)mx, (float)my, 0.0F, w.x, w.y, w.w, w.h});
         if (this.ripples.size() > 8) this.ripples.remove(0);
         return;
      }
   }

   private static float easeOutCubic(float t) {
      float inv = 1.0F - clamp01(t);
      return 1.0F - inv * inv * inv;
   }

   private static float easeOutBack(float t) {
      t = clamp01(t);
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float u = t - 1.0F;
      return 1.0F + c3 * u * u * u + c1 * u * u;
   }

   private void beginPageTransition() {
      this.cardAge.clear();
      this.cardSlide.clear();
      this.ripples.clear();
      this.categoryAnim = 0;
      this.profileAge = 0;
      this.profileThemesHover = 0;
   }

   private void renderDetail(GuiGraphicsExtractor ctx, List<W> layout) {
   }

   private int[] scrollbar() {
      int vh = this.viewportH();
      float maxScroll = Math.max(0.0F, (float)(this.contentH - vh));
      if (!(maxScroll <= 1.0F) && this.contentH > 0) {
         int trackTop = this.listTop();
         int sbW = 2;
         int sbX = this.isMasterDetail() ? this.masterX2() + this.masterW2() - sbW - 1 : this.winX() + this.winW() - 5;
         int thumbH = Math.max(18, Math.min(vh, Math.round(vh * ((float)vh / this.contentH))));
         int thumbY = trackTop + Math.round(this.scrollOffset / maxScroll * (vh - thumbH));
         return new int[]{sbX, sbW, trackTop, vh, thumbY, thumbH};
      }

      return null;
   }

   private void dragScrollTo(int my) {
      int vh = this.viewportH();
      float maxScroll = Math.max(0.0F, (float)(this.contentH - vh));
      if (!(maxScroll <= 0.0F)) {
         int trackTop = this.listTop();
         int thumbH = Math.max(18, Math.min(vh, Math.round(vh * ((float)vh / this.contentH))));
         float frac = Math.max(0.0F, Math.min(1.0F, (my - trackTop - thumbH / 2.0F) / Math.max(1.0F, (float)(vh - thumbH))));
         this.scrollTarget = frac * maxScroll;
         this.scrollOffset = this.scrollTarget;
      }
   }

   private void drawW(GuiGraphicsExtractor ctx, W w) {
      boolean sc = w.scissor != null;
      if (sc) {
         ctx.enableScissor(w.scissor[0], w.scissor[1], w.scissor[2], w.scissor[3]);
      }

      this.drawRowHover(ctx, w);

      switch (w.kind) {
         case K_TAB: {
            ModuleCategory cat = (ModuleCategory)w.target;
            float sel = this.tabSelAnim.getOrDefault(cat, w.sel ? 1.0F : 0.0F);
            boolean hover = this.mxCache >= w.x && this.mxCache <= w.x + w.w && this.myCache >= w.y && this.myCache <= w.y + w.h;
            float hoverAnim = expLerp(this.tabHoverAnim.getOrDefault(cat, 0.0F), hover ? 1 : 0, 13, this.frameDt);
            this.tabHoverAnim.put(cat, hoverAnim);
            float bgA = hoverAnim * 0.45F * (1.0F - sel);
            if (bgA > 0.01F) {
               Render2DUtil.roundedRect(ctx, w.x, w.y, w.w, w.h, 11, fadeAlpha(Render2DUtil.lerpColor(C_WINDOW, C_MODE_SEL, 0.5F), bgA));
               Render2DUtil.roundedOutline(ctx, w.x, w.y, w.w, w.h, 11, 0.6F, fadeAlpha(0x30878787, bgA));
            }
            String name = pizzaCatName(cat);
            String icon = pizzaCatIcon(cat);
            Identifier categoryIcon = CATEGORY_TEXTURES.get(cat);
            int iconTint = Render2DUtil.lerpColor(0xFF929292, -1, Math.max(sel, hoverAnim * 0.75F));
            if (categoryIcon != null) {
               Render2DUtil.roundedTexture(ctx, categoryIcon, w.x + 8, w.y + (w.h - 14) / 2.0F,
                  14, 14, 0, true, iconTint);
            } else {
               iconText(ctx, icon, w.x + 10, w.y + (w.h - 5.6F) / 2.0F, 9.0F, iconTint);
            }
            regText(ctx, name, w.x + 27 + hoverAnim, w.y + (w.h - 7.0F) / 2.0F, 10.0F,
               Render2DUtil.lerpColor(0xFF8C8E9C, 0xFFE9EAF2, Math.max(sel, hoverAnim * 0.65F)));
            if (sel>0.01F) {
               float cx = w.x + w.w - 10 + (1 - sel) * 3;
               float cy = w.y + w.h/2.0F;
               int col = (Math.round(255.0F * sel) << 24) | 0xFFFFFF;
               Render2DUtil.chevron(ctx, Math.round(cx-4), Math.round(cy-3), 6, 6, false, 1.2F, col);
            }
            break;
         }
         case K_LABEL: {
            float maxLbl = Math.max(0.0F, (float)(w.x + w.w - bindW(keyOf(w.target)) - 6 - w.x));
            String lbl = w.text;
            if (bookW(lbl, 8.0F) > maxLbl) {
               lbl = truncateToWidth(lbl, 8.0F, maxLbl);
            }

            bookText(ctx, lbl, w.x, w.y + (w.h - 5.6F) / 2.0F, 8.0F, 0xFFE9EAF2);
            break;
         }
         case K_BOOL: {
            BooleanSetting b = (BooleanSetting)w.target;
            float ax = this.boolAnim.computeIfAbsent(b, k -> b.isEnabled() ? 1.0F : 0.0F);
            int tw = 18;
            int th = 10;
            int tx = w.x + w.w - tw;
            int tyx = w.y + (w.h - th) / 2;
            boolean bListening = this.bindPopup.target() == b;
            int nameRight = tx - 6;
            if (bListening || b.getKey() != -1) {
               String bTxt = b.getKey() == -1 ? "—" : (b.isHoldBind() ? "H " : "") + keyName(b.getKey());
               float bTw = sfW(bTxt, 8.0F);
               int bW = (int)Math.ceil(bTw) + 4;
               int bX = tx - bW - 6;
               int bY = w.y + (w.h - 16) / 2;
               Render2DUtil.roundedRect(ctx, bX, bY, bW, 16, 3.0F, bListening ? C_TEXT : C_BIND);
               Render2DUtil.roundedOutline(ctx, bX, bY, bW, 16, 3.0F, outlineThickness(), outlineColor());
               sfText(ctx, bTxt, bX + 2, bY + 5.2F, 8.0F, bListening ? C_WINDOW : C_TEXT);
               nameRight = bX - 6;
            }

            float maxBoolName = Math.max(0.0F, (float)(nameRight - 4 - w.x));
            String boolName = Translations.text(b.getName());
            if (bookW(boolName, 8.0F) > maxBoolName) {
               boolName = truncateToWidth(boolName, 8.0F, maxBoolName);
            }

            bookText(ctx, boolName, w.x, w.y + (w.h - 8.0F) / 2.0F, 8.0F, 0xFFE9EAF2);
            Accent.toggle(ctx, tx, tyx, tw, th, ax, C_BIND, 0xFF8A8484);
            break;
         }
         case K_SLIDER: {
            SliderSetting sl = (SliderSetting)w.target;
            float denom = Math.max(1.0E-5F, sl.getMax() - sl.getMin());
            float ratio = Math.max(0.0F, Math.min(1.0F, (sl.getValue() - sl.getMin()) / denom));
            int trackH = 6;
            int trackY = w.y + 10;
            float shown = expLerp(this.sliderShown.getOrDefault(sl, ratio), ratio, this.draggingSlider == sl ? 30.0F : 14.0F, this.frameDt);
            this.sliderShown.put(sl, shown);
            int fillW = Math.round(shown * w.trackW);
            boolean overTrack = this.mxCache >= w.trackX - 4 && this.mxCache <= w.trackX + w.trackW + 4 && this.myCache >= trackY - 5 && this.myCache <= trackY + trackH + 5;
            float knob = expLerp(this.knobAnim.getOrDefault(sl, 0.0F), this.draggingSlider == sl ? 1.0F : (overTrack ? 0.5F : 0.0F), 16.0F, this.frameDt);
            this.knobAnim.put(sl, knob);
            boolean editing = this.editingSlider == sl;
            int[] fr = this.sliderFieldRect(w);
            float maxSlName = Math.max(0.0F, (float)(fr[0] - 4 - w.x));
            String slName = Translations.text(sl.getName());
            if (bookW(slName, 8.0F) > maxSlName) {
               slName = truncateToWidth(slName, 8.0F, maxSlName);
            }

            bookText(ctx, slName, w.x, w.y, 8.0F, 0xFFE9EAF2);
            String valTxt = editing ? this.sliderBuf : fmt(sl.getValue());
            Render2DUtil.roundedRect(ctx, fr[0], fr[1], fr[2], fr[3], 3.0F, editing ? C_BIND : 0xFF1C1C1C);
            Render2DUtil.roundedOutline(ctx, fr[0], fr[1], fr[2], fr[3], 3.0F, outlineThickness(), editing ? Accent.color(0.7F) : outlineColor());
            float fvw = bookW(valTxt, 8.0F);
            float fvx = fr[0] + (fr[2] - fvw) / 2.0F;
            if (editing) {
               this.drawEditOverlay(ctx, this.sliderEdit, fvx, fr[1] + 2.0F, fr[3] - 4.0F, true);
            }
            bookText(ctx, valTxt, fvx, fr[1] + (fr[3] - 6.5F) / 2.0F, 8.0F, 0xFFE9EAF2);
            if (this.draggingSlider == sl) {
               String vs = fmt(sl.getValue());
               float vw = bookW(vs, 8.0F);
               float vx = Math.max((float)w.trackX, Math.min(w.trackX + fillW - vw / 2.0F, w.trackX + w.trackW - vw));
               bookText(ctx, vs, vx, trackY - 8.0F - 2.0F, 8.0F, 0xFFE9EAF2);
            }

            Render2DUtil.roundedRect(ctx, w.trackX, trackY, w.trackW, trackH, trackH / 2.0F, C_MODE_SEL);
            if (fillW > 1) {
               Accent.glow(ctx, w.trackX, trackY, Math.max(trackH, fillW), trackH, trackH / 2.0F, 0.75F + 0.25F * knob, 4);
               Accent.fill(ctx, w.trackX, trackY, Math.max(trackH, fillW), trackH, trackH / 2.0F, 1.0F);
               float knobX = w.trackX + Math.max(trackH, fillW) - trackH / 2.0F;
               float ks = 8.0F + 3.0F * knob;
               if (knob > 0.01F) {
                  float ring = ks + 5.0F * knob;
                  Render2DUtil.roundedRect(ctx, knobX - ring / 2.0F, trackY + trackH / 2.0F - ring / 2.0F, ring, ring, ring / 2.0F, Accent.color(0.22F * knob));
               }
               Render2DUtil.roundedRect(ctx, knobX - ks / 2.0F, trackY + trackH / 2.0F - ks / 2.0F, ks, ks, ks / 2.0F, 0xFFFFFFFF);
            }
            break;
         }
         case K_BIND:
            boolean listening = this.listeningBind == w.target;
            int key = keyOf(w.target);
            int py = w.y + (w.h - 16) / 2;
            if (!listening && key == -1) {
               Render2DUtil.roundedOutline(ctx, w.x, py, w.w, 16, 3.0F, outlineThickness(), 0x24FFFFFF);
               Render2DUtil.roundedRect(ctx, w.x + w.w / 2 - 1, py + 8 - 1, 2, 2, 1.0F, 0xFF8C8E9C);
            } else {
               String txtx = listening ? "..." : keyName(key);
               Render2DUtil.roundedRect(ctx, w.x, py, w.w, 16, 3.0F, listening ? fadeAlpha(C_TEXT, this.listenPulse()) : C_BIND);
               Render2DUtil.roundedOutline(ctx, w.x, py, w.w, 16, 3.0F, outlineThickness(), outlineColor());
               float twx = PizzaDLCFonts.sfpro().visualWidth(txtx, 8.0F);
               sfText(ctx, txtx, w.x + (w.w - twx) / 2.0F, py + 5.2F, 8.0F, listening ? C_WINDOW : C_TEXT);
            }
            break;
         case K_STRING: {
            StringSetting ss = (StringSetting)w.target;
            boolean editing = this.editingString == ss;
            Render2DUtil.roundedRect(ctx, w.x, w.y + 1, w.w, w.h - 2, 3.0F, editing ? C_BIND : 0xFF1C1C1C);
            float maxStr = Math.max(0.0F, (float)(w.w - 8));
            float strY = w.y + (w.h - 8.0F) / 2.0F;
            if (editing) {
               String prefix = Translations.text(ss.getName()) + ": ";
               String full = this.stringEdit.text();
               float pw = bookW(prefix, 8.0F);
               float caretW = bookW(full.substring(0, this.stringEdit.caret()), 8.0F);
               float shift = Math.max(0.0F, pw + caretW + 6.0F - maxStr);
               Render2DUtil.roundedOutline(ctx, w.x, w.y + 1, w.w, w.h - 2, 3.0F, outlineThickness(), Accent.color(0.7F));
               ctx.enableScissor(w.x + 2, w.y + 1, w.x + w.w - 2, w.y + w.h - 1);
               float sx = w.x + 4 - shift;
               bookText(ctx, prefix, sx, strY, 8.0F, C_UID);
               this.drawEditOverlay(ctx, this.stringEdit, sx + pw, w.y + 3.0F, w.h - 6.0F, true);
               bookText(ctx, full, sx + pw, strY, 8.0F, 0xFFE9EAF2);
               ctx.disableScissor();
            } else {
               String txt = Translations.text(ss.getName()) + ": " + ss.getText();
               if (bookW(txt, 8.0F) > maxStr) {
                  txt = truncateToWidth(txt, 8.0F, maxStr);
               }
               bookText(ctx, txt, w.x + 4, strY, 8.0F, 0xFFE9EAF2);
            }
            break;
         }
         case K_THEME:
            this.drawThemeCard(ctx, w);
            break;
         case K_THEME_ADD:
            Render2DUtil.roundedRect(ctx, w.x, w.y, w.w, w.h, 6.0F, 0x10FFFFFF);
            Render2DUtil.roundedOutline(ctx, w.x, w.y, w.w, w.h, 6.0F, outlineThickness(), outlineColor());
            float acx = w.x + w.w / 2.0F;
            float acy = w.y + w.h / 2.0F;
            float as = 12.0F;
            Render2DUtil.line(ctx, acx - as, acy, acx + as, acy, 2.0F, 0xFF8C8E9C);
            Render2DUtil.line(ctx, acx, acy - as, acx, acy + as, 2.0F, 0xFF8C8E9C);
            break;
         case K_BUTTON: {
            ButtonSetting bt = (ButtonSetting)w.target;
            Render2DUtil.roundedRect(ctx, w.x, w.y, w.w, w.h, 3.0F, C_MODE_SEL);
            Render2DUtil.roundedOutline(ctx, w.x, w.y, w.w, w.h, 3.0F, outlineThickness(), outlineColor());
            String t = Translations.text(bt.label());
            float maxBtn = Math.max(0.0F, (float)(w.w - 8));
            if (bookW(t, 8.0F) > maxBtn) {
               t = truncateToWidth(t, 8.0F, maxBtn);
            }

            float tw = bookW(t, 8.0F);
            bookText(ctx, t, w.x + (w.w - tw) / 2.0F, w.y + (w.h - 5.6F) / 2.0F, 8.0F, 0xFFE9EAF2);
            break;
         }
         case K_DROP_HEAD:
            Setting ds = (Setting)w.target;
            float prog = this.dropAnim.getOrDefault(ds, ds == this.openDrop ? 1.0F : 0.0F);
            int[] fg = this.dropFieldGeom(w);
            int fx = fg[0];
            int fw = fg[1];
            float maxDropLbl = Math.max(0.0F, (float)(fx - 6 - w.x));
            String dropLbl = w.text;
            if (bookW(dropLbl, 8.0F) > maxDropLbl) {
               dropLbl = truncateToWidth(dropLbl, 8.0F, maxDropLbl);
            }

            bookText(ctx, dropLbl, w.x, w.y + (w.h - 5.6F) / 2.0F, 8.0F, C_TEXT);
            float botR = prog > 0.02F ? 0.0F : 3.0F;
            int fieldY = w.y + (w.h - 14) / 2;
            boolean multi = ds instanceof MultiModeSetting;
            boolean hasSel = !multi || !multiSelected((MultiModeSetting)ds).isEmpty();
            int fieldFill = C_BIND;
            int textCol = multi && !hasSel ? C_UID : C_TEXT;
            Render2DUtil.roundedRect(ctx, fx, fieldY, fw, 14, 3.0F, 3.0F, botR, botR, fieldFill);
            if (prog <= 0.02F) {
               Render2DUtil.roundedOutline(ctx, fx, fieldY, fw, 14, 3.0F, outlineThickness(), outlineColor());
            }

            float chSlot = fx + fw - 12 - 2;
            float tx0 = fx + 7;
            float tyx = w.y + (w.h - 5.6F) / 2.0F;
            float avail = chSlot - 4.0F - tx0;
            boolean overField = this.mxCache >= fx && this.mxCache <= fx + fw && this.myCache >= fieldY && this.myCache <= fieldY + 14;
            List<String> sel = multi ? multiSelected((MultiModeSetting)ds) : List.of();
            if (multi && overField && sel.size() > 2 && avail > 8.0F) {
               String full = String.join(", ", sel);
               float fullW = bookW(full, 8.0F);
               int mL = (int)tx0;
               int mR = (int)(chSlot - 4.0F);
               ctx.enableScissor(mL, fieldY, mR, fieldY + 14);
               float period = fullW + 24.0F;
               float off = (float)(System.nanoTime() / 1.0E9 * 22.0 % period);
               bookText(ctx, full, tx0 - off, tyx, 8.0F, textCol);
               bookText(ctx, full, tx0 - off + period, tyx, 8.0F, textCol);
               ctx.disableScissor();
               int fieldBg = Render2DUtil.lerpColor(C_MODE_SEL, -1, 0.078431375F);
               int mFade = Math.min(18, Math.max(2, (mR - mL) / 3));
               int ot = Math.max(1, Math.round(outlineThickness()));
               int gy = fieldY + ot;
               int gh = 14 - 2 * ot;
               Render2DUtil.gradientRect(ctx, mL, gy, mFade, gh, fieldBg, fieldBg & 0xFFFFFF, 0.0F);
               Render2DUtil.gradientRect(ctx, mR - mFade - 1, gy, mFade, gh, fieldBg & 0xFFFFFF, fieldBg, 0.0F);
            } else if (w.mode != null && avail > 8.0F) {
               bookText(ctx, truncateToWidth(w.mode, 8.0F, avail), tx0, tyx, 8.0F, textCol);
            }

            drawArrow(ctx, chSlot + 6.0F, w.y + w.h / 2.0F, prog * (float) Math.PI, 0xFF8C8E9C);
            break;
         case K_MODULE_ROW:
            Module m = (Module)w.target;
            float a = this.moduleAnim.computeIfAbsent(m, k -> m.isEnabled() ? 1.0F : 0.0F);
            float slx = this.moduleSelAnim.getOrDefault(m, w.sel ? 1.0F : 0.0F);
            boolean hover = this.mxCache >= w.x && this.mxCache <= w.x + w.w && this.myCache >= w.y && this.myCache <= w.y + w.h;
            float enter = this.cardEnter.computeIfAbsent(m, k->0.0F);
            float yOff = 0.0F;
            ctx.pose().pushMatrix();
            ctx.pose().translate(0, yOff);
            int selX = w.x;
            int selW = w.w;
            int pad = 8;
            float nameFS = 9.0F;
            float nameY = w.y + (22.0F - 5.6F) / 2.0F;
            int tx = selX + pad;

            int[] pr = modulePillRect(w, m);
            int bX = pr[0];
            int bY = pr[1];
            int bW = pr[2];
            int bH = pr[3];
            float maxNameW = Math.max(0.0F, (float)(bX - 8 - tx));
            String nameTxt = Translations.moduleName(m.getName());
            if (bookW(nameTxt, nameFS) > maxNameW) {
               nameTxt = truncateToWidth(nameTxt, nameFS, maxNameW);
            }
            this.drawSearchMatch(ctx, nameTxt, tx, nameY, nameFS);
            regText(ctx, nameTxt, tx, nameY, nameFS, C_TEXT);
            String bTxt = m.getKey() == -1 ? "—" : keyName(m.getKey());
            boolean bindListening = this.listeningBind == m;
            Render2DUtil.roundedRect(ctx, bX, bY, bW, bH, 3.0F, bindListening ? fadeAlpha(C_TEXT, this.listenPulse()) : C_BIND);
            Render2DUtil.roundedOutline(ctx, bX, bY, bW, bH, 3.0F, outlineThickness(), outlineColor());
            ctx.enableScissor(bX + 2, bY, bX + bW - 2, bY + bH);
            sfText(ctx, bindListening ? "..." : bTxt, bX + 3, bY + 5.2F, 8.0F, bindListening ? C_WINDOW : C_TEXT);
            ctx.disableScissor();

            boolean perm = m.isPermanent();
            if (perm) {
               int pW = (int)Math.ceil(bookW(Translations.text("ВСЕГДА"), 8.0F)) + 6;
               int pX = selX + selW - pad - pW;
               int pY = w.y + (22 - 16) / 2;
               Render2DUtil.roundedRect(ctx, pX, pY, pW, 16, 3.0F, C_BIND);
               bookText(ctx, Translations.text("ВСЕГДА"), pX + 3, pY + 5.2F, 8.0F, C_UID);
            } else {
               int tw = 28;
               int th = 14;
               int tgX = selX + selW - pad - tw;
               int tgY = w.y + (22 - th) / 2;
               Accent.toggle(ctx, tgX, tgY, tw, th, a, C_TOGGLE_OFF, 0xFF8A8484);
            }
            ctx.pose().popMatrix();
            break;
         case K_COLOR:
            ColorSetting cs = (ColorSetting)w.target;
            int[] sw = colorSwatchRect(w);
            float maxClrName = Math.max(0.0F, (float)(sw[0] - 6 - w.x));
            String clrName = Translations.text(cs.getName());
            if (bookW(clrName, 8.0F) > maxClrName) {
               clrName = truncateToWidth(clrName, 8.0F, maxClrName);
            }

            bookText(ctx, clrName, w.x, w.y + (w.h - 5.6F) / 2.0F, 8.0F, 0xFFE9EAF2);
            Render2DUtil.roundedRect(ctx, sw[0], sw[1], sw[2], sw[3], 3.0F, 0xFF26282C);
            Render2DUtil.roundedRect(ctx, sw[0], sw[1], sw[2], sw[3], 3.0F, cs.getArgb());
            Render2DUtil.roundedOutline(ctx, sw[0], sw[1], sw[2], sw[3], 3.0F, outlineThickness(), outlineColor());
            break;
         case K_CARD: {
            Module cardModule = (Module)w.target;
            boolean overCard = this.mxCache >= w.x && this.mxCache < w.x + w.w
               && this.myCache >= Math.max(w.y, this.listTop())
               && this.myCache < Math.min(w.y + w.h, this.listTop() + this.viewportH());
            float highlight = expLerp(this.cardHover.getOrDefault(cardModule, 0.0F), overCard ? 1 : 0, 12, this.frameDt);
            this.cardHover.put(cardModule, highlight);
            float on = expLerp(this.cardOnAnim.getOrDefault(cardModule, cardModule.isEnabled() ? 1.0F : 0.0F), cardModule.isEnabled() ? 1 : 0, 10, this.frameDt);
            this.cardOnAnim.put(cardModule, on);
            Boolean wasOn = this.cardWasOn.put(cardModule, cardModule.isEnabled());
            if (wasOn != null && wasOn != cardModule.isEnabled()) {
               this.cardFlash.put(cardModule, 1.0F);
            }
            float flash = Math.max(0.0F, this.cardFlash.getOrDefault(cardModule, 0.0F) - this.frameDt / 0.6F);
            this.cardFlash.put(cardModule, flash);
            Render2DUtil.softShadow(ctx, w.x, w.y, w.w, w.h, 6.0F, 2.5F + 3.5F * highlight, 1.0F);
            Render2DUtil.roundedRect(ctx, w.x, w.y, w.w, w.h, 6.0F, C_MOD);
            if (on > 0.01F) {
               Render2DUtil.gradientRectFull(ctx, w.x, w.y, w.w, w.h, Accent.color(0.07F * on), Accent.secondary(0.0F), 6.0F);
            }
            if (flash > 0.01F) {
               float sweep = easeOutCubic(1.0F - flash);
               int half = Math.max(1, Math.round(w.w * 0.22F));
               int bandX = Math.round(w.x - half * 2 + (w.w + half * 2) * sweep);
               int shine = Render2DUtil.lerpColor(0xFFFFFFFF, C_ACCENT, 0.4F) & 0xFFFFFF;
               int peak = shine | Math.round(30.0F * flash) << 24;
               ctx.enableScissor(w.x, w.y, w.x + w.w, w.y + w.h);
               Render2DUtil.gradientRectFull(ctx, bandX, w.y, half, w.h, shine, peak, 0.0F);
               Render2DUtil.gradientRectFull(ctx, bandX + half, w.y, half, w.h, peak, shine, 0.0F);
               ctx.disableScissor();
            }
            Render2DUtil.roundedOutline(ctx, w.x, w.y, w.w, w.h, 6.0F, 0.5F,
               Render2DUtil.lerpColor(Render2DUtil.lerpColor(0x16FFFFFF, 0x40FFFFFF, highlight), Accent.color(0.4F), on * 0.45F));
            Render2DUtil.rect(ctx, w.x + 8, w.y + 22, Math.max(1, w.w - 16), 1, 0x12FFFFFF);
            break;
         }
         case K_SECTION: {
            float tw = regW(w.text, 6.5F);
            float ty = w.y + (w.h - 4.5499997F) / 2.0F;
            regText(ctx, w.text, w.x, ty, 6.5F, 0xFF8C8E9C);
            int lineX = Math.round(w.x + tw + 5.0F);
            int lineY = Math.round(w.y + w.h / 2.0F);
            int lineW = w.x + w.w - lineX;
            if (lineW > 0) {
               Render2DUtil.rect(ctx, lineX, lineY, lineW, 1, 0x22FFFFFF);
            }
         }
      }

      if (sc) {
         ctx.disableScissor();
      }
   }

   private static int[] colorSwatchRect(W w) {
      int sw = 26;
      int sh = 10;
      return new int[]{w.x + w.w - sw, w.y + (w.h - sh) / 2, sw, sh};
   }

   private static void sfText(GuiGraphicsExtractor ctx, String t, float x, float y, float s, int c){ Render2DUtil.msdfText(ctx, PizzaDLCFonts.sfpro(), t, x, y, s, c); }
   private static void bookText(GuiGraphicsExtractor ctx, String t, float x, float y, float s, int c){ Render2DUtil.msdfText(ctx, PizzaDLCFonts.nexaBook(), t, x, y, s, c); }
   private static void regText(GuiGraphicsExtractor ctx, String t, float x, float y, float s, int c){ Render2DUtil.msdfText(ctx, PizzaDLCFonts.nexaRegular(), t, x, y, s, c); }
   private static void iconText(GuiGraphicsExtractor ctx, String t, float x, float y, float s, int c){ Render2DUtil.msdfText(ctx, PizzaDLCFonts.icomoon(), t, x, y, s, c); }
   private static float sfW(String t, float s){ return PizzaDLCFonts.sfpro().width(t,s); }
   private static float bookW(String t, float s){ return PizzaDLCFonts.nexaBook().width(t,s); }
   private static float regW(String t, float s){ return PizzaDLCFonts.nexaRegular().width(t,s); }

   private void drawThemeCard(GuiGraphicsExtractor ctx, W w) {
      Theme t = (Theme)w.target;
      boolean active = ThemeManager.current().name().equalsIgnoreCase(t.name());
      Render2DUtil.roundedRect(ctx, w.x, w.y, w.w, w.h, 6.0F, active ? C_CARD_ON : 0x10FFFFFF);
      Render2DUtil.roundedOutline(ctx, w.x, w.y, w.w, w.h, 6.0F, outlineThickness(), outlineColor());
      int lx = w.x + 8;
      int rx = w.x + w.w - 8;
      int[] tdr = deleteRect(w);
      float maxThemeName = Math.max(0.0F, (float)(tdr[0] - 4 - lx));
      String themeName = Translations.text(t.name());
      if (bookW(themeName, 11.0F) > maxThemeName) {
         themeName = truncateToWidth(themeName, 11.0F, maxThemeName);
      }

      bookText(ctx, themeName, lx, w.y + 8, 11.0F, active ? C_TEXT : 0xFFE9EAF2);
      this.drawThemeColorRow(ctx, w, 0);
      this.drawThemeColorRow(ctx, w, 1);
      int y = w.y + 8 + 17 + 28;
      int gx = lx - 8;
      int gy = y + 0;
      int gw = rx - lx + 16;
      int gh = w.y + w.h - 8 - gy;
      if (gh > 6) {
         Render2DUtil.gradientRect(ctx, gx, gy, gw, gh, t.accent(), t.secondary(), 6.0F);
      }

      int[] dr = deleteRect(w);
      Render2DUtil.roundedRect(ctx, dr[0], dr[1], dr[2], dr[3], 3.0F, 0x66FF4D4D);
      float dx0 = dr[0] + 3;
      float dy0 = dr[1] + 3;
      float dx1 = dr[0] + dr[2] - 3;
      float dy1 = dr[1] + dr[3] - 3;
      Render2DUtil.line(ctx, dx0, dy0, dx1, dy1, 1.0F, 0xFFE9EAF2);
      Render2DUtil.line(ctx, dx0, dy1, dx1, dy0, 1.0F, 0xFFE9EAF2);
   }

   private static int[] deleteRect(W card) {
      int del = 11;
      return new int[]{card.x + card.w - 8 - del, card.y + 7, del, del};
   }

   private void drawThemeColorRow(GuiGraphicsExtractor ctx, W card, int row) {
      Theme t = (Theme)card.target;
      int color = row == 0 ? t.accent() : t.secondary();
      String label = Translations.text(row == 0 ? Translations.text("Первый цвет") : Translations.text("Второй цвет"));
      int lx = card.x + 8;
      int rx = card.x + card.w - 8;
      int textY = card.y + 8 + 17 + row * 14;
      bookText(ctx, label, lx, textY, 8.0F, 0xFFE9EAF2);
      int sq = 11;
      int sqX = rx - sq;
      int rowY = textY - 2;
      Render2DUtil.roundedRect(ctx, sqX, rowY, sq, sq, 3.0F, 0xFF000000 | color & 0xFFFFFF);
      Render2DUtil.roundedOutline(ctx, sqX, rowY, sq, sq, 3.0F, outlineThickness(), outlineColor());
      boolean editing = this.editTheme == t && this.editSecondary == (row == 1);
      String disp = this.hexDisplay(t, row);
      int[] pr = this.hexPillRect(card, row);
      Render2DUtil.roundedRect(ctx, pr[0], pr[1], pr[2], pr[3], pr[3] / 4.0F, editing ? 0xE82A2D34 : 0xCC15171C);
      Render2DUtil.roundedOutline(ctx, pr[0], pr[1], pr[2], pr[3], pr[3] / 4.0F, outlineThickness(), outlineColor());
      float hw = bookW(disp, 8.0F);
      float tx = pr[0] + (pr[2] - hw) / 2.0F;
      float ty = textY + 0.5F;
      if (editing && this.editSelectAll && hw > 0.0F) {
         Render2DUtil.roundedRect(ctx, (int)tx - 1, pr[1] + 2, (int)Math.ceil(hw) + 2, pr[3] - 4, 2.0F, 0x803C6FE0);
      }

      bookText(ctx, disp, tx, ty, 8.0F, 0xFFE9EAF2);
      if (editing && !this.editSelectAll) {
         Render2DUtil.roundedRect(ctx, Math.round(tx + hw) + 1, pr[1] + 2, 1, pr[3] - 4, 0.5F, 0xFFE9EAF2);
      }
   }

   private String hexDisplay(Theme t, int row) {
      if (this.editTheme == t && this.editSecondary == (row == 1)) {
         return this.editText;
      }

      int color = row == 0 ? t.accent() : t.secondary();
      return String.format(Locale.US, "#%06X", color & 0xFFFFFF);
   }

   private int[] hexPillRect(W card, int row) {
      Theme t = (Theme)card.target;
      int rx = card.x + card.w - 8;
      int textY = card.y + 8 + 17 + row * 14;
      int pillW = (int)Math.ceil(bookW(this.hexDisplay(t, row), 8.0F)) + 8;
      int pillX = rx - 11 - 4 - pillW;
      return new int[]{pillX, textY - 2, pillW, 11};
   }

   private static int[] themeSwatchRect(W card, int row) {
      int rx = card.x + card.w - 8;
      int textY = card.y + 8 + 17 + row * 14;
      return new int[]{rx - 11, textY - 2, 11, 11};
   }

   private void startHexEdit(Theme t, boolean secondary) {
      this.editingString = null;
      this.listeningBind = null;
      this.editTheme = t;
      this.editSecondary = secondary;
      int color = secondary ? t.secondary() : t.accent();
      this.editText = String.format(Locale.US, "#%06X", color & 0xFFFFFF);
      this.editSelectAll = true;
   }

   private void commitHexEdit() {
      if (this.editTheme == null) return;

      Integer rgb = parseHex(this.editText);
      if (rgb != null) {
         int color = 0xFF000000 | rgb;
         if (this.editSecondary) {
            this.editTheme.setSecondary(color);
         } else {
            this.editTheme.setAccent(color);
         }

         ThemeManager.save();
      }

      this.editTheme = null;
      this.editSelectAll = false;
   }

   private void handleHexKey(KeyEvent e) {
      if (e.isSelectAll()) {
         this.editSelectAll = true;
      } else if (e.isCopy()) {
         this.setClip(this.editText);
      } else if (e.isCut()) {
         this.setClip(this.editText);
         this.editText = "";
         this.editSelectAll = false;
      } else if (e.isPaste()) {
         if (this.editSelectAll) {
            this.editText = "";
            this.editSelectAll = false;
         }

         for (char c : this.getClip().toCharArray()) {
            this.appendHex(c);
         }
      } else if (e.isConfirmation()) {
         this.commitHexEdit();
      } else if (e.isEscape()) {
         this.editTheme = null;
         this.editSelectAll = false;
      } else {
         int k = e.key();
         if (k == 259 || k == 261) {
            if (this.editSelectAll) {
               this.editText = "";
               this.editSelectAll = false;
            } else if (!this.editText.isEmpty()) {
               this.editText = this.editText.substring(0, this.editText.length() - 1);
            }
         }
      }
   }

   private void appendHex(char c) {
      boolean hexCh = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
      if (hexCh || c == '#') {
         if (this.editSelectAll) {
            this.editText = "";
            this.editSelectAll = false;
         }

         if (this.editText.length() < 7) {
            this.editText = this.editText + c;
         }
      }
   }

   private static int darkenColor(int argb, float f) {
      int a = argb >>> 24 & 0xFF;
      int r = (int)((argb >> 16 & 0xFF) * f);
      int g = (int)((argb >> 8 & 0xFF) * f);
      int b = (int)((argb & 0xFF) * f);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static Integer parseHex(String s) {
      String h = s.replace("#", "").trim();
      if (h.length() != 6) {
         return null;
      }

      try {
         return Integer.parseInt(h, 16);
      } catch (NumberFormatException ignored) {
         return null;
      }
   }

   private String getClip() {
      return this.minecraft.keyboardHandler.getClipboard();
   }

   private void setClip(String s) {
      this.minecraft.keyboardHandler.setClipboard(s);
   }

   public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
      if (this.closing) return true;
      if (this.profileSelected && e.button() == 0 && inside(e.x(), e.y(), this.profileLanguageRect())
         && e.y() >= this.listTop() && e.y() < this.listTop() + this.viewportH()) {
         this.minecraft.setScreenAndShow(new LanguageSelectScreen(this, false));
         return true;
      }
      if (e.button() == 0 || e.button() == 1) {
         this.spawnRipple(e.x(), e.y());
      }
      if (this.profileSelected && e.button() == 0 && inside(e.x(), e.y(), this.profileThemesRect())
         && e.y() >= this.listTop() && e.y() < this.listTop() + this.viewportH()) {
         this.profileSelected = false;
         this.beginPageTransition();
         selectedCategory = ModuleCategory.THEMES;
         selectedModule = null;
         this.scrollOffset = this.scrollTarget = 0;
         this.setSearchQuery("");
         this.searchOpen = this.searchFocused = false;
         return true;
      }
      if (this.listeningBind != null) {
         setKeyOn(this.listeningBind, -100 - e.button());
         this.listeningBind = null;
         return true;
      }

      int mx = (int)e.x();
      int my = (int)e.y();
      if (this.bindPopup.mouseClicked(mx, my, e.button())) {
         return true;
      }

      if (this.colorPicker.mouseClicked(mx, my)) {
         return true;
      }

      if (this.colorPicker.isOpen() || this.bindPopup.isOpen()) {
         return true;
      }

      if (this.editTheme != null) {
         this.commitHexEdit();
      }

      if (this.editingSlider != null) {
         this.commitSliderEdit();
      }

      this.listeningBind = null;
      this.editingString = null;
      int[] profileCard = this.profileCardRect();
      if (e.button() == 0 && profileCard[1] >= this.sidebarRowsBottom() + 4
         && inside(mx, my, profileCard)) {
         if (!this.profileSelected) this.beginPageTransition();
         this.profileSelected = true;
         this.scrollOffset = this.scrollTarget = 0;
         this.setSearchQuery("");
         this.searchOpen = false;
         this.openDrop = null;
         this.dropAnim.clear();
         this.searchFocused = false;
         this.draggingSlider = null;
         this.draggingScrollbar = this.autoScroll = false;
         return true;
      }
      int[] searchRect = this.floatingSearchRect();
      if (e.button() == 0 && mx >= searchRect[0] && mx < searchRect[0] + searchRect[2]
         && my >= searchRect[1] && my < searchRect[1] + searchRect[3]) {
         this.searchOpen = true;
         this.searchFocused = true;
         this.profileSelected = false;
         this.scrollOffset = this.scrollTarget = 0;
         this.openDrop = null;
         return true;
      }
      if (e.button() == 2) {
         if (this.autoScroll) {
            this.autoScroll = false;
         } else if (this.scrollbar() != null) {
            this.autoScroll = true;
            this.autoScrollAnchorX = mx;
            this.autoScrollAnchorY = my;
         }

         return true;
      }

      if (this.autoScroll) {
         this.autoScroll = false;
         return true;
      }

      int[] sb = this.scrollbar();
      if (sb != null && mx >= sb[0] - 3 && mx <= sb[0] + sb[1] + 3 && my >= sb[2] && my <= sb[2] + sb[3]) {
         this.draggingScrollbar = true;
         this.dragScrollTo(my);
         return true;
      }

      int clipTop = this.listTop() - 1;
      int clipBot = this.winY() + this.winH() - 7;
      List<W> layout = this.buildLayout();
      Setting wasOpen = this.openDrop;
      if (wasOpen != null) {
         for (W w : layout) {
            if (w.kind != K_DROP_HEAD || w.target != wasOpen) continue;

            int n = dropItemCount(wasOpen);
            int[] b = this.popupBox(w, n);
            int bottom = b[1] + b[3];
            if (mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= bottom) {
               int rel = my - (b[1] + 0);
               if (rel >= 0 && rel / 12 + this.dropScroll < n) {
                  this.applyDropItem(wasOpen, rel / 12 + this.dropScroll);
               }

               return true;
            }
            break;
         }
      }

      this.openDrop = null;
      if (this.searchOpen) {
         int[] sr = this.floatingSearchRect();
         if (mx >= sr[0] && mx <= sr[0]+sr[2] && my >= sr[1] && my <= sr[1]+sr[3]) {
            this.searchFocused = true;
            return true;
         }

         this.searchFocused = false;
      } else {
         this.searchFocused = false;
      }

      for (int wi = layout.size() - 1; wi >= 0; wi--) {
         W wx = layout.get(wi);
         if (mx >= wx.x
            && mx <= wx.x + wx.w
            && my >= wx.y
            && my <= wx.y + wx.h
            && (wx.kind == K_TAB
               ? my >= this.winY() && my <= this.winY() + this.winH()
               : my >= clipTop && my <= clipBot)
            && (!wx.detail || my >= this.detailSettingsTop() && my <= this.detailSettingsBot())) {
            switch (wx.kind) {
               case K_CARD:
                  return false;
               case K_TAB:
                  ModuleCategory nc = (ModuleCategory)wx.target;
                  if (this.profileSelected || nc != selectedCategory) {
                     this.categorySlideDir = nc.ordinal() > selectedCategory.ordinal() ? 1.0F : -1.0F;
                     this.beginPageTransition();
                  }
                  this.profileSelected = false;

                  selectedCategory = nc;
                  selectedModule = null;
                  this.scrollOffset = 0.0F;
                  this.scrollTarget = 0.0F;
                  this.setSearchQuery("");
                  this.searchFocused = false;
                  return true;
               case K_LABEL:
               default:
                  break;
               case K_BOOL:
                  if (e.button() == 1) {
                     this.bindPopup.open((BooleanSetting)wx.target, wx.x + wx.w - 100, wx.y + wx.h + 2, this.width, this.height);
                     return true;
                  }

                  ((BooleanSetting)wx.target).toggle();
                  return true;
               case K_SLIDER:
                  if (e.button() != 0) {
                     return true;
                  }

                  int[] fr = this.sliderFieldRect(wx);
                  if (mx >= fr[0] && mx <= fr[0] + fr[2] && my >= fr[1] && my <= fr[1] + fr[3]) {
                     this.startSliderEdit((SliderSetting)wx.target);
                     return true;
                  }

                  this.draggingSlider = (SliderSetting)wx.target;
                  this.sliderTrackX = wx.trackX;
                  this.sliderTrackW = wx.trackW;
                  this.applySlider(mx);
                  return true;
               case K_BIND:
                  this.listeningBind = wx.target;
                  return true;
               case K_STRING:
                  this.editingString = (StringSetting)wx.target;
                  this.stringEdit.set(this.editingString.getText());
                  return true;
               case K_THEME:
                  Theme t = (Theme)wx.target;
                  int[] dr = deleteRect(wx);
                  if (mx >= dr[0] && mx <= dr[0] + dr[2] && my >= dr[1] && my <= dr[1] + dr[3]) {
                     ThemeManager.removeTheme(t);
                     return true;
                  }

                  for (int row = 0; row < 2; row++) {
                     int[] sq = themeSwatchRect(wx, row);
                     if (mx >= sq[0] && mx <= sq[0] + sq[2] && my >= sq[1] && my <= sq[1] + sq[3]) {
                        boolean sec = row == 1;
                        this.colorPicker
                           .open(
                              t, sec ? t.secondary() : t.accent(), false, sec ? Translations.text("Второй цвет") : Translations.text("Первый цвет"), sq[0] - 250, sq[1] - 20, argb -> {
                                 if (sec) {
                                    t.setSecondary(argb);
                                 } else {
                                    t.setAccent(argb);
                                 }
                              }
                           );
                        return true;
                     }
                  }

                  for (int rowx = 0; rowx < 2; rowx++) {
                     int[] pr = this.hexPillRect(wx, rowx);
                     if (mx >= pr[0] && mx <= pr[0] + pr[2] && my >= pr[1] && my <= pr[1] + pr[3]) {
                        this.startHexEdit(t, rowx == 1);
                        return true;
                     }
                  }

                  ThemeManager.setActive(t.name());
                  return true;
               case K_THEME_ADD:
                  ThemeManager.addTheme();
                  return true;
               case K_BUTTON:
                  if (e.button() != 0) {
                     return true;
                  }

                  ((ButtonSetting)wx.target).click();
                  return true;
               case K_DROP_HEAD:
                  this.openDrop = wasOpen == wx.target ? null : (Setting)wx.target;
                  this.dropScroll = 0;
                  return true;
               case K_MODULE_ROW:
                  Module rm = (Module)wx.target;
                  if (my > wx.y + 22) return false;
                  int[] mpr = modulePillRect(wx, rm);
                  if (mx >= mpr[0] && mx <= mpr[0] + mpr[2] && my >= mpr[1] && my <= mpr[1] + mpr[3]) {
                     this.listeningBind = rm;
                     return true;
                  }

                  if (e.button() == 1) {
                     this.listeningBind = rm;
                     return true;
                  }

                  rm.toggle();
                  return true;
               case K_COLOR:
                  ColorSetting cs = (ColorSetting)wx.target;
                  int[] sr = colorSwatchRect(wx);
                  this.colorPicker.open(cs, cs.getArgb(), true, Translations.text(cs.getName()), sr[0] - 250, sr[1] - 20, cs::setArgb);
                  return true;
            }
         }
      }

      return super.mouseClicked(e, dbl);
   }

   public boolean mouseDragged(MouseButtonEvent e, double dragX, double dragY) {
      if (this.closing) return true;
      if (this.colorPicker.mouseDragged(e.x(), e.y())) {
         return true;
      }

      if (this.draggingScrollbar) {
         this.dragScrollTo((int)e.y());
         return true;
      }

      if (this.draggingSlider != null) {
         this.applySlider((int)e.x());
         return true;
      }

      return super.mouseDragged(e, dragX, dragY);
   }

   public boolean mouseReleased(MouseButtonEvent e) {
      if (this.closing) return true;
      if (this.colorPicker.mouseReleased()) {
         return true;
      }

      if (this.draggingScrollbar) {
         this.draggingScrollbar = false;
         return true;
      }

      if (this.draggingSlider != null) {
         this.draggingSlider = null;
         return true;
      }

      return super.mouseReleased(e);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
      if (this.closing) return true;
      if (this.colorPicker.isOpen()) return true;
      if (this.openDrop != null) {
         for (W w : this.buildLayout()) {
            if (w.kind != K_DROP_HEAD || w.target != this.openDrop) continue;

            int n = dropItemCount(this.openDrop);
            int[] box = this.popupBox(w, n);
            int visible = Math.max(1, box[3] / DROP_ITEM_H);
            this.dropScroll = Math.max(0, Math.min(n - visible, this.dropScroll - (int)Math.signum(vertical)));
            return true;
         }
         this.openDrop = null;
      }
      if (vertical != 0.0 && this.bindPopup.mouseScrolled(vertical)) {
         return true;
      }

      if (this.listeningBind != null && vertical != 0.0) {
         setKeyOn(this.listeningBind, vertical > 0.0 ? -200 : -201);
         this.listeningBind = null;
         return true;
      }

      if (vertical == 0.0) {
         return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
      }

      if (mouseX < this.contentX() || mouseX > this.winX() + this.winW()
         || mouseY < this.listTop() || mouseY > this.winY() + this.winH()) return false;
      if (this.isMasterDetail() && mouseX >= this.detailX()) {
         this.detailScrollTarget = this.clampDetailScroll(this.detailScrollTarget - (float)vertical * 28.0F);
      } else {
         this.scrollTarget = this.clampScroll(this.scrollTarget - (float)vertical * 28.0F);
      }

      return true;
   }

   public boolean keyPressed(KeyEvent e) {
      if (this.closing) return true;
      int k = e.key();
      boolean editingField = this.editingString != null || this.editingSlider != null || this.editTheme != null;
      boolean modalOpen = this.colorPicker.isOpen() || this.bindPopup.isOpen();
      if ((e.modifiers() & 2) != 0 && k == 70 && !editingField && !modalOpen && this.listeningBind == null) {
         if (this.profileSelected) {
            this.profileSelected = false;
            this.beginPageTransition();
            this.scrollOffset = this.scrollTarget = 0;
         }
         this.openDrop = null;
         this.searchOpen = true;
         this.searchFocused = true;
         this.searchCaretAge = 0;
         return true;
      }
      if (k == 256 && this.openDrop != null && !modalOpen) {
         this.openDrop = null;
         return true;
      }
      if (this.searchOpen && k == 256 && !modalOpen && !editingField && this.listeningBind == null) {
         this.searchOpen = false;
         this.searchFocused = false;
         this.setSearchQuery("");
         this.scrollOffset = 0.0F;
         this.scrollTarget = 0.0F;
         return true;
      }
      if (this.bindPopup.keyPressed(k)) {
         return true;
      }

      if (this.colorPicker.keyPressed(k)) {
         return true;
      }

      if (this.colorPicker.isOpen() && k == 256) {
         this.colorPicker.requestClose();
         return true;
      }

      if (this.listeningBind != null) {
         if (k == 256) {
            this.listeningBind = null;
         } else if (k == 259) {
            setKeyOn(this.listeningBind, -1);
            this.listeningBind = null;
         } else {
            setKeyOn(this.listeningBind, k);
            this.listeningBind = null;
         }
         return true;
      }

      if (this.editTheme != null) {
         this.handleHexKey(e);
         return true;
      }

      if (this.editingString != null) {
         if (k == 257 || k == 256) {
            this.editingString = null;
         } else if (this.stringEdit.key(e)) {
            this.editingString.setText(this.stringEdit.text());
         }

         return true;
      }

      if (this.editingSlider != null) {
         if (k == 257) {
            this.commitSliderEdit();
         } else if (k == 256) {
            this.editingSlider = null;
            this.sliderBuf = "";
         } else if (this.sliderEdit.key(e)) {
            this.sliderBuf = this.sliderEdit.text();
         }

         return true;
      }

      if (!this.searchFocused) {
         ClickGui guiMod = this.mm().get(ClickGui.class);
         if (guiMod != null && guiMod.getKey() != -1 && k == guiMod.getKey()) {
            if (!this.bindCloseArmed || System.currentTimeMillis() - this.openedAtMillis < 300L) {
               return true;
            }
            this.onClose();
            return true;
         }

         return super.keyPressed(e);
      }

      if (k == 257 || k == 256) {
         this.searchFocused = false;
         this.searchEdit.clearSelection();
         return true;
      }

      this.searchEdit.key(e);
      this.syncSearch();
      return true;
   }

   private void setSearchQuery(String query) {
      this.searchEdit.set(query);
      this.searchQuery = this.searchEdit.text();
   }

   private void syncSearch() {
      String text = this.searchEdit.text();
      if (!text.equals(this.searchQuery)) {
         this.searchQuery = text;
         this.scrollOffset = this.scrollTarget = 0;
      }
      this.searchCaretAge = 0;
   }

   private int searchResultCount() {
      int count = 0;
      for (Module m : this.mm().getModules()) {
         if (!m.isHidden() && this.matchesSearch(m)) count++;
      }
      return count;
   }

   public boolean keyReleased(KeyEvent e) {
      try {
         ClickGui guiMod = this.mm().get(ClickGui.class);
         if (guiMod != null && guiMod.getKey() != -1 && e.key() == guiMod.getKey()) {
            this.bindCloseArmed = true;
         }
      } catch (Throwable ignored) {
      }
      return super.keyReleased(e);
   }

   public boolean charTyped(CharacterEvent e) {
      if (this.closing) return true;
      if (this.profileSelected) return true;
      if (this.colorPicker.charTyped(e.codepointAsString())) {
         return true;
      }

      if (this.searchFocused) {
         this.searchEdit.insert(e.codepointAsString());
         this.syncSearch();
         return true;
      }

      if (this.editTheme != null) {
         String s = e.codepointAsString();
         if (!s.isEmpty()) {
            this.appendHex(s.charAt(0));
         }

         return true;
      }

      if (this.editingString != null) {
         String before = this.editingString.getText();
         int room = Math.max(0, this.editingString.getMaxLength() - before.length() + this.stringEdit.selected().length());
         String add = e.codepointAsString();
         if (add.length() <= room) {
            this.stringEdit.insert(add);
            this.editingString.setText(this.stringEdit.text());
         }

         return true;
      }

      if (this.editingSlider == null) {
         return super.charTyped(e);
      }

      this.sliderEdit.insert(e.codepointAsString());
      this.sliderBuf = this.sliderEdit.text();
      return true;
   }

   private void applySlider(int mx) {
      SliderSetting sl = this.draggingSlider;
      if (sl != null) {
         int trackW = Math.max(4, this.sliderTrackW);
         float ratio = Math.max(0.0F, Math.min(1.0F, (float)(mx - this.sliderTrackX) / trackW));
         float raw = sl.getMin() + ratio * (sl.getMax() - sl.getMin());
         float step = sl.getStep();
         if (step > 0.0F) {
            raw = Math.round(raw / step) * step;
         }

         sl.setCurrentValue(raw);
      }
   }

   private int[] sliderFieldRect(W w) {
      int fw = this.sliderPillW((SliderSetting)w.target);
      return new int[]{w.x + w.w - fw - 0, w.y + 8, fw, 10};
   }

   private int sliderPillW(SliderSetting sl) {
      Float anim = this.sliderAnim.get(sl);
      if (anim == null) {
         int t = this.sliderPillWTarget(sl);
         this.sliderAnim.put(sl, (float)t);
         return t;
      }

      return Math.round(anim);
   }

   private int sliderPillWTarget(SliderSetting sl) {
      String disp = this.editingSlider == sl ? this.sliderBuf + " " : fmt(sl.getValue());
      return (int)Math.ceil(bookW(disp, 8.0F)) + 8;
   }

   private static float expLerp(float cur, float target, float speed, float dt) {
      float t = 1.0F - (float)Math.exp(-speed * dt);
      return cur + (target - cur) * t;
   }

   private void startSliderEdit(SliderSetting sl) {
      this.editingSlider = sl;
      this.sliderBuf = fmt(sl.getValue());
      this.sliderEdit.set(this.sliderBuf);
      this.sliderEdit.selectAll();
   }

   private void drawSearchMatch(GuiGraphicsExtractor ctx, String name, float x, float y, float fs) {
      if (this.searchQuery.isEmpty()) return;
      int idx = name.toLowerCase(Locale.ROOT).indexOf(this.searchQuery.toLowerCase(Locale.ROOT));
      if (idx < 0) return;
      int end = Math.min(name.length(), idx + this.searchQuery.length());
      float x0 = x + regW(name.substring(0, idx), fs);
      float mw = regW(name.substring(idx, end), fs);
      float pulse = 0.75F + 0.25F * (float)Math.sin(System.nanoTime() / 1.0E9 * Math.PI * 1.6);
      float reveal = easeOutCubic(clamp01(this.searchEdit.sinceChange() / 0.25F));
      Render2DUtil.roundedRect(ctx, x0 - 1.5F, y - 2.5F, (mw + 3.0F) * reveal, fs + 1.0F, 2.5F, Accent.color(0.32F * pulse));
      Render2DUtil.roundedRect(ctx, x0 - 1.5F, y + fs - 1.0F, (mw + 3.0F) * reveal, 1.0F, 0.5F, Accent.color(0.9F));
   }

   private void drawEditOverlay(GuiGraphicsExtractor ctx, TextEdit edit, float textX, float top, float h, boolean book) {
      String text = edit.text();
      if (edit.hasSelection()) {
         float x0 = textX + (book ? bookW(text.substring(0, edit.selStart()), 8.0F) : regW(text.substring(0, edit.selStart()), 8.0F));
         float x1 = textX + (book ? bookW(text.substring(0, edit.selEnd()), 8.0F) : regW(text.substring(0, edit.selEnd()), 8.0F));
         Render2DUtil.roundedRect(ctx, x0 - 0.5F, top, x1 - x0 + 1.0F, h, 1.5F, Accent.color(0.45F));
      }
      float since = edit.sinceChange();
      float blink = since < 0.6F ? 1.0F : 0.25F + 0.75F * (0.5F + 0.5F * (float)Math.cos((since - 0.6F) * Math.PI * 2.0 / 1.1));
      float cx = textX + (book ? bookW(text.substring(0, edit.caret()), 8.0F) : regW(text.substring(0, edit.caret()), 8.0F));
      Render2DUtil.roundedRect(ctx, cx, top, 1.0F, h, 0.5F, fadeAlpha(C_TEXT, blink));
   }

   private void commitSliderEdit() {
      if (this.editingSlider == null) return;

      try {
         float v = Float.parseFloat(this.sliderBuf.trim());
         v = Math.max(this.editingSlider.getMin(), Math.min(this.editingSlider.getMax(), v));
         float step = this.editingSlider.getStep();
         if (step > 0.0F) {
            v = Math.round(v / step) * step;
         }

         this.editingSlider.setCurrentValue(v);
      } catch (NumberFormatException ignored) {
      }

      this.editingSlider = null;
      this.sliderBuf = "";
   }

   @Override
   public void tick() {
      super.tick();
      if (this.closeQueued) {
         this.closeQueued = false;
         this.finishClose();
      }
   }

   public boolean isClosing() {
      return this.closing;
   }

   private void finishClose() {
      ClickGui m = this.mm().get(ClickGui.class);
      if (m != null && m.isEnabled()) {
         m.setEnabled(false);
      }
      if (this.minecraft != null && this.minecraft.gui.screen() == this) {
         super.onClose();
      }
   }

   public void onClose() {
      if (this.closing) return;
      this.closing = true;
      this.commitSliderEdit();
      if (this.editTheme != null) this.commitHexEdit();
      this.colorPicker.close();
      this.bindPopup.close();
      this.draggingSlider = null;
      this.draggingScrollbar = false;
      this.autoScroll = false;
   }

   private static final class W {
      final int kind;
      final int x;
      final int y;
      final int w;
      final int h;
      final Object target;
      String text;
      String mode;
      boolean sel;
      boolean detail;
      int trackX;
      int trackW;
      int[] scissor;
      Object owner;
      int order;

      W(int kind, Object target, int x, int y, int w, int h) {
         this.kind = kind;
         this.target = target;
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
      }
   }
}
