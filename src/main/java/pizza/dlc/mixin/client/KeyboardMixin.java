package pizza.dlc.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.input.EventInput;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.ui.editor.ItemEditorScreen;
import pizza.dlc.api.util.ItemNbtViewer;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.modules.impl.visuals.Emotes;

@Mixin({KeyboardHandler.class})
public class KeyboardMixin implements QuickImports {
   @Inject(
      method = {"keyPress"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onKeyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
      if (StreamProofRenderer.ignoreMainInput(handle)) {
         ci.cancel();
         return;
      }
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      if (action == 1 && handle == window.handle() && ItemEditorScreen.tryOpen(event)) {
         ci.cancel();
      } else {
         if (action != 0 && handle == window.handle() && mc.gui.screen() != null) {
            ItemNbtViewer.onArrowKey(event.key());
         }

         if (mc.gui.screen() == null && handle == window.handle()) {
            int key = event.key();
            int scanCode = event.scancode();
            if (Emotes.consumeEscape(key, action)) {
               ci.cancel();
               return;
            }

            if (action == 1) {
               new EventInput(key, scanCode, false).call();
            } else if (action == 0) {
               new EventInput(key, scanCode, true).call();
            }

            PizzaDLC.getInstance().getBus().post(new EventKey(key, action));
            if (mc.gui.screen() != null) {
               ci.cancel();
            }
         }
      }
   }

   @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$activeTextInput(long handle, CharacterEvent event, CallbackInfo ci) {
      if (StreamProofRenderer.ignoreMainInput(handle)) ci.cancel();
   }
}
