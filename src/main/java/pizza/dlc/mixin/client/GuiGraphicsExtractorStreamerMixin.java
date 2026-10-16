package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pizza.dlc.client.modules.impl.misc.StreamerMode;

@Mixin({GuiGraphicsExtractor.class})
public class GuiGraphicsExtractorStreamerMixin {
   @ModifyVariable(
      method = {"text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private String pizzadlc$streamerString(String text) {
      return StreamerMode.replace(text);
   }

   @ModifyVariable(
      method = {"text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Component pizzadlc$streamerComponent(Component text) {
      return StreamerMode.replace(text);
   }
}
