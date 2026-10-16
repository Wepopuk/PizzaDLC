package pizza.dlc.api.draggable.grid;

import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import pizza.dlc.api.draggable.Draggable;
import pizza.dlc.api.draggable.grid.line.GridLine;
import pizza.dlc.api.draggable.grid.line.GridRotationType;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.client.PizzaDLCClient;

public class Grid implements QuickImports {
   private final ArrayList<GridLine> lines = new ArrayList<>();
   private GridLine activeVerticalLine;
   private GridLine activeHorizontalLine;

   public void render(GuiGraphicsExtractor ctx) {
      this.activeHorizontalLine = null;
      this.activeVerticalLine = null;
      if (PizzaDLCClient.getInstance() != null && PizzaDLCClient.getInstance().getDraggableManager() != null) {
         this.updateLineList();
         float closestX = Float.MAX_VALUE;
         float closestY = Float.MAX_VALUE;

         for (GridLine line : this.lines) {
            for (Draggable d : PizzaDLCClient.getInstance().getDraggableManager().getDraggables().values()) {
               if (!d.isDragging()) continue;

               float distance = this.calculateDistance(d, line)[0];
               if (line.getRotationType() == GridRotationType.HORIZONTAL) {
                  if (distance < 5.0F && distance < closestY) {
                     closestY = distance;
                     this.activeHorizontalLine = line;
                  }
               } else if (distance < 5.0F && distance < closestX) {
                  closestX = distance;
                  this.activeVerticalLine = line;
               }
            }
         }

         if (this.activeVerticalLine != null) {
            this.activeVerticalLine.render(ctx);
         }

         if (this.activeHorizontalLine != null) {
            this.activeHorizontalLine.render(ctx);
         }
      }
   }

   public void handleDragPositions() {
      if (PizzaDLCClient.getInstance() == null || PizzaDLCClient.getInstance().getDraggableManager() == null) return;

      for (Draggable d : PizzaDLCClient.getInstance().getDraggableManager().getDraggables().values()) {
         if (!d.isDragging()) continue;

         if (this.activeHorizontalLine != null) {
            float[] data = this.calculateDistance(d, this.activeHorizontalLine);
            if (data[0] < 5.0F) {
               d.setY(this.activeHorizontalLine.getCoordinate() - data[1]);
            }
         }

         if (this.activeVerticalLine != null) {
            float[] data = this.calculateDistance(d, this.activeVerticalLine);
            if (data[0] < 5.0F) {
               d.setX(this.activeVerticalLine.getCoordinate() - data[1]);
            }
         }
      }
   }

   private float[] calculateDistance(Draggable d, GridLine line) {
      boolean horizontal = line.getRotationType() == GridRotationType.HORIZONTAL;
      float coord = horizontal ? d.getY() : d.getX();
      float size = horizontal ? d.getHeight() : d.getWidth();
      float dist = Math.abs(coord - line.getCoordinate());
      float distHalf = Math.abs(coord + size / 2.0F - line.getCoordinate());
      float distFull = Math.abs(coord + size - line.getCoordinate());
      float minDist = Math.min(dist, Math.min(distHalf, distFull));
      float offset = minDist == distHalf ? size / 2.0F : (minDist == distFull ? size : 0.0F);
      return new float[]{minDist, offset};
   }

   public void addHorizontalLine(float y) {
      this.lines.add(new GridLine(y, GridRotationType.HORIZONTAL));
   }

   public void addVerticalLine(float x) {
      this.lines.add(new GridLine(x, GridRotationType.VERTICAL));
   }

   public void updateLineList() {
      this.lines.clear();
      float screenW = mc.getWindow().getGuiScaledWidth();
      float screenH = mc.getWindow().getGuiScaledHeight();
      this.addVerticalLine(screenW / 2.0F - 0.5F);
      this.addHorizontalLine(screenH / 2.0F - 0.5F);
      this.addHorizontalLine(6.0F);
      this.addHorizontalLine(screenH - 7.0F);
      this.addVerticalLine(6.0F);
      this.addVerticalLine(screenW - 7.0F);
      if (PizzaDLCClient.getInstance() != null && PizzaDLCClient.getInstance().getDraggableManager() != null) {
         for (Draggable other : PizzaDLCClient.getInstance().getDraggableManager().getDraggables().values()) {
            if (other.isDragging() || other.getWidth() <= 0.0F || other.getHeight() <= 0.0F) continue;

            float ox = other.getX();
            float oy = other.getY();
            float ow = other.getWidth();
            float oh = other.getHeight();
            this.addVerticalLine(ox);
            this.addVerticalLine(ox + ow / 2.0F);
            this.addVerticalLine(ox + ow);
            this.addHorizontalLine(oy);
            this.addHorizontalLine(oy + oh / 2.0F);
            this.addHorizontalLine(oy + oh);
         }
      }
   }

   public ArrayList<GridLine> getLines() {
      return this.lines;
   }

   public GridLine getActiveVerticalLine() {
      return this.activeVerticalLine;
   }

   public GridLine getActiveHorizontalLine() {
      return this.activeHorizontalLine;
   }
}
