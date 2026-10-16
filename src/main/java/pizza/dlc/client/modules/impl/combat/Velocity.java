package pizza.dlc.client.modules.impl.combat;

import java.util.Optional;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.events.impl.game.EventMotion;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.input.EventTravel;
import pizza.dlc.api.notification.NotificationManager;
import pizza.dlc.api.notification.NotificationType;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.mixin.client.ExplodePacketAccessor;
import pizza.dlc.mixin.client.SetEntityMotionPacketAccessor;

@ModuleInfo(
   name = "Velocity",
   category = ModuleCategory.COMBAT
)
public class Velocity extends Module {
   private static final String GRIM = "Grim";
   private static final String SIMPLE = "Обычный";
   private static final String AIR_STUCK = "AirStuck";
   private static final int MAX_ROTATION_PACKETS = 12;
   private static final double REACH = 2.99;
   private static final double MIN_KNOCKBACK = 0.05;
   private static final double JUMP_FACING = -0.3;
   private static final int REDUCE_TICKS = 3;
   // Grim AntiKB / AntiExplosion lose this much VL per knockback taken honestly (check decay)
   private static final double KB_DECAY = 0.025;
   private static final double EXPLOSION_DECAY = 0.05;
   private static final int SIM_TICKS = 40;
   private static final int GROUND_SCAN = 64;
   private final ModeSetting mode = new ModeSetting("Режим", this, GRIM, SIMPLE, AIR_STUCK);
   private final SliderSetting freezeTicks = new SliderSetting("Заморозка, тиков", this, 8.0F, 1.0F, 40.0F, 1.0F);
   private final BooleanSetting freezeRotation = new BooleanSetting("Слать ротацию", this);
   private final BooleanSetting freezeExplosions = new BooleanSetting("Замораживать от взрывов", this);
   private final BooleanSetting jumpReset = new BooleanSetting(
      "Прыжок при отдаче", "Прыгает в тик отдачи, если смотришь на атакующего: прыжок на бегу гасит часть отдачи", this
   );
   private final BooleanSetting attackReduce = new BooleanSetting(
      "Удар при отдаче", "Бьёт атакующего со спринтом и полной полоской: ванильный удар на бегу режет скорость на 40%", this
   );
   private final SliderSetting horizontal = new SliderSetting("Горизонталь", this, 0.0F, 0.0F, 100.0F, 1.0F);
   private final SliderSetting vertical = new SliderSetting("Вертикаль", this, 0.0F, 0.0F, 100.0F, 1.0F);
   private final SliderSetting kbBudget = new SliderSetting("Лимит отмен", this, 4.0F, 0.0F, 30.0F, 1.0F);
   private final BooleanSetting dangerOnly = new BooleanSetting(
      "Только опасная отдача", "Тратит лимит только на удары, которые скинут с высоты, в лаву или в пустоту", this
   );
   private final SliderSetting dangerDrop = new SliderSetting("Опасная высота", this, 4.0F, 2.0F, 20.0F, 0.5F);
   private final BooleanSetting explosions = new BooleanSetting("Взрывы", this);
   private final SliderSetting explosionBudget = new SliderSetting("Лимит взрывов", this, 2.0F, 0.0F, 30.0F, 1.0F);
   private volatile int attackerId = -1;
   private double knockX;
   private double knockZ;
   private boolean knockFresh;
   private int reduceTicks;
   private boolean jumpNow;
   private ClientPacketListener session;
   private double kbViolations;
   private double explosionViolations;
   private boolean kbWarned;
   private boolean explosionWarned;
   private int frozenTicks;
   private int rotationPackets;

   public Velocity() {
      this.jumpReset.setEnabled(true);
      this.attackReduce.setEnabled(true);
      this.explosions.setEnabled(true);
      this.jumpReset.setVisible(() -> this.mode.is(GRIM));
      this.attackReduce.setVisible(() -> this.mode.is(GRIM));
      this.horizontal.setVisible(() -> this.mode.is(SIMPLE));
      this.vertical.setVisible(() -> this.mode.is(SIMPLE));
      this.kbBudget.setVisible(() -> this.mode.is(SIMPLE));
      this.dangerOnly.setVisible(() -> this.mode.is(SIMPLE));
      this.dangerDrop.setVisible(() -> this.mode.is(SIMPLE) && this.dangerOnly.isEnabled());
      this.explosions.setVisible(() -> this.mode.is(SIMPLE));
      this.explosionBudget.setVisible(() -> this.mode.is(SIMPLE) && this.explosions.isEnabled());
      this.freezeRotation.setEnabled(true);
      this.freezeExplosions.setEnabled(true);
      this.freezeTicks.setVisible(() -> this.mode.is(AIR_STUCK));
      this.freezeRotation.setVisible(() -> this.mode.is(AIR_STUCK));
      this.freezeExplosions.setVisible(() -> this.mode.is(AIR_STUCK));
   }

   @Override
   public void onDisable() {
      this.knockFresh = false;
      this.reduceTicks = 0;
      this.jumpNow = false;
      this.frozenTicks = 0;
      super.onDisable();
   }

