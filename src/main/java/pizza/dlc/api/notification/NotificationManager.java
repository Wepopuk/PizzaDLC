package pizza.dlc.api.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import pizza.dlc.api.animation.Animation;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.theme.Accent;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.impl.visuals.hud.HudStyle;

public final class NotificationManager {
   private static final List<Notification> notifications = new CopyOnWriteArrayList<>();
   private static final List<String> IGNORED_TITLES = Arrays.asList("ClickGui", "Interface", "Console");
   private static final Map<ModuleCategory, Identifier> CATEGORY_ICONS = Map.of(
      ModuleCategory.COMBAT, categoryIcon("combat"),
      ModuleCategory.MOVEMENT, categoryIcon("movement"),
      ModuleCategory.VISUALS, categoryIcon("visuals"),
      ModuleCategory.PLAYER, categoryIcon("player"),
      ModuleCategory.OTHER, categoryIcon("other")
   );
   private static final float H = 30.0F;
   private static final float RADIUS = 7.0F;
   private static final float PAD = 5.0F;
   private static final float TILE = 20.0F;
   private static final float TILE_RADIUS = 5.0F;
   private static final float TILE_ICON = 12.0F;
   private static final float TEXT_GAP = 7.0F;
   private static final float RIGHT_PAD = 8.0F;
   private static final float TITLE_FS = 8.5F;
   private static final float MSG_FS = 7.0F;
   private static final float LINE_GAP = 3.5F;
   private static final float TOGGLE_W = 18.0F;
   private static final float TOGGLE_H = 10.0F;
   private static final float TOGGLE_GAP = 8.0F;
   private static final float MIN_W = 130.0F;
   private static final float MAX_W = 230.0F;
   private static final float MARGIN = 4.0F;
   private static final float GAP_V = 4.0F;
   private static final int MAX_VISIBLE = 6;
   private static final int MAX_STORED = 10;
   private static final long TOGGLE_DELAY_MS = 90L;
   private static final long TOGGLE_DURATION_MS = 2000L;
   private static final int TOGGLE_OFF = 0xFF534E4E;
   private static final int KNOB_OFF = 0xFF8A8484;
   private static final String PREVIEW_TITLE = "Уведомления";
   private static final String PREVIEW_MESSAGE = "Перетащи, чтобы сдвинуть";
   private static final Draggable DRAG = new Draggable("Notifications", 0.0F, 4.0F, 0.0F, 0.0F);
   private static boolean defaultAnchored;
   private static float columnW = -1.0F;
   private static long lastNs;

   private NotificationManager() {
   }

   private static Identifier categoryIcon(String name) {
      return Identifier.fromNamespaceAndPath("pizzadlc", "textures/gui/figma/" + name + ".png");
   }

   public static void add(String title, String message, NotificationType type, long duration) {
      add(title, message, type, duration, null, false);
   }

   public static void add(String title, String message, NotificationType type, long duration, ItemStack icon) {
      add(title, message, type, duration, icon, false);
   }

   public static void add(String title, String message, NotificationType type, long duration, ItemStack icon, boolean themeAccent) {
      if (title == null || IGNORED_TITLES.contains(title)) return;

      push(new Notification(title, message, type, duration, icon, themeAccent));
   }

   public static void addToggle(String title, ModuleCategory category, boolean enabled) {
      if (title == null || IGNORED_TITLES.contains(title)) return;

      String message = enabled ? "Включён" : "Выключен";
      NotificationType type = enabled ? NotificationType.SUCCESS : NotificationType.ERROR;
      int toggle = enabled ? 1 : 0;

      for (Notification n : notifications) {
         if (!n.isToggle() || !n.getTitle().equals(title)) continue;

         n.retoggle(message, type, toggle);
         return;
      }

      push(new Notification(title, message, type, TOGGLE_DURATION_MS, null, false, category, toggle));
   }

   private static void push(Notification n) {
      if (notifications.size() >= MAX_STORED) {
         notifications.remove(0);
      }

      notifications.add(n);
   }

