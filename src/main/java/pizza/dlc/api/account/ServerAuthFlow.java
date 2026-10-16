package pizza.dlc.api.account;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One connection's auth state. Shared credentials require explicit destination approval. */
public final class ServerAuthFlow {
   private static final Pattern LOGIN = Pattern.compile("/(login|l)(?=\\s|$)");
   private static final Pattern REGISTER = Pattern.compile("/(register|reg)(?=\\s|$)");
   private static final Pattern ARGUMENT = Pattern.compile("\\G\\s*(<[^>]+>|\\[[^]]+\\]|\\{[^}]+\\}|\\([^)]+\\)|[\\p{L}\\p{N}_-]+)");
   private boolean loginAttempted;
   private boolean registerAttempted;
   private boolean authenticated;
   private Attempt pending;
   private long pendingAt;
   private boolean manualRequired;
   private Kind failure;
   private boolean registrationFormatCorrected;

   /** Only an explicit, exact-server entry can authorize automatic transmission. */
   public Attempt automatic(String message, ServerLoginStore.Entry saved, long now) {
      return automatic(message, saved, now, null);
   }

   public Attempt automatic(String message, ServerLoginStore.Entry saved, long now, CommandNode<?> commands) {
      expire(now);
      if (saved == null || saved.server() == null || saved.server().isEmpty()) return null;
      Attempt attempt = propose(message, saved, now, commands);
      return attempt != null && approve(attempt, now) ? attempt : null;
   }

   /** Returns a reviewable candidate without sending or treating a shared password as permission. */
   public Attempt propose(String message, ServerLoginStore.Entry credential, long now) {
      return propose(message, credential, now, null);
   }

   public Attempt propose(String message, ServerLoginStore.Entry credential, long now, CommandNode<?> commands) {
      expire(now);
      if (manualRequired || authenticated || pending != null || credential == null || !credential.enabled()
         || !ServerLoginStore.validPassword(credential.password())) return null;
      String text = clean(message);
      Matcher register = REGISTER.matcher(text);
      if (isRegistrationPrompt(message) && !registerAttempted && register.find()) {
         int slots = registrationPasswordCount(message, commands);
         if (slots != 1 && slots != 2) return null;
         Attempt attempt = new Attempt(Kind.REGISTER, register.group(1), credential.password(), slots == 2, false);
         return attempt.wireCommand().length() <= 256 ? attempt : null;
      }
      if (isLoginPrompt(message) && !loginAttempted) {
         Matcher login = LOGIN.matcher(text);
         if (!login.find()) return null;
         String command = credential.server().isEmpty() ? login.group(1) : credential.command();
         if (!"login".equals(command) && !"l".equals(command)) return null;
         return new Attempt(Kind.LOGIN, command, credential.password(), false, false);
      }
      return null;
   }

   public boolean approve(Attempt attempt, long now) {
      expire(now);
      if (manualRequired || pending != null || authenticated || (attempt.kind() == Kind.LOGIN ? loginAttempted : registerAttempted)) return false;
      if (attempt.kind() == Kind.LOGIN) loginAttempted = true;
      else registerAttempted = true;
      remember(attempt, now);
      return true;
   }

   public void manual(String command, long now) {
      Attempt attempt = parseManual(command);
      if (attempt == null) return;
      manualRequired = true;
      if (attempt.kind() == Kind.LOGIN) loginAttempted = true;
      else registerAttempted = true;
      authenticated = false;
      remember(attempt, now);
   }

   public Attempt complete(String message, long now) {
      expire(now);
      if (pending == null) return null;
      String text = clean(message);
      if (isFailure(text)) {
         manualRequired = true;
         if (!pending.manual()) failure = pending.kind();
         pending = null;
         return null;
      }
      boolean success = pending.kind() == Kind.REGISTER ? isRegistrationSuccess(text) : isLoginSuccess(text);
      if (!success) return null;
      Attempt result = pending;
      pending = null;
      if (result.kind() == Kind.LOGIN) authenticated = true;
      return result;
   }

   public void expire(long now) {
      if (pending != null && now - pendingAt > 120000) {
         if (!pending.manual()) {
            manualRequired = true;
            failure = pending.kind();
         }
         pending = null;
      }
   }

   public Kind takeFailure() {
      Kind result = failure;
      failure = null;
      return result;
   }

   /** A server may reject the argument count and supply a more precise usage line. */
   public Attempt correctRegistrationFormat(String message, CommandNode<?> commands, long now) {
      expire(now);
      if (manualRequired || pending == null || pending.manual() || pending.kind() != Kind.REGISTER || registrationFormatCorrected) return null;
      String text = clean(message);
      if (!(text.contains("usage") || text.contains("использован") || text.contains("синтаксис")
         || text.contains("аргумент") || text.contains("arguments"))) return null;
      Matcher register = REGISTER.matcher(text);
      if (!register.find() || !register.group(1).equals(pending.command())) return null;
      int count = registrationPasswordCount(message, commands);
      if ((count != 1 && count != 2) || pending.repeatPassword() == (count == 2)) return null;
      Attempt corrected = new Attempt(Kind.REGISTER, pending.command(), pending.password(), count == 2, false);
      if (corrected.wireCommand().length() > 256) return null;
      registrationFormatCorrected = true;
      remember(corrected, now);
      return corrected;
   }

