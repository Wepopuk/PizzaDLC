package pizza.dlc.mixin.client;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.misc.StreamerMode;

@Mixin({PlayerTabOverlay.class})
public class PlayerTabOverlayStreamerMixin {
   @Inject(
      method = {"getNameForDisplay"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void pizzadlc$streamerName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
      cir.setReturnValue(StreamerMode.replace((Component)cir.getReturnValue()));
   }

   @ModifyArg(
      method = {"extractRenderState"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
      ),
      index = 1
   )
   private Component pizzadlc$streamerTabNameDraw(Component name) {
      return StreamerMode.replace(name);
   }
}
