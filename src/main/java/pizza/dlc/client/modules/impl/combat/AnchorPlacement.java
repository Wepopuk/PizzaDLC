package pizza.dlc.client.modules.impl.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Terrain exclusions shared by every Anchor Aura placement path. */
public final class AnchorPlacement {
   private AnchorPlacement() {}

   public static boolean allowed(Level world, BlockPos pos) {
      if (world.getBlockState(pos.below()).is(Blocks.OBSIDIAN)) return false;
      if (CrystalAura.isReservedForExcavation(pos) || CrystalAura.isPlannedAnchorConflict(pos)) return false;
      int sides=0;
      for (Direction direction:Direction.Plane.HORIZONTAL) {
         BlockPos side=pos.relative(direction);
         if (!world.getBlockState(side).getCollisionShape(world,side).isEmpty()) sides++;
      }
      // An open one-block pit or a covered excavation/tunnel must stay empty.
      if (sides>=3) return false;
      if (sides>=2) {
         for (int y=1;y<=2;y++) {
            BlockPos roof=pos.above(y);
            if (!world.getBlockState(roof).getCollisionShape(world,roof).isEmpty()) return false;
         }
      }
      return true;
   }
   public static boolean supportAllowed(Level world,BlockPos support) {
      return !world.getBlockState(support).is(Blocks.OBSIDIAN);
   }
}
