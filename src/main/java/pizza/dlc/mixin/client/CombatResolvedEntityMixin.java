package pizza.dlc.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.interfaces.ResolvedRotationAccess;

@Mixin(value = ClientPacketListener.class, remap = false)
public class CombatResolvedEntityMixin {
    @Shadow private ClientLevel level;

    @Inject(remap = false, method = "handleMoveEntity", at = @At(remap = false, value = "TAIL"))
    private void pizzadlc$resolve(ClientboundMoveEntityPacket packet, CallbackInfo ci) {
        Entity entity = packet.getEntity(level);
        if (entity == null) return;
        var interpolation = entity.getInterpolation();
        var resolved = (ResolvedRotationAccess)entity;
        resolved.setResolvedYaw(packet.hasRotation() ? packet.getYRot() : interpolation != null ? interpolation.yRot() : entity.getYRot());
        resolved.setResolvedPitch(packet.hasRotation() ? packet.getXRot() : interpolation != null ? interpolation.xRot() : entity.getXRot());
        resolved.setResolvedPos(interpolation != null ? interpolation.position() : entity.position());
    }

    @Inject(remap = false, method = "handleEntityPositionSync", at = @At(remap = false, value = "TAIL"))
    private void pizzadlc$resolvePosition(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
        Entity entity = level.getEntity(packet.id());
        if (entity == null) return;
        var resolved = (ResolvedRotationAccess)entity;
        resolved.setResolvedYaw(packet.values().yRot());
        resolved.setResolvedPitch(packet.values().xRot());
        resolved.setResolvedPos(packet.values().position());
    }
}
