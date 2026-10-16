package pizza.dlc.api.util.combat;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.network.protocol.Packet;

public final class CombatPackets implements pizza.dlc.api.interfaces.QuickImports {
   private static final List<Packet<?>> f_1398 = new ArrayList<>();

   @Generated
   private CombatPackets() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void m_1293(Packet<?> var0) {
      f_1398.add(var0);
      mc.getConnection().send(var0);
   }

   @Generated
   public static List<Packet<?>> m_1294() {
      return f_1398;
   }

   public static void m_1295(Packet<?> var0) {
      mc.getConnection().send(var0);
   }
}
