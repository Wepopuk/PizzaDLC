package zako.opt.mixin.particle;

import java.util.Map;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={QuadParticleRenderState.class})
public interface QuadParticleRenderStateAccessor {
    @Accessor(value="particles")
    public Map<SingleQuadParticle.Layer, Object> zakoopt$particles();

    @Accessor(value="particleCount")
    public int zakoopt$particleCount();

    @Accessor(value="particleCount")
    public void zakoopt$setParticleCount(int var1);
}

