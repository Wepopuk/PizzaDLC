package pizza.dlc.mixin.client;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.ui.skin.OptionControlSkin;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin({AbstractButton.class})
public class AbstractButtonGlassMixin {
   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$optionControl(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (OptionControlSkin.drawButton(g, (AbstractWidget)(Object)this)) ci.cancel();
   }
   @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$skipHiddenButtonKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
      if (CleanRenderScope.hidesInputWidget((AbstractWidget)(Object)this)) cir.setReturnValue(false);
   }
}
