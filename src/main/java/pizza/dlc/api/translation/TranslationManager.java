package pizza.dlc.api.translation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import pizza.dlc.PizzaDLC;

public final class TranslationManager {
   private static final class Holder {
      private static final TranslationManager INSTANCE = new TranslationManager(PizzaDLC.DIRECTORY.toPath().resolve("language.json"));
   }

   private final Path preferenceFile;
   private final Map<ClientLanguage, Map<String, String>> messages = new EnumMap<>(ClientLanguage.class);
   private final Map<ClientLanguage, Map<String, String>> labels = new EnumMap<>(ClientLanguage.class);
   private volatile ClientLanguage current = ClientLanguage.RUSSIAN;
   private volatile boolean selectionRequired = true;

   public TranslationManager(Path preferenceFile) {
      this.preferenceFile = preferenceFile;
      for (ClientLanguage language : ClientLanguage.values()) {
         try (var stream = TranslationManager.class.getResourceAsStream("/assets/pizzadlc/lang/client/" + language.code() + ".json")) {
            if (stream == null) throw new IOException("Missing client language " + language.code());
            JsonObject data = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            messages.put(language, strings(data.getAsJsonObject("messages")));
            labels.put(language, strings(data.getAsJsonObject("labels")));
         } catch (Exception exception) {
            throw new IllegalStateException("Cannot load client language " + language.code(), exception);
         }
      }
      if (Files.isRegularFile(preferenceFile)) {
         try {
            JsonObject data = JsonParser.parseString(Files.readString(preferenceFile, StandardCharsets.UTF_8)).getAsJsonObject();
            ClientLanguage saved = ClientLanguage.byCode(data.get("language").getAsString());
            if (saved != null) { current = saved; selectionRequired = false; }
         } catch (Exception exception) {
            PizzaDLC.LOGGER.warn("Cannot read client language preference; showing language picker", exception);
         }
      }
   }

   private static Map<String, String> strings(JsonObject data) {
      Map<String, String> values = new HashMap<>();
      data.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue().getAsString()));
      return Map.copyOf(values);
   }

   public static TranslationManager getInstance() { return Holder.INSTANCE; }
   public ClientLanguage language() { return current; }
   public boolean needsSelection() { return selectionRequired; }
   public Path preferenceFile() { return preferenceFile; }

   public synchronized boolean setLanguage(String code) {
      ClientLanguage language = ClientLanguage.byCode(code);
      if (language == null) return false;
      Path temporary = preferenceFile.resolveSibling(preferenceFile.getFileName() + ".tmp");
      try {
         Path parent = preferenceFile.toAbsolutePath().getParent();
         if (parent != null) Files.createDirectories(parent);
         JsonObject data = new JsonObject();
         data.addProperty("language", language.code());
         Files.writeString(temporary, data + "\n", StandardCharsets.UTF_8);
         try {
            Files.move(temporary, preferenceFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, preferenceFile, StandardCopyOption.REPLACE_EXISTING);
         }
         current = language;
         selectionRequired = false;
         return true;
      } catch (IOException exception) {
         PizzaDLC.LOGGER.error("Cannot save client language preference", exception);
         return false;
      }
   }

   public String translate(ClientLanguage language, String key) {
      return messages.get(language).getOrDefault(key, messages.get(ClientLanguage.ENGLISH).getOrDefault(key, key));
   }
   public String translate(String key) { return translate(current, key); }
   public String translate(String key, Object... args) { return String.format(Locale.ROOT, translate(key), args); }
   public String text(ClientLanguage language, String source) { return source == null ? "" : labels.get(language).getOrDefault(source, source); }
   public String text(String source) { return text(current, source); }
   public List<String> getAvailableLanguages() { return Arrays.stream(ClientLanguage.values()).map(ClientLanguage::code).toList(); }
   public String getCurrentLanguage() { return current.code(); }
}
