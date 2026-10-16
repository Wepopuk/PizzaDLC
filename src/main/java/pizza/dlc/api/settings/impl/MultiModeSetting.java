package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;

public class MultiModeSetting extends Setting {
   private final List<BooleanSetting> boolSettings = new ArrayList<>();

   public MultiModeSetting(String name, Parent parent, String... values) {
      super(name, parent);
      Arrays.stream(values).forEach(value -> this.boolSettings.add((BooleanSetting)new BooleanSetting(value, null).setVisible(() -> false)));
   }

   public BooleanSetting get(String name) {
      return this.boolSettings.stream().filter(value -> value.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public BooleanSetting get(int index) {
      return index >= 0 && index < this.boolSettings.size() ? this.boolSettings.get(index) : null;
   }

   public BooleanSetting getRandomEnabledElement() {
      List<BooleanSetting> enabledElements = this.boolSettings.stream().filter(BooleanSetting::isEnabled).toList();
      if (!enabledElements.isEmpty()) {
         Random random = new Random();
         return enabledElements.get(random.nextInt(enabledElements.size()));
      }

      return null;
   }

   @Override
   public JsonElement save() {
      JsonObject object = new JsonObject();

      for (BooleanSetting setting : this.boolSettings) {
         JsonObject settingObject = new JsonObject();
         settingObject.addProperty("enabled", setting.isEnabled());
         settingObject.addProperty("key", setting.getKey());
         object.add(setting.getName(), settingObject);
      }

      return object;
   }

   @Override
   public void load(JsonElement element) {
      JsonObject object = element.getAsJsonObject();

      for (BooleanSetting setting : this.boolSettings) {
         if (!object.has(setting.getName())) continue;

         JsonObject settingObject = object.get(setting.getName()).getAsJsonObject();
         if (settingObject.has("enabled")) {
            setting.setEnabled(settingObject.get("enabled").getAsBoolean());
         }

         if (settingObject.has("key")) {
            setting.setKey(settingObject.get("key").getAsInt());
         }
      }
   }

   public List<BooleanSetting> getBoolSettings() {
      return this.boolSettings;
   }
}
