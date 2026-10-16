package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.util.game.DestroyHold;

@Mixin({Minecraft.class})
public class MinecraftClientMixin {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$onTick(CallbackInfo ci) {
      if (pizza.dlc.api.neuro.NeuroSystem.get()!=null) pizza.dlc.api.neuro.NeuroSystem.get().lifecycle();
      new EventTick().call();
   }

   @Inject(method="startAttack", at=@At("HEAD"))
   private void pizzadlc$manualAttack(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
      if (pizza.dlc.api.neuro.NeuroSystem.get()!=null) pizza.dlc.api.neuro.NeuroSystem.get().manualAttack();
   }

   @WrapOperation(
      method = {"continueAttack"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V"
      )
   )
   private void pizzadlc$keepModuleDestroy(MultiPlayerGameMode gameMode, Operation<Void> original) {
      if (DestroyHold.active()) return;

      original.call(gameMode);
   }
}
