package pizza.dlc.mixin.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.MenuAnim;
import pizza.dlc.api.ui.menu.MenuKit;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.menu.SettingsSidebar;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

@Mixin(Screen.class)
public abstract class ScreenSkinMixin {
   @Unique
   private boolean pizzadlc$animPushed;
   @Shadow
   public int width;
   @Shadow
   public int height;

   @Shadow
   protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

   @Shadow
   public abstract List<? extends GuiEventListener> children();

   @Shadow
   protected abstract void extractPanorama(GuiGraphicsExtractor graphics, float partialTick);

   @Redirect(
      method = "extractBackground",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractPanorama(Lnet/minecraft/client/gui/GuiGraphicsExtractor;F)V")
   )
   private void pizzadlc$skinBackground(Screen screen, GuiGraphicsExtractor graphics, float partialTick) {
      if (CleanRenderScope.isActive()) this.extractPanorama(graphics, partialTick);
      else MenuKit.background(graphics, this.width, this.height);
   }

   @WrapMethod(method = "extractRenderStateWithTooltipAndSubtitles")
   private void pizzadlc$windowLayout(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
      Screen self = (Screen)(Object)this;
      if (!ScreenFrame.isFramed(self)) {
         original.call(graphics, mouseX, mouseY, partialTick);
         return;
      }
      ScreenFrame.withLayout(CleanRenderScope.isActive(), () -> {
         self.resize(this.width, this.height);
         try { original.call(graphics, mouseX, mouseY, partialTick); }
         finally {
            ScreenFrame.withLayout(StreamProofRenderer.isVanillaGameplayActive(), () -> self.resize(this.width, this.height));
         }
      });
   }

   @Inject(method = "extractBackground", at = @At("RETURN"))
   private void pizzadlc$frameWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      Screen self = (Screen)(Object)this;
      ScreenFrame.Frame f = ScreenFrame.of(self);
      if (f != null) {
         graphics.pose().pushMatrix();
         MenuAnim.scaleAround(graphics, f, MenuAnim.windowScale(self));
         MenuKit.window(graphics, f.winX(), f.winY(), f.winW(), f.winH(), 1.0F);
         if (f.sidebar()) {
            Render2DUtil.rect(graphics, f.contentX() - 5, f.winY() + 12, 1, f.winH() - 24, MenuKit.DIVIDER);
         }

         graphics.pose().popMatrix();
      }
   }

   @Inject(
      method = "extractRenderStateWithTooltipAndSubtitles",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V")
   )
   private void pizzadlc$animContentBegin(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (CleanRenderScope.isActive()) return;
      Screen self = (Screen)(Object)this;
      this.pizzadlc$animPushed = MenuAnim.pushContent(graphics, self, ScreenFrame.of(self));
   }

   @Inject(
      method = "extractRenderStateWithTooltipAndSubtitles",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
         shift = At.Shift.AFTER
      )
   )
   private void pizzadlc$animContentEnd(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (this.pizzadlc$animPushed) {
         graphics.pose().popMatrix();
         this.pizzadlc$animPushed = false;
      }

      Screen self = (Screen)(Object)this;
      if (!CleanRenderScope.isActive()) MenuAnim.overlay(graphics, self, ScreenFrame.of(self));
   }

   @Inject(method = {"init(II)V", "rebuildWidgets()V"}, at = @At("RETURN"))
   private void pizzadlc$settingsSidebar(CallbackInfo ci) {
      Screen self = (Screen)(Object)this;
      if (ScreenFrame.isFramed(self)) {
         MenuAnim.begin(self);
      }

      if (ScreenFrame.isSettings(self)) {
         for (GuiEventListener child : this.children()) {
            if (!(child instanceof SettingsSidebar)) continue;

            return;
         }

         SettingsSidebar sidebar = this.addRenderableWidget(new SettingsSidebar(self));
         CleanRenderScope.clientMenuWidget(sidebar);
      }
   }
}
