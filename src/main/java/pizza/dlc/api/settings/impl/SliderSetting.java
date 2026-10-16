package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;

public class SliderSetting extends Setting {
   private float min;
   private float max;
   private float step;
   private float value;

   public SliderSetting(String name, Parent parent, float defaultValue, float min, float max, float step) {
      super(name, parent);
      this.step = step;
      this.min = min;
      this.max = max;
      this.value = defaultValue;
   }

   @Override
   public JsonElement save() {
      return new JsonPrimitive(this.value);
   }

   @Override
   public void load(JsonElement element) {
      if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) return;

      this.setCurrentValue(element.getAsFloat());
   }

   public void setCurrentValue(float currentValue) {
      this.value = Math.max(this.min, Math.min(this.max, currentValue));
   }

   public float getMin() {
      return this.min;
   }

   public float getMax() {
      return this.max;
   }

   public float getStep() {
      return this.step;
   }

   public float getValue() {
      return this.value;
   }

   public void setMin(float min) {
      this.min = min;
   }

   public void setMax(float max) {
      this.max = max;
   }

   public void setStep(float step) {
      this.step = step;
   }

   public void setValue(float value) {
      this.value = value;
   }
}
