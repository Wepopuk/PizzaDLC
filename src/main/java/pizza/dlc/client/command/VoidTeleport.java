package pizza.dlc.client.command;

import java.util.Locale;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.api.util.game.NetworkUtils;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

public final class VoidTeleport implements QuickImports {
    private static final VoidTeleport INSTANCE = new VoidTeleport();

    private static final double STEP = 9.5D;
    private static final int MAX_PACKETS = 20;
    private static final double MAX_CLIP = STEP * MAX_PACKETS;
    private static final int PAUSE_TICKS = 7;

    private volatile Stage stage = Stage.IDLE;
    private volatile boolean rolledBack;

    private LocalPlayer activePlayer;
    private ClientLevel activeLevel;
    private Vec3 position;
    private Vec3 target;
    private double voidY;

    private int chargeTicks;
    private int pauseTicks;

    private VoidTeleport() {
        PizzaDLC.getInstance().getBus().subscribe(this);
    }

    public static VoidTeleport get() {
        return INSTANCE;
    }

    public synchronized void start(LocalPlayer player, ClientLevel level, double x, double y, double z) {
        if (stage != Stage.IDLE) {
            ChatUtility.sendError("Телепорт уже выполняется. Для остановки: .tp cancel.");
            return;
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            ChatUtility.sendError("Координаты должны быть конечными числами.");
            return;
        }
        if (!player.connection.getConnection().isConnected() || !player.isAlive() || player.isPassenger()) {
            ChatUtility.sendError("Для телепорта нужно быть живым и выйти из транспорта.");
            return;
        }
        if (!player.getAbilities().mayfly) {
            ChatUtility.sendError("Нужен /fly.");
            return;
        }

        double underBedrock = level.getMinY()
                - player.getDimensions(Pose.STANDING).height()
                - 0.05D;

        if (player.getY() < underBedrock || y < underBedrock) {
            ChatUtility.sendError("Начальная и целевая Y должны быть выше точки клипа под бедроком.");
            return;
        }

        if (player.getY() - underBedrock > MAX_CLIP
                || y - underBedrock > MAX_CLIP) {

            ChatUtility.sendError(String.format(
                    Locale.ROOT,
                    "Слишком высоко: клип под бедрок работает до Y=%.0f.",
                    underBedrock + MAX_CLIP
            ));
            return;
        }

        if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        activePlayer = player;
        activeLevel = level;
        position = player.position();
        target = new Vec3(x, y, z);
        voidY = underBedrock;
        rolledBack = false;
        chargeTicks = packetsFor(position.y - voidY) + 2;
        pauseTicks = 0;
        stage = Stage.DESCEND;
        ChatUtility.send("Телепорт начат. Для остановки: .tp cancel.");
    }

