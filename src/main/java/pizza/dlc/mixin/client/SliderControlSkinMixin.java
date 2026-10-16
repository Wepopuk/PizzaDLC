package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.menu.ScreenFrame;
import pizza.dlc.api.ui.skin.OptionControlLayout;
import pizza.dlc.api.ui.skin.OptionControlSkin;

@Mixin(AbstractSliderButton.class)
public abstract class SliderControlSkinMixin {
   @Shadow protected double value;
   @Shadow private boolean dragging;
   @Shadow protected abstract void setValue(double value);

   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$customSlider(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (OptionControlSkin.drawSlider(g, (AbstractSliderButton)(Object)this, this.value, this.dragging)) ci.cancel();
   }

   @Inject(method = "setValueFromMouse", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$customTrackInput(MouseButtonEvent event, CallbackInfo ci) {
      if (ScreenFrame.isVanillaLayout()) return;
      this.setValue(OptionControlLayout.ratio(event.x(), OptionControlSkin.sliderLayout((AbstractSliderButton)(Object)this)));
      ci.cancel();
   }
}
