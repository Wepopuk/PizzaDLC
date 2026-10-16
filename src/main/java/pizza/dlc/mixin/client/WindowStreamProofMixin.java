package pizza.dlc.mixin.client;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin(Window.class)
public class WindowStreamProofMixin {
   @ModifyVariable(method = "setWindowCloseCallback", at = @At("HEAD"), argsOnly = true)
   private Runnable pizzadlc$lastWindowClose(Runnable original) {
      return () -> {
         if (!StreamProofRenderer.keepMainWindowAlive((Window)(Object)this)) original.run();
      };
   }

   @Inject(method = "shouldClose", at = @At("RETURN"), cancellable = true)
   private void pizzadlc$keepCleanWindow(CallbackInfoReturnable<Boolean> cir) {
      if (cir.getReturnValueZ() && StreamProofRenderer.keepMainWindowAlive((Window)(Object)this)) cir.setReturnValue(false);
   }

   @Inject(method = "isFocused", at = @At("RETURN"), cancellable = true)
   private void pizzadlc$focusEitherWindow(CallbackInfoReturnable<Boolean> cir) {
      if (StreamProofRenderer.isFeedFocused()) cir.setReturnValue(true);
      else if (StreamProofRenderer.isMainHidden()) cir.setReturnValue(false);
   }

   @Inject(method = "isIconified", at = @At("RETURN"), cancellable = true)
   private void pizzadlc$visibleFeed(CallbackInfoReturnable<Boolean> cir) {
      if (StreamProofRenderer.hasVisibleFeed()) cir.setReturnValue(false);
   }

   @Inject(method = "onFocus", at = @At("TAIL"))
   private void pizzadlc$switchInputWindow(long handle, boolean focused, CallbackInfo ci) {
      StreamProofRenderer.mainFocusChanged(focused);
   }

   @Inject(method = "onResize", at = @At("TAIL"))
   private void pizzadlc$syncWindowSize(long handle, int width, int height, CallbackInfo ci) {
      StreamProofRenderer.windowSizeChanged(handle, width, height);
   }

   @Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$fullscreenFeed(CallbackInfo ci) {
      if (StreamProofRenderer.toggleFeedFullscreen()) ci.cancel();
   }
}
