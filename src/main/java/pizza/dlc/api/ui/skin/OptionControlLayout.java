package pizza.dlc.api.ui.skin;

import net.minecraft.client.gui.components.AbstractWidget;

/** Shared visual and input geometry for settings rows and sliders, in GUI coordinates. */
public final class OptionControlLayout {
   public static final int ROW_HEIGHT = 34;

   private OptionControlLayout() { }

   public static Row row(int x, int y, int width, int height) {
      int pad = Math.min(10, Math.max(3, width / 24));
      int controlW = Math.max(60, Math.min(216, Math.round(width * .46F)));
      controlW = Math.min(controlW, Math.max(1, width - pad * 2 - 24));
      int controlX = x + width - pad - controlW;
      return new Row(x + pad, Math.max(1, controlX - x - pad - 12), controlX,
         y + Math.max(0, (height - 20) / 2), controlW, Math.min(20, height));
   }

   public static Slider slider(int x, int y, int width, int height, float valueTextWidth) {
      int pad = Math.min(6, Math.max(2, width / 24));
      float valueW = Math.min(Math.max(24, valueTextWidth + 12), Math.max(16, width * .42F));
      int valueX = Math.round(x + width - pad - valueW);
      int valueY = y + Math.max(0, (height - 14) / 2);
      float trackX = x + pad + 4;
      float trackEnd = Math.max(trackX + 1, valueX - 10);
      float trackY = y + height / 2.0F - 2;
      return new Slider(trackX, trackY, trackEnd - trackX, 4, valueX, valueY, valueW, Math.min(14, height));
   }

   public static double ratio(double mouseX, Slider slider) {
      return Math.max(0, Math.min(1, (mouseX - slider.trackX()) / slider.trackW()));
   }

   public record Row(int labelX, int labelW, int controlX, int controlY, int controlW, int controlH) {
      public void place(AbstractWidget widget) {
         // Minecraft's setRectangle takes width/height before x/y; use explicit setters.
         widget.setSize(controlW, controlH);
         widget.setPosition(controlX, controlY);
      }
   }
   public record Slider(float trackX, float trackY, float trackW, float trackH, int valueX, int valueY, float valueW, int valueH) {
      public float knobX(double value) { return trackX + (float)Math.max(0, Math.min(1, value)) * trackW; }
   }
}
