package pizza.dlc.mixin.client;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.client.modules.impl.player.NoPush;

@Mixin({Player.class})
public abstract class PlayerNoPushMixin implements QuickImports {
   @Inject(
      method = {"isPushedByFluid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$noPushFluids(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player && NoPush.noFluids()) {
         cir.setReturnValue(false);
      }
   }
}
