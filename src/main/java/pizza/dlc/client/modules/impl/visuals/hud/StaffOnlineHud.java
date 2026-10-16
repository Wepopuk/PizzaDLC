package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import pizza.dlc.api.animation.Animation;
import pizza.dlc.api.animation.Easing;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.render.msdf.ReplaceSymbols;
import pizza.dlc.api.util.render.Render2DUtil;

public final class StaffOnlineHud implements QuickImports {
   private static final String HEADER_TEXT = "Staff";
   private static final String EMPTY_TEXT = "Нет персонала";
   private static final float ROLE_GAP = 4.0F;
   private static final float STATUS_GAP = 10.0F;
   private static final long ROW_MS = 300L;
   private static final long PANEL_MS = 260L;
   private static final Easing EASE = Easing.EASE_OUT_CUBIC;
   private static final long SCAN_INTERVAL_MS = 2000L;
   private long lastScanMs = 0L;
   private Object lastLevel = null;
   private final Map<String, Staff> staff = new LinkedHashMap<>();
   private final Draggable drag = new Draggable("StaffOnline", 4.0F, 100.0F, 0.0F, 0.0F);
   private final Animation animW = new Animation(EASE, PANEL_MS);
   private final Animation panelVis = new Animation(EASE, PANEL_MS);
   private static final String[][] STAFF_PREFIXES = new String[][]{
      {"ст.модер", "Ст.Модер"},
      {"ст.модератор", "Ст.Модер"},
      {"sr.moder", "Sr.Moder"},
      {"sr.moderator", "Sr.Moder"},
      {"гл.админ", "Гл.Админ"},
      {"модератор", "Модер"},
      {"модер", "Модер"},
      {"moderator", "Moder"},
      {"moder", "Moder"},
      {"хелпер", "Хелпер"},
      {"helper", "Helper"},
      {"админ", "Админ"},
      {"admin", "Admin"},
      {"owner", "Owner"},
      {"владелец", "Владелец"},
      {"куратор", "Куратор"},
      {"curator", "Curator"},
      {"стажер", "Стажер"},
      {"trainee", "Trainee"},
      {"ютубер", "Ютубер"},
      {"youtuber", "Ютубер"},
      {"youtube", "Ютубер"},
      {"yt", "YT"},
      {"media", "Media"},
      {"медиа", "Медиа"}
   };
   private static final Map<Integer, String> ICON_ROLE_MAP = new HashMap<>();
   private static final Map<String, Integer> ROLE_PRIORITY = new HashMap<>();

   private void scanStaff() {
      if (mc.level == null || mc.getConnection() == null) return;

      if (mc.level != this.lastLevel) {
         this.lastLevel = mc.level;
         this.staff.clear();
      }

      long now = System.currentTimeMillis();
      if (now - this.lastScanMs >= SCAN_INTERVAL_MS) {
         this.lastScanMs = now;
         Set<String> found = new HashSet<>();
         Scoreboard sb = mc.level.getScoreboard();
         String self = mc.player != null ? mc.player.getGameProfile().name() : null;

         for (PlayerTeam team : sb.getPlayerTeams()) {
            if (team.getPlayers().isEmpty()) continue;

            String teamName = team.getName();
            if (!teamName.startsWith("CIT-") && !teamName.startsWith("fb-") && !teamName.startsWith("LD_") && !teamName.startsWith("TAB-Sidebar-")) {
               String prefixRaw = team.getPlayerPrefix().getString();
               String prefixClean = prefixRaw.replaceAll("§[0-9a-fk-or]", "").toLowerCase(Locale.ROOT).trim();
               String role = this.matchPrefix(prefixClean);
               if (role == null) {
                  role = this.detectRoleFromIcon(prefixRaw);
               }

               if (role != null) {
                  int roleColor = rankColorFromPrefix(prefixRaw);

                  for (String name : team.getPlayers()) {
                     if (name.indexOf(167) < 0 && (self == null || !name.equalsIgnoreCase(self))) {
                        found.add(name);
                        Staff s = this.staff.computeIfAbsent(name, k -> new Staff());
                        s.role = role;
                        s.roleColor = roleColor;
                        s.status = this.computeStatus(name);
                        s.present = true;
                     }
                  }
               }
            }
         }

         for (Entry<String, Staff> e : this.staff.entrySet()) {
            if (found.contains(e.getKey())) continue;

            e.getValue().present = false;
         }
      }
   }

