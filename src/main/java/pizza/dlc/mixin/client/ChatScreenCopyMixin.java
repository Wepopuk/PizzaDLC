package pizza.dlc.mixin.client;

import pizza.dlc.api.translation.Translations;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.interfaces.IChatMessageAccess;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.util.LegacyText;
import pizza.dlc.api.util.game.PasswordHider;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.modules.impl.misc.Tweaks;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin({ChatScreen.class})
public class ChatScreenCopyMixin {
   private static long pizzadlc$lastClickMs;
   private static double pizzadlc$lastX;
   private static double pizzadlc$lastY;
   @Shadow
   protected EditBox input;
   @Unique
   private static final int pizzadlc$BTN = 12;
   @Unique
   private static final int pizzadlc$GAP = 2;
   @Unique
   private static final int pizzadlc$MARGIN = 2;

   @Inject(
      method = {"mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$copyOnDoubleClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (event.button() == 0 && Tweaks.copyChatEnabled()) {
         long now = System.currentTimeMillis();
         boolean isDouble = now - pizzadlc$lastClickMs < 250L && Math.abs(event.x() - pizzadlc$lastX) < 6.0 && Math.abs(event.y() - pizzadlc$lastY) < 6.0;
         pizzadlc$lastClickMs = now;
         pizzadlc$lastX = event.x();
         pizzadlc$lastY = event.y();
         if (isDouble) {
            pizzadlc$lastClickMs = 0L;
            Minecraft mc = Minecraft.getInstance();
            GuiMessage msg = ((IChatMessageAccess)mc.gui.hud.getChat()).pizzadlc$messageAt(event.x(), event.y());
            if (msg != null) {
               String legacy = LegacyText.toLegacy(msg.content());
               if (!legacy.isEmpty()) {
                  mc.keyboardHandler.setClipboard(legacy);
                  NotificationManager.add("Tweaks", Translations.text("Сообщение скопировано"), NotificationType.INFO, 2000L);
                  cir.setReturnValue(true);
               }
            }
         }
      }
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$passwordShrink(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (pizza.dlc.api.util.render.streamproof.CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive()) {
         if (this.input != null) this.input.setWidth(ctx.guiWidth() - this.input.getX() - 2);
         return;
      }
      if (this.input != null) {
         int w = ctx.guiWidth() - 12 - 2 - 2 - this.input.getX();
         if (w > 20 && this.input.getWidth() != w) {
            this.input.setWidth(w);
         }
      }
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$passwordRender(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (pizza.dlc.api.util.render.streamproof.CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive()) return;
      int btnX = ctx.guiWidth() - 12 - 2;
      int btnY = ctx.guiHeight() - 12 - 2;
      Render2DUtil.roundedRect(ctx, btnX, btnY, 12, 12, 2.0F, 0xE0101319);
      MsdfFont f = PizzaDLCFonts.sfpro();
      float fs = 8.0F;
      float lw = Render2DUtil.msdfWidth(f, "H", fs);
      Render2DUtil.msdfText(ctx, f, "H", btnX + (12.0F - lw) / 2.0F, btnY + (12.0F - 0.7F * fs) / 2.0F, fs, PasswordHider.isEnabled() ? 0xFF55FF55 : -1);
      if (this.input != null) {
         String text = this.input.getValue();
         int argStart = PasswordHider.getArgStart(text);
         if (argStart != -1) {
            String password = text.substring(argStart);
            if (!password.isEmpty()) {
               Font font = Minecraft.getInstance().font;
               int passX = this.input.getX() + font.width(text.substring(0, argStart));
               int passW = font.width(password) + 2;
               Render2DUtil.rect(ctx, passX, this.input.getY() - 1, passW, this.input.getHeight() - 1, 0xFF000000);
            }
         }
      }
   }

   @Inject(
      method = {"mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$passwordButtonClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      if (event.button() == 0) {
         Window win = Minecraft.getInstance().getWindow();
         int btnX = win.getGuiScaledWidth() - 12 - 2;
         int btnY = win.getGuiScaledHeight() - 12 - 2;
         if (event.x() >= btnX && event.x() <= btnX + 12 && event.y() >= btnY && event.y() <= btnY + 12) {
            PasswordHider.toggle();
            cir.setReturnValue(true);
         }
      }
   }
}
