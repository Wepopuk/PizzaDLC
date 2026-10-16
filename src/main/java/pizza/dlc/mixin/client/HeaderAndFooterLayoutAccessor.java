package pizza.dlc.mixin.client;

import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HeaderAndFooterLayout.class)
public interface HeaderAndFooterLayoutAccessor {
   @Accessor("screen")
   Screen pizzadlc$getScreen();
}
