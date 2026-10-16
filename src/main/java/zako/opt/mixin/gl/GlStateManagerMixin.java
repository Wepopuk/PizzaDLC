package zako.opt.mixin.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zako.opt.ZakoOptConfig;

@Mixin(value={GlStateManager.class})
public class GlStateManagerMixin {
    @Shadow
    private static int readFbo;
    @Shadow
    private static int writeFbo;

    @Inject(method={"_glBindFramebuffer"}, at={@At(value="HEAD")}, cancellable=true)
    private static void zakoopt$singleBind(int target, int fbo, CallbackInfo ci) {
        if (target == 36160 && readFbo != fbo && writeFbo != fbo && ZakoOptConfig.microOpts()) {
            GL30.glBindFramebuffer((int)36160, (int)fbo);
            readFbo = fbo;
            writeFbo = fbo;
            ci.cancel();
        }
    }
}

