package pizza.dlc.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardStreamProofAccessor {
   @Invoker("keyPress") void pizzadlc$keyPress(long handle, int action, KeyEvent event);
   @Invoker("charTyped") void pizzadlc$charTyped(long handle, CharacterEvent event);
}
