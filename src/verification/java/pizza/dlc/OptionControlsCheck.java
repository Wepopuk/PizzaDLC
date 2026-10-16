package pizza.dlc;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import pizza.dlc.api.render.msdf.MsdfFont;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.skin.OptionControlLayout;
import pizza.dlc.api.ui.skin.OptionControlText;

/** Layout/input agreement, localized units, and the Minecraft methods used by UI mixins. */
public final class OptionControlsCheck {
   public static void main(String[] args) throws Exception {
      var button = Button.builder(Component.literal("Option"), ignored -> { }).bounds(0, 0, 150, 20).build();
      var nativeSlider = new AbstractSliderButton(0, 0, 150, 20, Component.literal("Volume: 70%"), .7) {
         @Override protected void updateMessage() { }
         @Override protected void applyValue() { }
      };
      for (int width = 240; width <= 1920; width += 16) {
         for (int height : new int[]{180, 240, 360, 1080}) {
            var frame = ScreenFrame.fit(width, height, true, 160);
            if (frame == null) continue;
            check(frame.winX() >= 0 && frame.winY() >= 0 && frame.winX() + frame.winW() <= width
               && frame.bottom() <= height, "Frame leaves screen");
            check(frame.contentX() + frame.contentW() <= frame.winX() + frame.winW() - 8, "Content leaves frame");
            if (frame.sidebar()) check(frame.sideX() + frame.sidebarW() + 8 == frame.contentX(), "Sidebar overlaps options");
            var row = OptionControlLayout.row(frame.contentX() + 8, frame.winY() + 35, frame.contentW() - 20, 30);
            for (var widget : java.util.List.of(button, nativeSlider)) {
               row.place(widget);
               check(widget.getX() == row.controlX() && widget.getY() == row.controlY()
                  && widget.getWidth() == row.controlW() && widget.getHeight() == row.controlH(), "Actual Minecraft widget has wrong bounds");
               check(widget.isMouseOver(row.controlX() + row.controlW() / 2.0, row.controlY() + 10)
                  && !widget.isMouseOver(row.labelX(), row.controlY() + 10), "Widget hit area is outside right control column");
            }
            check(row.labelX() + row.labelW() + 12 <= row.controlX(), "Label overlaps control");
            check(row.controlX() + row.controlW() <= frame.contentX() + frame.contentW() - 8, "Control leaves content");
            for (float textWidth : new float[]{12, 28, 60, 120, 300}) {
               var slider = OptionControlLayout.slider(row.controlX(), row.controlY(), row.controlW(), row.controlH(), textWidth);
               check(slider.trackW() > 0 && slider.trackX() - 4 >= row.controlX(), "Invalid track");
               check(slider.trackX() + slider.trackW() + 4 < slider.valueX(), "Knob overlaps value");
               check(slider.valueX() + slider.valueW() <= row.controlX() + row.controlW() + .01F, "Value leaves control");
               check(OptionControlLayout.ratio(slider.trackX() - 50, slider) == 0
                  && OptionControlLayout.ratio(slider.trackX() + slider.trackW() + 50, slider) == 1, "Track endpoints unreachable");
               for (int i = 0; i <= 100; i++) {
                  double value = i / 100.0;
                  check(Math.abs(OptionControlLayout.ratio(slider.knobX(value), slider) - value) < .00001, "Visual knob and drag mapping disagree");
               }
            }
         }
      }
      check(ScreenFrame.fit(320, 180, true, 160).sidebar(), "Small settings window loses categories");
      check(ScreenFrame.fit(160, 90, true, 160) == null, "Tiny window needs native fallback");
      Language original = Language.getInstance();
      try {
         for (String caption : new String[]{"Volume", "Громкость", "Гучність", "Głośność", "Lautstärke"}) {
            Language.inject(new Language() {
               @Override public String getOrDefault(String key, String fallback) {
                  return switch (key) {
                     case "options.generic_value" -> "%s: %s";
                     case "options.percent_value" -> "%s: %s%%";
                     case "options.percent_add_value" -> "%s: +%s%%";
                     case "options.pixel_value" -> "%s: %spx";
                     default -> original.getOrDefault(key, fallback);
                  };
               }
               @Override public boolean has(String key) { return original.has(key); }
               @Override public boolean isDefaultRightToLeft() { return false; }
               @Override public FormattedCharSequence getVisualOrder(FormattedText text) { return original.getVisualOrder(text); }
            });
            for (var sample : Map.of("options.generic_value", "Speakers: HDMI", "options.percent_value", "70%",
               "options.percent_add_value", "+70%", "options.pixel_value", "70px").entrySet()) {
               Object value = sample.getKey().equals("options.generic_value") ? "Speakers: HDMI" : 70;
               var parsed = OptionControlText.from(Component.translatable(sample.getKey(), Component.literal(caption), value));
               check(parsed.label().equals(caption) && parsed.value().equals(sample.getValue()), "Lost caption/units: " + parsed);
            }
            var literal = OptionControlText.from(Component.literal(caption + ": Speakers: HDMI"));
            check(literal.label().equals(caption) && literal.value().equals("Speakers: HDMI"), "Value colon was mistaken for label separator");
         }
      } finally { Language.inject(original); }
      check(OptionControlText.from(Component.literal("Menu")).value().isEmpty(), "Action label became a value");
      var book = MsdfFont.load("nexa_book");
      for (String nativeLabel : new String[]{"Chat\u2010Textdeckkraft", "Text\u00ADhintergrund\u2011Deckkraft", "Chat\u2010Textgröße", "100\u00A0%", "Звʼязок", "⌘", "‚"}) {
         String display = OptionControlText.display(nativeLabel);
         check(display.codePoints().allMatch(cp -> cp == ' ' || book.glyph(cp) != null), "German caption fell back to pixel font: " + display);
      }
      var menuGlyphs = JsonParser.parseString(Files.readString(Path.of("tools/minecraft_ui_glyphs.json"))).getAsJsonObject().getAsJsonArray("codepoints");
      for (String fontName : new String[]{"nexa_regular", "nexa_book", "sfpro", "regular"}) {
         var font = MsdfFont.load(fontName);
         for (var codepoint : menuGlyphs) check(font.glyph(codepoint.getAsInt()) != null,
            "Missing actual Minecraft menu glyph in " + fontName + ": U+" + Integer.toHexString(codepoint.getAsInt()));
      }
      method("net/minecraft/client/gui/GuiGraphicsExtractor", "text", "(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V");
      method("net/minecraft/client/gui/GuiGraphicsExtractor", "text", "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V");
      method("net/minecraft/client/gui/GuiGraphicsExtractor", "text", "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V");
      method("net/minecraft/client/gui/GuiGraphicsExtractor", "centeredText", "(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V");
      method("net/minecraft/client/gui/GuiGraphicsExtractor", "centeredText", "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V");
      method("net/minecraft/client/gui/GuiGraphicsExtractor", "centeredText", "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V");
      method("net/minecraft/client/gui/components/AbstractSliderButton", "setValueFromMouse", "(Lnet/minecraft/client/input/MouseButtonEvent;)V");
      method("net/minecraft/client/gui/components/AbstractSliderButton", "extractWidgetRenderState", "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V");
      method("net/minecraft/client/gui/components/AbstractButton", "extractWidgetRenderState", "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V");
      method("net/minecraft/client/gui/components/AbstractSelectionList", "updateSizeAndPosition", "(IIII)V");
      method("net/minecraft/client/gui/components/AbstractSelectionList", "setScrollAmount", "(D)V");
      method("net/minecraft/client/gui/components/OptionsList$Entry", "extractContent", "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZF)V");
      var subclass = node("net/minecraft/client/OptionInstance$OptionInstanceSliderButton");
      var render = subclass.methods.stream().filter(m -> m.name.equals("extractWidgetRenderState")).findFirst().orElseThrow();
      boolean callsBase = false;
      for (var instruction : render.instructions) if (instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
         && call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("extractWidgetRenderState")) callsBase = true;
      check(callsBase, "Slider rendering hook would bypass delayed option application");
      exportLayout();
      System.out.println("Option controls: actual Minecraft widget bounds/hit areas, responsive columns, slider mapping, localized fonts/values and API checks passed.");
   }