   private Status computeStatus(String name) {
      if (mc.getConnection() == null) {
         return Status.VANISHED;
      }

      PlayerInfo info = mc.getConnection().getPlayerInfoIgnoreCase(name);
      if (info == null) {
         return Status.VANISHED;
      }

      if (info.getGameMode() == GameType.SPECTATOR) {
         return Status.SPEC;
      }

      UUID id = info.getProfile().id();
      if (mc.level != null) {
         for (Player pl : mc.level.players()) {
            if (!pl.getUUID().equals(id)) continue;

            return Status.NEAR;
         }
      }

      return Status.NONE;
   }

   private String matchPrefix(String text) {
      for (String[] pair : STAFF_PREFIXES) {
         if (!text.contains(pair[0])) continue;

         return pair[1];
      }

      return null;
   }

   private String detectRoleFromIcon(String prefixRaw) {
      for (int i = 0; i < prefixRaw.length(); i++) {
         String role = ICON_ROLE_MAP.get(prefixRaw.codePointAt(i));
         if (role != null) {
            return role;
         }
      }

      return null;
   }

   private static int rankColorFromPrefix(String prefixRaw) {
      int i = 0;

      while (i < prefixRaw.length()) {
         int cp = prefixRaw.codePointAt(i);
         if (cp > 127) {
            int c = ReplaceSymbols.rankColor(cp);
            if (c != 0) {
               return c;
            }
         }

         i += Character.charCount(cp);
      }

      return 0;
   }

   public void render(GuiGraphicsExtractor ctx, boolean editMode) {
      if (mc.level == null) return;

      this.scanStaff();
      MsdfFont font = HudStyle.body();
      float maxRowW = 0.0F;
      float contentH = 0.0F;
      int presentCount = 0;
      Iterator<Entry<String, Staff>> it = this.staff.entrySet().iterator();

      while (it.hasNext()) {
         Entry<String, Staff> en = it.next();
         Staff s = en.getValue();
         float v = s.anim.run(s.present ? 1.0F : 0.0F);
         if (!s.present && v <= 0.001F) {
            it.remove();
         } else {
            if (s.present) {
               presentCount++;
            }

            contentH += v * HudStyle.ROW_H;
            float rowW = font.width(pizza.dlc.api.translation.Translations.text(s.role), HudStyle.ROW_FS) + ROLE_GAP + font.width(en.getKey(), HudStyle.ROW_FS) + STATUS_GAP + HudStyle.pillWidth(s.status.label);
            maxRowW = Math.max(maxRowW, rowW);
         }
      }

      boolean empty = this.staff.isEmpty();
      boolean placeholder = empty && editMode;
      if (placeholder) {
         contentH = HudStyle.ROW_H;
         maxRowW = Math.max(maxRowW, font.width(pizza.dlc.api.translation.Translations.text(EMPTY_TEXT), HudStyle.ROW_FS));
      }

      boolean wantVisible = !empty || editMode;
      float pa = this.panelVis.run(wantVisible ? 1.0F : 0.0F);
      if (!wantVisible && pa < 0.01F) {
         this.animW.setValue(0.0F);
      } else {
         String badge = String.valueOf(presentCount);
         int targetW = (int)Math.ceil(Math.max(HudStyle.headerWidth(HEADER_TEXT, badge), HudStyle.PAD * 2 + maxRowW));
         if (this.animW.getValue() < 1.0F) {
            this.animW.setValue(targetW);
         }

         int panelW = Math.round(this.animW.run(targetW));
         float panelHf = HudStyle.listPanelH(contentH);
         int panelH = Math.round(panelHf);
         this.drag.setWidth(panelW);
         this.drag.setHeight(panelH);
         if (!this.drag.isPositioned()) {
            this.drag.setX(4.0F);
            this.drag.setY(101.0F);
            this.drag.syncRenderPosition();
            this.drag.setPositioned(true);
         }

         this.drag.update(ctx);
         int panelX = (int)this.drag.getRenderX();
         int panelY = (int)this.drag.getRenderY();
         HudStyle.panel(ctx, panelX, panelY, panelW, panelHf, HudStyle.RADIUS, pa);
         HudStyle.header(ctx, panelX, panelY, panelW, HEADER_TEXT, badge, pa);
         float rowY = panelY + HudStyle.HEADER_H + HudStyle.ROWS_TOP;
         ctx.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
         if (placeholder) {
            float textY = HudStyle.textY(rowY, HudStyle.ROW_H, HudStyle.ROW_FS);
            Render2DUtil.msdfText(ctx, font, pizza.dlc.api.translation.Translations.text(EMPTY_TEXT), panelX + HudStyle.PAD, textY, HudStyle.ROW_FS, HudStyle.fade(HudStyle.TEXT_DIM, 0.7F * pa));
         } else {
            List<Entry<String, Staff>> ordered = new ArrayList<>(this.staff.entrySet());
            ordered.sort((ax, b) -> {
               int pa2 = ROLE_PRIORITY.getOrDefault(((Staff)ax.getValue()).role, 0);
               int pb2 = ROLE_PRIORITY.getOrDefault(b.getValue().role, 0);
               return pa2 != pb2 ? Integer.compare(pb2, pa2) : ((String)ax.getKey()).compareToIgnoreCase(b.getKey());
            });

            for (Entry<String, Staff> en : ordered) {
               Staff s = en.getValue();
               String name = en.getKey();
               float v = s.anim.getValue();
               float slotH = v * HudStyle.ROW_H;
               if (v > 0.001F) {
                  float a = v * pa;
                  float slide = (1.0F - v) * HudStyle.SLIDE;
                  float textY = HudStyle.textY(rowY, slotH, HudStyle.ROW_FS);
                  float x = panelX + HudStyle.PAD + slide;
                  int roleCol = s.roleColor != 0 ? s.roleColor | 0xFF000000 : HudStyle.TEXT_DIM;
                  String role = pizza.dlc.api.translation.Translations.text(s.role);
                  Render2DUtil.msdfText(ctx, font, role, x, textY, HudStyle.ROW_FS, HudStyle.fade(roleCol, a));
                  float nameX = x + font.width(role, HudStyle.ROW_FS) + ROLE_GAP;
                  float pillW = HudStyle.pillWidth(s.status.label);
                  float pillX = panelX + panelW - HudStyle.PAD - pillW + slide;
                  String shown = HudStyle.truncate(font, name, HudStyle.ROW_FS, pillX - 6.0F - nameX);
                  Render2DUtil.msdfText(ctx, font, shown, nameX, textY, HudStyle.ROW_FS, HudStyle.fade(HudStyle.TEXT, a));
                  HudStyle.pill(ctx, pillX, rowY + (slotH - HudStyle.PILL_H) / 2.0F, pillW, s.status.label, s.status.color, a);
               }

               rowY += slotH;
            }
         }

         ctx.disableScissor();
      }
   }

