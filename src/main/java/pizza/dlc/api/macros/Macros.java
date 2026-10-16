package pizza.dlc.api.macros;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import meteordevelopment.orbit.EventHandler;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.macros.constructor.MacroBlock;
import pizza.dlc.api.util.crypt.CryptUtility;

public class Macros {
   private List<Macro> registeredMacros = new ArrayList<>();
   private final File file = new File(PizzaDLC.DIRECTORY, "macros." + "pizzadlc".toLowerCase());
   private final Gson gson = new GsonBuilder().registerTypeAdapter(MacroBlock.class, new MacroSerializer()).setPrettyPrinting().create();

   public Macros() {
      PizzaDLC.getInstance().getBus().subscribe(this);
      this.load();
   }

   @EventHandler
   private void onKey(EventKey event) {
      if (event.getAction() != 1) return;

      for (Macro macro : this.registeredMacros) {
         if (macro.getKey() != event.getKey()) continue;

         macro.execute();
      }
   }

   public void addMacro(Macro macro) {
      this.registeredMacros.add(macro);
      this.save();
   }

   public void save() {
      try {
         if (!PizzaDLC.DIRECTORY.exists()) {
            PizzaDLC.DIRECTORY.mkdirs();
         }

         String json = this.gson.toJson(this.registeredMacros);
         byte[] encrypted = CryptUtility.encryptData(json.getBytes(StandardCharsets.UTF_8), "macros_salt");
         String base64 = Base64.getEncoder().encodeToString(encrypted);

         java.nio.file.Path target = this.file.toPath();
         java.nio.file.Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
         java.nio.file.Files.writeString(tmp, base64, StandardCharsets.UTF_8);
         java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to save macros", ex);
      }
   }

   public void load() {
      if (!this.file.exists()) return;

      try {
         try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
            String base64 = reader.readLine();
            if (base64 == null || base64.isEmpty()) {
               return;
            }

            byte[] encrypted = Base64.getDecoder().decode(base64);
            byte[] decrypted = CryptUtility.decryptData(encrypted, "macros_salt");
            if (decrypted != null) {
               String json = new String(decrypted, StandardCharsets.UTF_8);
               List<Macro> loaded = (List<Macro>)this.gson.fromJson(json, (new TypeToken<List<Macro>>() {
                  {
                     Objects.requireNonNull(Macros.this);
                  }
               }).getType());
               if (loaded != null) {
                  this.registeredMacros = loaded;
               }

               return;
            }
         }
      } catch (Exception ex) {
         PizzaDLC.LOGGER.error("Failed to load macros (corrupted file or wrong salt)", ex);
      }
   }

   public List<Macro> getRegisteredMacros() {
      return this.registeredMacros;
   }
}
