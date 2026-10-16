package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.account.AccountSwitcherScreen;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.ui.menu.MainMenuScreen;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
   protected TitleScreenMixin(Component title) { super(title); }

   @Inject(method = "init", at = @At("RETURN"))
   private void pizzadlc$customizeTitle(CallbackInfo ci) {
      for (var child : this.children()) {
         if (child instanceof AbstractWidget widget && ((Object)this instanceof MainMenuScreen || widget instanceof SpriteIconButton || widget instanceof PlainTextButton
            || widget.getMessage().getContents() instanceof TranslatableContents text && "menu.online".equals(text.getKey()))) {
            CleanRenderScope.vanillaMenuWidget(widget);
         }
      }
      Button accounts = this.addRenderableWidget(Button.builder(Component.literal("Аккаунты"),
         button -> Minecraft.getInstance().setScreenAndShow(new AccountSwitcherScreen(this)))
      .bounds(this.width / 2 - 100, this.height / 4 + 96, 200, 20).build());
      CleanRenderScope.clientMenuWidget(accounts);
   }

   @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/TitleScreen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V"))
   private void pizzadlc$shaderBackground(TitleScreen screen, GuiGraphicsExtractor graphics, float partialTick) {
      if (CleanRenderScope.isActive()) this.extractPanorama(graphics, partialTick);
      else Render2DUtil.menuBackground(graphics, this.width, this.height);
   }

   @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"))
   private void pizzadlc$logo(LogoRenderer logo, GuiGraphicsExtractor graphics, int width, float fade) {
      if (CleanRenderScope.isActive()) logo.extractRenderState(graphics, width, fade);
   }

   @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"))
   private void pizzadlc$splash(SplashRenderer splash, GuiGraphicsExtractor graphics, int width, Font font, float fade) {
      if (CleanRenderScope.isActive()) splash.extractRenderState(graphics, width, font, fade);
   }

   @Redirect(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
   private void pizzadlc$version(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
      if (CleanRenderScope.isActive()) graphics.text(font, text, x, y, color);
   }
}
