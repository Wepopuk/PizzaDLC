package zako.opt.mixin.hud;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;
import zako.opt.hud.MonitorRate;

@Mixin({GuiRenderer.class})
public class GuiRendererMixin {
    @Unique
    private long zakoopt$lastAnimatedRefresh;

    @Inject(method = {"prepareItemElements"}, at = {@At("HEAD")})
    private void zakoopt$decideRefresh(CallbackInfo ci) {
        long now = System.nanoTime();
        boolean refresh = !ZakoOptConfig.guiAnimatedItems() || now - this.zakoopt$lastAnimatedRefresh >= MonitorRate.intervalNs(now);
        if (refresh) {
            this.zakoopt$lastAnimatedRefresh = now;
        }

        MonitorRate.refreshAnimated = refresh;
    }
}
