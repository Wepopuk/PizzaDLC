package pizza.dlc.mixin.client;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.combat.CombatHudText;


import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.emote.EmoteWheel;
import pizza.dlc.api.events.impl.input.EventInput;
import pizza.dlc.api.events.impl.input.EventKey;
import pizza.dlc.api.events.impl.input.EventMouse;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.client.modules.impl.misc.FreeCam;
import pizza.dlc.client.modules.impl.misc.Zoom;
import pizza.dlc.client.modules.impl.visuals.Emotes;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin({MouseHandler.class})
public class MouseMixin implements QuickImports {
   @Inject(
      method = {"onButton"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onButton(long handle, MouseButtonInfo info, int action, CallbackInfo ci) {
      if (StreamProofRenderer.ignoreMainInput(handle)) {
         ci.cancel();
         return;
      }
      if (mc.gui.screen() == null && handle == window.handle()) {
         if (StreamProofRenderer.isVanillaGameplayActive()) return;
         int button = info.button();
         if (Emotes.onWheelMouseButton(button, action)) {
            ci.cancel();
            return;
         }

         new EventInput(button, button, action == 0).call();
         PizzaDLC.getInstance().getBus().post(new EventMouse(button, action));
         PizzaDLC.getInstance().getBus().post(new EventKey(-100 - button, action));
         if (mc.gui.screen() != null) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"onScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onScroll(long handle, double horizontal, double vertical, CallbackInfo ci) {
      if (StreamProofRenderer.ignoreMainInput(handle)) {
         ci.cancel();
         return;
      }
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      if (mc.gui.screen() == null && handle == window.handle()) {
         if (EmoteWheel.onScroll(vertical)) {
            ci.cancel();
            return;
         }
         Zoom zoom = Zoom.get();
         if (zoom != null && zoom.handleScroll(vertical)) {
            ci.cancel();
         } else {
            if (vertical != 0.0) {
               int code = vertical > 0.0 ? -200 : -201;
               EventKey ev = new EventKey(code, 1);
               PizzaDLC.getInstance().getBus().post(ev);
               if (ev.isCancelled()) {
                  ci.cancel();
               }
            }
         }
      }
   }

   @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
   private void pizzadlc$activeMouseMovement(long handle, double x, double y, CallbackInfo ci) {
      if (StreamProofRenderer.ignoreMainInput(handle)) ci.cancel();
   }

   @Redirect(
      method = {"turnPlayer"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
      )
   )
   private void pizzadlc$freecamLook(LocalPlayer player, double yRot, double xRot) {
      if (EmoteWheel.onMouseTurn(yRot, xRot)) return;

      FreeCam fc = FreeCam.get();
      if (fc != null && fc.isEnabled()) {
         fc.addLookDelta(yRot, xRot);
      } else {
         if (pizza.dlc.api.neuro.NeuroSystem.managed()) {
            pizza.dlc.api.neuro.NeuroSystem.get().mouse(yRot,xRot); player.turn(yRot,xRot); return;
         }
         if (pizza.dlc.api.util.combat.CombatRuntime.mouseTurn(yRot, xRot)) return;
         player.turn(yRot, xRot);
      }
   }
}