   public static void render(GuiGraphicsExtractor ctx) {
      long nowNs = System.nanoTime();
      float dt = lastNs == 0L ? 0.016F : Math.min(0.1F, (float)(nowNs - lastNs) / 1.0E9F);
      lastNs = nowNs;
      boolean editMode = Minecraft.getInstance().gui.screen() instanceof ChatScreen;
      List<Notification> visible = new ArrayList<>();

      for (Notification n : notifications) {
         Animation a = n.getAnimation();
         boolean expired = n.isExpired();
         a.run(expired ? 0.0F : 1.0F);
         if (expired && a.getValue() <= 0.02F) {
            notifications.remove(n);
         } else {
            visible.add(n);
         }
      }

      if (visible.size() > MAX_VISIBLE) {
         visible = visible.subList(visible.size() - MAX_VISIBLE, visible.size());
      }

      if (visible.isEmpty() && !editMode) {
         columnW = -1.0F;
         return;
      }

      float targetW = 0.0F;
      if (visible.isEmpty()) {
         targetW = cardWidth(PREVIEW_TITLE, PREVIEW_MESSAGE, false);
      } else {
         for (Notification n : visible) {
            targetW = Math.max(targetW, cardWidth(n.getTitle(), n.getMessage(), n.isToggle()));
         }
      }

      columnW = columnW < 0.0F ? targetW : HudStyle.expLerp(columnW, targetW, 12.0F, dt);
      float w = columnW;
      updateDrag(ctx, w, H);
      float screenW = ctx.guiWidth();
      float screenH = ctx.guiHeight();
      float dragCx = DRAG.getRenderX() + DRAG.getWidth() / 2.0F;
      int alignMode;
      float anchorX;
      if (dragCx < screenW / 3.0F) {
         alignMode = -1;
         anchorX = DRAG.getRenderX();
      } else if (dragCx > screenW * 2.0F / 3.0F) {
         alignMode = 1;
         anchorX = DRAG.getRenderX() + DRAG.getWidth();
      } else {
         alignMode = 0;
         anchorX = dragCx;
      }

      boolean stackUp = DRAG.getRenderY() + DRAG.getHeight() / 2.0F > screenH * 0.75F;
      float anchorY = stackUp ? DRAG.getRenderY() + DRAG.getHeight() : DRAG.getRenderY();
      float left = boxLeft(alignMode, anchorX, w);
      if (visible.isEmpty()) {
         drawCard(ctx, PREVIEW_TITLE, PREVIEW_MESSAGE, NotificationType.SUCCESS, null, null, Notification.NO_TOGGLE, 0.0F, left, stackUp ? anchorY - H : anchorY, w, 0.6F, -1.0F);
      } else {
         long nowMs = System.currentTimeMillis();
         float slot = 0.0F;

         for (Notification n : visible) {
            float av = n.getAnimation().getValue();
            boolean leaving = n.isExpired();
            if (!n.placed) {
               n.renderY = slot;
               n.placed = true;
            } else {
               n.renderY = HudStyle.expLerp(n.renderY, slot, 14.0F, dt);
            }

            if (n.isToggle() && nowMs - n.getStartTime() >= TOGGLE_DELAY_MS) {
               n.toggleAnim = HudStyle.expLerp(n.toggleAnim, n.getToggle() == 1 ? 1.0F : 0.0F, 16.0F, dt);
            }

            float x = left + alignMode * (1.0F - av) * (w + MARGIN * 2.0F);
            float y = stackUp ? anchorY - H - n.renderY : anchorY + n.renderY;
            if (alignMode == 0) {
               y += (stackUp ? 1.0F : -1.0F) * (1.0F - av) * 10.0F;
            }

            float progress = 1.0F - Math.max(0.0F, Math.min(1.0F, (float)(nowMs - n.getStartTime()) / (float)n.getDuration()));
            drawCard(ctx, n.getTitle(), n.getMessage(), n.getType(), n.getIcon(), n.getCategory(), n.getToggle(), n.toggleAnim, x, y, w, av, progress);
            slot += (H + GAP_V) * (leaving ? av : 1.0F);
         }
      }
   }

