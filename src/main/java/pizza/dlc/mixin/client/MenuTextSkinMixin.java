package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.skin.UiSkin;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

/** Native list labels and section headers share the client font with option controls. */
@Mixin(GuiGraphicsExtractor.class)
public class MenuTextSkinMixin {
   @Unique private static boolean pizzadlc$menuText() {
      if (CleanRenderScope.isActive()) return false;
      var mc = Minecraft.getInstance();
      return mc != null && mc.gui != null && ScreenFrame.isFramed(mc.gui.screen());
   }

   @Inject(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$string(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (pizzadlc$menuText() && text != null && UiSkin.drawMenuText((GuiGraphicsExtractor)(Object)this, Component.literal(text), x, y, color)) ci.cancel();
   }

   @Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$component(Font font, Component text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (pizzadlc$menuText() && UiSkin.drawMenuText((GuiGraphicsExtractor)(Object)this, text, x, y, color)) ci.cancel();
   }

   @Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$formatted(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
      if (pizzadlc$menuText() && UiSkin.drawMenuText((GuiGraphicsExtractor)(Object)this, UiSkin.component(text), x, y, color)) ci.cancel();
   }

   @Inject(method = "centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$centeredString(Font font, String text, int x, int y, int color, CallbackInfo ci) {
      if (pizzadlc$menuText() && text != null && UiSkin.drawCentered((GuiGraphicsExtractor)(Object)this, Component.literal(text), x, y + 4.5F, color)) ci.cancel();
   }

   @Inject(method = "centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$centeredComponent(Font font, Component text, int x, int y, int color, CallbackInfo ci) {
      if (pizzadlc$menuText() && UiSkin.drawCentered((GuiGraphicsExtractor)(Object)this, text, x, y + 4.5F, color)) ci.cancel();
   }

   @Inject(method = "centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$centeredFormatted(Font font, FormattedCharSequence text, int x, int y, int color, CallbackInfo ci) {
      if (pizzadlc$menuText() && UiSkin.drawCentered((GuiGraphicsExtractor)(Object)this, UiSkin.component(text), x, y + 4.5F, color)) ci.cancel();
   }
}
