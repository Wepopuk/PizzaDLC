package pizza.dlc.client.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.settings.Setting;
import pizza.dlc.api.settings.api.Parent;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.client.modules.impl.misc.Sounds;

public class Module extends Parent implements QuickImports {
   private final ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
   private final String name;
   private final String description;
   private final ModuleCategory category;
   private int key;
   private boolean enabled;
   private volatile boolean streamProofSuspended;
   private final List<Setting> settings = new ArrayList<>();

   public Module() {
      this.name = this.getModuleInfo().name();
      this.description = this.getModuleInfo().description();
      this.category = this.getModuleInfo().category();
      this.key = this.getModuleInfo().key();
   }

   public Module(String name, String description, ModuleCategory category) {
      this.name = name;
      this.description = description;
      this.category = category;
      this.key = -1;
   }

   public void addSettings(Setting... settings) {
      for (Setting setting : settings) {
         if (setting == null || this.settings.contains(setting)) continue;

         this.settings.add(setting);
      }
   }

   public boolean isHidden() {
      return this.moduleInfo != null && this.moduleInfo.hidden();
   }

   public boolean isPermanent() {
      return this.moduleInfo != null && this.moduleInfo.permanent();
   }

   public void onEnable() {
      PizzaDLC.getInstance().getBus().subscribe(this);
   }

   public void onDisable() {
      PizzaDLC.getInstance().getBus().unsubscribe(this);
   }

   public void toggle() {
      if (this.isPermanent()) return;

      this.setEnabled(!this.enabled);

      if (!this.isHidden()) {
         try {
            NotificationManager.addToggle(this.getName(), this.getCategory(), this.enabled);
         } catch (Exception ignored) {
         }
      }

      this.tryPlayToggleSound();
   }

   private void tryPlayToggleSound() {
      try {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         if (cc == null || cc.getModuleManager() == null) {
            return;
         }

         Sounds sounds = cc.getModuleManager().get(Sounds.class);
         if (sounds == null || !sounds.isEnabled()) {
            return;
         }

         float volume = sounds.volume.getValue();
         if (volume <= 0.001F) {
            return;
         }

         float pitch = this.enabled ? sounds.pitchOn.getValue() : sounds.pitchOff.getValue();
         mc.getSoundManager().play(SimpleSoundInstance.forUI(sounds.selected(), pitch, volume));
      } catch (Throwable ignored) {
      }
   }

   public void setEnabled(boolean enabled) {
      if (enabled && pizza.dlc.api.neuro.NeuroSystem.blocksCombat(this)) return;
      if (!enabled && this.isPermanent()) return;

      if (this.enabled != enabled) {
         this.enabled = enabled;
         if (this.streamProofSuspended) return;
         if (enabled) {
            this.onEnable();
         } else {
            this.onDisable();
         }
      }
   }

   public final void setStreamProofSuspended(boolean suspended) {
      if (this.streamProofSuspended == suspended) return;
      this.streamProofSuspended = suspended;
      if (!this.enabled) return;
      if (suspended) {
         try {
            this.onDisable();
         } finally {
            PizzaDLC instance = PizzaDLC.getInstance();
            if (instance != null && instance.getBus() != null) instance.getBus().unsubscribe(this);
         }
      } else {
         this.onEnable();
      }
   }

   public JsonObject save() {
      JsonObject object = new JsonObject();
      String n = this.getName();
      if (!n.equalsIgnoreCase("Click UI")
         && !n.equalsIgnoreCase("ClickUI")
         && !n.equalsIgnoreCase("ClickGUI")
         && !n.equalsIgnoreCase("Click Gui")) {
         object.addProperty("enabled", this.enabled);
      } else {
         object.addProperty("enabled", false);
      }

      object.addProperty("key", this.key);
      if (!this.settings.isEmpty()) {
         JsonObject settingsObject = new JsonObject();

         for (Setting setting : this.settings) {
            try {
               settingsObject.add(setting.configKey(), setting.save());
            } catch (Exception ex) {
               PizzaDLC.LOGGER.error("Failed to save setting {}.{}", this.getName(), setting.getName(), ex);
            }
         }

         object.add("Settings", settingsObject);
      }

      return object;
   }

   public void load(JsonObject object) {
      if (object.has("enabled")) {
         boolean savedState = object.get("enabled").getAsBoolean();

         try {
            this.setEnabled(savedState);
         } catch (Exception ex) {
            PizzaDLC.LOGGER.error("Failed to restore state of {}", this.getName(), ex);
         }
      }

      if (object.has("key")) {
         this.setKey(object.get("key").getAsInt());
      }

      if (object.has("Settings") && !this.settings.isEmpty()) {
         JsonObject settingsObject = object.getAsJsonObject("Settings");

         for (Setting setting : this.settings) {
            if (!settingsObject.has(setting.configKey())) continue;

            JsonElement element = settingsObject.get(setting.configKey());

            try {
               setting.load(element);
            } catch (Exception ex) {
               PizzaDLC.LOGGER.error("Failed to load setting {}.{}", this.getName(), setting.getName(), ex);
            }
         }
      }
   }

   public ModuleInfo getModuleInfo() {
      return this.moduleInfo;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public ModuleCategory getCategory() {
      return this.category;
   }

   public int getKey() {
      return this.key;
   }

   public boolean isEnabled() {
      return this.enabled && !this.streamProofSuspended && !CleanRenderScope.isActive();
   }

   public List<Setting> getSettings() {
      return this.settings;
   }

   public void setKey(int key) {
      this.key = key;
   }
}
