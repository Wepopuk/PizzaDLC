package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.function.Supplier;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;
import pizza.dlc.api.util.color.ColorRGBA;

public class ColorSetting extends Setting {
   private int argb;

   public ColorSetting(String name, Parent parent, int argb) {
      super(name, parent);
      this.argb = argb;
   }

   public ColorSetting(String name, Parent parent, ColorRGBA color) {
      super(name, parent);
      this.argb = color.getRGB();
   }

   public ColorSetting(String name, Parent parent, int argb, Supplier<Boolean> visible) {
      super(name, parent);
      this.argb = argb;
      this.setHideCondition(() -> !visible.get());
   }

   public int getArgb() {
      return this.argb;
   }

   public void setArgb(int argb) {
      this.argb = argb;
   }

   public void setColor(int argb) {
      this.argb = argb;
   }

   public void setColor(ColorRGBA color) {
      this.argb = color.getRGB();
   }

   public ColorRGBA getColor() {
      return new ColorRGBA(this.argb);
   }

   public int getIntColor() {
      return this.argb;
   }

   @Override
   public JsonElement save() {
      return new JsonPrimitive(this.argb);
   }

   @Override
   public void load(JsonElement element) {
      if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return;

      this.argb = element.getAsInt();
   }
}
