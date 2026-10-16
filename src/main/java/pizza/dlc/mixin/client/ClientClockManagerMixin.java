package pizza.dlc.mixin.client;

import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.visuals.Ambience;

@Mixin({ClientClockManager.class})
public class ClientClockManagerMixin {
   @Inject(
      method = {"getTotalTicks(Lnet/minecraft/core/Holder;)J"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$time(Holder<WorldClock> definition, CallbackInfoReturnable<Long> cir) {
      if (Ambience.timeOn()) {
         cir.setReturnValue(Ambience.timeValue());
      }
   }
}
