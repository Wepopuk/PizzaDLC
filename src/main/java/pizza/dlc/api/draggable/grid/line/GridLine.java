package pizza.dlc.api.draggable.grid.line;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render2DUtil;

public class GridLine implements QuickImports {
   private final float coordinate;
   private final GridRotationType rotationType;

   private static int color() {
      return ThemeManager.accentAlpha(153);
   }

   public void render(GuiGraphicsExtractor ctx) {
      int w = mc.getWindow().getGuiScaledWidth();
      int h = mc.getWindow().getGuiScaledHeight();
      if (this.rotationType == GridRotationType.HORIZONTAL) {
         Render2DUtil.line(ctx, 0.0F, this.coordinate, w, this.coordinate, 1.0F, color());
      } else {
         Render2DUtil.line(ctx, this.coordinate, 0.0F, this.coordinate, h, 1.0F, color());
      }
   }

   public float getCoordinate() {
      return this.coordinate;
   }

   public GridRotationType getRotationType() {
      return this.rotationType;
   }

   public GridLine(float coordinate, GridRotationType rotationType) {
      this.coordinate = coordinate;
      this.rotationType = rotationType;
   }
}