   private static void exportLayout() throws Exception {
      var font = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/pizzadlc/fonts/msdf/nexa_book.json"))).getAsJsonObject();
      Map<Integer, Float> advances = new java.util.HashMap<>();
      font.getAsJsonArray("glyphs").forEach(item -> {
         var glyph = item.getAsJsonObject();
         advances.put(glyph.get("unicode").getAsInt(), glyph.get("advance").getAsFloat());
      });
      ArrayList<Object> screens = new ArrayList<>();
      for (int width : new int[]{650, 320}) {
         int height = width == 320 ? 180 : 390;
         var frame = ScreenFrame.fit(width, height, true, 160);
         ArrayList<Object> rows = new ArrayList<>();
         for (int i = 0; i < 8; i++) {
            String value = new String[]{"70%", "100%", "12", "60%", "ON", "OFF", "Fancy", "Open"}[i];
            float textWidth = (float)value.codePoints().mapToDouble(cp -> advances.getOrDefault(cp, .5F)).sum() * 7.5F;
            int x = frame.contentX() + 10, y = frame.winY() + 36 + i * OptionControlLayout.ROW_HEIGHT;
            int rowWidth = frame.contentW() - 20;
            var row = OptionControlLayout.row(x, y, rowWidth, 30);
            rows.add(Map.of("x", x, "y", y, "width", rowWidth, "height", 30, "row", row,
               "slider", OptionControlLayout.slider(row.controlX(), row.controlY(), row.controlW(), row.controlH(), textWidth), "value", value));
         }
         screens.add(Map.of("width", width, "height", height, "frame", frame, "rows", rows));
      }
      Path file = Path.of("build/settings-previews/layout.json");
      Files.createDirectories(file.getParent());
      Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(screens));
   }
   private static ClassNode node(String name) throws Exception {
      try (var stream = OptionControlsCheck.class.getClassLoader().getResourceAsStream(name + ".class")) {
         check(stream != null, "Missing Minecraft target " + name);
         ClassNode node = new ClassNode();
         new ClassReader(stream).accept(node, 0);
         return node;
      }
   }
   private static void method(String owner, String name, String descriptor) throws Exception {
      check(node(owner).methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor)), "Minecraft API changed: " + owner + "." + name);
   }
   private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