   static {
      ICON_ROLE_MAP.put(42245, "YouTube");
      ICON_ROLE_MAP.put(42249, "Хелпер");
      ICON_ROLE_MAP.put(42259, "Мл.Модер");
      ICON_ROLE_MAP.put(42263, "Модер");
      ICON_ROLE_MAP.put(42273, "Модер+");
      ICON_ROLE_MAP.put(42277, "Ст.Модер");
      ICON_ROLE_MAP.put(42291, "Мл.Админ");
      ICON_ROLE_MAP.put(42295, "Админ");
      ICON_ROLE_MAP.put(42281, "Гл.Модер");
      ROLE_PRIORITY.put("Админ", 100);
      ROLE_PRIORITY.put("Мл.Админ", 90);
      ROLE_PRIORITY.put("Гл.Модер", 85);
      ROLE_PRIORITY.put("Ст.Модер", 80);
      ROLE_PRIORITY.put("Модер+", 75);
      ROLE_PRIORITY.put("Модер", 70);
      ROLE_PRIORITY.put("Мл.Модер", 65);
      ROLE_PRIORITY.put("Хелпер", 50);
      ROLE_PRIORITY.put("YouTube", 40);
   }

   private static final class Staff {
      final Animation anim = new Animation(StaffOnlineHud.EASE, ROW_MS);
      String role = "";
      int roleColor = 0;
      Status status = Status.NONE;
      boolean present = true;
   }

   static enum Status {
      NONE("ON", HudStyle.GOOD),
      NEAR("N", HudStyle.WARN),
      SPEC("GM3", HudStyle.WARN),
      VANISHED("V", HudStyle.BAD);

      final String label;
      final int color;

      private Status(String label, int color) {
         this.label = label;
         this.color = color;
      }
   }
}
