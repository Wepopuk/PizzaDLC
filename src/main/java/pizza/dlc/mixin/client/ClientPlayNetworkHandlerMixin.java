package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.events.impl.game.EventMessage;
import pizza.dlc.api.util.game.LootGuard;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.modules.impl.player.NoFall;

@Mixin({ClientPacketListener.class})
public class ClientPlayNetworkHandlerMixin {
   @Inject(
      method = {"sendChat"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onSendChat(String content, CallbackInfo ci) {
      if (StreamProofRenderer.handleWindowCommand(content)) {
         ci.cancel();
         return;
      }
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      EventMessage event = new EventMessage(content).call();
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleMovePlayer"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$afterTeleport(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
      NoFall.onTeleportApplied();
   }

   @Inject(
      method = {"handleSetEntityMotion"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$afterMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
      NoFall.onMotionApplied(packet.id());
   }

   @Inject(
      method = {"handleExplosion"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$afterExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
      LootGuard.onExplosion(Minecraft.getInstance().level, packet.center(), packet.radius());
   }
}
