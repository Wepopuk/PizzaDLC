package zako.opt.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.particle.ParallelParticles;
import zako.opt.particle.ParticleLod;

@Mixin(value={QuadParticleGroup.class})
public abstract class QuadParticleGroupMixin
extends ParticleGroup<SingleQuadParticle> {
    @Shadow
    @Final
    QuadParticleRenderState particleTypeRenderState;

    private QuadParticleGroupMixin(ParticleEngine engine) {
        super(engine);
    }

    @Inject(method={"extractRenderState"}, at={@At(value="HEAD")}, cancellable=true)
    private void zakoopt$parallel(Frustum frustum, Camera camera, float partialTick, CallbackInfoReturnable<ParticleGroupRenderState> cir) {
        if (ParallelParticles.extract(this.particles, this.particleTypeRenderState, frustum, camera, partialTick)) {
            cir.setReturnValue(this.particleTypeRenderState);
        }
    }

    @WrapOperation(method={"extractRenderState"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/culling/Frustum;pointInFrustum(DDD)Z")})
    private boolean zakoopt$lod(Frustum frustum, double x, double y, double z, Operation<Boolean> original, Frustum frustumArg, Camera camera, float partialTick, @Local SingleQuadParticle particle) {
        return (Boolean)original.call(new Object[]{frustum, x, y, z}) != false && ParticleLod.keep(particle, x, y, z, camera.position());
    }
}

