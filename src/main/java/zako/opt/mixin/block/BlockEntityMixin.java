package zako.opt.mixin.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import zako.opt.block.CachedLight;
import zako.opt.block.CachedValidity;
import zako.opt.block.TickCached;

@Mixin(value={BlockEntity.class})
public class BlockEntityMixin
implements TickCached,
CachedLight,
CachedValidity {
    @Unique
    private Object zakoopt$cached;
    @Unique
    private long zakoopt$cachedTick = Long.MIN_VALUE;
    @Unique
    private int zakoopt$light;
    @Unique
    private BlockState zakoopt$validState;
    @Unique
    private boolean zakoopt$valid;
    @Unique
    private long zakoopt$lightTick = Long.MIN_VALUE;
    @Unique
    private int zakoopt$pairLight;
    @Unique
    private long zakoopt$pairLightTick = Long.MIN_VALUE;

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

    @Override
    public int zakoopt$light() {
        return this.zakoopt$light;
    }

    @Override
    public long zakoopt$lightTick() {
        return this.zakoopt$lightTick;
    }

    @Override
    public void zakoopt$setLight(int light, long tick) {
        this.zakoopt$light = light;
        this.zakoopt$lightTick = tick;
    }

    @Override
    public int zakoopt$pairLight() {
        return this.zakoopt$pairLight;
    }

    @Override
    public long zakoopt$pairLightTick() {
        return this.zakoopt$pairLightTick;
    }

    @Override
    public void zakoopt$setPairLight(int light, long tick) {
        this.zakoopt$pairLight = light;
        this.zakoopt$pairLightTick = tick;
    }

    @Override
    public BlockState zakoopt$validState() {
        return this.zakoopt$validState;
    }

    @Override
    public boolean zakoopt$valid() {
        return this.zakoopt$valid;
    }

    @Override
    public void zakoopt$setValid(BlockState state, boolean valid) {
        this.zakoopt$validState = state;
        this.zakoopt$valid = valid;
    }
}

