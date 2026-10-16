package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;

public class StringSetting extends Setting {
   private String text = "";
   private int maxLength = 100;

   public StringSetting(String name, Parent parent) {
      super(name, parent);
   }

   public StringSetting(String name, String description, Parent parent) {
      super(name, description, parent);
   }

   public StringSetting(String name, String description, String defaultValue, Parent parent) {
      super(name, description, parent);
      this.text = defaultValue;
   }

   public StringSetting(String name, String description, String defaultValue, int maxLength, Parent parent) {
      super(name, description, parent);
      this.text = defaultValue;
      this.maxLength = maxLength;
   }

   @Override
   public JsonElement save() {
      JsonObject object = new JsonObject();
      object.addProperty("text", this.text);
      object.addProperty("maxLength", this.maxLength);
      return object;
   }

   @Override
   public void load(JsonElement element) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("text")) {
         this.setText(object.get("text").getAsString());
      }

      if (object.has("maxLength")) {
         this.setMaxLength(object.get("maxLength").getAsInt());
      }
   }

   public void setText(String text) {
      this.text = text;
   }

   public void setMaxLength(int maxLength) {
      this.maxLength = maxLength;
   }

   public String getText() {
      return this.text;
   }

   public int getMaxLength() {
      return this.maxLength;
   }
}
