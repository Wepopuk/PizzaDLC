package pizza.dlc.api.macros.constructor.impl;

import net.minecraft.world.InteractionHand;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.macros.constructor.MacroBlock;

public class SwingBlock implements MacroBlock, QuickImports {
   @Override
   public void execute() {
      if (mc.player == null) return;

      mc.player.swing(InteractionHand.MAIN_HAND);
   }

   @Override
   public String renderName() {
      return "Swing hand";
   }
}
