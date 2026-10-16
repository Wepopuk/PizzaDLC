package pizza.dlc.mixin.client;

import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LanguageSelectScreen.class)
public interface LanguageSelectScreenAccessor {
   @Invoker("onDone")
   void pizzadlc$onDone();
}