   private static void updateDrag(GuiGraphicsExtractor ctx, float width, float height) {
      if (!DRAG.isPositioned()) {
         DRAG.setX(ctx.guiWidth() - MARGIN - width);
         DRAG.setY(MARGIN);
         DRAG.syncRenderPosition();
         DRAG.setPositioned(true);
         defaultAnchored = true;
      } else if (defaultAnchored && !DRAG.isDragging()) {
         DRAG.setX(ctx.guiWidth() - MARGIN - width);
         DRAG.syncRenderPosition();
      }

      DRAG.setWidth(width);
      DRAG.setHeight(height);
      DRAG.update(ctx);
      if (DRAG.isDragging()) {
         defaultAnchored = false;
      }
   }

   private static void drawCard(
      GuiGraphicsExtractor ctx,
      String title,
      String msg,
      NotificationType type,
      ItemStack icon,
      ModuleCategory category,
      int toggle,
      float toggleAnim,
      float x,
      float y,
      float w,
      float a,
      float progress
   ) {
      if (a <= 0.01F) return;

      boolean isToggle = toggle != Notification.NO_TOGGLE;
      int accent = typeColor(type);
      HudStyle.panel(ctx, x, y, w, H, RADIUS, a);
      float tileX = x + PAD;
      float tileY = y + (H - TILE) / 2.0F;
      boolean hasItem = icon != null && !icon.isEmpty();
      Identifier categoryTex = category != null ? CATEGORY_ICONS.get(category) : null;
      boolean neutralTile = hasItem || categoryTex != null;
      int tileBg = neutralTile ? HudStyle.PILL : Render2DUtil.lerpColor(HudStyle.PILL, accent, 0.16F);
      Render2DUtil.roundedRect(ctx, tileX, tileY, TILE, TILE, TILE_RADIUS, HudStyle.fade(tileBg, a));
      float iconX = tileX + (TILE - TILE_ICON) / 2.0F;
      float iconY = tileY + (TILE - TILE_ICON) / 2.0F;
      if (hasItem) {
         if (a > 0.35F) {
            ctx.pose().pushMatrix();
            ctx.pose().translate(iconX, iconY);
            ctx.pose().scale(TILE_ICON / 16.0F);
            ctx.item(icon, 0, 0);
            ctx.pose().popMatrix();
         }
      } else if (categoryTex != null) {
         int tint = isToggle ? Render2DUtil.lerpColor(HudStyle.TEXT_OFF, HudStyle.TEXT, toggleAnim) : HudStyle.TEXT;
         Render2DUtil.roundedTexture(ctx, categoryTex, iconX, iconY, TILE_ICON, TILE_ICON, 0.0F, true, HudStyle.fade(tint, a));
      } else {
         drawGlyph(ctx, type, tileX + TILE / 2.0F, tileY + TILE / 2.0F, HudStyle.fade(accent, a));
      }

      float textX = tileX + TILE + TEXT_GAP;
      float right = x + w - RIGHT_PAD - (isToggle ? TOGGLE_W + TOGGLE_GAP : 0.0F);
      float blockH = 0.7F * TITLE_FS + LINE_GAP + 0.7F * MSG_FS;
      float titleY = y + (H - blockH) / 2.0F - 0.5F;
      float msgY = titleY + 0.7F * TITLE_FS + LINE_GAP;
      String shownTitle = HudStyle.truncate(HudStyle.title(), displayTitle(title, isToggle), TITLE_FS, right - textX);
      String shownMsg = HudStyle.truncate(HudStyle.body(), pizza.dlc.api.translation.Translations.text(msg), MSG_FS, right - textX);
      Render2DUtil.msdfText(ctx, HudStyle.title(), shownTitle, textX, titleY, TITLE_FS, HudStyle.fade(HudStyle.TEXT, a));
      Render2DUtil.msdfText(ctx, HudStyle.body(), shownMsg, textX, msgY, MSG_FS, HudStyle.fade(HudStyle.TEXT_DIM, a));
      if (isToggle) {
         drawToggle(ctx, x + w - RIGHT_PAD - TOGGLE_W, y + (H - TOGGLE_H) / 2.0F, toggleAnim, a);
      }

      if (progress > 0.0F) {
         HudStyle.accentBar(ctx, textX, y + H - 4.0F, x + w - RIGHT_PAD - textX, 1.0F, progress, a);
      }
   }

