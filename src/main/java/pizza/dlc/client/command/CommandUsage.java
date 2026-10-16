package pizza.dlc.client.command;

import net.minecraft.ChatFormatting;
import pizza.dlc.api.util.game.ChatUtility;

public final class CommandUsage {
   private CommandUsage() {
   }

   public static void send(String command, String... usages) {
      ChatUtility.send(ChatFormatting.GRAY + "Usage for " + ChatFormatting.WHITE + command + ChatFormatting.GRAY + ":");

      for (String usage : usages) {
         ChatUtility.send(ChatFormatting.DARK_GRAY + " - " + ChatFormatting.WHITE + usage);
      }
   }
}
