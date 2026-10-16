package pizza.dlc.api.interfaces;

import net.minecraft.client.multiplayer.chat.GuiMessage;

public interface IChatMessageAccess {
   GuiMessage pizzadlc$messageAt(double mouseX, double mouseY);
}
