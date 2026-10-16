package pizza.dlc.api.macros.constructor;

import java.util.ArrayList;
import java.util.List;
import pizza.dlc.api.macros.constructor.impl.JumpBlock;
import pizza.dlc.api.macros.constructor.impl.SwingBlock;

public class MacroBlocks {
   private final ArrayList<MacroBlock> handled = new ArrayList<>();

   public MacroBlocks() {
      this.handled.addAll(List.of(new JumpBlock(), new SwingBlock()));
   }

   public ArrayList<MacroBlock> getHandled() {
      return this.handled;
   }
}
