package pizza.dlc.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.entity.LivingEntity;
@Mixin(value = LivingEntity.class, remap = false)
public interface LivingEntityGravityAccessor { @Invoker(value = "getEffectiveGravity", remap = false) double pizzadlc$getEffectiveGravity(); }