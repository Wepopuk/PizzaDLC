package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;

public class BooleanSetting extends Setting {
   private boolean enabled = false;
   private int key = -1;
   private boolean holdBind = false;

   public BooleanSetting(String name, Parent parent) {
      super(name, parent);
   }

   public BooleanSetting(String name, String description, Parent parent) {
      super(name, description, parent);
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   @Override
   public JsonElement save() {
      JsonObject object = new JsonObject();
      object.addProperty("enabled", this.enabled);
      object.addProperty("key", this.key);
      object.addProperty("holdBind", this.holdBind);
      return object;
   }

   @Override
   public void load(JsonElement element) {
      JsonObject object = element.getAsJsonObject();
      if (object.has("enabled")) {
         this.setEnabled(object.get("enabled").getAsBoolean());
      }

      if (object.has("key")) {
         this.setKey(object.get("key").getAsInt());
      }

      if (object.has("holdBind")) {
         this.setHoldBind(object.get("holdBind").getAsBoolean());
      }
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   @Override
   public void setKey(int key) {
      this.key = key;
   }

   public void setHoldBind(boolean holdBind) {
      this.holdBind = holdBind;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   @Override
   public int getKey() {
      return this.key;
   }

   public boolean isHoldBind() {
      return this.holdBind;
   }
}
