package pizza.dlc.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   targets = {"net.minecraft.world.item.ItemCooldowns$CooldownInstance"}
)
public interface ItemCooldownInstanceAccessor {
   @Accessor("startTime")
   int pizzadlc$getStartTime();

   @Accessor("endTime")
   int pizzadlc$getEndTime();
}
