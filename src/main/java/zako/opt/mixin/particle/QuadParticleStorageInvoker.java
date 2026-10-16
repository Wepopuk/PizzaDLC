package zako.opt.mixin.particle;

import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets={"net/minecraft/client/renderer/state/level/QuadParticleRenderState$Storage"})
public interface QuadParticleStorageInvoker {
    @Invoker(value="forEachParticle")
    public void zakoopt$forEach(QuadParticleRenderState.ParticleConsumer var1);

    @Invoker(value="grow")
    public void zakoopt$grow();

    @Accessor(value="capacity")
    public int zakoopt$capacity();

    @Accessor(value="floatValues")
    public float[] zakoopt$floats();

    @Accessor(value="intValues")
    public int[] zakoopt$ints();

    @Accessor(value="currentParticleIndex")
    public int zakoopt$count();

    @Accessor(value="currentParticleIndex")
    public void zakoopt$setCount(int var1);
}

