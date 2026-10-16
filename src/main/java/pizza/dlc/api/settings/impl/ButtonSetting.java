package pizza.dlc.api.settings.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import java.util.function.Supplier;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;

public class ButtonSetting extends Setting {
   private final Runnable action;
   private Supplier<String> labelSupplier;

   public ButtonSetting(String name, Parent parent, Runnable action) {
      super(name, parent);
      this.action = action;
   }

   public void click() {
      if (this.action == null) return;

      try {
         this.action.run();
      } catch (Throwable ignored) {
      }
   }

   public String label() {
      return this.labelSupplier != null ? this.labelSupplier.get() : this.name;
   }

   public ButtonSetting setLabelSupplier(Supplier<String> s) {
      this.labelSupplier = s;
      return this;
   }

   @Override
   public JsonElement save() {
      return JsonNull.INSTANCE;
   }

   @Override
   public void load(JsonElement element) {
   }

   public Runnable getAction() {
      return this.action;
   }

   public Supplier<String> getLabelSupplier() {
      return this.labelSupplier;
   }
}
