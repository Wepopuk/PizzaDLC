package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;

public class BindSetting extends Setting {
   private int key;

   public BindSetting(String name, Parent parent, int defaultKey) {
      super(name, parent);
      this.key = defaultKey;
   }

   @Override
   public JsonElement save() {
      return new JsonPrimitive(this.key);
   }

   @Override
   public void load(JsonElement element) {
      this.setKey(element.getAsInt());
   }

   @Override
   public int getKey() {
      return this.key;
   }

   @Override
   public void setKey(int key) {
      this.key = key;
   }
}
