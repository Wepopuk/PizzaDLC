package zako.opt.mixin.hud;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.render.GuiItemAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.hud.MonitorRate;

// Ported for 26.2: animated GUI items are re-drawn into GuiItemAtlas (was ItemRenderer prepare lambda in 1.21.11)
@Mixin({GuiItemAtlas.class})
public class GuiItemAtlasMixin {
    @ModifyExpressionValue(
        method = {"getOrUpdate"},
        at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;isAnimated()Z")}
    )
    private boolean zakoopt$throttleAnimated(boolean animated) {
        return animated && MonitorRate.refreshAnimated;
    }
}
