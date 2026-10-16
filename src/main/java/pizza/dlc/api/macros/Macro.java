package pizza.dlc.api.macros;

import java.util.ArrayList;
import java.util.List;
import pizza.dlc.api.macros.constructor.MacroBlock;

public class Macro {
   private String name;
   private int key = -1;
   private final List<MacroBlock> instructions = new ArrayList<>();

   public Macro() {
   }

   public Macro(String name, List<MacroBlock> blocks) {
      this.name = name;
      this.instructions.addAll(blocks);
   }

   public Macro(List<MacroBlock> blocks) {
      this.instructions.addAll(blocks);
   }

   public void execute() {
      if (this.instructions.isEmpty()) return;

      for (MacroBlock block : this.instructions) {
         block.execute();
      }
   }

   public String getName() {
      return this.name;
   }

   public int getKey() {
      return this.key;
   }

   public List<MacroBlock> getInstructions() {
      return this.instructions;
   }

   public void setName(String name) {
      this.name = name;
   }

   public void setKey(int key) {
      this.key = key;
   }
}
