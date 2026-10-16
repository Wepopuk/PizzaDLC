package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.game.EventClickSlot;
import pizza.dlc.api.util.game.SwapSystem;

@Mixin({MultiPlayerGameMode.class})
public class ClientPlayerInteractionManagerMixin {
   @Inject(
      method = {"handleContainerInput"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onClickSlot(int syncId, int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
      if (PizzaDLC.getInstance() != null) {
         if (!SwapSystem.isInternalClick() && SwapSystem.grimDefer(() -> {
            MultiPlayerGameMode gm = Minecraft.getInstance().gameMode;
            if (gm != null) {
               gm.handleContainerInput(syncId, slotId, button, input, player);
            }
         })) {
            ci.cancel();
         } else {
            EventClickSlot event = new EventClickSlot(input, slotId, button, syncId).call();
            if (event.isCancelled()) {
               ci.cancel();
            } else {
               SwapSystem.testMaskBefore();
            }
         }
      }
   }

   @Inject(
      method = {"handleContainerInput"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$onClickSlotAfter(int syncId, int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
      SwapSystem.testMaskAfter();
   }
}
