package pizza.dlc.mixin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.util.game.NetworkUtils;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin({Connection.class})
public class ClientConnectionMixin {
   @Inject(
      method = {"channelRead0"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      PizzaDLC instance = PizzaDLC.getInstance();
      if (instance != null && instance.getBus() != null) {
         PacketEvent.Receive event = new PacketEvent.Receive(packet);
         instance.getBus().post(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"sendPacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onSend(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
      if (NetworkUtils.isSilentPacket(packet)) return;
      if (StreamProofRenderer.isVanillaGameplayActive()) return;
      PizzaDLC instance = PizzaDLC.getInstance();
      if (instance != null && instance.getBus() != null) {
         PacketEvent.Send event = new PacketEvent.Send(packet);
         instance.getBus().post(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
