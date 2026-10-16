package pizza.dlc.api.macros.constructor.impl;

import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.macros.constructor.MacroBlock;

public class SendMessageBlock implements MacroBlock, QuickImports {
   private final String message;

   public SendMessageBlock(String message) {
      this.message = message;
   }

   @Override
   public void execute() {
      if (mc.player == null || mc.player.connection == null) return;

      if (this.message.startsWith("/")) {
         mc.player.connection.sendCommand(this.message.substring(1));
      } else {
         mc.player.connection.sendChat(this.message);
      }
   }

   @Override
   public String renderName() {
      return "Message: " + this.message;
   }
}
