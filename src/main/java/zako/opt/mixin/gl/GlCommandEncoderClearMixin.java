package zako.opt.mixin.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import zako.opt.ZakoOptConfig;

@Mixin(targets = {"com.mojang.blaze3d.opengl.GlCommandEncoder"})
public class GlCommandEncoderClearMixin {
    @WrapOperation(
        method = {"clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V"},
        at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_glBindFramebuffer(II)V")}
    )
    private void zakoopt$keepBound(int target, int framebuffer, Operation<Void> original) {
        if (framebuffer != 0 || !ZakoOptConfig.lazyClear()) {
            original.call(target, framebuffer);
        }
    }
}
