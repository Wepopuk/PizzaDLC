package pizza.dlc.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin(InputConstants.class)
public class InputConstantsStreamProofMixin {
   @Redirect(method = "isKeyDown", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetKey(JI)I"))
   private static int pizzadlc$readActiveKeyboard(long handle, int key) {
      return GLFW.glfwGetKey(StreamProofRenderer.inputHandle(handle), key);
   }

   @Inject(method = "grabOrReleaseMouse", at = @At("HEAD"), cancellable = true)
   private static void pizzadlc$captureActiveCursor(Window window, int mode, double x, double y, CallbackInfo ci) {
      if (StreamProofRenderer.setFeedMouseMode(mode, x, y)) ci.cancel();
   }

   @Redirect(method = "updateRawMouseInput", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;handle()J"))
   private static long pizzadlc$rawMouseWindow(Window window) {
      return StreamProofRenderer.inputHandle(window.handle());
   }
}
