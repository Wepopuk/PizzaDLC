package pizza.dlc.client.modules.impl.visuals.hud;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.profiler.PizzaDLCProfiler;
import pizza.dlc.api.util.render.Render2DUtil;

public final class ProfilerHud implements QuickImports {
   private static final String HEADER_TEXT = "Профайлер";
   private static final int ROWS = 10;
   private static final float SIZE = 7.0F;
   private static final int ROW_H = 10;
   private static final int W = 140;
   private final Draggable drag = new Draggable("Profiler", 0.0F, 0.0F, 0.0F, 0.0F);

   public void render(GuiGraphicsExtractor ctx, boolean editMode) {
      List<PizzaDLCProfiler.Row> rows = PizzaDLCProfiler.top(ROWS);
      if (!rows.isEmpty() || editMode) {
         MsdfFont font = HudStyle.body();
         MsdfFont mono = HudStyle.mono();
         float contentH = Math.max(1, rows.size()) * ROW_H;
         float panelHf = HudStyle.listPanelH(contentH);
         this.drag.setWidth(W);
         this.drag.setHeight(Math.round(panelHf));
         if (!this.drag.isPositioned()) {
            this.drag.setX(4.0F);
            this.drag.setY(124.0F);
            this.drag.syncRenderPosition();
            this.drag.setPositioned(true);
         }

         this.drag.update(ctx);
         int px = (int)this.drag.getRenderX();
         int py = (int)this.drag.getRenderY();
         float frame = (float)PizzaDLCProfiler.frameMs();
         HudStyle.panel(ctx, px, py, W, panelHf, HudStyle.RADIUS, 1.0F);
         HudStyle.header(ctx, px, py, W, HEADER_TEXT, String.format(Locale.ROOT, "%.2f ms", frame), 1.0F);
         float y = py + HudStyle.HEADER_H + HudStyle.ROWS_TOP;

         for (PizzaDLCProfiler.Row row : rows) {
            float share = frame > 0.01F ? (float)(row.ms() / frame) : 0.0F;
            String right = String.format(Locale.ROOT, "%.2f  %.0f%%", row.ms(), share * 100.0F);
            float rightW = mono.width(right, SIZE);
            float rightX = px + W - HudStyle.PAD - rightW;
            float textY = HudStyle.textY(y, ROW_H, SIZE);
            String left = HudStyle.truncate(font, row.name(), SIZE, rightX - 6.0F - (px + HudStyle.PAD));
            Render2DUtil.msdfText(ctx, font, left, px + HudStyle.PAD, textY, SIZE, HudStyle.TEXT);
            Render2DUtil.msdfText(ctx, mono, right, rightX, textY, SIZE, weightColor(share));
            y += ROW_H;
         }
      }
   }

   private static int weightColor(float share) {
      if (share < 0.1F) {
         return HudStyle.TEXT_DIM;
      }

      if (share < 0.25F) {
         return HudStyle.TEXT;
      }

      return share < 0.4F ? HudStyle.WARN : HudStyle.BAD;
   }
}
