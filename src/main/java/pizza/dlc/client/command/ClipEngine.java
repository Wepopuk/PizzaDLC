package pizza.dlc.client.command;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.interfaces.QuickImports;

public class ClipEngine implements QuickImports {
   public static final ClipEngine INSTANCE = new ClipEngine();
   private static final double STEP = 9.0;

   public void clip(double dx, double dy, double dz, double blocks) {
      if (mc.player == null || blocks <= 1.0E-6) return;

      LocalPlayer p = mc.player;
      double x = p.getX();
      double y = p.getY();
      double z = p.getZ();
      double remaining = blocks;

      while (remaining > 1.0E-6) {
         double step = Math.min(remaining, STEP);
         x += dx * step;
         y += dy * step;
         z += dz * step;
         remaining -= step;
         p.connection.send(new Pos(x, y, z, false, false));
      }

      p.setPos(x, y, z);
      p.setDeltaMovement(Vec3.ZERO);
      p.fallDistance = 0.0;
   }
}
