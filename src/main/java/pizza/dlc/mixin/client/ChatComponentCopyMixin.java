package pizza.dlc.mixin.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pizza.dlc.api.interfaces.IChatMessageAccess;

@Mixin({ChatComponent.class})
public class ChatComponentCopyMixin implements IChatMessageAccess {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private List<Line> trimmedMessages;
   @Shadow
   private int chatScrollbarPos;

   @Shadow
   private double getScale() {
      throw new AssertionError();
   }

   @Shadow
   private int getLineHeight() {
      throw new AssertionError();
   }

   @Shadow
   public int getLinesPerPage() {
      throw new AssertionError();
   }

   @Override
   public GuiMessage pizzadlc$messageAt(double mouseX, double mouseY) {
      if (this.trimmedMessages.isEmpty()) {
         return null;
      }

      double scale = this.getScale();
      if (scale <= 0.0) {
         return null;
      }

      int eh = this.getLineHeight();
      if (eh <= 0) {
         return null;
      }

      int screenH = this.minecraft.getWindow().getGuiScaledHeight();
      int chatBottom = Mth.floor((screenH - 40) / scale);
      double localY = mouseY / scale;
      if (localY > chatBottom) {
         return null;
      }

      int i = (int)((chatBottom - localY) / eh);
      if (i >= 0 && i < this.getLinesPerPage()) {
         int idx = i + this.chatScrollbarPos;
         if (idx >= 0 && idx < this.trimmedMessages.size()) {
            Line line = this.trimmedMessages.get(idx);
            double localX = mouseX / scale - 4.0;
            return !(localX < 0.0) && !(localX > this.minecraft.font.width(line.content()) + 2) ? line.parent() : null;
         }

         return null;
      }

      return null;
   }
}
