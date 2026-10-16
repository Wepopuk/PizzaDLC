package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.animation.Animation;
import pizza.dlc.api.animation.Easing;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.render.ArrayListShadow;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;

public final class ArrayListHud implements QuickImports {
   private static final float SIZE = 8.0F;
   private static final int ROW_H = 12;
   private static final float ROW_STEP = 11.7F;
   private static final float GAP = -0.3F;
   private static final int PAD_X = 5;
   private static final float RADIUS = 3.0F;
   private static final int MARGIN = 4;
   private static final long ROW_MS = 280L;
   private static final Easing EASE = Easing.EASE_OUT_CUBIC;
   private static final int MODE_GAP = 4;
   private final Map<Module, Animation> entries = new LinkedHashMap<>();
   private final Draggable drag = new Draggable("ArrayList", 0.0F, 0.0F, 0.0F, 0.0F);
   private int anchorMode = 1;
   private float anchorX;
   private boolean anchorInit;

   public void render(GuiGraphicsExtractor ctx, boolean editMode, Predicate<Module> shown, boolean showMode, boolean lowercase) {
      if (mc.level == null) return;

      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         MsdfFont font = HudStyle.title();

         for (Module m : cc.getModuleManager().getModules()) {
            if (!isCandidate(m, shown) || this.entries.containsKey(m)) continue;

            this.entries.put(m, new Animation(EASE, ROW_MS));
         }

         List<Row> rows = new ArrayList<>();
         Iterator<Entry<Module, Animation>> it = this.entries.entrySet().iterator();

         while (it.hasNext()) {
            Entry<Module, Animation> en = it.next();
            Module mx = en.getKey();
            boolean active = isCandidate(mx, shown);
            float v = en.getValue().run(active ? 1.0F : 0.0F);
            if (!active && v <= 0.001F) {
               it.remove();
            } else {
               String name = pizza.dlc.api.translation.Translations.moduleName(mx.getName());
               String mode = showMode ? modeOf(mx) : null;
               if (lowercase) {
                  name = name.toLowerCase(Locale.ROOT);
                  if (mode != null) {
                     mode = mode.toLowerCase(Locale.ROOT);
                  }
               }

               rows.add(new Row(name, mode, v, rowWidth(font, name, mode)));
            }
         }

         if (rows.isEmpty() && editMode) {
            String ph = pizza.dlc.api.translation.Translations.text("ArrayList");
            if (lowercase) ph = ph.toLowerCase(Locale.ROOT);
            rows.add(new Row(ph, null, 0.4F, rowWidth(font, ph, null)));
         }

         if (!rows.isEmpty()) {
            rows.sort(Comparator.<Row>comparingDouble(rxx -> rxx.w()).reversed());
            float maxW = 0.0F;
            float contentH = 0.0F;

            for (Row r : rows) {
               maxW = Math.max(maxW, r.w());
               contentH += r.v() * ROW_STEP;
            }

            int panelW = (int)Math.ceil(maxW);
            int panelH = Math.max(ROW_H, Math.round(contentH));
            this.drag.setWidth(panelW);
            this.drag.setHeight(panelH);
            if (!this.drag.isPositioned()) {
               this.drag.setX(ctx.guiWidth() - MARGIN - panelW);
               this.drag.setY(MARGIN);
               this.drag.syncRenderPosition();
               this.drag.setPositioned(true);
               this.anchorMode = 1;
               this.anchorX = ctx.guiWidth() - MARGIN;
               this.anchorInit = true;
            }

            if (this.anchorInit && !this.drag.isDragging()) {
               this.drag.setX(this.anchorMode == 1 ? this.anchorX - panelW : (this.anchorMode == -1 ? this.anchorX : this.anchorX - panelW / 2.0F));
               this.drag.syncRenderPosition();
            }

            this.drag.update(ctx);
            float screenW = ctx.guiWidth();
            float screenH = ctx.guiHeight();
            float dragX = this.drag.getRenderX();
            float dragY = this.drag.getRenderY();
            if (this.drag.isDragging() || !this.anchorInit) {
               float dcx = dragX + panelW / 2.0F;
               if (dcx < screenW / 3.0F) {
                  this.anchorMode = -1;
                  this.anchorX = dragX;
               } else if (dcx > screenW * 2.0F / 3.0F) {
                  this.anchorMode = 1;
                  this.anchorX = dragX + panelW;
               } else {
                  this.anchorMode = 0;
                  this.anchorX = dragX + panelW / 2.0F;
               }

               this.anchorInit = true;
            }

            int alignMode = this.anchorMode;
            boolean stackUp = dragY + panelH / 2.0F > screenH * 0.75F;
            float anchorY = stackUp ? dragY + panelH : dragY;
            boolean flushLeft = alignMode == -1;
            float curY = anchorY;

            for (Row r : rows) {
               float v = r.v();
               float slotH = v * ROW_H;
               float rowTop = stackUp ? curY - slotH : curY;
               if (v > 0.001F) {
                  float slide = alignMode * (1.0F - v) * (r.w() + 4.0F);
                  float lx = boxLeft(alignMode, this.anchorX, r.w()) + slide;
                  ArrayListShadow.submit(lx, rowTop, r.w(), slotH, RADIUS, flushLeft, v);
                  HudStyle.panelBg(
                     ctx, lx, rowTop, r.w(), slotH, flushLeft ? 0.0F : RADIUS, flushLeft ? RADIUS : 0.0F, flushLeft ? RADIUS : 0.0F, flushLeft ? 0.0F : RADIUS, v
                  );
                  float tx = lx + PAD_X;
                  float textY = HudStyle.textY(rowTop, slotH, SIZE);
                  Render2DUtil.msdfText(ctx, font, r.name(), tx, textY, SIZE, HudStyle.fade(HudStyle.TEXT, v));
                  if (r.mode() != null) {
                     float nameW = font.width(r.name(), SIZE);
                     Render2DUtil.msdfText(ctx, HudStyle.body(), r.mode(), tx + nameW + MODE_GAP, textY, SIZE, HudStyle.fade(HudStyle.TEXT_DIM, v));
                  }
               }

               curY += stackUp ? -(slotH + GAP * v) : slotH + GAP * v;
            }
         }
      }
   }

   private static float rowWidth(MsdfFont font, String name, String mode) {
      float w = font.width(name, SIZE) + PAD_X * 2;
      if (mode != null) {
         w += MODE_GAP + HudStyle.body().width(mode, SIZE);
      }

      return w;
   }

   private static float boxLeft(int alignMode, float anchorX, float w) {
      if (alignMode == -1) {
         return anchorX;
      }

      return alignMode == 1 ? anchorX - w : anchorX - w / 2.0F;
   }

   private static boolean isCandidate(Module m, Predicate<Module> shown) {
      return m.isEnabled() && !m.isHidden() && !m.isPermanent() && (shown == null || shown.test(m));
   }

   private static String modeOf(Module m) {
      for (Setting s : m.getSettings()) {
         if (!(s instanceof ModeSetting md)) continue;

         Supplier<Boolean> hc = md.getHideCondition();
         if (hc == null || !Boolean.TRUE.equals(hc.get())) {
            return pizza.dlc.api.translation.Translations.text(md.get());
         }
      }

      return null;
   }

   private record Row(String name, String mode, float v, float w) {
   }
}
