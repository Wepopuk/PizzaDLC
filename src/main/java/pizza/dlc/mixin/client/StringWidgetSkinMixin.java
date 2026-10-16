package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractStringWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.skin.UiSkin;

@Mixin(AbstractStringWidget.class)
public class StringWidgetSkinMixin {
   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$nexaText(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      AbstractWidget self = (AbstractWidget)(Object)this;
      if (UiSkin.drawString(graphics, self, self.getMessage())) {
         ci.cancel();
      }
   }
}
