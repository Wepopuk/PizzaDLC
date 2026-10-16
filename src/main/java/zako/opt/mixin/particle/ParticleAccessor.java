package zako.opt.mixin.particle;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={Particle.class})
public interface ParticleAccessor {
    @Accessor(value="x")
    public double zakoopt$x();

    @Accessor(value="y")
    public double zakoopt$y();

    @Accessor(value="z")
    public double zakoopt$z();
}

