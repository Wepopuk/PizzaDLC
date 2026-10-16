package pizza.dlc.api.account;

import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pizza.dlc.api.filemanager.api.ManagerFileAbstract;

public final class MicrosoftAccountStore extends ManagerFileAbstract<MicrosoftAccountStore.Entry> {
   private static MicrosoftAccountStore instance;

   private MicrosoftAccountStore() {
      super("msa.json", machineKey(), (new TypeToken<List<Entry>>() {}).getType(), ArrayList::new);
   }

   public static synchronized MicrosoftAccountStore get() {
      if (instance == null) {
         instance = new MicrosoftAccountStore();
      }

      return instance;
   }

   private static String machineKey() {
      return pizza.dlc.api.filemanager.ClientDataMigration.credentialKey("msa")
         + System.getProperty("user.name", "")
         + "|"
         + Objects.toString(System.getenv("COMPUTERNAME"), "")
         + "|"
         + System.getProperty("os.name", "");
   }

   public synchronized Entry find(String name) {
      return this.getItems().stream().filter(e -> e != null && e.name() != null && e.name().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public synchronized void put(MicrosoftAuth.Session s) {
      String uuid = s.uuid().toString();
      Entry old = this.getItems().stream().filter(e -> e != null && uuid.equals(e.uuid())).findFirst().orElse(null);
      String refresh = s.refreshToken() != null ? s.refreshToken() : (old != null ? old.refreshToken() : null);
      this.getItems().removeIf(e -> e == null || uuid.equals(e.uuid()) || s.name().equalsIgnoreCase(e.name()));
      this.add(new Entry(s.name(), uuid, refresh, s.accessToken(), s.expiresAtMs()));
      this.save();
   }

   public synchronized void remove(String name) {
      if (!this.getItems().removeIf(e -> e == null || e.name() != null && e.name().equalsIgnoreCase(name))) return;

      this.save();
   }

   public record Entry(String name, String uuid, String refreshToken, String accessToken, long expiresAt) {
   }
}
