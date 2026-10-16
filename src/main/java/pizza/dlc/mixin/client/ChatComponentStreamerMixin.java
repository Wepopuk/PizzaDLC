package pizza.dlc.mixin.client;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pizza.dlc.client.modules.impl.misc.StreamerMode;

@Mixin({ChatComponent.class})
public class ChatComponentStreamerMixin {
   @ModifyVariable(
      method = {"addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Component pizzadlc$streamerChat(Component message) {
      return StreamerMode.replace(message);
   }
}
