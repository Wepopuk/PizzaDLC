package pizza.dlc.api.macros.constructor;

import java.util.ArrayList;
import pizza.dlc.api.settings.Setting;

public interface MacroBlock {
   default ArrayList<Setting> getSettings() {
      return new ArrayList<>();
   }

   void execute();

   String renderName();
}
