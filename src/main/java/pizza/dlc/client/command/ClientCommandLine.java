package pizza.dlc.client.command;

public final class ClientCommandLine {
   public static final String PREFIX = ".";

   private ClientCommandLine() {
   }

   public static boolean isCommand(String message) {
      return message != null && message.startsWith(PREFIX);
   }

   public static String bodyOf(String message) {
      return isCommand(message) ? message.substring(PREFIX.length()).trim() : "";
   }
}