    @EventHandler
    private synchronized void onTick(EventTick event) {
        if (rolledBack) {
            rolledBack = false;
            ChatUtility.sendError("Сервер откатил телепорт.");
        }

        if (stage == Stage.IDLE) {
            return;
        }

        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;

        if (player == null || level == null || player != activePlayer || level != activeLevel
                || !player.connection.getConnection().isConnected() || !player.isAlive()
                || StreamProofRenderer.isVanillaGameplayActive()) {
            reset();
            return;
        }

        if (!player.getAbilities().mayfly || player.isPassenger()) {
            reset();
            ChatUtility.sendError("Телепорт остановлен: нужен /fly и отсутствие транспорта.");
            return;
        }
        if (!player.getAbilities().flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        if (pauseTicks > 0) {
            pauseTicks--;

            player.setPos(position);
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0.0F;

            return;
        }

        switch (stage) {
            case DESCEND ->
                    clip(
                            new Vec3(position.x, voidY, position.z),
                            Stage.TRAVEL
                    );

            case TRAVEL ->
                    travel(level);

            case ASCEND ->
                    ascend(player, level);

            default -> {
            }
        }

        player.setPos(position);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
    }

    @EventHandler
    private synchronized void onReceive(PacketEvent.Receive event) {
        if (stage != Stage.IDLE && event.getPacket() instanceof ClientboundPlayerPositionPacket) {
            reset();
            rolledBack = true;
        }
    }

    @EventHandler(priority = -1000)
    private void onSend(PacketEvent.Send event) {
        if (stage != Stage.IDLE && mc.player == activePlayer && mc.level == activeLevel
                && (event.getPacket() instanceof ServerboundMovePlayerPacket
                || event.getPacket() instanceof ServerboundClientTickEndPacket)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private synchronized void onWorldLoad(EventWorldLoad event) {
        reset();
    }

    public synchronized void cancel() {
        boolean active = stage != Stage.IDLE;
        reset();
        ChatUtility.send(active ? "Телепорт остановлен." : "Нет активного телепорта.");
    }

    private void reset() {
        stage = Stage.IDLE;
        pauseTicks = 0;
        chargeTicks = 0;
        rolledBack = false;
        activePlayer = null;
        activeLevel = null;
    }

    private void travel(ClientLevel level) {
        double dx = target.x - position.x;
        double dz = target.z - position.z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 1.0E-3D) {
            stage = Stage.ASCEND;
            chargeTicks = -1;
            pauseTicks = PAUSE_TICKS;
            return;
        }

        double scale = Math.min(STEP, distance) / distance;

        Vec3 next = new Vec3(
                position.x + dx * scale,
                voidY,
                position.z + dz * scale
        );

        if (!level.hasChunk(
                SectionPos.blockToSectionCoord(next.x),
                SectionPos.blockToSectionCoord(next.z)
        )) {
            return;
        }

        send(next);
        position = next;
    }

    private void ascend(LocalPlayer player, ClientLevel level) {
        if (chargeTicks < 0) {
            Double freeY = findFreeY(player, level);

            if (freeY == null) {
                reset();
                ChatUtility.sendError(
                        "Над целью нет свободного места, укажите другую Y."
                );
                return;
            }

            target = new Vec3(
                    target.x,
                    freeY,
                    target.z
            );

            chargeTicks = packetsFor(freeY - voidY) + 2;
        }

        clip(target, Stage.IDLE);

        if (stage == Stage.IDLE) {
            reset();
            ChatUtility.send(String.format(
                    Locale.ROOT,
                    "Телепортирован на X=%.2f, Y=%.2f, Z=%.2f.",
                    target.x,
                    target.y,
                    target.z
            ));
        }
    }

    private void clip(Vec3 destination, Stage next) {
        if (chargeTicks-- > 0) {
            return;
        }

        int packets = packetsFor(
                Math.abs(destination.y - position.y)
        );

        for (int i = 1; i < packets; i++) {
            send(position);
        }

        send(destination);

        position = destination;
        stage = next;

        if (next != Stage.IDLE) {
            pauseTicks = PAUSE_TICKS;
        }
    }

    private Double findFreeY(LocalPlayer player, ClientLevel level) {
        for (
                double y = target.y;
                y <= voidY + MAX_CLIP;
                y = Math.floor(y) + 1.0D
        ) {
            Vec3 candidate = new Vec3(
                    target.x,
                    y,
                    target.z
            );

            if (level.noCollision(
                    player,
                    player.getDimensions(Pose.STANDING)
                            .makeBoundingBox(candidate)
            )) {
                return y;
            }
        }

        return null;
    }

    private static int packetsFor(double distance) {
        return Math.max(
                1,
                (int) Math.ceil(distance / STEP)
        );
    }

    private void send(Vec3 pos) {
        NetworkUtils.sendSilentPacket(activePlayer.connection.getConnection(),
                new ServerboundMovePlayerPacket.Pos(
                        pos.x,
                        pos.y,
                        pos.z,
                        false,
                        false
                )
        );

        NetworkUtils.sendSilentPacket(activePlayer.connection.getConnection(),
                ServerboundClientTickEndPacket.INSTANCE
        );
    }

    private enum Stage {
        IDLE,
        DESCEND,
        TRAVEL,
        ASCEND
    }
}