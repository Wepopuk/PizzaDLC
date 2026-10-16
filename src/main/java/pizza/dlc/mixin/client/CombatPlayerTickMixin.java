package pizza.dlc.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.combat.CombatRuntime;

@Mixin(value = LocalPlayer.class, remap = false)
public class CombatPlayerTickMixin {
    @Inject(remap = false, method = "tick", at = @At(remap = false, value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V"))
    private void pizzadlc$preTick(CallbackInfo ci) { CombatRuntime.preTick(); }

    @Inject(remap = false, method = "tick", at = @At(remap = false, value = "TAIL"))
    private void pizzadlc$postTick(CallbackInfo ci) { CombatRuntime.postTick(); }
}
