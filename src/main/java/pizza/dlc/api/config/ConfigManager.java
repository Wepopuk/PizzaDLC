package pizza.dlc.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.util.crypt.CryptUtility;
import pizza.dlc.client.PizzaDLCClient;

public class ConfigManager {
   public static final File configDirectory = new File(PizzaDLC.DIRECTORY, "configs");
   public static final String AUTOSAVE_CONFIG_NAME = "autosave";
   public static final String LEGACY_AUTOSAVE_CONFIG_NAME = "current_config";
   ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
      Thread.ofPlatform().name("PizzaDLC config autosave").daemon(true).factory());
   private String activeConfigName = null;

   public ConfigManager() {
      configDirectory.mkdirs();
      migrateLegacyAutosave();
      this.scheduler.scheduleAtFixedRate(() -> Minecraft.getInstance().execute(() -> {
         try {
            if (PizzaDLCClient.getInstance() != null && PizzaDLCClient.getInstance().isConfigLoaded()) {
               this.saveConfig(AUTOSAVE_CONFIG_NAME);
            }
         } catch (Exception ex) {
            PizzaDLC.LOGGER.error("Autosave failed", ex);
         }
      }), 5L, 5L, TimeUnit.MINUTES);
   }

   private static void migrateLegacyAutosave() {
      try {
         String ext = "." + "pizzadlc".toLowerCase();
         File legacy = new File(configDirectory, LEGACY_AUTOSAVE_CONFIG_NAME + ext);
         File autosave = new File(configDirectory, AUTOSAVE_CONFIG_NAME + ext);
         if (!legacy.exists() || autosave.exists()) {
            return;
         }

         boolean renamed = legacy.renameTo(autosave);
         if (renamed) {
            return;
         }

         try (
            InputStream in = new FileInputStream(legacy);
               OutputStream out = new FileOutputStream(autosave);
         ) {
            byte[] buf = new byte[8192];

            int r;
            while ((r = in.read(buf)) >= 0) {
               out.write(buf, 0, r);
            }
         }

         legacy.delete();
      } catch (Exception ignored) {
      }
   }

   public boolean loadConfig(String configName) {
      if (configName == null) {
         return false;
      }

      Config config = this.findConfig(configName);
      if (config == null) {
         return false;
      }

      try {
         try (BufferedReader reader = new BufferedReader(new FileReader(config.getFile()))) {
            String encryptedDataBase64 = reader.readLine();
            if (encryptedDataBase64 == null) {
               return false;
            }

            byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
            byte[] decryptedData = CryptUtility.decryptData(encryptedData, "config");
            if (decryptedData == null) {
               return false;
            }

            String json = new String(decryptedData, StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            config.load(object);
            if (!isReservedConfigName(configName)) {
               this.activeConfigName = configName;
            }

            return true;
         }
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to load config {}", configName, ex);
         return false;
      }
   }

   public synchronized boolean saveConfig(String configName) {
      try {
         if (configName == null) {
            return false;
         }

         Config config;
         if ((config = this.findConfig(configName)) == null) {
            config = new Config(configName);
         }

         JsonObject savedData = config.save();
         if (savedData == null) {
            return false;
         }

         String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(savedData);
         contentPrettyPrint = Base64.getEncoder().encodeToString(CryptUtility.encryptData(contentPrettyPrint.getBytes(StandardCharsets.UTF_8), "config"));

         Path target = config.getFile().toPath();
         Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
         Files.writeString(tmp, contentPrettyPrint, StandardCharsets.UTF_8);
         try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
         }
         if (!isReservedConfigName(configName)) {
            this.activeConfigName = configName;
         }

         return true;
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to save config {}", configName, ex);
         return false;
      }
   }

   private static boolean isReservedConfigName(String configName) {
      return configName == null || configName.equalsIgnoreCase(AUTOSAVE_CONFIG_NAME) || configName.equalsIgnoreCase(LEGACY_AUTOSAVE_CONFIG_NAME);
   }

   public void clearActiveConfigName() {
      this.activeConfigName = null;
   }

   public Config findConfig(String configName) {
      if (configName == null) {
         return null;
      }

      return new File(configDirectory, configName + "." + "pizzadlc".toLowerCase()).exists() ? new Config(configName) : null;
   }

   public List<String> configNames() {
      File[] files = configDirectory.listFiles();
      List<String> names = new ArrayList<>();
      if (files != null) {
         for (File file : files) {
            if (file == null || !file.isFile()) continue;

            String fname = file.getName();
            if (fname != null) {
               String ext = "." + "pizzadlc".toLowerCase();
               String lower = fname.toLowerCase();
               if (lower.endsWith(ext)) {
                  String base = fname.substring(0, Math.max(0, fname.length() - ext.length()));
                  if (!base.equalsIgnoreCase(AUTOSAVE_CONFIG_NAME) && !base.equalsIgnoreCase(LEGACY_AUTOSAVE_CONFIG_NAME)) {
                     names.add(fname);
                  }
               }
            }
         }
      }

      return names;
   }

   public boolean deleteConfig(String configName) {
      if (configName == null) {
         return false;
      }

      Config config = this.findConfig(configName);
      if (config == null) {
         return false;
      }

      File f = config.getFile();
      boolean ok = f.exists() && f.delete();
      if (ok && this.activeConfigName != null && this.activeConfigName.equalsIgnoreCase(configName)) {
         this.activeConfigName = null;
      }

      return ok;
   }

   public void save() {
      this.scheduler.shutdown();
      this.saveConfig(AUTOSAVE_CONFIG_NAME);
   }

   public ScheduledExecutorService getScheduler() {
      return this.scheduler;
   }

   public String getShifr() {
      return "config";
   }

   public String getActiveConfigName() {
      return this.activeConfigName;
   }
}
