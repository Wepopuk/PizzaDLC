package pizza.dlc.api.account;

import pizza.dlc.api.translation.Translations;
import com.google.common.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import pizza.dlc.api.filemanager.api.ManagerFileAbstract;

/** Server passwords are kept separately from the unencrypted account list. */
public final class ServerLoginStore extends ManagerFileAbstract<ServerLoginStore.Entry> {
   private static ServerLoginStore instance;

   private ServerLoginStore() {
      super("server-logins.json", pizza.dlc.api.filemanager.ClientDataMigration.credentialKey("server-logins") + System.getProperty("user.name", "")
         + "|" + Objects.toString(System.getenv("COMPUTERNAME"), "") + "|" + System.getProperty("os.name", ""),
         (new TypeToken<List<Entry>>() {}).getType(), ArrayList::new);
   }

   public static synchronized ServerLoginStore get() {
      if (instance == null) instance = new ServerLoginStore();
      return instance;
   }

   public synchronized List<Entry> forAccount(String account) {
      return this.getItems().stream().filter(e -> e != null && account.equalsIgnoreCase(e.account())
         && e.server() != null && !e.server().isEmpty()).toList();
   }

   /** Shared passwords are local defaults, never authorization to send to a new server. */
   public synchronized Entry defaultForAccount(String account) {
      return this.getItems().stream().filter(e -> e != null && account.equalsIgnoreCase(e.account())
         && "".equals(e.server())).findFirst().orElse(null);
   }

   public synchronized void putDefault(String account, String password, boolean enabled) {
      if (password != null && !password.isEmpty() && (!validPassword(password) || password.length() > 120)) {
         throw new IllegalArgumentException(Translations.text("Общий пароль: 1–120 символов без пробелов"));
      }
      this.getItems().removeIf(e -> e != null && account.equalsIgnoreCase(e.account()) && "".equals(e.server()));
      if (password != null && !password.isEmpty()) this.add(new Entry(account, "", password, "login", enabled));
      this.save();
   }

   public synchronized Entry find(String account, String address) {
      String server = normalizeAddress(address);
      if (server.isEmpty()) return null;
      return forAccount(account).stream().filter(e -> server.equals(e.server())).findFirst().orElse(null);
   }

   public synchronized void put(String account, String address, String password, String command, boolean enabled) {
      String server = normalizeAddress(address);
      if (server.isEmpty()) throw new IllegalArgumentException(Translations.text("Укажи адрес сервера: play.example.net[:порт]"));
      if (!validPassword(password)) throw new IllegalArgumentException(Translations.text("Пароль: 1–240 символов без пробелов"));
      if (!"login".equals(command) && !"l".equals(command)) throw new IllegalArgumentException(Translations.text("Команда: /login или /l"));
      this.getItems().removeIf(e -> e != null && account.equalsIgnoreCase(e.account()) && server.equals(e.server()));
      this.add(new Entry(account, server, password, command, enabled));
      this.save();
   }

   /** A reviewed destination may follow changes to the account's shared password. */
   public synchronized void putSharedServer(String account, String address, String command, boolean enabled) {
      String server = normalizeAddress(address);
      Entry shared = defaultForAccount(account);
      if (server.isEmpty() || shared == null || !validPassword(shared.password())) throw new IllegalArgumentException(Translations.text("Сначала укажи общий пароль аккаунта"));
      if (!"login".equals(command) && !"l".equals(command)) throw new IllegalArgumentException(Translations.text("Команда: /login или /l"));
      this.getItems().removeIf(e -> e != null && account.equalsIgnoreCase(e.account()) && server.equals(e.server()));
      this.add(new Entry(account, server, "", command, enabled, true));
      this.save();
   }

   public synchronized Entry resolveCredential(Entry saved) {
      if (saved == null || !saved.useSharedPassword()) return saved;
      Entry shared = defaultForAccount(saved.account());
      if (shared == null) return null;
      return new Entry(saved.account(), saved.server(), shared.password(), saved.command(), saved.enabled() && shared.enabled(), true);
   }

   public synchronized void remove(String account, String server) {
      this.getItems().removeIf(e -> e != null && account.equalsIgnoreCase(e.account()) && server.equals(e.server()));
      this.save();
   }

   public synchronized void removeAccount(String account) {
      this.getItems().removeIf(e -> e != null && account.equalsIgnoreCase(e.account()));
      this.save();
   }

   public synchronized void rename(String oldName, String newName) {
      List<Entry> old = this.getItems().stream().filter(e -> e != null && oldName.equalsIgnoreCase(e.account())).toList();
      if (old.isEmpty()) return;
      this.getItems().removeIf(e -> e != null && (oldName.equalsIgnoreCase(e.account()) || newName.equalsIgnoreCase(e.account())));
      for (Entry e : old) this.add(new Entry(newName, e.server(), e.password(), e.command(), e.enabled(), e.useSharedPassword()));
      this.save();
   }

   public static boolean validPassword(String password) {
      return password != null && !password.isEmpty() && password.length() <= 240
         && password.codePoints().noneMatch(c -> Character.isWhitespace(c) || Character.isISOControl(c) || c == 167);
   }

   /** Exact host and port matching; the standard port can be omitted. */
   public static String normalizeAddress(String address) {
      if (address == null) return "";
      String value = address.trim().toLowerCase(Locale.ROOT);
      String host;
      String port = "25565";
      if (value.startsWith("[")) {
         int end = value.indexOf(']');
         if (end < 0) return "";
         host = value.substring(0, end + 1);
         if (!host.matches("\\[[0-9a-f:]+\\]") || !host.contains("::") && host.chars().filter(c -> c == ':').count() < 2) return "";
         if (end + 1 < value.length()) {
            if (value.charAt(end + 1) != ':') return "";
            port = value.substring(end + 2);
         }
      } else {
         int colon = value.indexOf(':');
         host = colon < 0 ? value : value.substring(0, colon);
         if (colon >= 0) port = value.substring(colon + 1);
         if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
         if (host.isEmpty() || host.length() > 253) return "";
         for (String label : host.split("\\.", -1)) {
            if (!label.matches("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) return "";
         }
      }
      if (!port.matches("[0-9]{1,5}")) return "";
      int number = Integer.parseInt(port);
      return number < 1 || number > 65535 ? "" : host + ":" + number;
   }

   public record Entry(String account, String server, String password, String command, boolean enabled, boolean useSharedPassword) {
      public Entry(String account, String server, String password, String command, boolean enabled) {
         this(account, server, password, command, enabled, false);
      }
      @Override
      public String toString() {
         return "ServerLogin[account=" + account + ", server=" + server + ", enabled=" + enabled + "]";
      }
   }
}
