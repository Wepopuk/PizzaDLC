package zako.opt.block;

import net.minecraft.world.level.block.state.BlockState;

public interface CachedValidity {
    public BlockState zakoopt$validState();

    public boolean zakoopt$valid();

    public void zakoopt$setValid(BlockState var1, boolean var2);
}

