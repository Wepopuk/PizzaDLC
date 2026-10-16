package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.animation.Animation;
import pizza.dlc.api.animation.Easing;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.util.input.KeyNameUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;

public final class HotKeysHud implements QuickImports {
   private static final String HEADER_TEXT = "Hotkeys";
   private static final float KEY_GAP = 10.0F;
   private static final long ROW_MS = 300L;
   private static final long PANEL_MS = 260L;
   private static final Easing ROW_EASE = Easing.EASE_OUT_CUBIC;
   private static final Easing PANEL_EASE = Easing.EASE_OUT_CUBIC;
   private final Map<Object, Animation> entries = new LinkedHashMap<>();
   private final Draggable drag = new Draggable("HotKeys", 0.0F, 0.0F, 0.0F, 0.0F);
   private final Animation animW = new Animation(PANEL_EASE, PANEL_MS);
   private final Animation panelVis = new Animation(PANEL_EASE, PANEL_MS);
   private boolean defaultAnchored;

   public void render(GuiGraphicsExtractor ctx, boolean editMode) {
      if (mc.level == null) return;

      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         for (Module m : cc.getModuleManager().getModules()) {
            if (isCandidate(m) && !this.entries.containsKey(m)) {
               this.entries.put(m, new Animation(ROW_EASE, ROW_MS));
            }

            for (Setting s : m.getSettings()) {
               if (s instanceof BooleanSetting b) {
                  if (isCandidate(b) && !this.entries.containsKey(b)) {
                     this.entries.put(b, new Animation(ROW_EASE, ROW_MS));
                  }
               } else if (s instanceof MultiModeSetting mm) {
                  for (BooleanSetting bx : mm.getBoolSettings()) {
                     if (!isCandidate(bx) || this.entries.containsKey(bx)) continue;

                     this.entries.put(bx, new Animation(ROW_EASE, ROW_MS));
                  }
               }
            }
         }

         MsdfFont font = HudStyle.body();
         float maxRowW = 0.0F;
         float contentH = 0.0F;
         int activeCount = 0;
         Iterator<Entry<Object, Animation>> it = this.entries.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Object, Animation> en = it.next();
            Object row = en.getKey();
            boolean active = isCandidate(row);
            float v = en.getValue().run(active ? 1.0F : 0.0F);
            if (!active && v <= 0.001F) {
               it.remove();
            } else {
               if (active) {
                  activeCount++;
               }

               contentH += v * HudStyle.ROW_H;
               float rowW = font.width(nameOf(row), HudStyle.ROW_FS) + KEY_GAP + HudStyle.pillWidth(keyName(keyOf(row)));
               maxRowW = Math.max(maxRowW, rowW);
            }
         }

         boolean placeholder = this.entries.isEmpty() && editMode;
         String phName = "Module";
         String phKey = "Key";
         if (placeholder) {
            contentH = HudStyle.ROW_H;
            maxRowW = Math.max(maxRowW, font.width(phName, HudStyle.ROW_FS) + KEY_GAP + HudStyle.pillWidth(phKey));
         }

         boolean wantVisible = !this.entries.isEmpty() || editMode;
         float pa = this.panelVis.run(wantVisible ? 1.0F : 0.0F);
         if (!wantVisible && pa < 0.01F) {
            this.animW.setValue(0.0F);
         } else {
            String badge = String.valueOf(activeCount);
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
               this.drag.setX(ctx.guiWidth() - 4 - panelW);
               this.drag.setY(4.0F);
               this.drag.syncRenderPosition();
               this.drag.setPositioned(true);
               this.defaultAnchored = true;
            } else if (this.defaultAnchored && !this.drag.isDragging()) {
               this.drag.setX(ctx.guiWidth() - 4 - panelW);
               this.drag.syncRenderPosition();
            }

            this.drag.update(ctx);
            if (this.drag.isDragging()) {
               this.defaultAnchored = false;
            }

            int panelX = (int)this.drag.getRenderX();
            int panelY = (int)this.drag.getRenderY();
            HudStyle.panel(ctx, panelX, panelY, panelW, panelHf, HudStyle.RADIUS, pa);
            HudStyle.header(ctx, panelX, panelY, panelW, HEADER_TEXT, badge, pa);
            float rowY = panelY + HudStyle.HEADER_H + HudStyle.ROWS_TOP;
            ctx.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
            if (placeholder) {
               this.drawRow(ctx, font, phName, phKey, panelX, panelW, rowY, HudStyle.ROW_H, 0.0F, 0.4F * pa);
            } else {
               for (Entry<Object, Animation> en : this.entries.entrySet()) {
                  Object row = en.getKey();
                  float v = en.getValue().getValue();
                  float slotH = v * HudStyle.ROW_H;
                  if (v > 0.001F) {
                     this.drawRow(ctx, font, nameOf(row), keyName(keyOf(row)), panelX, panelW, rowY, slotH, (1.0F - v) * HudStyle.SLIDE, v * pa);
                  }

                  rowY += slotH;
               }
            }

            ctx.disableScissor();
         }
      }
   }

   private void drawRow(GuiGraphicsExtractor ctx, MsdfFont font, String name, String key, int panelX, int panelW, float rowY, float slotH, float slide, float a) {
      float pillW = HudStyle.pillWidth(key);
      float pillX = panelX + panelW - HudStyle.PAD - pillW + slide;
      float maxName = pillX - KEY_GAP + 4.0F - (panelX + HudStyle.PAD + slide);
      String shown = HudStyle.truncate(font, name, HudStyle.ROW_FS, maxName);
      Render2DUtil.msdfText(ctx, font, shown, panelX + HudStyle.PAD + slide, HudStyle.textY(rowY, slotH, HudStyle.ROW_FS), HudStyle.ROW_FS, HudStyle.fade(HudStyle.TEXT, a));
      HudStyle.pill(ctx, pillX, rowY + (slotH - HudStyle.PILL_H) / 2.0F, pillW, key, HudStyle.TEXT, a);
   }

   private static boolean isCandidate(Object row) {
      if (row instanceof Module m) {
         return m.isEnabled() && m.getKey() != -1 && !m.isHidden() && !m.isPermanent();
      }

      return row instanceof BooleanSetting b && b.isEnabled() && b.getKey() != -1;
   }

   private static String nameOf(Object row) {
      if (row instanceof Module m) {
         return pizza.dlc.api.translation.Translations.moduleName(m.getName());
      }

      return row instanceof BooleanSetting b ? (b.isHoldBind() ? "H " : "") + pizza.dlc.api.translation.Translations.text(b.getName()) : "";
   }

   private static int keyOf(Object row) {
      if (row instanceof Module m) {
         return m.getKey();
      }

      return row instanceof BooleanSetting b ? b.getKey() : -1;
   }

   private static String keyName(int code) {
      return KeyNameUtil.displayName(code);
   }
}
