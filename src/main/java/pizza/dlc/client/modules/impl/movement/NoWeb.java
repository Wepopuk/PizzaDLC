package pizza.dlc.client.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "NoWeb",
   category = ModuleCategory.MOVEMENT,
   description = "Убирает замедление в паутине"
)
public class NoWeb extends Module {
   private static final String FIXED = "Фиксированный";
   private static final String CUSTOM = "Кастомный";
   private static final String IGNORE = "Игнор";
   private static final double FIXED_HORIZONTAL = 0.19175;
   private static final double FIXED_VERTICAL = 0.995;
   private static NoWeb instance;
   private final ModeSetting mode = new ModeSetting("Режим", this, FIXED, CUSTOM, IGNORE);
   private final SliderSetting customHorizontalSpeed = new SliderSetting("Горизонтальная скорость", this, 0.19175F, 0.01F, 1.5F, 0.001F);
   private final SliderSetting customVerticalSpeed = new SliderSetting("Вертикальная скорость", this, 0.995F, 0.01F, 1.5F, 0.001F);
   private final BooleanSetting grim = new BooleanSetting("Grim", this);
   private final BooleanSetting jumpBypass = new BooleanSetting("JumpBypass", this);
   private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
   private boolean sprintBlocked;

   public NoWeb() {
      instance = this;
      this.customHorizontalSpeed.setVisible(() -> this.mode.is(CUSTOM));
      this.customVerticalSpeed.setVisible(() -> this.mode.is(CUSTOM));
      this.grim.setVisible(() -> this.mode.is(IGNORE));
      this.jumpBypass.setVisible(() -> this.mode.is(IGNORE));
   }

   public static NoWeb getInstance() {
      return instance;
   }

   public static boolean isIgnoreActive() {
      return instance != null && instance.isEnabled() && instance.mode.is(IGNORE);
   }

   public static void onEntityCollideCobweb(BlockPos pos) {
      if (instance == null || !instance.isEnabled() || !instance.mode.is(IGNORE)) return;
      if (instance.grim.isEnabled() && mc.player != null && mc.level != null) {
         try (BlockStatePredictionHandler updates = mc.level.getBlockStatePredictionHandler().startPredicting()) {
            mc.player.connection.send(
               new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP, updates.currentSequence())
            );
         }
      }
   }

   @Override
   public void onDisable() {
      this.sprintBlocked = false;
      super.onDisable();
   }

   @EventHandler
   public void onTick(EventUpdate event) {
      if (mc.player == null || mc.level == null) return;

      if (this.mode.is(IGNORE)) {
         if (this.jumpBypass.isEnabled()) {
            if (this.isNearCobweb(2.0)) {
               if (mc.options.keyJump.isDown()) {
                  if (mc.player.isSprinting()) {
                     mc.player.setSprinting(false);
                     this.sprintBlocked = true;
                  }
               } else if (mc.player.onGround() && this.sprintBlocked) {
                  mc.player.setSprinting(true);
                  this.sprintBlocked = false;
               }
            } else if (this.sprintBlocked && mc.player.onGround()) {
               this.sprintBlocked = false;
            }
         }

         return;
      }

      if (!this.isInWeb()) return;

      mc.player.setDeltaMovement(0.0, 0.0, 0.0);

      double verticalSpeed = this.mode.is(CUSTOM) ? this.customVerticalSpeed.getValue() : FIXED_VERTICAL;
      double horizontalSpeed = this.mode.is(CUSTOM) ? this.customHorizontalSpeed.getValue() : FIXED_HORIZONTAL;

      double targetY = 0.0;
      if (mc.options.keyJump.isDown()) {
         targetY = verticalSpeed;
      } else if (mc.options.keyShift.isDown()) {
         targetY = -verticalSpeed;
      }

      mc.player.setDeltaMovement(0.0, targetY, 0.0);

      setSpeed(mc.player, horizontalSpeed);
   }

   private static void setSpeed(LocalPlayer player, double speed) {
      Vec2 move = player.input.getMoveVector();
      if (move.lengthSquared() < 1.0E-4F) {
         player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
         return;
      }

      float yaw = player.getYRot() * (float) (Math.PI / 180.0);
      double sin = Mth.sin(yaw);
      double cos = Mth.cos(yaw);
      double x = move.x * cos - move.y * sin;
      double z = move.y * cos + move.x * sin;
      double length = Math.sqrt(x * x + z * z);
      player.setDeltaMovement(x / length * speed, player.getDeltaMovement().y, z / length * speed);
   }

   private boolean isNearCobweb(double maxDistance) {
      if (mc.player == null || mc.level == null) return false;

      double playerX = mc.player.getX();
      double playerY = mc.player.getY();
      double playerZ = mc.player.getZ();
      int blockX = mc.player.getBlockX();
      int blockY = mc.player.getBlockY();
      int blockZ = mc.player.getBlockZ();
      double maxDistanceSq = maxDistance * maxDistance;

      for (int x = -2; x <= 2; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
               int cx = blockX + x;
               int cy = blockY + y;
               int cz = blockZ + z;
               this.mutablePos.set(cx, cy, cz);
               if (mc.level.getBlockState(this.mutablePos).getBlock() instanceof WebBlock) {
                  double dx = playerX - (cx + 0.5);
                  double dy = playerY - (cy + 0.5);
                  double dz = playerZ - (cz + 0.5);
                  if (dx * dx + dy * dy + dz * dz <= maxDistanceSq) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean isInWeb() {
      if (mc.player == null || mc.level == null) return false;

      LocalPlayer player = mc.player;
      AABB playerBox = player.getBoundingBox();
      BlockPos playerPosition = player.blockPosition();

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               BlockPos pos = playerPosition.offset(x, y, z);
               if (playerBox.intersects(new AABB(pos)) && mc.level.getBlockState(pos).is(Blocks.COBWEB)) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
