package pizza.dlc.api.filemanager.api;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Collection;
import java.util.function.Supplier;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.util.crypt.CryptUtility;

public class ManagerFileAbstract<T> {
   private Collection<T> items;
   private final String fileName;
   private final String shifr;
   private final Type type;
   private final Supplier<Collection<T>> collectionSupplier;

   public ManagerFileAbstract(String fileName, String shifr, Type type, Supplier<Collection<T>> collectionSupplier) {
      this.fileName = fileName;
      this.shifr = shifr;
      this.type = type;
      this.collectionSupplier = collectionSupplier;
      File file = new File(PizzaDLC.DIRECTORY, fileName);
      if (!file.exists()) {
         try {
            file.createNewFile();
            this.items = collectionSupplier.get();
         } catch (Exception ignored) {
            this.items = collectionSupplier.get();
         }
      } else {
         this.load();
      }
   }

   public synchronized void save() {
      Gson gson = new Gson();
      String json = gson.toJson(this.items);

      try {
         String content = this.shifr.isEmpty() ? json : Base64.getEncoder().encodeToString(CryptUtility.encryptData(json.getBytes(StandardCharsets.UTF_8), this.shifr));
         Path target = new File(PizzaDLC.DIRECTORY, this.fileName).toPath();
         Path tmp = target.resolveSibling(this.fileName + ".tmp");
         Files.writeString(tmp, content, StandardCharsets.UTF_8);
         try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Exception ignored) {
      }
   }

   public void load() {
      try (BufferedReader reader = new BufferedReader(new FileReader(new File(PizzaDLC.DIRECTORY, this.fileName)))) {
         Gson gson = new Gson();
         if (!this.shifr.isEmpty()) {
            String encryptedDataBase64 = reader.readLine();
            if (encryptedDataBase64 != null && !encryptedDataBase64.isEmpty()) {
               byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
               byte[] decryptedData = CryptUtility.decryptData(encryptedData, this.shifr);
               String json = new String(decryptedData, StandardCharsets.UTF_8);
               this.items = (Collection<T>)gson.fromJson(json, this.type);
            } else {
               this.items = this.collectionSupplier.get();
            }
         } else {
            this.items = (Collection<T>)gson.fromJson(reader, this.type);
         }

         if (this.items == null) {
            this.items = this.collectionSupplier.get();
         }
      } catch (Exception ignored) {
         this.items = this.collectionSupplier.get();
      }
   }

   public void add(T item) {
      this.items.add(item);
   }

   public void remove(T item) {
      this.items.remove(item);
   }

   public Collection<T> getItems() {
      return this.items;
   }
}