   /** Count the command's arguments, not mentions of passwords elsewhere in the message. */
   public static int registrationPasswordCount(String message, CommandNode<?> commands) {
      Matcher register = REGISTER.matcher(clean(message));
      if (!register.find()) return 0;
      String tail = clean(message).substring(register.end());
      Matcher arguments = ARGUMENT.matcher(tail);
      int slots = 0;
      while (arguments.find()) {
         String token = arguments.group(1);
         boolean wrapped = "<[{(".indexOf(token.charAt(0)) >= 0;
         if (!passwordSlot(token)) {
            if (wrapped) return 0; // Extra required data (email, username, etc.) needs manual input.
            break;
         }
         if (++slots > 2) return 0;
      }
      if (slots > 0) return slots;
      if (commands == null) return 0;
      CommandNode<?> node = commands.getChild(register.group(1));
      for (int depth = 0; node != null && depth < 3 && node.getRedirect() != null; depth++) node = node.getRedirect();
      for (int count = 1; node != null && count <= 2; count++) {
         if (node.getChildren().size() != 1) return 0;
         CommandNode<?> child = node.getChildren().iterator().next();
         if (!(child instanceof ArgumentCommandNode<?, ?> arg) || !passwordSlot(child.getName())) return 0;
         if (arg.getType() instanceof StringArgumentType string && string.getType() == StringArgumentType.StringType.GREEDY_PHRASE) return 0;
         if (child.getCommand() != null) return count;
         node = child;
      }
      return 0;
   }

   private static boolean passwordSlot(String token) {
      String value = token.toLowerCase(Locale.ROOT).replaceAll("^[<\\[\\{(]|[>\\]\\})]$", "");
      return value.contains("парол") || value.contains("password") || value.contains("passwd")
         || value.contains("повтор") || value.contains("подтвержд") || value.contains("confirm")
         || value.contains("repeat") || value.matches("(?:pass|pw|pwd)[_-]?[12]?");
   }

   private void remember(Attempt attempt, long now) {
      pending = attempt;
      pendingAt = now;
   }

   public static Attempt parseManual(String command) {
      if (command == null) return null;
      String value = command.startsWith("/") ? command.substring(1) : command;
      String[] words = value.trim().split("\\s+");
      if (words.length < 2 || !ServerLoginStore.validPassword(words[1])) return null;
      String name = words[0].toLowerCase(Locale.ROOT);
      if ((name.equals("login") || name.equals("l")) && words.length == 2) return new Attempt(Kind.LOGIN, name, words[1], false, true);
      if ((name.equals("register") || name.equals("reg")) && (words.length == 2 || words.length == 3 && words[1].equals(words[2]))) {
         Attempt attempt = new Attempt(Kind.REGISTER, name, words[1], words.length == 3, true);
         return attempt.wireCommand().length() <= 256 ? attempt : null;
      }
      return null;
   }

   public static boolean isLoginPrompt(String message) {
      String text = clean(message);
      if (!LOGIN.matcher(text).find() || isFailure(text) || isLoginSuccess(text) || isRegistrationSuccess(text)) return false;
      if (REGISTER.matcher(text).find()) return false;
      return asksForPassword(text) && !isRegistrationPrompt(message);
   }

   public static boolean isRegistrationPrompt(String message) {
      String text = clean(message);
      if (!REGISTER.matcher(text).find() || isFailure(text) || isLoginSuccess(text) || isRegistrationSuccess(text)) return false;
      if (LOGIN.matcher(text).find() && !(text.contains("не зарегистр") || text.contains("not registered") || text.contains("зарегистриру"))) return false;
      return asksForPassword(text) || text.contains("зарегистриру") || text.contains("not registered") || text.contains("не зарегистр");
   }

   private static boolean asksForPassword(String text) {
      return text.contains("пароль") || text.contains("password") || text.contains("<pass>")
         || text.contains("войд") || text.contains("авториз") || text.contains("введ")
         || text.contains("please") || text.contains("use ") || text.contains("log in");
   }

   private static boolean isFailure(String text) {
      return text.contains("неверн") || text.contains("неправильн") || text.contains("wrong password")
         || text.contains("incorrect password") || text.contains("invalid password") || text.contains("passwords do not match")
         || text.contains("пароли не совпад") || text.contains("не совпадают парол") || text.contains("пароль не подош")
         || text.contains("already registered") || text.contains("уже зарегистр") || text.contains("слишком корот")
         || text.contains("too short") || text.contains("registration failed") || text.contains("login failed")
         || text.contains("ошибка авториза") || text.contains("ошибка регистра") || text.contains("не удалось");
   }

   private static boolean isLoginSuccess(String text) {
      return text.contains("успешно авториз") || text.contains("авторизация успеш") || text.contains("успешно вош")
         || text.contains("вы вошли") || text.contains("успешный вход") || text.contains("вход выполнен")
         || text.contains("successfully logged in") || text.contains("logged in successfully") || text.contains("login successful")
         || text.contains("successfully authenticated") || text.contains("authentication successful") || text.contains("now logged in");
   }

   private static boolean isRegistrationSuccess(String text) {
      return text.contains("успешно зарегистр") || text.contains("регистрация успеш") || text.contains("успешная регистра")
         || text.contains("successfully registered") || text.contains("registered successfully") || text.contains("registration successful");
   }

   private static String clean(String message) {
      return message == null ? "" : message.replaceAll("§.", "").toLowerCase(Locale.ROOT);
   }

   public enum Kind { LOGIN, REGISTER }

   public record Attempt(Kind kind, String command, String password, boolean repeatPassword, boolean manual) {
      public String wireCommand() {
         return command + " " + password + (repeatPassword ? " " + password : "");
      }

      public String loginCommand() {
         return kind == Kind.LOGIN ? command : "login";
      }

      @Override
      public String toString() {
         return "AuthAttempt[kind=" + kind + ", command=" + command + ", manual=" + manual + "]";
      }
   }
}
