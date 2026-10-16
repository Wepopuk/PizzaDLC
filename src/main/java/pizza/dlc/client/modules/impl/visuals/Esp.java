package pizza.dlc.client.modules.impl.visuals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.events.impl.render.EventRenderWorld2D;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.render.msdf.ReplaceSymbols;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.profiler.PizzaDLCProfiler;
import pizza.dlc.api.util.render.EspIconScope;
import pizza.dlc.api.util.render.ProjectionUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.client.modules.impl.combat.AntiBot;
import pizza.dlc.client.modules.impl.misc.FixHP;
import pizza.dlc.client.modules.impl.visuals.hud.HudStyle;

@ModuleInfo(
   name = "Esp",
   category = ModuleCategory.VISUALS,
   key = 345
)
public class Esp extends Module {
   private static final float NAME_FS = 8.5F;
   private static final float CLAN_FS = 8.0F;
   private static final float BADGE_FS = 6.5F;
   private static final int BADGE_H = 9;
   private static final float BADGE_PAD = 2.5F;
   private static final int TAG_ROW = 13;
   private static final int TAG_BAR_SPACE = 2;
   private static final float TAG_BAR_INSET = 5.0F;
   private static final float TAG_BAR_H = 1.2F;
   private static final int TAG_PAD = 4;
   private static final float TAG_GAP = 3.0F;
   private static final float TAG_RADIUS = 4.0F;
   private static final int ABSORB_COLOR = 0xFFFFC94D;
   private static final int ICON = 16;
   private static final int ITEM_STRIP_PAD = 1;
   private static final int ITEM_STRIP_GAP = 2;
   private static final float ITEM_STRIP_RADIUS = 3.0F;
   private static final float LABEL_SIZE = 5.5F;
   private static final float TAG_BASE_SCALE = 0.85F;
   private static final float TAG_DIST_START = 6.0F;
   private static final float TAG_DIST_FACTOR = 0.02F;
   private final MultiModeSetting targets = new MultiModeSetting("Цели", this, "Игроки", "Мобы", "Монстры", "Предметы", "Себя");
   private final MultiModeSetting draw = new MultiModeSetting("Рисовать", this, "3D Бокс", "2D Бокс", "Неймтег", "Akrien OLD");
   private final MultiModeSetting box = new MultiModeSetting("Настройки бокса", this, "Заливка", "Обводка")
      .setVisible(() -> this.draw.get("3D Бокс").isEnabled());
   private final MultiModeSetting nametag = new MultiModeSetting("Настройки тега", this, "Предметы");
   private final BooleanSetting hpBar = new BooleanSetting("HP-бар", "Полоска HP слева от 2D-бокса", this)
      .setVisible(() -> this.draw.get("2D Бокс").isEnabled());
   private final BooleanSetting hideDisplays = new BooleanSetting(
      "Скрыть голограммы", "Убирать серверные text-display голограммы (неймтеги 1.21+, которые не прячутся как обычные)", this
   );
   private final List<TagPos> tagPositions = new ArrayList<>();
   private final List<Box2D> boxRects = new ArrayList<>();
   private boolean prevBox3D;
   private static final NameParts EMPTY_PARTS = new NameParts(null, null);
   private final Map<UUID, CachedParts> partsCache = new HashMap<>();

   private static int color() {
      return ThemeManager.accent();
   }

   public Esp() {
      this.targets.get("Игроки").setEnabled(true);
      this.targets.get("Мобы").setEnabled(true);
      this.targets.get("Монстры").setEnabled(true);
      this.targets.get("Предметы").setEnabled(true);
      this.draw.get("3D Бокс").setEnabled(true);
      this.draw.get("Неймтег").setEnabled(true);
      this.box.get("Обводка").setEnabled(true);
      this.nametag.get("Предметы").setEnabled(true);
   }

