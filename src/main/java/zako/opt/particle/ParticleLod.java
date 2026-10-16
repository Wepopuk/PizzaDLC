package zako.opt.particle;

import net.minecraft.world.phys.Vec3;
import net.minecraft.client.particle.SingleQuadParticle;
import zako.opt.ZakoOptConfig;

public final class ParticleLod {
    private ParticleLod() {
    }

    public static boolean keep(SingleQuadParticle particle, double x, double y, double z, Vec3 cam) {
        if (!ZakoOptConfig.particleLod()) {
            return true;
        }
        double dx = x - cam.x;
        double dy = y - cam.y;
        double dz = z - cam.z;
        double d2 = dx * dx + dy * dy + dz * dz;
        if (d2 < 256.0) {
            return true;
        }
        int h = System.identityHashCode(particle) >>> 4;
        return d2 < 1024.0 ? (h & 1) == 0 : (h & 3) == 0;
    }
}

