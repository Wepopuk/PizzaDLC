package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pizza.dlc.api.util.combat.CombatRuntime;

@Mixin(value = LivingEntity.class, remap = false)
public class CombatGlidingMixin {
    @ModifyReturnValue(remap = false, method = "updateFallFlyingMovement", at = @At(remap = false, value = "RETURN"))
    private Vec3 pizzadlc$glidingVelocity(Vec3 velocity) {
        return (Object) this == Minecraft.getInstance().player ? CombatRuntime.glidingVelocity(velocity) : velocity;
    }
}
