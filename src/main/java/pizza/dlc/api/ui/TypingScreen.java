package pizza.dlc.api.ui;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;

public interface TypingScreen {
   boolean isTyping();

   static boolean isTyping(Screen screen) {
      if (screen == null) return false;
      if (screen instanceof ChatScreen || screen instanceof AbstractSignEditScreen || screen instanceof BookEditScreen) return true;
      if (screen.getFocused() instanceof EditBox || screen.getFocused() instanceof MultiLineEditBox) return true;
      return screen instanceof TypingScreen typing && typing.isTyping();
   }
}
