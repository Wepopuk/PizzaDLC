package pizza.dlc;

import java.lang.reflect.Field;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import pizza.dlc.api.ui.skin.OptionControlSkin;
import pizza.dlc.api.ui.skin.UiSkin;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import pizza.dlc.api.util.render.MsdfTextRenderState;
import pizza.dlc.mixin.client.GuiGraphicsExtractorAccessor;

/** Runs production drawing into Minecraft's GUI command buffer without a window or GPU. */
public final class OptionRenderingCheck {
   public static void main(String[] args) throws Exception {
      int commands = 0;
      for (int width : new int[]{156, 444}) {
         for (int offset : new int[]{0, -17}) {
            for (int index = 0; index < 5; index++) {
               int x = 180, y = 50 + index * 34 + offset;
               var slider = new AbstractSliderButton(0, 0, 150, 20, Component.literal("Chat\u2010Textdeckkraft: 70\u00A0%"), .7) {
                  @Override protected void updateMessage() { }
                  @Override protected void applyValue() { }
               };
               var button = Button.builder(Component.literal("Telemetry"), ignored -> { }).bounds(0, 0, 150, 20).build();
               for (var widget : java.util.List.of(slider, button)) {
                  var graphics = RecordingGraphics.create();
                  graphics.enableScissor(170, 25, 650, 350);
                  widget.active = index % 2 == 0;
                  OptionControlSkin.row(graphics, widget, x, y, width, 30, -1, -1);
                  if (widget instanceof AbstractSliderButton control) {
                     check(OptionControlSkin.drawSlider(graphics, control, index / 4.0, true), "Slider drawing skipped");
                  } else check(OptionControlSkin.drawButton(graphics, widget), "Button drawing skipped");
                  ArrayList<GuiElementRenderState> elements = new ArrayList<>();
                  graphics.guiRenderState.forEachElement(elements::add, GuiRenderState.TraverseRange.ALL);
                  check(elements.size() >= 5, "Control produced too few drawing commands");
                  for (var element : elements) {
                     var bounds = element.bounds();
                     if (bounds == null) continue;
                     check(bounds.left() >= x - 8 && bounds.right() <= x + width + 8
                        && bounds.top() >= y - 8 && bounds.bottom() <= y + 38,
                        "Drawing spills into other setting rows: " + element.getClass().getSimpleName() + " " + bounds);
                  }
                  if (widget instanceof AbstractSliderButton) {
                     check(elements.stream().anyMatch(element -> element instanceof MsdfTextRenderState text
                        && text.text().startsWith("Chat") && (width < 400 || text.text().equals("Chat-Textdeckkraft"))),
                        "German caption did not use the client font");
                  }
                  commands += elements.size();
               }
            }
         }
      }
      for (String nativeLabel : new String[]{"Chat\u2010Textdeckkraft", "Chat\u2010Textgröße", "Звʼязок", "⌘", "‚"}) {
         var graphics = RecordingGraphics.create();
         var text = Component.literal(nativeLabel).withStyle(Style.EMPTY.withColor(0x33CC77));
         check(UiSkin.drawMenuText(graphics, text, 30, 40, 0xFFFFFFFF), "Native list label fell back to Minecraft font: " + nativeLabel);
         ArrayList<GuiElementRenderState> elements = new ArrayList<>();
         graphics.guiRenderState.forEachElement(elements::add, GuiRenderState.TraverseRange.ALL);
         check(elements.stream().anyMatch(element -> element instanceof MsdfTextRenderState state
            && state.color() == 0xFF33CC77), "Menu text lost style color");
         var formatted = FormattedCharSequence.forward(nativeLabel, Style.EMPTY.withColor(0x33CC77));
         var restored = UiSkin.component(formatted);
         check(restored.getString().equals(nativeLabel), "Formatted menu text changed");
         check(UiSkin.drawCentered(graphics, restored, 325, 50, 0xFFFFFFFF), "Centered menu text fell back to Minecraft font");
      }
      System.out.println("Option rendering: " + commands + " real GUI commands checked; controls stay inside their rows, German captions use MSDF.");
   }

   private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }

   private static final class RecordingGraphics extends GuiGraphicsExtractor implements GuiGraphicsExtractorAccessor {
      private RecordingGraphics() { super(null, new GuiRenderState(), -1, -1); }
      @Override public int guiWidth() { return 650; }
      @Override public int guiHeight() { return 390; }
      @Override public GuiRenderState pizzadlc$guiRenderState() { return this.guiRenderState; }

      private static RecordingGraphics create() throws Exception {
         // Only the test skips the constructor: the normal constructor requires live GPU atlases.
         Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
         Field singleton = unsafeClass.getDeclaredField("theUnsafe");
         singleton.setAccessible(true);
         Object unsafe = singleton.get(null);
         var graphics = (RecordingGraphics)unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, RecordingGraphics.class);
         set(graphics, "pose", new Matrix3x2fStack(16));
         set(graphics, "guiRenderState", new GuiRenderState());
         var constructor = GuiGraphicsExtractor.ScissorStack.class.getDeclaredConstructor(ScreenRectangle.class);
         constructor.setAccessible(true);
         set(graphics, "scissorStack", constructor.newInstance(new ScreenRectangle(0, 0, 650, 390)));
         return graphics;
      }
      private static void set(RecordingGraphics graphics, String name, Object value) throws Exception {
         Field field = GuiGraphicsExtractor.class.getDeclaredField(name);
         field.setAccessible(true);
         field.set(graphics, value);
      }
   }
}