   private boolean accept(Entity ent) {
      if (ent != mc.player) {
         if (ent instanceof ItemEntity) {
            return this.targets.get("Предметы").isEnabled();
         }

         if (!(ent instanceof LivingEntity)) {
            return false;
         }

         String key = ent instanceof Player ? "Игроки" : (ent instanceof Enemy ? "Монстры" : "Мобы");
         return this.targets.get(key).isEnabled();
      }

      return !mc.options.getCameraType().isFirstPerson() && this.targets.get("Себя").isEnabled();
   }

   public boolean acceptsAkrienOld(Entity entity) {
      return this.isEnabled() && this.draw.get("Akrien OLD").isEnabled() && entity instanceof Player && this.accept(entity);
   }

   public static boolean isAkrienOldActive() {
      if (pizza.dlc.api.util.render.streamproof.CleanRenderScope.isActive()
         || pizza.dlc.api.util.render.streamproof.StreamProofRenderer.isVanillaGameplayActive()) return false;
      PizzaDLCClient client = PizzaDLCClient.getInstance();
      Esp esp = client != null && client.getModuleManager() != null ? client.getModuleManager().get(Esp.class) : null;
      return esp != null && esp.isEnabled() && esp.draw.get("Akrien OLD").isEnabled();
   }

   public static boolean shouldRenderAkrienOld(Entity entity) {
      if (!isAkrienOldActive()) return false;
      return PizzaDLCClient.getInstance().getModuleManager().get(Esp.class).acceptsAkrienOld(entity);
   }

   public static boolean shouldHideVanillaName(Entity ent) {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         Esp esp = cc.getModuleManager().get(Esp.class);
         if (esp == null || !esp.isEnabled()) {
            return false;
         }

         return esp.draw.get("Неймтег").isEnabled() && esp.accept(ent);
      }

