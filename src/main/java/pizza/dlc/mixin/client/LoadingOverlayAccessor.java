package pizza.dlc.mixin.client;

import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LoadingOverlay.class)
public interface LoadingOverlayAccessor {
   @Accessor("fadeOutStart")
   long pizzadlc$getFadeOutStart();
}
