package pizza.dlc.api.config;

import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import pizza.dlc.PizzaDLC;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;

public class Config {
   private final String name;
   private final File file;

   public Config(String name) {
      this.name = name;
      this.file = new File(ConfigManager.configDirectory, name + ".pizzadlc");
      if (!this.file.exists()) {
         try {
            this.file.createNewFile();
         } catch (IOException ex) {
            PizzaDLC.LOGGER.error("Failed to create config file {}", this.file, ex);
         }
      }
   }

   public JsonObject save() {
      try {
         JsonObject root = new JsonObject();
         JsonObject modulesObject = new JsonObject();

         for (Module module : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
            modulesObject.add(module.getName(), module.save());
         }

         root.add("Modules", modulesObject);
         if (PizzaDLCClient.getInstance().getDraggableManager() != null) {
            root.add("Draggables", PizzaDLCClient.getInstance().getDraggableManager().save());
         }

         return root;
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to serialize config", ex);
         return null;
      }
   }

   public void load(JsonObject object) {
      if (object.has("Modules")) {
         try {
            JsonObject modulesObject = object.getAsJsonObject("Modules");

            for (Module module : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
               JsonObject saved = moduleState(modulesObject, module.getName());
               if (saved != null) module.load(saved);
            }
         } catch (Exception ex) {
            PizzaDLC.LOGGER.error("Failed to load modules from config", ex);
         }
      }

      if (object.has("Draggables") && PizzaDLCClient.getInstance().getDraggableManager() != null) {
         try {
            PizzaDLCClient.getInstance().getDraggableManager().load(object.getAsJsonObject("Draggables"));
         } catch (Exception ex) {
            PizzaDLC.LOGGER.error("Failed to load draggables from config", ex);
         }
      }
   }

   public static JsonObject captureState() {
      try {
         JsonObject root = new JsonObject();
         JsonObject modulesObject = new JsonObject();

         for (Module module : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
            modulesObject.add(module.getName(), module.save());
         }

         root.add("Modules", modulesObject);
         return root;
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to capture config state", ex);
         return null;
      }
   }

   public static void applyState(JsonObject object) {
      if (object == null) return;

      if (object.has("Modules")) {
         try {
            JsonObject modulesObject = object.getAsJsonObject("Modules");

            for (Module module : PizzaDLCClient.getInstance().getModuleManager().getModules()) {
               JsonObject saved = moduleState(modulesObject, module.getName());
               if (saved != null) module.load(saved);
            }
         } catch (Exception ex) {
            PizzaDLC.LOGGER.error("Failed to apply module state", ex);
         }
      }
   }

   public String getName() {
      return this.name;
   }

   public static JsonObject moduleState(JsonObject modules, String name) {
      if ("Attack Aura".equals(name)) {
         JsonObject current = savedModule(modules, name);
         if (current != null && current.has("Settings") && current.get("Settings").isJsonObject()
            && current.getAsJsonObject("Settings").has("Тип ротации")) return current;
         JsonObject renamed = savedModule(modules, "KillAura");
         if (renamed != null) return renamed;
         if (current == null) current = savedModule(modules, "AttackAura");
         if (current == null) return null;
         // The removed aura's settings belong to a different implementation.
         JsonObject migrated = current.deepCopy();
         migrated.remove("Settings");
         return migrated;
      }
      String key = modules.has(name) ? name : "Animations".equals(name) ? "AnimatedHands" : name;
      return savedModule(modules, key);
   }

   private static JsonObject savedModule(JsonObject modules, String name) {
      return modules.has(name) && modules.get(name).isJsonObject() ? modules.getAsJsonObject(name) : null;
   }

   public File getFile() {
      return this.file;
   }
}
