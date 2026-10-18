package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pizza.dlc.client.modules.impl.misc.StreamerMode;

@Mixin({GuiGraphicsExtractor.class})
public class GuiGraphicsExtractorStreamerMixin {
   // text(...) with shadow
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

   @ModifyVariable(
      method = {"text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private FormattedCharSequence pizzadlc$streamerSequence(FormattedCharSequence text) {
      return StreamerMode.replace(text);
   }

   // text(...) without shadow
   @ModifyVariable(
      method = {"text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private String pizzadlc$streamerStringPlain(String text) {
      return StreamerMode.replace(text);
   }

   @ModifyVariable(
      method = {"text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Component pizzadlc$streamerComponentPlain(Component text) {
      return StreamerMode.replace(text);
   }

   @ModifyVariable(
      method = {"text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private FormattedCharSequence pizzadlc$streamerSequencePlain(FormattedCharSequence text) {
      return StreamerMode.replace(text);
   }

   // centeredText(...)
   @ModifyVariable(
      method = {"centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private String pizzadlc$streamerCenteredString(String text) {
      return StreamerMode.replace(text);
   }

   @ModifyVariable(
      method = {"centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Component pizzadlc$streamerCenteredComponent(Component text) {
      return StreamerMode.replace(text);
   }

   @ModifyVariable(
      method = {"centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private FormattedCharSequence pizzadlc$streamerCenteredSequence(FormattedCharSequence text) {
      return StreamerMode.replace(text);
   }

   // word-wrapped text (books, descriptions, long messages)
   @ModifyVariable(
      method = {"textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private FormattedText pizzadlc$streamerWrapped(FormattedText text) {
      return text instanceof Component c ? StreamerMode.replace(c) : text;
   }

   @ModifyVariable(
      method = {"textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private FormattedText pizzadlc$streamerWrappedShadow(FormattedText text) {
      return text instanceof Component c ? StreamerMode.replace(c) : text;
   }

   // text with backdrop
   @ModifyVariable(
      method = {"textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Component pizzadlc$streamerBackdrop(Component text) {
      return StreamerMode.replace(text);
   }
}
