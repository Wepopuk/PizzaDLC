package pizza.dlc.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LocalPlayer.class})
public interface LocalPlayerSprintAccessor {
   @Invoker("canStartSprinting")
   boolean pizzadlc$canStartSprinting();
}
