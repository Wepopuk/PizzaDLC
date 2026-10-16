package pizza.dlc.api.events.impl.combat;

import lombok.Generated;
import net.minecraft.network.protocol.Packet;

public class CombatPacketEvent extends pizza.dlc.api.events.Event {
   private final Packet<?> packet;

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }

   @Generated
   public CombatPacketEvent(Packet<?> var1) {
      this.packet = var1;
   }
}
