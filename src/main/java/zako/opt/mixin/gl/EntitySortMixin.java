package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

// Ported for 26.2: translucent sorting is decided in RenderTypeFeatureRenderer$Group (was BufferSource.endBatch in 1.21.11)
@Mixin(targets = {"net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer$Group"})
public class EntitySortMixin {
    @ModifyExpressionValue(
        method = {"getOrAddDraw"},
        at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;sortOnUpload()Z")}
    )
    private boolean zakoopt$skipSkinSort(boolean sort, RenderType renderType) {
        return sort && (!ZakoOptConfig.noEntitySort() || !"entity_translucent".equals(((RenderTypeAccessor)renderType).zakoopt$name()));
    }
}
