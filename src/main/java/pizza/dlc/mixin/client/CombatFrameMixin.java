package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.combat.CombatRuntime;

@Mixin(value = Minecraft.class, remap = false)
public class CombatFrameMixin {
    @Inject(remap = false, method = "runTick", at = @At(remap = false, value = "HEAD"))
    private void pizzadlc$frame(boolean render, CallbackInfo ci) { CombatRuntime.frame(); }
}
