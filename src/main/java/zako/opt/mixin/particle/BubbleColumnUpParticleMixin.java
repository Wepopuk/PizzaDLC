package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

@Mixin(value={BubbleColumnUpParticle.class})
public class BubbleColumnUpParticleMixin {
    @Unique
    private static final Long2ObjectOpenHashMap<FluidState> zakoopt$fluids = new Long2ObjectOpenHashMap();
    @Unique
    private static long zakoopt$tick = Long.MIN_VALUE;

    @WrapOperation(method={"tick"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;")})
    private FluidState zakoopt$cachedFluid(ClientLevel level, BlockPos pos, Operation<FluidState> original) {
        long key;
        FluidState fluid;
        if (!ZakoOptConfig.microOpts()) {
            return (FluidState)original.call(new Object[]{level, pos});
        }
        long tick = level.getGameTime();
        if (tick != zakoopt$tick) {
            zakoopt$fluids.clear();
            zakoopt$tick = tick;
        }
        if ((fluid = (FluidState)zakoopt$fluids.get(key = pos.asLong())) == null) {
            fluid = (FluidState)original.call(new Object[]{level, pos});
            zakoopt$fluids.put(key, fluid);
        }
        return fluid;
    }
}