      return false;
   }

   public static boolean shouldHideDisplay(Entity ent) {
      if (!(ent instanceof TextDisplay)) {
         return false;
      }

      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         Esp esp = cc.getModuleManager().get(Esp.class);
         return esp != null && esp.isEnabled() && esp.hideDisplays.isEnabled();
      }

      return false;
   }

   private static AABB interpBox(Entity ent, float pt) {
      Vec3 ip = ent.getPosition(pt);
      return ent.getBoundingBox().move(ip.x - ent.getX(), ip.y - ent.getY(), ip.z - ent.getZ());
   }

   private void enforceBoxExclusive() {
      boolean b3 = this.draw.get("3D Бокс").isEnabled();
      boolean b2 = this.draw.get("2D Бокс").isEnabled();
      if (b3 && b2) {
         if (!this.prevBox3D) {
            this.draw.get("2D Бокс").setEnabled(false);
         } else {
            this.draw.get("3D Бокс").setEnabled(false);
         }
      }

      this.prevBox3D = this.draw.get("3D Бокс").isEnabled();
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      this.enforceBoxExclusive();
      this.tagPositions.clear();
      this.boxRects.clear();
      if (mc.level != null && mc.player != null) {
         float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         boolean nt = this.draw.get("Неймтег").isEnabled();
         boolean box3D = this.draw.get("3D Бокс").isEnabled();
         boolean box2D = this.draw.get("2D Бокс").isEnabled();
         boolean fill = this.box.get("Заливка").isEnabled();
         boolean outline = this.box.get("Обводка").isEnabled();

         for (Entity ent : mc.level.entitiesForRendering()) {
            if (!this.accept(ent)) continue;

            AABB bb = interpBox(ent, pt);
            if (box3D) {
               if (fill) {
                  Render3DUtil.drawFilledBox(e, bb, color() & 0xFFFFFF | 0x40000000, true);
               }

               if (outline) {
                  Render3DUtil.drawBox(e, bb, color(), true);
               }
            }

            if (box2D) {
               float[] rect = screenRect(bb);
               if (rect != null) {
                  float hp = ent instanceof LivingEntity le
                     ? Math.max(0.0F, Math.min(1.0F, FixHP.getHealth(le) / Math.max(1.0F, FixHP.getMaxHealth(le))))
                     : -1.0F;
                  this.boxRects.add(new Box2D(rect[0], rect[1], rect[2], rect[3], hp));
               }
            }

            if (nt) {
               Vector2f s = ProjectionUtil.worldToScreen(new Vec3((bb.minX + bb.maxX) / 2.0, bb.maxY + 0.2, (bb.minZ + bb.maxZ) / 2.0));
               if (s != null) {
                  this.tagPositions.add(new TagPos(ent, s.x, s.y));
               }
            }
         }
      }
   }

   private static float[] screenRect(AABB bb) {
      float minX = 1.0E9F;
      float minY = 1.0E9F;
      float maxX = -1.0E9F;
      float maxY = -1.0E9F;

      for (int i = 0; i < 8; i++) {
         Vec3 corner = new Vec3((i & 1) == 0 ? bb.minX : bb.maxX, (i & 2) == 0 ? bb.minY : bb.maxY, (i & 4) == 0 ? bb.minZ : bb.maxZ);
         Vector2f s = ProjectionUtil.worldToScreen(corner);
         if (s == null) {
            return null;
         }

         minX = Math.min(minX, s.x);
         minY = Math.min(minY, s.y);
         maxX = Math.max(maxX, s.x);
         maxY = Math.max(maxY, s.y);
      }

      return maxX - minX < 10000.0F && maxY - minY < 10000.0F ? new float[]{minX, minY, maxX, maxY} : null;
   }

   @EventHandler
   public void onRender2D(EventRenderWorld2D e) {
      if (mc.level == null || mc.player == null) return;

      long prof = PizzaDLCProfiler.begin();
      GuiGraphicsExtractor ctx = e.getContext();
      if (!this.boxRects.isEmpty()) {
         int c = color();
         boolean showHp = this.hpBar.isEnabled();

         for (Box2D b : this.boxRects) {
            this.draw2DCorners(ctx, b.x1(), b.y1(), b.x2(), b.y2(), c);
            if (showHp && b.hp() >= 0.0F) {
               this.drawHpBar(ctx, b.x1(), b.y1(), b.y2(), b.hp());
            }
         }
      }

      float gw = ctx.guiWidth();
      float gh = ctx.guiHeight();
      float margin = 150.0F;

      for (TagPos tp : this.tagPositions) {
         if (tp.x() < -margin || tp.x() > gw + margin || tp.y() < -margin || tp.y() > gh + margin) continue;

         if (tp.ent() instanceof LivingEntity le) {
            this.drawNametag(ctx, le, tp.x(), tp.y());
         } else if (tp.ent() instanceof ItemEntity ie) {
            this.drawItemTag(ctx, ie, tp.x(), tp.y());
         }
      }

      PizzaDLCProfiler.end("ESP-неймтеги", prof);
   }

   private void draw2DCorners(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color) {
      float len = Math.max(2.0F, Math.min(x2 - x1, y2 - y1) * 0.25F);
      float t = 1.0F;
      Render2DUtil.line(ctx, x1, y1, x1 + len, y1, t, color);
      Render2DUtil.line(ctx, x1, y1, x1, y1 + len, t, color);
      Render2DUtil.line(ctx, x2, y1, x2 - len, y1, t, color);
      Render2DUtil.line(ctx, x2, y1, x2, y1 + len, t, color);
      Render2DUtil.line(ctx, x1, y2, x1 + len, y2, t, color);
      Render2DUtil.line(ctx, x1, y2, x1, y2 - len, t, color);
      Render2DUtil.line(ctx, x2, y2, x2 - len, y2, t, color);
      Render2DUtil.line(ctx, x2, y2, x2, y2 - len, t, color);
   }

   private void drawHpBar(GuiGraphicsExtractor ctx, float x1, float y1, float y2, float hpFrac) {
      float barW = 2.0F;
      float gap = 2.0F;
      float bx = x1 - gap - barW;
      float h = Math.max(1.0F, y2 - y1);
      float fillH = h * Math.max(0.0F, Math.min(1.0F, hpFrac));
      Render2DUtil.roundedRect(ctx, bx, y1, barW, h, 0.5F, 0xA0000000);
      if (fillH >= 1.0F) {
         Render2DUtil.gradientRectV(
            ctx, Math.round(bx), Math.round(y2 - fillH), Math.round(barW), Math.round(fillH), ThemeManager.accent(), ThemeManager.secondary(), 0.5F
         );
      }
   }

   private int drawTag(
      GuiGraphicsExtractor ctx,
      boolean friend,
      boolean bot,
      String prefix,
      int prefixColor,
      boolean prefixGradient,
      String name,
      Component nameComp,
      List<Seg> clan,
      String right,
      int rightColor,
      float hpFrac
   ) {
      MsdfFont title = HudStyle.title();
      MsdfFont body = HudStyle.body();
      List<Badge> badges = new ArrayList<>();
      if (friend) {
         badges.add(new Badge("F", HudStyle.GOOD, false));
      }

      if (bot) {
         badges.add(new Badge("BOT", HudStyle.BAD, false));
      }

      if (prefix != null && !prefix.isBlank()) {
         badges.add(new Badge(prefix, prefixColor, prefixGradient));
      }

      boolean hasName = name != null && !name.isEmpty();
      boolean hasClan = clan != null && !clan.isEmpty();
      boolean hasRight = right != null && !right.isEmpty();
      if (badges.isEmpty() && !hasName && !hasClan && !hasRight) {
         return 0;
      }

      float content = 0.0F;
      boolean any = false;

      for (Badge b : badges) {
         content += (any ? TAG_GAP : 0.0F) + badgeWidth(b.text());
         any = true;
      }

      float nameW = hasName ? title.width(name, NAME_FS) : 0.0F;
      if (hasName) {
         content += (any ? TAG_GAP : 0.0F) + nameW;
         any = true;
      }

      if (hasClan) {
         for (Seg s : clan) {
            content += body.width(s.text(), CLAN_FS);
         }

         any = true;
      }

      float pillW = hasRight ? HudStyle.pillWidth(right) : 0.0F;
      if (hasRight) {
         content += (any ? TAG_GAP + 1.0F : 0.0F) + pillW;
      }

      boolean bar = hpFrac >= 0.0F;
      int w = (int)Math.ceil(content) + TAG_PAD * 2;
      int h = bar ? TAG_ROW + TAG_BAR_SPACE : TAG_ROW;
      float x = -w / 2.0F;
      float y = -h;
      HudStyle.panelFlat(ctx, x, y, w, h, TAG_RADIUS, 1.0F);
      float textY = HudStyle.textY(y, TAG_ROW, NAME_FS);
      float tx = x + TAG_PAD;
      any = false;

      for (Badge b : badges) {
         if (any) {
            tx += TAG_GAP;
         }

         float bw = badgeWidth(b.text());
         int col = 0xFF000000 | b.color();
         float by = y + (TAG_ROW - BADGE_H) / 2.0F;
         Render2DUtil.roundedRect(ctx, tx, by, bw, (float)BADGE_H, 3.0F, col & 0xFFFFFF | 0x30000000);
         float bty = HudStyle.textY(by, BADGE_H, BADGE_FS);
         if (b.gradient()) {
            this.drawGradient(ctx, title, b.text(), tx + BADGE_PAD, bty, BADGE_FS, col);
         } else {
            Render2DUtil.msdfText(ctx, title, b.text(), tx + BADGE_PAD, bty, BADGE_FS, col);
         }

         tx += bw;
         any = true;
      }

      if (hasName) {
         if (any) {
            tx += TAG_GAP;
         }

         if (nameComp != null) {
            this.drawColoredName(ctx, title, nameComp, tx, textY);
         } else {
            Render2DUtil.msdfText(ctx, title, name, tx, textY, NAME_FS, HudStyle.TEXT);
         }

         tx += nameW;
      }

      if (hasClan) {
         float clanY = textY + 0.7F * (NAME_FS - CLAN_FS);

         for (Seg s : clan) {
            int col = s.color() == -1 ? HudStyle.TEXT_DIM : s.color();
            Render2DUtil.msdfText(ctx, body, s.text(), tx, clanY, CLAN_FS, col);
            tx += body.width(s.text(), CLAN_FS);
         }
      }

      if (hasRight) {
         HudStyle.pill(ctx, x + w - TAG_PAD - pillW, y + (TAG_ROW - HudStyle.PILL_H) / 2.0F, pillW, right, rightColor, 1.0F);
      }

      if (bar) {
         HudStyle.bar(ctx, x + TAG_BAR_INSET, y + h - 2.5F, w - TAG_BAR_INSET * 2.0F, TAG_BAR_H, hpFrac, HudStyle.healthColor(hpFrac), 1.0F);
      }

      return h;
   }

   private static float badgeWidth(String text) {
      return HudStyle.title().width(text, BADGE_FS) + BADGE_PAD * 2.0F;
   }

   private void drawNametag(GuiGraphicsExtractor ctx, LivingEntity ent, float cx, float cy) {
      boolean items = this.nametag.get("Предметы").isEnabled();
      String nm = ent.getName().getString();
      String prefix = null;
      int prefixColor = -1;
      boolean prefixGradient = false;
      List<Seg> clan = null;
      if (ent instanceof Player p) {
         NameParts np = this.partsOf(p);
         if (np.prefix() != null) {
            prefix = np.prefix().text();
            prefixColor = np.prefix().color();
            prefixGradient = np.prefix().gradient();
         }

         clan = np.suffix();
      }

      float hpF = Math.max(0.0F, Math.min(1.0F, FixHP.getHealth(ent) / Math.max(1.0F, FixHP.getMaxHealth(ent))));
      String hpStr = fmt1(FixHP.getHealth(ent) + ent.getAbsorptionAmount());
      int hpCol = ent.getAbsorptionAmount() > 0.0F ? ABSORB_COLOR : HudStyle.healthColor(hpF);
      boolean friend = ent instanceof Player
         && PizzaDLC.getInstance().getFriendManager() != null
         && PizzaDLC.getInstance().getFriendManager().isFriend(ent.getName().getString());
      boolean bot = AntiBot.isBot(ent);
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      ctx.pose().scale(this.computeTagScale(ent));
      int tagH = this.drawTag(ctx, friend, bot, prefix, prefixColor, prefixGradient, nm, null, clan, hpStr, hpCol, hpF);
      if (items && ent instanceof Player) {
         this.drawItemRow(ctx, ent, tagH);
      }

      ctx.pose().popMatrix();
   }

   private void drawItemRow(GuiGraphicsExtractor ctx, LivingEntity ent, int tagH) {
      List<ItemStack> row = new ArrayList<>();
      addIcon(row, ent.getOffhandItem());
      addIcon(row, ent.getItemBySlot(EquipmentSlot.HEAD));
      addIcon(row, ent.getItemBySlot(EquipmentSlot.CHEST));
      addIcon(row, ent.getItemBySlot(EquipmentSlot.LEGS));
      addIcon(row, ent.getItemBySlot(EquipmentSlot.FEET));
      addIcon(row, ent.getMainHandItem());
      if (!row.isEmpty()) {
         int stripW = row.size() * ICON + ITEM_STRIP_PAD * 2;
         int stripH = ICON + ITEM_STRIP_PAD * 2;
         int sx = -stripW / 2;
         int sy = -tagH - ITEM_STRIP_GAP - stripH;
         HudStyle.panelFlat(ctx, sx, sy, stripW, stripH, ITEM_STRIP_RADIUS, 0.85F);
         int ix = sx + ITEM_STRIP_PAD;
         int iy = sy + ITEM_STRIP_PAD;
         EspIconScope.begin();

         try {
            for (ItemStack st : row) {
               ctx.item(st, ix, iy);
               this.drawIconLabel(ctx, st, ix, iy);
               ix += ICON;
            }
         } finally {
            EspIconScope.end();
         }
      }
   }

   private void drawIconLabel(GuiGraphicsExtractor ctx, ItemStack st, int ix, int iy) {
      if (st.isDamageableItem() && st.getMaxDamage() > 0 && st.isDamaged()) {
         float frac = Math.max(0.0F, Math.min(1.0F, (float)(st.getMaxDamage() - st.getDamageValue()) / st.getMaxDamage()));
         Render2DUtil.roundedRect(ctx, ix + 2.0F, iy + 14.0F, 12.0F, 1.5F, 0.75F, 0xC0000000);
         if (frac * 12.0F >= 1.0F) {
            Render2DUtil.roundedRect(ctx, ix + 2.0F, iy + 14.0F, 12.0F * frac, 1.5F, 0.75F, HudStyle.healthColor(frac));
         }
      } else if (st.getCount() > 1) {
         MsdfFont font = HudStyle.mono();
         String cnt = String.valueOf(st.getCount());
         float w = Render2DUtil.msdfWidth(font, cnt, LABEL_SIZE);
         this.labelShadow(ctx, font, cnt, ix + 16 - w - 0.5F, iy + 16 - LABEL_SIZE, -1);
      }
   }

   private void labelShadow(GuiGraphicsExtractor ctx, MsdfFont font, String s, float x, float y, int color) {
      Render2DUtil.msdfText(ctx, font, s, x + 0.5F, y + 0.5F, LABEL_SIZE, 0xFF000000);
      Render2DUtil.msdfText(ctx, font, s, x, y, LABEL_SIZE, color);
   }

   private void drawItemTag(GuiGraphicsExtractor ctx, ItemEntity ie, float cx, float cy) {
      ItemStack st = ie.getItem();
      if (!st.isEmpty()) {
         Component nameComp = st.getHoverName();
         String nm = nameComp != null ? nameComp.getString() : null;
         String cnt = st.getCount() > 1 ? "x" + st.getCount() : null;
         ctx.pose().pushMatrix();
         ctx.pose().translate(cx, cy);
         ctx.pose().scale(this.computeTagScale(ie));
         this.drawTag(ctx, false, false, null, 0, false, nm, nameComp, null, cnt, HudStyle.WARN, -1.0F);
         ctx.pose().popMatrix();
      }
   }

   private NameParts partsOf(Player p) {
      Component display = p.getDisplayName();
      if (display == null) {
         return EMPTY_PARTS;
      }

      String full = display.getString();
      UUID id = p.getUUID();
      CachedParts c = this.partsCache.get(id);
      if (c != null && c.display().equals(full)) {
         return c.parts();
      }

      String name = p.getName().getString();
      int nameIdx = name != null && !name.isEmpty() ? full.indexOf(name) : -1;
      Prefix prefix = nameIdx > 0 ? this.buildPrefix(display, nameIdx) : null;
      List<Seg> suffix = nameIdx >= 0 ? this.buildSuffix(display, nameIdx + name.length()) : null;
      NameParts parts = new NameParts(prefix, suffix);
      if (this.partsCache.size() > 256) {
         this.partsCache.clear();
      }

      this.partsCache.put(id, new CachedParts(full, parts));
      return parts;
   }

   private Prefix buildPrefix(Component display, int nameIdx) {
      StringBuilder ranks = new StringBuilder();
      StringBuilder plain = new StringBuilder();
      int[] pos = new int[]{0};
      int[] rankColor = new int[]{0};
      boolean[] gotRankColor = new boolean[]{false};
      int[] plainColor = new int[]{-1};
      boolean[] gotPlainColor = new boolean[]{false};
      display.visit((style, str) -> {
         int sc = style.getColor() != null ? 0xFF000000 | style.getColor().getValue() : -1;
         int i = 0;

         while (i < str.length() && pos[0] < nameIdx) {
            int cp = str.codePointAt(i);
            int cc = Character.charCount(cp);
            if (cp == 167 && i + 1 < str.length()) {
               pos[0] += 2;
               i += 2;
            } else {
               String repl = ReplaceSymbols.replacement(cp);

               if (repl != null && repl.length() > 1) {
                  if (ranks.length() > 0) {
                     ranks.append(' ');
                  }

                  ranks.append(repl);
                  if (!gotRankColor[0]) {
                     int rc = ReplaceSymbols.rankColor(cp);
                     rankColor[0] = rc != 0 ? rc : sc;
                     gotRankColor[0] = true;
                  }
               } else {
                  String add = repl != null ? repl : new String(Character.toChars(cp));
                  plain.append(add);
                  if (!gotPlainColor[0] && !add.isBlank()) {
                     plainColor[0] = sc;
                     gotPlainColor[0] = true;
                  }
               }

               pos[0] += cc;
               i += cc;
            }
         }

         return pos[0] >= nameIdx ? Optional.of(true) : Optional.empty();
      }, Style.EMPTY);
      boolean isRank = ranks.length() > 0;
      if (!isRank) {
         ReplaceSymbols.RankMatch rm = ReplaceSymbols.recoverRanksFromText(plain.toString());
         if (rm != null && !rm.text().isBlank()) {
            int col = rm.color() != 0 ? rm.color() : (gotPlainColor[0] ? plainColor[0] : -1);
            return new Prefix(rm.text(), col, true);
         }
      }

      String pre = (isRank ? ranks.toString() : plain.toString()).strip();
      if (pre.isBlank()) {
         return null;
      }

      int finalColor = isRank ? (gotRankColor[0] ? rankColor[0] : -1) : (gotPlainColor[0] ? plainColor[0] : -1);
      return new Prefix(pre, finalColor, isRank);
   }

   private List<Seg> buildSuffix(Component display, int nameEnd) {
      List<Seg> segs = new ArrayList<>();
      int[] pos = new int[]{0};
      int[] legacy = new int[]{0};
      int[] curColor = new int[]{0};
      StringBuilder[] cur = new StringBuilder[]{new StringBuilder()};
      display.visit((style, str) -> {
         int sc = style.getColor() != null ? 0xFF000000 | style.getColor().getValue() : -1;
         int i = 0;

         while (i < str.length()) {
            int cp = str.codePointAt(i);
            int cc = Character.charCount(cp);
            if (pos[0] >= nameEnd) {
               if (cp == 167 && i + 1 < str.length()) {
                  char code = Character.toLowerCase(str.charAt(i + 1));
                  if (code == 'x' && i + 13 < str.length()) {
                     int rgb = 0;
                     boolean ok = true;

                     for (int k = 0; k < 6 && ok; k++) {
                        int si = i + 2 + k * 2;
                        int d = str.charAt(si) == 167 ? Character.digit(str.charAt(si + 1), 16) : -1;
                        if (d < 0) {
                           ok = false;
                        } else {
                           rgb = rgb << 4 | d;
                        }
                     }

                     if (ok) {
                        legacy[0] = 0xFF000000 | rgb;
                        pos[0] += 14;
                        i += 14;
                        continue;
                     }
                  }

                  ChatFormatting f = ChatFormatting.getByCode(code);
                  if (f == ChatFormatting.RESET) {
                     legacy[0] = 0;
                  } else if (f != null) {
                     TextColor tc = TextColor.fromLegacyFormat(f);
                     if (tc != null) {
                        legacy[0] = 0xFF000000 | tc.getValue();
                     }
                  }

                  pos[0] += 2;
                  i += 2;
                  continue;
               }

               String add = null;
               String repl = ReplaceSymbols.replacement(cp);
               if (repl != null) {
                  if (repl.length() == 1 && repl.charAt(0) >= ' ' && repl.charAt(0) <= '~') {
                     add = repl;
                  }
               } else if (cp >= 32 && cp <= 126 || Character.isLetterOrDigit(cp)) {
                  add = new String(Character.toChars(cp));
               }

               if (add != null) {
                  int eff = legacy[0] != 0 ? legacy[0] : sc;
                  if (cur[0].length() > 0 && eff != curColor[0]) {
                     segs.add(new Seg(cur[0].toString(), curColor[0]));
                     cur[0] = new StringBuilder();
                  }

                  curColor[0] = eff;
                  cur[0].append(add);
               }
            }

            pos[0] += cc;
            i += cc;
         }

         return Optional.empty();
      }, Style.EMPTY);
      if (cur[0].length() > 0) {
         segs.add(new Seg(cur[0].toString(), curColor[0]));
      }

      while (!segs.isEmpty()) {
         Seg last = segs.get(segs.size() - 1);
         String t = last.text();
         int end = t.length();

         while (end > 0 && Character.isWhitespace(t.charAt(end - 1))) {
            end--;
         }

         if (end == t.length()) {
            break;
         }

         if (end != 0) {
            segs.set(segs.size() - 1, new Seg(t.substring(0, end), last.color()));
            break;
         }

         segs.remove(segs.size() - 1);
      }

      for (Seg s : segs) {
         if (s.text().isBlank()) continue;

         return segs;
      }

      return null;
   }

   private void drawColoredName(GuiGraphicsExtractor ctx, MsdfFont font, Component comp, float x, float y) {
      float[] cx = new float[]{x};
      comp.visit((style, str) -> {
         int color = style.getColor() != null ? 0xFF000000 | style.getColor().getValue() : HudStyle.TEXT;
         String text = ReplaceSymbols.normalize(str);
         Render2DUtil.msdfText(ctx, font, text, cx[0], y, NAME_FS, color);
         cx[0] += Render2DUtil.msdfWidth(font, text, NAME_FS);
         return Optional.empty();
      }, Style.EMPTY);
   }

   private void drawGradient(GuiGraphicsExtractor ctx, MsdfFont font, String text, float x, float y, float size, int base) {
      int end = darken(base, 0.8F);
      int n = text.length();
      float cx = x;

      for (int i = 0; i < n; i++) {
         String ch = String.valueOf(text.charAt(i));
         float ratio = n <= 1 ? 0.0F : (float)i / (n - 1);
         Render2DUtil.msdfText(ctx, font, ch, cx, y, size, Render2DUtil.lerpColor(base, end, ratio));
         cx += Render2DUtil.msdfWidth(font, ch, size);
      }
   }

   private static int darken(int argb, float f) {
      int a = argb >>> 24;
      int r = (int)((argb >> 16 & 0xFF) * f);
      int g = (int)((argb >> 8 & 0xFF) * f);
      int b = (int)((argb & 0xFF) * f);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private float computeTagScale(Entity ent) {
      if (mc.player == null) {
         return 1.0F;
      }

      double distSq = mc.player.distanceToSqr(ent);
      double startSq = 36.0;
      float extra = distSq > startSq ? (float)Math.sqrt(distSq) - TAG_DIST_START : 0.0F;
      float d = Math.max(0.7F, Math.min(1.0F, 1.0F - extra * TAG_DIST_FACTOR));
      return TAG_BASE_SCALE * d;
   }

   private static void addIcon(List<ItemStack> list, ItemStack st) {
      if (st.isEmpty()) return;

      list.add(st);
   }

   private static String fmt1(float v) {
      int x = Math.round(v * 10.0F);
      return x / 10 + "." + x % 10;
   }

   private record Badge(String text, int color, boolean gradient) {
   }

   private record Box2D(float x1, float y1, float x2, float y2, float hp) {
   }

   private record CachedParts(String display, NameParts parts) {
   }

   private record NameParts(Prefix prefix, List<Seg> suffix) {
   }

   private record Prefix(String text, int color, boolean gradient) {
   }

   private record Seg(String text, int color) {
   }

   private record TagPos(Entity ent, float x, float y) {
   }
}