   @EventHandler
   public void onReceive(PacketEvent.Receive e) {
      if (mc.player == null) return;

      if (this.mode.is(GRIM)) {
         this.watchKnockback(e);
         return;
      }

      if (this.mode.is(AIR_STUCK)) {
         this.freezeOnKnockback(e);
         return;
      }

      this.syncSession();
      float h = this.horizontal.getValue() / 100.0F;
      float v = this.vertical.getValue() / 100.0F;
      boolean reduces = h < 1.0F || v < 1.0F;
      if (e.getPacket() instanceof ClientboundSetEntityMotionPacket vel) {
         if (vel.id() != mc.player.getId() || !reduces) return;

         Vec3 m = vel.movement();
         if (!this.spend(m, true)) return;

         if (h == 0.0F && v == 0.0F) {
            e.setCancelled(true);
            return;
         }

         ((SetEntityMotionPacketAccessor)(Object)vel).pizzadlc$setMovement(new Vec3(m.x * h, m.y * v, m.z * h));
      } else if (e.getPacket() instanceof ClientboundExplodePacket exp) {
         Optional<Vec3> kb = exp.playerKnockback();
         if (!this.explosions.isEnabled() || !reduces || kb.isEmpty()) return;

         Vec3 k = kb.get();
         if (!this.spend(mc.player.getDeltaMovement().add(k), false)) return;

         if (h == 0.0F && v == 0.0F) {
            ((ExplodePacketAccessor)(Object)exp).pizzadlc$setPlayerKnockback(Optional.empty());
            return;
         }

         ((ExplodePacketAccessor)(Object)exp).pizzadlc$setPlayerKnockback(Optional.of(new Vec3(k.x * h, k.y * v, k.z * h)));
      }
   }

   // Grim keeps one AntiKB/AntiExplosion VL counter per connection: +1 per altered knockback, -decay per knockback taken honestly.
   private boolean spend(Vec3 motion, boolean knockback) {
      double budget = (knockback ? this.kbBudget : this.explosionBudget).getValue();
      double used = knockback ? this.kbViolations : this.explosionViolations;
      double decay = knockback ? KB_DECAY : EXPLOSION_DECAY;
      boolean worth = !this.dangerOnly.isEnabled() || this.dangerous(motion);
      boolean affordable = used + 1.0 <= budget;
      if (worth && affordable) {
         this.setViolations(knockback, used + 1.0);
         return true;
      }

      this.setViolations(knockback, Math.max(0.0, used - decay));
      if (worth && !affordable) this.warnOnce(knockback);
      return false;
   }

   private void setViolations(boolean knockback, double value) {
      if (knockback) {
         this.kbViolations = value;
      } else {
         this.explosionViolations = value;
      }
   }

   private void warnOnce(boolean knockback) {
      if (knockback ? this.kbWarned : this.explosionWarned) return;

      if (knockback) {
         this.kbWarned = true;
      } else {
         this.explosionWarned = true;
      }

      String what = knockback ? "отдачи" : "взрывов";
      NotificationManager.add("Velocity", "Лимит отмен " + what + " исчерпан — дальше отдача проходит честно", NotificationType.WARNING, 3000L);
   }

   private void syncSession() {
      ClientPacketListener connection = mc.getConnection();
      if (connection == this.session) return;

      this.session = connection;
      this.kbViolations = 0.0;
      this.explosionViolations = 0.0;
      this.kbWarned = false;
      this.explosionWarned = false;
   }

   // Follows the knockback flight and checks where it lands: a drop over the limit, lava or the void count as dangerous.
   private boolean dangerous(Vec3 motion) {
      LocalPlayer self = mc.player;
      Level level = self.level();
      double startY = self.getY();
      double x = self.getX();
      double y = startY;
      double z = self.getZ();
      double vx = motion.x;
      double vy = motion.y;
      double vz = motion.z;
      for (int tick = 0; tick < SIM_TICKS; tick++) {
         x += vx;
         y += vy;
         z += vz;
         vx *= 0.91;
         vz *= 0.91;
         vy = (vy - 0.08) * 0.98;
         if (vy > 0.0) continue;

         int bx = Mth.floor(x);
         int bz = Mth.floor(z);
         int top = Mth.floor(y);
         for (int by = top; by >= top - GROUND_SCAN; by--) {
            BlockPos pos = new BlockPos(bx, by, bz);
            BlockState state = level.getBlockState(pos);
            if (state.getFluidState().is(FluidTags.LAVA)) return true;
            if (state.getFluidState().is(FluidTags.WATER)) return false;
            if (state.getCollisionShape(level, pos).isEmpty()) continue;

            double ground = by + state.getCollisionShape(level, pos).max(net.minecraft.core.Direction.Axis.Y);
            if (y > ground + 0.01) break;
            return startY - ground > this.dangerDrop.getValue();
         }

         if (y < level.getMinY()) return true;
      }

      return startY - y > this.dangerDrop.getValue();
   }

