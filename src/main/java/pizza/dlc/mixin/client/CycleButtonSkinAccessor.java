package pizza.dlc.mixin.client;

import java.util.function.Function;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CycleButton.class)
public interface CycleButtonSkinAccessor {
   @Accessor("name") Component pizzadlc$getOptionName();
   @Accessor("valueStringifier") Function<Object, Component> pizzadlc$getValueStringifier();
}
