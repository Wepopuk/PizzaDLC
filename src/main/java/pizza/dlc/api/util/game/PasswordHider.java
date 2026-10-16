package pizza.dlc.api.util.game;

import java.util.Locale;

public final class PasswordHider {
   private static boolean enabled = true;
   private static final String[] AUTH_COMMANDS = new String[]{
      "/reg ", "/register ", "/login ", "/l ", "/log ", "/pas ", "/pass ", "/password ", "/changepassword ", "/changepass "
   };

   private PasswordHider() {
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void toggle() {
      enabled = !enabled;
   }

   public static boolean isPasswordInput(String text) {
      return getArgStart(text) != -1;
   }

   public static int getArgStart(String text) {
      if (enabled && text != null && !text.isEmpty()) {
         String lower = text.toLowerCase(Locale.ROOT);

         for (String cmd : AUTH_COMMANDS) {
            if (!lower.startsWith(cmd)) continue;

            return cmd.length();
         }

         return -1;
      }

      return -1;
   }
}