   // Like Air Stuck: the knockback is dropped and the player hangs in place (no travel, rotation-only packets) for a few ticks.
   private void freezeOnKnockback(PacketEvent.Receive e) {
      if (e.getPacket() instanceof ClientboundSetEntityMotionPacket vel && vel.id() == mc.player.getId()) {
         e.setCancelled(true);
         mc.execute(this::startFreeze);
      } else if (e.getPacket() instanceof ClientboundExplodePacket exp && this.freezeExplosions.isEnabled() && exp.playerKnockback().isPresent()) {
         ((ExplodePacketAccessor)(Object)exp).pizzadlc$setPlayerKnockback(Optional.empty());
         mc.execute(this::startFreeze);
      }
   }

   private void startFreeze() {
      if (this.frozenTicks <= 0) this.rotationPackets = 0;
      this.frozenTicks = Math.round(this.freezeTicks.getValue());
   }

   @EventHandler
   public void onTravel(EventTravel e) {
      if (this.frozenTicks > 0 && this.mode.is(AIR_STUCK) && mc.player != null && e.getEntity() == mc.player) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void onMotion(EventMotion e) {
      if (this.frozenTicks <= 0 || !this.mode.is(AIR_STUCK) || mc.player == null) return;

      this.frozenTicks--;
      e.setCancelled(true);
      if (this.freezeRotation.isEnabled() && this.rotationPackets < MAX_ROTATION_PACKETS) {
         e.setRotationOnly(true);
         e.addPostAction(() -> this.rotationPackets++);
      }
   }

   // Grim mode only uses vanilla mechanics Grim simulates itself: sprint-attack slowdown (x0.6) and a sprint jump (+0.2 along the look).
   private void watchKnockback(PacketEvent.Receive e) {
      if (e.getPacket() instanceof ClientboundDamageEventPacket damage && damage.entityId() == mc.player.getId()) {
         this.attackerId = damage.sourceCauseId();
      } else if (e.getPacket() instanceof ClientboundSetEntityMotionPacket vel && vel.id() == mc.player.getId()) {
         Vec3 m = vel.movement();
         if (Math.hypot(m.x, m.z) < MIN_KNOCKBACK) return;

         mc.execute(() -> {
            this.knockX = m.x;
            this.knockZ = m.z;
            this.knockFresh = true;
            this.reduceTicks = REDUCE_TICKS;
         });
      }
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      LocalPlayer self = mc.player;
      this.jumpNow = false;
      if (self == null || mc.level == null || !this.mode.is(GRIM)) return;

      if (this.knockFresh) {
         this.knockFresh = false;
         this.jumpNow = this.jumpReset.isEnabled() && self.onGround() && self.isSprinting() && this.facingAttacker();
      }

      if (this.reduceTicks > 0) {
         this.reduceTicks--;
         if (this.attackReduce.isEnabled() && this.tryReduce(self)) {
            this.reduceTicks = 0;
         }
      }
   }

   @EventHandler
   public void onInputMove(EventInputMove e) {
      if (this.jumpNow) {
         e.setJump(true);
      }
   }

   private boolean facingAttacker() {
      float yaw = jumpYaw() * (float) (Math.PI / 180.0);
      double lookX = -Mth.sin(yaw);
      double lookZ = Mth.cos(yaw);
      double length = Math.hypot(this.knockX, this.knockZ);
      return (lookX * this.knockX + lookZ * this.knockZ) / length <= JUMP_FACING;
   }

   private static float jumpYaw() {
      RotationController c = RotationController.INSTANCE;
      if (c != null && c.isActive()) {
         RotationPlan plan = c.getCurrentRotationPlan();
         if (plan != null && plan.isMoveCorrection()) return c.getMoveRotation().getYaw();
      }

      return mc.player.getYRot();
   }

   private boolean tryReduce(LocalPlayer self) {
      if (!self.isSprinting() || !self.wasSprinting || self.isUsingItem() || self.getAttackStrengthScale(0.5F) <= 0.9F || mc.gameMode == null) return false;

      Entity attacker = mc.level.getEntity(this.attackerId);
      if (!(attacker instanceof LivingEntity living) || !living.isAlive() || attacker == self || !this.reachable(self, attacker)) return false;

      mc.gameMode.attack(self, attacker);
      self.swing(InteractionHand.MAIN_HAND);
      return true;
   }

   private boolean reachable(LocalPlayer self, Entity target) {
      AABB box = target.getBoundingBox();
      Vec3 eye = self.getEyePosition();
      if (box.contains(eye)) return true;

      RotationController c = RotationController.INSTANCE;
      Angle own = new Angle(self.getYRot(), self.getXRot());
      Angle sent = c != null && c.getServerAngle() != null ? c.getServerAngle() : own;
      Angle next = c != null && c.isActive() ? c.getRotation() : own;
      return hits(eye, sent, box) || hits(eye, next, box);
   }

   private static boolean hits(Vec3 eye, Angle angle, AABB box) {
      return box.clip(eye, eye.add(angle.toVector().scale(REACH))).isPresent();
   }
}
