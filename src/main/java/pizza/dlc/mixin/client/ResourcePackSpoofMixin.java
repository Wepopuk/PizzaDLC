package pizza.dlc.mixin.client;

import java.util.UUID;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.misc.SRPSpoofer;

@Mixin({ClientCommonPacketListenerImpl.class})
public abstract class ResourcePackSpoofMixin {
   @Shadow
   public abstract void send(Packet<?> packet);

   @Inject(
      method = {"handleResourcePackPush"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$spoofResourcePack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
      if (SRPSpoofer.INSTANCE != null && SRPSpoofer.INSTANCE.isEnabled()) {
         UUID id = packet.id();
         this.send(new ServerboundResourcePackPacket(id, Action.ACCEPTED));
         this.send(new ServerboundResourcePackPacket(id, Action.DOWNLOADED));
         this.send(new ServerboundResourcePackPacket(id, Action.SUCCESSFULLY_LOADED));
         ci.cancel();
      }
   }
}
