package pizza.dlc.mixin.client;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundSetEntityMotionPacket.class})
public interface SetEntityMotionPacketAccessor {
   @Mutable
   @Accessor("movement")
   void pizzadlc$setMovement(Vec3 movement);
}
