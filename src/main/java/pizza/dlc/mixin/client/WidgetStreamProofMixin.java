package pizza.dlc.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin(AbstractWidget.class)
public class WidgetStreamProofMixin {
   @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$hideClientWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (CleanRenderScope.hidesWidget((AbstractWidget)(Object)this)) ci.cancel();
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$skipHiddenWidgetClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (CleanRenderScope.hidesInputWidget((AbstractWidget)(Object)this)) cir.setReturnValue(false);
   }

   @Inject(method = "nextFocusPath", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$skipHiddenWidgetFocus(FocusNavigationEvent event, CallbackInfoReturnable<ComponentPath> cir) {
      if (CleanRenderScope.hidesInputWidget((AbstractWidget)(Object)this)) cir.setReturnValue(null);
   }

   @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$skipHiddenWidgetHover(double x, double y, CallbackInfoReturnable<Boolean> cir) {
      if (CleanRenderScope.hidesInputWidget((AbstractWidget)(Object)this)) cir.setReturnValue(false);
   }

   @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$skipHiddenWidgetNarration(CallbackInfoReturnable<Boolean> cir) {
      if (CleanRenderScope.hidesInputWidget((AbstractWidget)(Object)this)) cir.setReturnValue(false);
   }
}
