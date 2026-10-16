package pizza.dlc.api.settings;

import com.google.gson.JsonElement;
import java.util.function.Supplier;
import pizza.dlc.api.settings.api.Parent;
import pizza.dlc.client.modules.Module;

public abstract class Setting {
   protected String name;
   protected String description;
   protected Supplier<Boolean> hideCondition;
   protected String group;
   protected int key = -1;
   protected Parent parent;

   public abstract JsonElement save();

   public abstract void load(JsonElement element);

   public Setting(String name, Parent parent) {
      this(name, null, parent);
   }

   public Setting(String name, String description, Parent parent) {
      this.name = name;
      this.description = description;
      this.parent = parent;
      if (parent instanceof Module module) {
         module.getSettings().add(this);
      }
   }

   public <T extends Setting> T setVisible(Supplier<Boolean> show) {
      this.hideCondition = () -> !show.get();
      return (T)this;
   }

   public <T extends Setting> T group(String group) {
      this.group = group;
      return (T)this;
   }

   public String configKey() {
      return this.group != null && !this.group.isEmpty() ? this.group + "/" + this.name : this.name;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Supplier<Boolean> getHideCondition() {
      return this.hideCondition;
   }

   public String getGroup() {
      return this.group;
   }

   public int getKey() {
      return this.key;
   }

   public Parent getParent() {
      return this.parent;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public void setHideCondition(Supplier<Boolean> hideCondition) {
      this.hideCondition = hideCondition;
   }

   public void setGroup(String group) {
      this.group = group;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public void setParent(Parent parent) {
      this.parent = parent;
   }

   public Setting() {
   }
}
