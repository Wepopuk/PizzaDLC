package zako.opt.mixin.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;
import zako.opt.block.CachedValidity;

@Mixin(value={BlockEntityRenderDispatcher.class})
public class BlockEntityRenderDispatcherMixin {
    @WrapOperation(method={"tryExtractRenderState"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/block/entity/BlockEntityType;isValid(Lnet/minecraft/world/level/block/state/BlockState;)Z")})
    private boolean zakoopt$cachedValid(BlockEntityType<?> type, BlockState state, Operation<Boolean> original, BlockEntity blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        if (!ZakoOptConfig.signCache()) {
            return (Boolean)original.call(new Object[]{type, state});
        }
        CachedValidity cache = (CachedValidity)blockEntity;
        if (cache.zakoopt$validState() != state) {
            cache.zakoopt$setValid(state, (Boolean)original.call(new Object[]{type, state}));
        }
        return cache.zakoopt$valid();
    }
}

