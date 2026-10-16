package zako.opt.block;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.BaseSpawner;
import zako.opt.ZakoOptConfig;
import zako.opt.block.TickCached;

public final class SpawnerCache {
    private SpawnerCache() {
    }

    public static boolean enabled() {
        return ZakoOptConfig.spawnerTickCache();
    }

    public static EntityRenderState get(BaseSpawner spawner, long tick) {
        TickCached c = (TickCached)spawner;
        return c.zakoopt$cachedTick() == tick ? (EntityRenderState)c.zakoopt$cached() : null;
    }

    public static void put(BaseSpawner spawner, long tick, EntityRenderState state) {
        ((TickCached)spawner).zakoopt$setCached(state, tick);
    }
}

