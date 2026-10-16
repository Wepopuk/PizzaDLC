package zako.opt.mixin.block;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.block.TickCached;

@Mixin(value={BaseSpawner.class})
public class BaseSpawnerMixin
implements TickCached {
    @Unique
    private Object zakoopt$cached;
    @Unique
    private long zakoopt$cachedTick = Long.MIN_VALUE;

    @Override
    public Object zakoopt$cached() {
        return this.zakoopt$cached;
    }

    @Override
    public long zakoopt$cachedTick() {
        return this.zakoopt$cachedTick;
    }

    @Override
    public void zakoopt$setCached(Object value, long tick) {
        this.zakoopt$cached = value;
        this.zakoopt$cachedTick = tick;
    }
}

