package zako.opt.entity;

import com.google.common.collect.MapMaker;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3fc;
import zako.opt.ZakoOptConfig;

public final class ItemLod {
    private static final float EDGE = 0.0626f;
    private static final Map<BakedQuad, Flat> FLAT = new MapMaker().weakKeys().makeMap();

    private ItemLod() {
    }

    public static boolean enabled() {
        return ZakoOptConfig.itemLod();
    }

    public static float distanceSq() {
        float d = ZakoOptConfig.itemLodDistance();
        return d * d;
    }

    public static List<BakedQuad> flat(List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return quads;
        }
        Flat cached = FLAT.get(quads.getFirst());
        if (cached == null || cached.sourceSize != quads.size()) {
            cached = new Flat(quads.size(), ItemLod.compute(quads));
            FLAT.put(quads.getFirst(), cached);
        }
        return cached.quads == null ? quads : cached.quads;
    }

    private static List<BakedQuad> compute(List<BakedQuad> quads) {
        if (quads.size() < 8) {
            return null;
        }
        for (BakedQuad q2 : quads) {
            if (q2.direction().getAxis() == Direction.Axis.Z || ItemLod.thin(q2)) continue;
            return null;
        }
        List<BakedQuad> faces = List.copyOf(quads.stream().filter(q -> q.direction().getAxis() == Direction.Axis.Z).toList());
        return faces.isEmpty() ? null : faces;
    }

    private static boolean thin(BakedQuad q) {
        float minZ = Float.MAX_VALUE;
        float maxZ = -3.4028235E38f;
        for (int i = 0; i < 4; ++i) {
            Vector3fc p = q.position(i);
            minZ = Math.min(minZ, p.z());
            maxZ = Math.max(maxZ, p.z());
        }
        return maxZ - minZ <= 0.0626f;
    }

    private record Flat(int sourceSize, List<BakedQuad> quads) {
    }
}

