package pizza.dlc.mixin.client;

import java.util.Optional;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientboundExplodePacket.class})
public interface ExplodePacketAccessor {
   @Mutable
   @Accessor("playerKnockback")
   void pizzadlc$setPlayerKnockback(Optional<Vec3> knockback);
}
