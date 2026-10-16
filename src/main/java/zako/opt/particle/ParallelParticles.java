package zako.opt.particle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import zako.opt.ZakoOptConfig;
import zako.opt.mixin.particle.ParticleAccessor;
import zako.opt.mixin.particle.QuadParticleRenderStateAccessor;
import zako.opt.mixin.particle.QuadParticleStorageInvoker;
import zako.opt.particle.ParticleLod;
import zako.opt.particle.Workers;

public final class ParallelParticles {
    private static final int MIN_PARTICLES = 1024;
    private static final int BLOCK = 256;
    private static final List<QuadParticleRenderState> LOCAL = new ArrayList<QuadParticleRenderState>();
    private static SingleQuadParticle[] scratch = new SingleQuadParticle[0];

    private ParallelParticles() {
    }

    private static boolean enabled() {
        return ZakoOptConfig.parallelParticles();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean extract(Queue<SingleQuadParticle> particles, QuadParticleRenderState dest, Frustum frustum, Camera camera, float partialTick) {
        Throwable error;
        int n = particles.size();
        if (n < MIN_PARTICLES || !ParallelParticles.enabled()) {
            return false;
        }
        SingleQuadParticle[] arr = particles.toArray(scratch);
        scratch = arr;
        int blocks = (n + BLOCK - 1) / BLOCK;
        while (LOCAL.size() < blocks) {
            LOCAL.add(new QuadParticleRenderState());
        }
        Vec3 cam = camera.position();
        try {
            error = Workers.run(blocks, b -> ParallelParticles.extractRange(arr, b * BLOCK, Math.min(n, (b + 1) * BLOCK), LOCAL.get(b), frustum, camera, cam, partialTick));
        }
        finally {
            Arrays.fill(arr, 0, n, null);
        }
        if (error != null) {
            throw new ReportedException(CrashReport.forThrowable((Throwable)error, (String)"Rendering Particle (zakoopt parallel)"));
        }
        for (int b = 0; b < blocks; ++b) {
            QuadParticleRenderState local = LOCAL.get(b);
            ParallelParticles.merge(local, dest);
            local.clear();
        }
        return true;
    }

    private static void extractRange(SingleQuadParticle[] arr, int from, int to, QuadParticleRenderState out, Frustum frustum, Camera camera, Vec3 cam, float partialTick) {
        for (int i = from; i < to; ++i) {
            double z;
            double y;
            SingleQuadParticle p = arr[i];
            ParticleAccessor pos = (ParticleAccessor)p;
            double x = pos.zakoopt$x();
            if (!frustum.pointInFrustum(x, y = pos.zakoopt$y(), z = pos.zakoopt$z()) || !ParticleLod.keep(p, x, y, z, cam)) continue;
            p.extract(out, camera, partialTick);
        }
    }

    private static void merge(QuadParticleRenderState from, QuadParticleRenderState to) {
        QuadParticleRenderStateAccessor toAcc = (QuadParticleRenderStateAccessor)to;
        for (Map.Entry<SingleQuadParticle.Layer, Object> e : ((QuadParticleRenderStateAccessor)from).zakoopt$particles().entrySet()) {
            QuadParticleStorageInvoker src = (QuadParticleStorageInvoker)e.getValue();
            int n = src.zakoopt$count();
            if (n == 0) continue;
            Object existing = toAcc.zakoopt$particles().get(e.getKey());
            if (existing == null) {
                float[] f = src.zakoopt$floats();
                int[] in = src.zakoopt$ints();
                to.add(e.getKey(), f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7], f[8], f[9], f[10], f[11], in[0], in[1]);
                ParallelParticles.copy(src, 1, n - 1, (QuadParticleStorageInvoker)toAcc.zakoopt$particles().get(e.getKey()));
            } else {
                ParallelParticles.copy(src, 0, n, (QuadParticleStorageInvoker)existing);
            }
            toAcc.zakoopt$setParticleCount(toAcc.zakoopt$particleCount() + n - (existing == null ? 1 : 0));
        }
    }

    private static void copy(QuadParticleStorageInvoker src, int from, int n, QuadParticleStorageInvoker dst) {
        if (n <= 0) {
            return;
        }
        int at = dst.zakoopt$count();
        while (dst.zakoopt$capacity() < at + n) {
            dst.zakoopt$grow();
        }
        System.arraycopy(src.zakoopt$floats(), from * 12, dst.zakoopt$floats(), at * 12, n * 12);
        System.arraycopy(src.zakoopt$ints(), from * 2, dst.zakoopt$ints(), at * 2, n * 2);
        dst.zakoopt$setCount(at + n);
    }

}

