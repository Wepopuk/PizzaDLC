package pizza.dlc.api.macros.constructor.impl;

import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.macros.constructor.MacroBlock;

public class JumpBlock implements MacroBlock, QuickImports {
   @Override
   public void execute() {
      if (mc.player == null || !mc.player.onGround() || mc.options.keyJump.isDown()) return;

      mc.player.jumpFromGround();
   }

   @Override
   public String renderName() {
      return "Jump from ground";
   }
}