   private static void drawToggle(GuiGraphicsExtractor ctx, float x, float y, float t, float a) {
      Render2DUtil.roundedRect(ctx, x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2.0F, HudStyle.fade(TOGGLE_OFF, a));
      Accent.glow(ctx, x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2.0F, t * a, 4);
      Accent.fill(ctx, x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2.0F, t * a);
      float knob = TOGGLE_H - 4.0F;
      float kx = x + 2.0F + t * (TOGGLE_W - knob - 4.0F);
      Render2DUtil.roundedRect(ctx, kx, y + 2.0F, knob, knob, knob / 2.0F, HudStyle.fade(Render2DUtil.lerpColor(KNOB_OFF, 0xFFFFFFFF, t), a));
   }

   private static void drawGlyph(GuiGraphicsExtractor ctx, NotificationType type, float cx, float cy, int color) {
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      switch (type) {
         case SUCCESS:
            Render2DUtil.check(ctx, -6, -6, 12, 12, 1.7F, color);
            break;
         case ERROR:
            stroke(ctx, 9.0F, 1.7F, (float)(Math.PI / 4), color);
            stroke(ctx, 9.0F, 1.7F, (float)(-Math.PI / 4), color);
            break;
         case WARNING:
            Render2DUtil.roundedRect(ctx, -0.85F, -5.0F, 1.7F, 6.5F, 0.85F, color);
            Render2DUtil.roundedRect(ctx, -1.0F, 2.8F, 2.0F, 2.0F, 1.0F, color);
            break;
         default:
            Render2DUtil.roundedRect(ctx, -1.0F, -5.0F, 2.0F, 2.0F, 1.0F, color);
            Render2DUtil.roundedRect(ctx, -0.85F, -1.8F, 1.7F, 6.8F, 0.85F, color);
      }

      ctx.pose().popMatrix();
   }

   private static void stroke(GuiGraphicsExtractor ctx, float len, float thick, float angle, int color) {
      ctx.pose().pushMatrix();
      ctx.pose().rotate(angle);
      Render2DUtil.roundedRect(ctx, -len / 2.0F, -thick / 2.0F, len, thick, thick / 2.0F, color);
      ctx.pose().popMatrix();
   }

   private static int typeColor(NotificationType type) {
      return switch (type) {
         case SUCCESS -> HudStyle.GOOD;
         case ERROR -> HudStyle.BAD;
         case WARNING -> HudStyle.WARN;
         default -> HudStyle.TEXT;
      };
   }

   private static String displayTitle(String title, boolean toggle) {
      var client = pizza.dlc.client.PizzaDLCClient.getInstance();
      boolean module = client != null && client.getModuleManager() != null && title != null
         && client.getModuleManager().get(title) != null;
      return toggle || module ? pizza.dlc.api.translation.Translations.moduleName(title)
         : pizza.dlc.api.translation.Translations.text(title);
   }

   private static float cardWidth(String title, String msg, boolean toggle) {
      title = displayTitle(title, toggle);
      msg = pizza.dlc.api.translation.Translations.text(msg);
      float text = Math.max(HudStyle.title().width(title, TITLE_FS), HudStyle.body().width(msg, MSG_FS));
      float w = PAD + TILE + TEXT_GAP + text + RIGHT_PAD + (toggle ? TOGGLE_W + TOGGLE_GAP : 0.0F);
      return Math.max(MIN_W, Math.min(MAX_W, w));
   }

   private static float boxLeft(int alignMode, float anchorX, float w) {
      if (alignMode == -1) {
         return anchorX;
      }

      return alignMode == 1 ? anchorX - w : anchorX - w / 2.0F;
   }
}
