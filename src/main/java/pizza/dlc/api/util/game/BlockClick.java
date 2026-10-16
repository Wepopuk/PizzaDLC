package pizza.dlc.api.util.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DragonEggBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockClick {
   private BlockClick() {
   }

   public static boolean isInteractable(Level level, BlockPos pos, BlockState state) {
      if (state.getMenuProvider(level, pos) != null) return true;
      Block b = state.getBlock();
      return b instanceof DoorBlock
         || b instanceof TrapDoorBlock
         || b instanceof FenceGateBlock
         || b instanceof ButtonBlock
         || b instanceof LeverBlock
         || b instanceof NoteBlock
         || b instanceof DiodeBlock
         || b instanceof DaylightDetectorBlock
         || b instanceof RedStoneWireBlock
         || b instanceof BedBlock
         || b instanceof RespawnAnchorBlock
         || b instanceof CakeBlock
         || b instanceof BellBlock
         || b instanceof JukeboxBlock
         || b instanceof LecternBlock
         || b instanceof EnderChestBlock
         || b instanceof SignBlock
         || b instanceof DecoratedPotBlock
         || b instanceof ChiseledBookShelfBlock
         || b instanceof ShelfBlock
         || b instanceof FlowerPotBlock
         || b instanceof ComposterBlock
         || b instanceof AbstractCauldronBlock
         || b instanceof BeehiveBlock
         || b instanceof CampfireBlock
         || b instanceof AbstractCandleBlock
         || b instanceof DragonEggBlock
         || b instanceof CopperGolemStatueBlock
         || b instanceof VaultBlock
         || b instanceof TrialSpawnerBlock
         || b instanceof SpawnerBlock
         || b instanceof CommandBlock
         || b instanceof StructureBlock
         || b instanceof JigsawBlock;
   }
}
