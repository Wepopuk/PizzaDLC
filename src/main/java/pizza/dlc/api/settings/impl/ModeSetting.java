package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;

public class ModeSetting extends Setting {
   private final List<String> modes;
   private String currentMode;
   private final Map<String, Integer> modeKeys;

   public ModeSetting(String name, Parent parent, String... modes) {
      super(name, parent);
      this.modes = Arrays.asList(modes);
      this.currentMode = modes.length > 0 ? modes[0] : "";
      this.modeKeys = new HashMap<>();

      for (String mode : modes) {
         this.modeKeys.put(mode, -1);
      }
   }

   public String get() {
      return this.currentMode;
   }

   public boolean is(int index) {
      return index >= 0 && index < this.modes.size() && this.is(this.modes.get(index));
   }

   public int getIndex() {
      int index = 0;

      for (String val : this.modes) {
         if (val.equalsIgnoreCase(this.currentMode)) {
            return index;
         }

         index++;
      }

      return 0;
   }

   public void setCurrentMode(String mode) {
      if (!this.modes.contains(mode)) return;

      this.currentMode = mode;
   }

   public void setModeKey(String mode, int keyCode) {
      if (!this.modes.contains(mode)) return;

      this.modeKeys.put(mode, keyCode);
   }

   public String getModeString(int key) {
      for (Entry<String, Integer> entry : this.modeKeys.entrySet()) {
         if (entry.getValue() != key) continue;

         return entry.getKey();
      }

      return this.getName();
   }

   public int getModeKey(String mode) {
      return this.modeKeys.getOrDefault(mode, -1);
   }

   public void nextMode() {
      int index = this.modes.indexOf(this.currentMode);
      index = (index + 1) % this.modes.size();
      this.currentMode = this.modes.get(index);
   }

   public boolean is(String mode) {
      return this.currentMode.equalsIgnoreCase(mode);
   }

   public String getTranslatedMode(String mode) {
      return pizza.dlc.api.translation.Translations.text(mode);
   }

   public String getTranslatedCurrentMode() {
      return getTranslatedMode(this.currentMode);
   }

   @Override
   public JsonElement save() {
      JsonObject object = new JsonObject();
      object.addProperty("currentMode", this.currentMode);
      JsonObject keys = new JsonObject();

      for (Entry<String, Integer> entry : this.modeKeys.entrySet()) {
         keys.addProperty(entry.getKey(), entry.getValue());
      }

      object.add("modeKeys", keys);
      return object;
   }

   @Override
   public void load(JsonElement element) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("currentMode")) {
         this.setCurrentMode(object.get("currentMode").getAsString());
      }

      if (object.has("modeKeys")) {
         JsonObject keys = object.get("modeKeys").getAsJsonObject();

         for (String mode : this.modes) {
            if (!keys.has(mode)) continue;

            this.modeKeys.put(mode, keys.get(mode).getAsInt());
         }
      }
   }

   public List<String> getModes() {
      return this.modes;
   }

   public String getCurrentMode() {
      return this.currentMode;
   }
}
