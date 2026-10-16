package pizza.dlc;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import pizza.dlc.api.account.ServerAutoLogin;
import pizza.dlc.api.account.ServerAuthFlow;
import pizza.dlc.api.account.ServerLoginStore;
import pizza.dlc.api.filemanager.impl.AltAccountManager;

/** Legacy migration, note persistence, password isolation and server matching. */
public final class AccountDetailsCheck {
   public static void main(String[] args) throws Exception {
      check(Path.of("").toAbsolutePath().endsWith(Path.of("build", "account-check")), "Run through verifyAccountDetails");
      Files.createDirectories(PizzaDLC.DIRECTORY.toPath());
      Path altsFile = PizzaDLC.DIRECTORY.toPath().resolve("alts.json");
      Files.writeString(altsFile, "[{\"name\":\"Tester\",\"createdMs\":12,\"lastUsedMs\":14,\"selected\":false,\"favorite\":false,\"premium\":false}]");
      AltAccountManager alts = new AltAccountManager();
      check(alts.getItems().iterator().next().note().isEmpty(), "Legacy accounts should load without a note");
      alts.setNote("Tester", "Основной · ранг VIP");
      alts.toggleFavorite("Tester");
      alts.selectAlt("Tester");
      alts.addPremium("Tester");
      check(alts.getSelected().note().equals("Основной · ранг VIP"), "Account operations lost the note");
      alts.load();
      check(alts.getSelected().favorite() && alts.getSelected().premium(), "Flags lost during save/load");
      check(alts.getSelected().note().equals("Основной · ранг VIP"), "Note did not persist");

      ServerLoginStore store = ServerLoginStore.get();
      store.getItems().clear();
      store.put("Tester", " Play.Example.Net ", "sampleSecret_123", "login", true);
      store.put("Tester", "play.example.net:25566", "otherSecret", "l", false);
      store.putDefault("Tester", "sharedSecret", true);
      check(store.defaultForAccount("tester").password().equals("sharedSecret"), "Shared password must follow the account");
      check(store.defaultForAccount("OtherAccount") == null, "Shared password leaked to another account");
      check(store.forAccount("tester").size() == 2, "Multiple servers must be retained");
      check(store.find("TESTER", "play.example.net.:25565") != null, "Standard port/case normalization failed");
      check(store.find("Tester", "play.example.net:25566").command().equals("l"), "Alternate login command lost");
      check(!store.find("Tester", "play.example.net:25566").enabled(), "Disabled state lost");
      check(store.find("OtherAccount", "play.example.net") == null, "Password leaked between accounts");
      check(store.find("Tester", "sub.play.example.net") == null, "Subdomains must not match");
      check(store.find("Tester", "play.example.net:25567") == null, "Ports must match exactly");
      check(store.find("Tester", "play.example.net.evil.org") == null, "Suffixes must not match");
      check(ServerLoginStore.normalizeAddress("[::1]").equals("[::1]:25565"), "IPv6 default port failed");
      for (String bad : new String[]{"", "example.net:0", "example.net:65536", "https://example.net", "example.net/login", "*.example.net", "example.net:", "host:abc", "host:1:2"}) {
         check(ServerLoginStore.normalizeAddress(bad).isEmpty(), "Invalid address accepted: " + bad);
      }
      for (String bad : new String[]{"", "two words", "line\nbreak", "tab\tsecret", "§asecret", "a".repeat(241)}) {
         check(!ServerLoginStore.validPassword(bad), "Invalid password accepted");
      }
      try {
         store.put("Tester", "play.example.net", "bad password", "login", true);
         throw new AssertionError("Invalid password should reject saving");
      } catch (IllegalArgumentException expected) {
         check(store.find("Tester", "play.example.net").password().equals("sampleSecret_123"), "Invalid edit destroyed previous password");
      }
      String encrypted = Files.readString(PizzaDLC.DIRECTORY.toPath().resolve("server-logins.json"));
      check(!encrypted.contains("sampleSecret_123") && !encrypted.contains("play.example.net"), "Passwords stored as plaintext");
      check(!Files.readString(altsFile).contains("sampleSecret_123"), "Password entered unencrypted account list");
      check(!store.find("Tester", "play.example.net").toString().contains("sampleSecret_123"), "Password visible in diagnostics");
      store.load();
      check(store.find("Tester", "play.example.net").password().equals("sampleSecret_123"), "Encrypted save/load failed");
      check(store.defaultForAccount("Tester").password().equals("sharedSecret"), "Shared password did not persist");
      verifyAuthFlow(store);
      alts.rename("Tester", "Renamed");
      check(store.find("Tester", "play.example.net") == null && store.find("Renamed", "play.example.net") != null, "Rename lost server credentials");
      check(alts.getSelected().note().equals("Основной · ранг VIP"), "Rename lost note");
      check(store.defaultForAccount("Renamed") != null && store.defaultForAccount("Tester") == null, "Rename lost shared password");
      alts.removeAlt("Renamed");
      check(store.forAccount("Renamed").isEmpty(), "Removing account left passwords behind");
      check(store.defaultForAccount("Renamed") == null, "Removing account left shared password behind");

      for (String prompt : new String[]{"Войдите: /login <пароль>", "§aPlease use /l <password>", "Авторизуйтесь командой /login пароль", "Use /login <pass>"}) {
         check(ServerAutoLogin.isLoginPrompt(prompt), "Login prompt was missed");
      }
      for (String message : new String[]{"Регистрация: /register <пароль>", "Player: hello", "You successfully logged in with /login", "Неверный пароль, /login пароль", "Use /lobby", "Команда /login", "Use /loginadmin password"}) {
         check(!ServerAutoLogin.isLoginPrompt(message), "Non-login message triggered password sending");
      }
      System.out.println("Account notes and server login checks passed");
   }

