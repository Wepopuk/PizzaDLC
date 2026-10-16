package zako.opt.mixin.particle;

import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zako.opt.ZakoOptConfig;

@Mixin(value={Particle.class})
public class ParticleMixin {
    @Shadow
    @Final
    protected ClientLevel level;
    @Shadow
    protected boolean hasPhysics;
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;
    @Unique
    private boolean zakoopt$physicsBefore;
    @Unique
    private long zakoopt$lightTick = Long.MIN_VALUE;
    @Unique
    private int zakoopt$light;

    @Inject(method={"getLightCoords"}, at={@At(value="HEAD")}, cancellable=true)
    private void zakoopt$cachedLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (this.zakoopt$lightTick == this.level.getGameTime() && ZakoOptConfig.particleLight()) {
            cir.setReturnValue(this.zakoopt$light);
        }
    }

    @Inject(method={"getLightCoords"}, at={@At(value="RETURN")})
    private void zakoopt$storeLight(float partialTick, CallbackInfoReturnable<Integer> cir) {
        this.zakoopt$light = cir.getReturnValueI();
        this.zakoopt$lightTick = this.level.getGameTime();
    }

    @Inject(method={"move"}, at={@At(value="HEAD")})
    private void zakoopt$cheapPhysics(double dx, double dy, double dz, CallbackInfo ci) {
        this.zakoopt$physicsBefore = this.hasPhysics;
        if (this.hasPhysics && ZakoOptConfig.particlePhysics() && this.zakoopt$skipCollision()) {
            this.hasPhysics = false;
        }
    }

    @Inject(method={"move"}, at={@At(value="RETURN")})
    private void zakoopt$restorePhysics(double dx, double dy, double dz, CallbackInfo ci) {
        this.hasPhysics = this.zakoopt$physicsBefore;
    }

    @Unique
    private boolean zakoopt$skipCollision() {
        Object self = this;
        if (self instanceof BaseAshSmokeParticle || self instanceof CampfireSmokeParticle || self instanceof BubbleColumnUpParticle) {
            return true;
        }
        Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
        double ddx = this.x - cam.x;
        double ddy = this.y - cam.y;
        double ddz = this.z - cam.z;
        return ddx * ddx + ddy * ddy + ddz * ddz > 256.0;
    }
}

