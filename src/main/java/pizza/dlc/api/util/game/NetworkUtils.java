package pizza.dlc.api.util.game;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

/** Sends an explicitly selected packet without posting the client's packet event. */
public final class NetworkUtils {
    private static final ThreadLocal<Packet<?>> SILENT_PACKET = new ThreadLocal<>();

    private NetworkUtils() {
    }

    public static boolean isSilentPacket(Packet<?> packet) {
        return SILENT_PACKET.get() == packet;
    }

    public static void sendSilentPacket(Connection connection, Packet<?> packet) {
        if (!connection.isConnected()) return;

        Packet<?> previous = SILENT_PACKET.get();
        SILENT_PACKET.set(packet);
        try {
            connection.send(packet);
        } finally {
            if (previous == null) {
                SILENT_PACKET.remove();
            } else {
                SILENT_PACKET.set(previous);
            }
        }
    }
}