   private static void verifyAuthFlow(ServerLoginStore store) {
      var shared = store.defaultForAccount("Tester");
      var saved = store.find("Tester", "play.example.net");
      var disabled = store.find("Tester", "play.example.net:25566");
      String reg = "Зарегистрируйтесь: /register <пароль> <повтор пароля>";
      String login = "Войдите: /login <пароль>";
      ServerAuthFlow unknown = new ServerAuthFlow();
      check(unknown.automatic(reg, null, 0) == null, "Unknown server must require approval");
      check(unknown.automatic(reg, shared, 0) == null, "Shared password must not authorize arbitrary destinations");
      var proposed = unknown.propose(reg, shared, 0);
      check(proposed != null && proposed.wireCommand().equals("register sharedSecret sharedSecret"), "Double-password registration format");
      check(unknown.complete("Вы успешно зарегистрировались!", 1) == null, "Proposal must not mark an auth attempt as sent");
      check(unknown.approve(proposed, 2), "Explicit destination approval should start registration");
      check(!unknown.approve(proposed, 3), "Double approval must not duplicate registration");
      check(unknown.complete("Welcome to our server", 4) == null, "Greeting is not authentication success");
      var registered = unknown.complete("Вы успешно зарегистрировались!", 5);
      check(registered != null && registered.kind() == ServerAuthFlow.Kind.REGISTER, "Registration success not detected");
      check(unknown.propose(reg, shared, 6) == null, "Repeated registration prompts must not flood commands");
      check(unknown.automatic(login, saved, 7) != null, "A server requiring login after registration should work");
      check(unknown.complete("You have successfully logged in!", 8) != null, "Login success not detected");
      check(unknown.automatic(login, saved, 9) == null, "Authenticated session should not resend passwords");
      check(new ServerAuthFlow().automatic(login, disabled, 0) == null, "Disabled server must not send credentials");
      var single = new ServerAuthFlow().propose("Use /reg <password>", shared, 0);
      check(single != null && single.wireCommand().equals("reg sharedSecret"), "Single-password registration format");
      check(!single.toString().contains("sharedSecret"), "Auth diagnostics exposed password");
      for (String prompt : new String[]{"Use /reg [password]", "Введите /register {пароль}", "Используйте /register <пароль>. Никому не сообщайте свой пароль", "Use /register password. Do not share your password"}) {
         var attempt = new ServerAuthFlow().propose(prompt, shared, 0);
         check(attempt != null && !attempt.repeatPassword(), "Single argument or trailing password advice miscounted: " + prompt);
      }
      for (String prompt : new String[]{"Use /reg [password] [confirmPassword]", "Введите /register пароль пароль", "Use /register <pass> <repeat>", "Введите /reg (пароль) (подтверждение)"}) {
         var attempt = new ServerAuthFlow().propose(prompt, shared, 0);
         check(attempt != null && attempt.repeatPassword(), "Two registration arguments miscounted: " + prompt);
      }
      for (String prompt : new String[]{"Зарегистрируйтесь /register", "Use /register <password> <email>", "Use /register <email> <password>", "Use /register <password> <password> <password>"}) {
         check(new ServerAuthFlow().propose(prompt, shared, 0) == null, "Unknown or unsupported registration syntax must require manual input");
      }
      CommandDispatcher<Object> oneArg = new CommandDispatcher<>();
      oneArg.register(LiteralArgumentBuilder.<Object>literal("register").then(RequiredArgumentBuilder.<Object, String>argument("password", StringArgumentType.word()).executes(ctx -> 1)));
      var treeOne = new ServerAuthFlow().propose("Зарегистрируйтесь /register", shared, 0, oneArg.getRoot());
      check(treeOne != null && !treeOne.repeatPassword(), "Single registration argument from the server command tree");
      CommandDispatcher<Object> twoArgs = new CommandDispatcher<>();
      twoArgs.register(LiteralArgumentBuilder.<Object>literal("reg").then(RequiredArgumentBuilder.<Object, String>argument("password", StringArgumentType.word())
         .then(RequiredArgumentBuilder.<Object, String>argument("repeatPassword", StringArgumentType.word()).executes(ctx -> 1))));
      var treeTwo = new ServerAuthFlow().propose("Зарегистрируйтесь /reg", shared, 0, twoArgs.getRoot());
      check(treeTwo != null && treeTwo.repeatPassword(), "Two registration arguments from the server command tree");
      ServerAuthFlow syntax = new ServerAuthFlow();
      check(syntax.automatic(reg, saved, 0) != null, "Initial registration attempt");
      var correction = syntax.correctRegistrationFormat("Usage: /register <password>", null, 1);
      check(correction != null && !correction.repeatPassword(), "Server usage reply should correct argument count");
      check(syntax.correctRegistrationFormat("Usage: /register <password> <password>", null, 2) == null, "Argument count correction must not loop");
      check(new ServerAuthFlow().propose("Commands: /register <password> <password> or /login <password>", shared, 0) == null, "General command help must not trigger registration/login");
      ServerAuthFlow wrongGlobal = new ServerAuthFlow();
      check(wrongGlobal.automatic(login, saved, 0) != null, "Automatic login should try once");
      check(wrongGlobal.complete("Неверный пароль. Введите /login пароль", 1) == null, "Wrong automatic password must not be saved");
      check(wrongGlobal.takeFailure() == ServerAuthFlow.Kind.LOGIN && wrongGlobal.takeFailure() == null, "Show manual-login warning only once");
      check(wrongGlobal.automatic(login, saved, 2) == null && wrongGlobal.automatic(reg, saved, 3) == null,
         "Failed automatic login must stop both login and registration attempts");
      check(wrongGlobal.propose(reg, shared, 4) == null, "Do not propose another shared-password attempt after failure");
      wrongGlobal.manual("login correctManualSecret", 5);
      var manualRecovery = wrongGlobal.complete("Login successful", 6);
      check(manualRecovery != null && manualRecovery.manual(), "Manual login must still work after automatic failure");
      ServerAuthFlow badRegistration = new ServerAuthFlow();
      badRegistration.automatic(reg, saved, 0);
      badRegistration.complete("Passwords do not match", 1);
      check(badRegistration.correctRegistrationFormat("Usage: /register <password>", null, 2) == null, "No format retry after credential failure");
      ServerAuthFlow failed = new ServerAuthFlow();
      failed.manual("/login badSecret", 0);
      check(failed.complete("Wrong password", 1) == null, "Failed password must not be saved");
      check(failed.complete("Login successful", 2) == null, "Stale success must not resurrect a failed password");
      failed.manual("/l replacementSecret", 3);
      var replacement = failed.complete("Авторизация успешна", 4);
      check(replacement != null && replacement.manual() && replacement.password().equals("replacementSecret"), "Successful manual replacement should be offered for saving");
      ServerAuthFlow timedOut = new ServerAuthFlow();
      timedOut.manual("login oldSecret", 0);
      check(timedOut.complete("Login successful", 120001) == null, "Expired manual credentials must not be suggested");
      ServerAuthFlow reconnect = new ServerAuthFlow();
      check(reconnect.automatic(login, saved, 0) != null, "Reconnect should reset the attempt guard");
      reconnect.manual("register newSecret newSecret", 1);
      check(reconnect.complete("Registration successful", 2).manual(), "Manual registration should be captured");
      for (String command : new String[]{"login user password", "register one two", "register one two three", "changepassword old new", "l", "say password"}) {
         check(ServerAuthFlow.parseManual(command) == null, "Unrelated or invalid manual command captured");
      }
      store.putDefault("Tester", "sharedSecret", false);
      check(new ServerAuthFlow().propose(reg, store.defaultForAccount("Tester"), 0) == null, "Disabled shared password must not produce proposals");
      store.putDefault("Tester", "sharedSecret", true);
      store.putSharedServer("Tester", "shared.example.net", "login", true);
      var sharedServer = store.find("Tester", "shared.example.net");
      check(sharedServer.useSharedPassword() && sharedServer.password().isEmpty(), "Approved shared-password destination should retain the binding");
      store.putDefault("Tester", "changedSharedSecret", true);
      check(store.resolveCredential(sharedServer).password().equals("changedSharedSecret"), "Approved destination must use the current shared password");
      check(store.resolveCredential(saved).password().equals("sampleSecret_123"), "Per-server override must take priority over shared password");
      store.save();
      store.load();
      check(store.find("Tester", "shared.example.net").useSharedPassword(), "Shared-password server binding did not persist");
      store.putDefault("Tester", "changedSharedSecret", false);
      check(!store.resolveCredential(sharedServer).enabled(), "Disabling shared passwords must suspend approved shared destinations");
      store.putDefault("Tester", "", false);
      check(store.resolveCredential(sharedServer) == null, "Removing shared password must prevent its transmission");
      store.putDefault("Tester", "sharedSecret", true);
   }

   private static void check(boolean ok, String message) {
      if (!ok) throw new AssertionError(message);
   }
}
