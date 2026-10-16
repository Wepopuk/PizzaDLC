package pizza.dlc.client.modules.impl.combat;

import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.attribute.EnvironmentAttributes;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.api.rotation.angle.GrimSmoothMode;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ColorSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.game.BlockClick;
import pizza.dlc.api.util.game.ExplosionUtil;
import pizza.dlc.api.util.game.LootGuard;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(name = "Anchor Aura", description = "Ставит, заряжает и взрывает якоря возрождения в лучшем месте", category = ModuleCategory.COMBAT)
public class AnchorAura extends Module {
   private static final float ANCHOR_POWER = 5.0F;
   private static final double BLAST_DIAMETER = ANCHOR_POWER * 2.0;
   private static final double TARGET_RADIUS = 6.5D;
   private static final double NEGLIGIBLE_SELF_DAMAGE = 0.5D;
   private static final double MAX_SELF_DAMAGE_UNSAFE = 30.0D;
   private static final double TARGET_SEARCH_RANGE = 10.0D;
   private static final int ACTION_PRIORITY = 52;
   private static final int STAGE_TIMEOUT = 10;
   private static final int BLAST_WAIT_TICKS = 6;
   private static final float CLEAR_MAX_RESISTANCE = 7.0F;
   private static final long CLEAR_REQUEST_MS = 300L;
   private static final long CRYSTAL_YIELD_MS = 400L;
   private static final long RENDER_MS = 350L;
   private static final int SIM_TICKS = 80;
   private static final int MIN_LAUNCH_TICKS = 3;
   private static final int WAIT_TIMEOUT = 100;
   private static final int MAX_LEAD_TICKS = 6;
   private static final double ARRIVAL_HEIGHT = 0.5D;
   private static final double ARRIVAL_SPREAD = 2.5D;
   private static final double AIR_DRAG = 0.91D;
   private static final double GRAVITY = 0.08D;
   private static final double VERTICAL_DRAG = 0.98D;
   private static final int GROUND_SCAN = 48;
   private static final List<AABB> UNIT = List.of(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
   private static AnchorAura instance;
   private static volatile boolean windowClicked;
   private final MultiModeSetting targets = new MultiModeSetting(
      "Цели", this, CombatTargets.PLAYERS, CombatTargets.NAKED, CombatTargets.MOBS, CombatTargets.ANIMALS, CombatTargets.FRIENDS
   );
   private final SliderSetting range = new SliderSetting("Дальность", this, 4.5F, 2.0F, 6.0F, 0.1F);
   private final SliderSetting minDamage = new SliderSetting("Мин. урон по цели", this, 6.0F, 1.0F, 20.0F, 0.5F);
   private final ModeSetting lootGuard = new ModeSetting("Не взрывать ресы", this, LootGuard.VALUABLE, LootGuard.ALL, LootGuard.OFF);
   private final BooleanSetting yieldCrystal = new BooleanSetting(
      "Уступать Crystal Aura", "Пока Crystal Aura бахает кристаллы, якоря ждут и включаются, когда кристаллам некуда встать", this
   );
   private final BooleanSetting juggle = new BooleanSetting(
      "Подброс",
      "Взрыв подкидывает цель; пока она летит, под точкой приземления стоит заряженный якорь и взрывается, когда она подлетает. Crystal Aura в это время бахает кристаллами. Не уступает Crystal Aura",
      this
   );
   private final BooleanSetting pauseOnUse = new BooleanSetting(
      "Пауза при еде/копании", "Пока ты держишь ПКМ или копаешь сам, якоря не ставятся", this
   );
   private final BooleanSetting safeYourSelf = new BooleanSetting("Не взрывать себя", "Не ставит туда, где урон по тебе слишком большой или смертельный", this);
   private final SliderSetting maxSelfDamage = new SliderSetting("Макс. урон по себе", this, 8.0F, 1.0F, 20.0F, 0.5F);
   private final SliderSetting minHealthLeft = new SliderSetting("Оставлять HP", this, 4.0F, 0.0F, 20.0F, 0.5F);
   private final BooleanSetting render = new BooleanSetting("Рендер", this);
   private final ColorSetting color = new ColorSetting("Цвет", this, 0xFFFF8A3D);
   private Stage stage = Stage.IDLE;
   private BlockPos anchorPos;
   private float[] aim;
   private int stageTicks;
   private BlockPos blastPos;
   private int blastWait;
   private BlockPos clearTarget;
   private long clearRequestedAt;
   private boolean rotating;
   private BlockPos renderPos;
   private long renderAt;
   private boolean juggling;
   private LivingEntity juggleTarget;
   private BlockPos parkedAnchor;

   public AnchorAura() {
      instance = this;
      this.targets.get(CombatTargets.PLAYERS).setEnabled(true);
      this.targets.get(CombatTargets.NAKED).setEnabled(true);
      this.yieldCrystal.setEnabled(true);
      this.juggle.setEnabled(true);
      this.pauseOnUse.setEnabled(true);
      this.safeYourSelf.setEnabled(true);
      this.render.setEnabled(true);
      this.maxSelfDamage.setVisible(this.safeYourSelf::isEnabled);
      this.minHealthLeft.setVisible(this.safeYourSelf::isEnabled);
      this.color.setVisible(this.render::isEnabled);
   }

   public static boolean clickWindowUsed() {
      AnchorAura module = instance;
      return module != null && module.isEnabled() && windowClicked;
   }

   public static boolean canClear(BlockPos pos) {
      AnchorAura module = instance;
      LocalPlayer self = mc.player;
      ClientLevel world = mc.level;
      if (module == null || !module.isEnabled() || self == null || world == null || !module.stocked(self) || module.anchorsHarmless(world, self)) return false;
      if (module.stage != Stage.IDLE) return pos.equals(module.clearTarget);

      return module.findClearPlan(self, world, pos) != null;
   }

   public static boolean requestClear(BlockPos pos) {
      if (!canClear(pos)) return false;

      AnchorAura module = instance;
      module.clearTarget = pos.immutable();
      module.clearRequestedAt = System.currentTimeMillis();
      return true;
   }

   @Override
   public void onDisable() {
      this.reset();
      this.releaseRotation();
      CombatSlots.release(this);
      CombatSync.free(this);
      super.onDisable();
   }

   @EventHandler
   public void onWorldLoad(EventWorldLoad e) {
      this.parkedAnchor = null;
      this.reset();
      this.releaseRotation();
   }

   @EventHandler
   public void onSend(PacketEvent.Send e) {
      if (e.getPacket() instanceof ServerboundUseItemOnPacket) {
         windowClicked = true;
      } else if (e.getPacket() instanceof ServerboundMovePlayerPacket || e.getPacket() instanceof ServerboundClientTickEndPacket) {
         windowClicked = false;
      }
   }

   @EventHandler
   public void onTick(EventTick e) {
      RotationController c = RotationController.INSTANCE;
      if (c == null || mc.player == null || this.aim == null) return;

      c.rotateTo(this.rotationPlan(this.aim), ACTION_PRIORITY, this);
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      CombatSlots.tick();
      LocalPlayer self = mc.player;
      ClientLevel world = mc.level;
      if (self == null || world == null || mc.gameMode == null) return;

      if (this.anchorsHarmless(world, self) || mc.gui.screen() instanceof AbstractContainerScreen<?> || self.isUsingItem()) {
         this.idle();
         return;
      }

      if (this.stage != Stage.IDLE) {
         this.tickSequence(self, world);
         return;
      }

      if (this.blastPos != null) {
         if (world.getBlockState(this.blastPos).is(Blocks.RESPAWN_ANCHOR) && --this.blastWait > 0) return;

         this.blastPos = null;
      }

      boolean clearing = this.clearTarget != null
         && System.currentTimeMillis() - this.clearRequestedAt <= CLEAR_REQUEST_MS
         && !world.getBlockState(this.clearTarget).isAir();
      if (!clearing) this.clearTarget = null;

      if (!this.stocked(self)
         || !clearing && (this.pauseOnUse.isEnabled() && CombatSync.userBusy()
            || this.yieldCrystal.isEnabled() && !this.juggle.isEnabled() && CrystalAura.recentlyActive(CRYSTAL_YIELD_MS)
            || CombatSync.blocked(this, CombatSync.PRIORITY_ANCHOR))) {
         this.idle();
         return;
      }

      int anchorSlot = this.findHotbar(self, Items.RESPAWN_ANCHOR);
      LivingEntity target = clearing ? null : this.findTarget(self, world);
      if (target != null && this.detonateParked(self, world, target)) return;

      boolean airborne = target != null && this.juggle.isEnabled() && isAirborne(target);
      Plan plan = clearing ? this.findClearPlan(self, world, this.clearTarget) : airborne ? this.findJugglePlan(self, world, target) : null;
      boolean juggled = plan != null && airborne;
      if (plan == null && !clearing && target != null) plan = this.findPlan(self, world, target);
      if (plan == null) {
         this.idle();
         return;
      }

      if (!AnchorPlacement.allowed(world,plan.pos()) || !AnchorPlacement.supportAllowed(world,plan.support())) {
         this.idle();
         return;
      }

      this.aim = plan.rot();
      this.aimAt(this.aim);
      this.mark(plan.pos());
      if (windowClicked) return;

      BlockHitResult hit = this.rayHit(plan.support(), self);
      if (hit == null || hit.getDirection() != plan.face()) return;

      CombatSync.claim(this, CombatSync.PRIORITY_ANCHOR, 3, true);
      CombatSlots.select(this, anchorSlot, 4);
      this.click(self, hit);
      this.anchorPos = plan.pos();
      this.stage = Stage.CHARGE;
      this.stageTicks = 0;
      this.juggling = juggled;
      this.juggleTarget = juggled ? target : null;
      if (clearing) this.clearRequestedAt = System.currentTimeMillis();
   }

   private void tickSequence(LocalPlayer self, ClientLevel world) {
      if (this.anchorPos == null || !AnchorPlacement.allowed(world,this.anchorPos)) {
         this.reset();this.releaseRotation();CombatSlots.release(this);return;
      }
      this.stageTicks++;
      BlockState state = world.getBlockState(this.anchorPos);
      if (!state.is(Blocks.RESPAWN_ANCHOR)) {
         if (this.stageTicks > STAGE_TIMEOUT) this.reset();
         return;
      }

      if (this.stage == Stage.CHARGE && state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
         this.afterCharge();
      }

      if (this.stage == Stage.WAIT && !this.arrivalReady(self, world, state)) return;

      if (this.lootAt(this.anchorPos)) {
         this.reset();
         return;
      }

      CombatSync.claim(this, CombatSync.PRIORITY_ANCHOR, 3, true);
      this.aimAt(this.aim);
      this.mark(this.anchorPos);
      if (windowClicked) return;

      BlockHitResult hit = this.rayHit(this.anchorPos, self);
      int slot = this.stage == Stage.CHARGE ? this.findHotbar(self, Items.GLOWSTONE) : this.detonateSlot(self);
      if (hit == null || slot < 0) {
         if (this.stageTicks > STAGE_TIMEOUT) this.reset();
         return;
      }

      CombatSlots.select(this, slot, 4);
      this.click(self, hit);
      if (this.stage == Stage.CHARGE) {
         this.afterCharge();
      } else {
         this.blastPos = this.anchorPos;
         this.blastWait = BLAST_WAIT_TICKS;
         this.reset();
      }
   }

   private Plan findPlan(LocalPlayer self, ClientLevel world, LivingEntity target) {
      Vec3 eye = self.getEyePosition();
      double reach = this.range.getValue();
      int radius = Mth.ceil(reach) + 1;
      BlockPos origin = BlockPos.containing(eye);
      double minDamage = this.minDamage(target);
      List<Entity> blockers = world.getEntities((Entity)null, self.getBoundingBox().inflate(reach + 3.0), e -> e.isAlive() && (e.blocksBuilding || e instanceof EndCrystal));
      Plan best = null;
      for (BlockPos mutable : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
         Vec3 center = Vec3.atCenterOf(mutable);
         if (center.distanceToSqr(target.position()) > TARGET_RADIUS * TARGET_RADIUS || blastCeiling(center, target) < minDamage) continue;

         BlockState state = world.getBlockState(mutable);
         if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || this.obstructed(new AABB(mutable), blockers)) continue;

         BlockPos pos = mutable.immutable();
         Plan support = this.findSupport(world, pos, eye, reach);
         if (support == null) continue;

         double damage = ExplosionUtil.damage(center, target, ANCHOR_POWER);
         if (damage < minDamage || best != null && damage <= best.damage()) continue;

         double selfDamage = ExplosionUtil.damage(center, self, ANCHOR_POWER);
         if (!this.selfDamageAllowed(self, selfDamage, damage) || this.lootAt(pos)) continue;

         best = new Plan(pos, support.support(), support.face(), support.rot(), damage);
      }

      return best;
   }

   private void afterCharge() {
      this.stageTicks = 0;
      if (!this.juggling) {
         this.stage = Stage.DETONATE;
         return;
      }

      this.stage = Stage.WAIT;
      this.aim = null;
      this.releaseRotation();
      CombatSync.free(this);
   }

   private boolean arrivalReady(LocalPlayer self, ClientLevel world, BlockState state) {
      LivingEntity target = this.juggleTarget;
      if (state.getValue(RespawnAnchorBlock.CHARGE) == 0 || target == null || !target.isAlive() || target.isRemoved() || this.stageTicks > WAIT_TIMEOUT) {
         this.park();
         return false;
      }

      this.mark(this.anchorPos);
      Vec3 center = Vec3.atCenterOf(this.anchorPos);
      boolean arriving = this.ticksUntilArrival(world, target, this.anchorPos) <= 1 + this.latencyTicks();
      boolean landedNear = target.onGround() && ExplosionUtil.damage(center, target, ANCHOR_POWER) >= this.minDamage(target);
      if (!arriving && !landedNear) return false;
      if (!this.clearSafe(self, ExplosionUtil.damage(center, self, ANCHOR_POWER)) || this.lootAt(this.anchorPos)) return false;

      float[] rot = this.faceAim(self, this.anchorPos);
      if (rot == null) return false;

      this.aim = rot;
      this.stage = Stage.DETONATE;
      this.stageTicks = 0;
      return true;
   }

   private boolean detonateParked(LocalPlayer self, ClientLevel world, LivingEntity target) {
      BlockPos pos = this.parkedAnchor;
      if (pos == null) return false;

      if (!AnchorPlacement.allowed(world,pos)) { this.parkedAnchor=null;return false; }
      BlockState state = world.getBlockState(pos);
      if (!state.is(Blocks.RESPAWN_ANCHOR) || state.getValue(RespawnAnchorBlock.CHARGE) == 0) {
         this.parkedAnchor = null;
         return false;
      }

      Vec3 center = Vec3.atCenterOf(pos);
      double damage = ExplosionUtil.damage(center, target, ANCHOR_POWER);
      if (damage < this.minDamage(target) || !this.selfDamageAllowed(self, ExplosionUtil.damage(center, self, ANCHOR_POWER), damage) || this.lootAt(pos)) {
         return false;
      }

      float[] rot = this.faceAim(self, pos);
      if (rot == null) return false;

      this.parkedAnchor = null;
      this.anchorPos = pos;
      this.aim = rot;
      this.stage = Stage.DETONATE;
      this.stageTicks = 0;
      this.aimAt(rot);
      this.mark(pos);
      return true;
   }

   private void park() {
      if (this.anchorPos != null && mc.level != null && mc.level.getBlockState(this.anchorPos).is(Blocks.RESPAWN_ANCHOR)) {
         this.parkedAnchor = this.anchorPos;
      }

      this.reset();
   }

   private Plan findJugglePlan(LocalPlayer self, ClientLevel world, LivingEntity target) {
      Landing landing = this.predictLanding(world, target);
      if (landing == null || landing.ticks() < MIN_LAUNCH_TICKS) return null;

      BlockPos pos = landing.pos();
      BlockState state = world.getBlockState(pos);
      if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || target.getBoundingBox().minY < pos.getY() + 1.0) return null;

      List<Entity> blockers = world.getEntities((Entity)null, new AABB(pos).inflate(1.0), e -> e.isAlive() && (e.blocksBuilding || e instanceof EndCrystal));
      if (this.obstructed(new AABB(pos), blockers)) return null;

      Plan support = this.findSupport(world, pos, self.getEyePosition(), this.range.getValue());
      if (support == null || this.lootAt(pos)) return null;

      double selfDamage = ExplosionUtil.damage(Vec3.atCenterOf(pos), self, ANCHOR_POWER);
      if (!this.clearSafe(self, selfDamage)) return null;

      return new Plan(pos, support.support(), support.face(), support.rot(), 0.0);
   }

   private Landing predictLanding(ClientLevel world, LivingEntity target) {
      double x = target.getX();
      double y = target.getY();
      double z = target.getZ();
      double vx = x - target.xo;
      double vy = y - target.yo;
      double vz = z - target.zo;
      int columnX = Integer.MIN_VALUE;
      int columnZ = Integer.MIN_VALUE;
      double ground = Double.NaN;
      for (int tick = 1; tick <= SIM_TICKS; tick++) {
         vx *= AIR_DRAG;
         vz *= AIR_DRAG;
         vy = (vy - GRAVITY) * VERTICAL_DRAG;
         x += vx;
         y += vy;
         z += vz;
         int cx = Mth.floor(x);
         int cz = Mth.floor(z);
         if (cx != columnX || cz != columnZ) {
            columnX = cx;
            columnZ = cz;
            ground = groundTop(world, cx, cz, y - vy);
         }

         if (!Double.isNaN(ground) && y <= ground) {
            return new Landing(new BlockPos(cx, Mth.floor(ground + 1.0E-4), cz), tick);
         }
      }

      return null;
   }

   private int ticksUntilArrival(ClientLevel world, LivingEntity target, BlockPos anchor) {
      double top = anchor.getY() + 1.0 + ARRIVAL_HEIGHT;
      double ax = anchor.getX() + 0.5;
      double az = anchor.getZ() + 0.5;
      double x = target.getX();
      double y = target.getY();
      double z = target.getZ();
      double vx = x - target.xo;
      double vy = y - target.yo;
      double vz = z - target.zo;
      for (int tick = 0; tick <= SIM_TICKS; tick++) {
         if (y <= top) {
            return Math.abs(x - ax) <= ARRIVAL_SPREAD && Math.abs(z - az) <= ARRIVAL_SPREAD ? tick : Integer.MAX_VALUE;
         }

         vx *= AIR_DRAG;
         vz *= AIR_DRAG;
         vy = (vy - GRAVITY) * VERTICAL_DRAG;
         x += vx;
         y += vy;
         z += vz;
      }

      return Integer.MAX_VALUE;
   }

   private static double groundTop(ClientLevel world, int x, int z, double fromY) {
      int top = Mth.floor(fromY);
      for (int by = top; by >= top - GROUND_SCAN; by--) {
         BlockPos pos = new BlockPos(x, by, z);
         VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
         if (!shape.isEmpty()) return by + shape.max(Direction.Axis.Y);
      }

      return Double.NaN;
   }

   private static boolean isAirborne(LivingEntity target) {
      return !target.onGround() && !target.isInWater() && !target.isInLava() && !target.isPassenger() && !target.isFallFlying() && !target.onClimbable();
   }

   private float[] faceAim(LocalPlayer self, BlockPos block) {
      Vec3 eye = self.getEyePosition();
      double reach = this.range.getValue();
      float[] best = null;
      double bestDist = Double.MAX_VALUE;
      for (Direction face : Direction.values()) {
         Vec3 point = facePoint(block, face);
         Angle angle = AngleUtil.calculateAngle(point);
         BlockHitResult hit = raycast(angle, block, eye, reach);
         if (hit == null || hit.getDirection() != face) continue;

         double dist = eye.distanceToSqr(point);
         if (dist < bestDist) {
            bestDist = dist;
            best = new float[]{angle.getYaw(), angle.getPitch()};
         }
      }

      return best;
   }

   private int latencyTicks() {
      int ping = 0;
      if (mc.getConnection() != null && mc.player != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
         if (info != null) ping = info.getLatency();
      }

      return Mth.clamp(Math.round(ping / 50.0F), 0, MAX_LEAD_TICKS);
   }

   private Plan findClearPlan(LocalPlayer self, ClientLevel world, BlockPos block) {
      BlockState blockState = world.getBlockState(block);
      if (blockState.isAir()
         || blockState.liquid()
         || blockState.getDestroySpeed(world, block) < 0.0F
         || blockState.getBlock().getExplosionResistance() > CLEAR_MAX_RESISTANCE) {
         return null;
      }

      Vec3 eye = self.getEyePosition();
      double reach = this.range.getValue();
      List<Entity> blockers = world.getEntities((Entity)null, new AABB(block).inflate(2.0), e -> e.isAlive() && (e.blocksBuilding || e instanceof EndCrystal));
      Plan best = null;
      double bestSelfDamage = Double.MAX_VALUE;
      for (Direction dir : Direction.values()) {
         BlockPos pos = block.relative(dir);
         BlockState state = world.getBlockState(pos);
         if (!state.canBeReplaced() || !state.getFluidState().isEmpty() || this.obstructed(new AABB(pos), blockers)) continue;

         Plan support = this.findSupport(world, pos, eye, reach);
         if (support == null) continue;

         double selfDamage = ExplosionUtil.damage(Vec3.atCenterOf(pos), self, ANCHOR_POWER);
         if (!this.clearSafe(self, selfDamage) || selfDamage >= bestSelfDamage || this.lootAt(pos)) continue;

         bestSelfDamage = selfDamage;
         best = new Plan(pos, support.support(), support.face(), support.rot(), 0.0);
      }

      return best;
   }

   private boolean lootAt(BlockPos pos) {
      return LootGuard.threatened(mc.level, Vec3.atCenterOf(pos), ANCHOR_POWER, this.lootGuard.get());
   }

   private boolean clearSafe(LocalPlayer self, double selfDamage) {
      if (!this.safeYourSelf.isEnabled()) return selfDamage <= MAX_SELF_DAMAGE_UNSAFE;
      if (selfDamage <= NEGLIGIBLE_SELF_DAMAGE) return true;
      return selfDamage <= this.maxSelfDamage.getValue() && self.getHealth() + self.getAbsorptionAmount() - selfDamage >= this.minHealthLeft.getValue();
   }

   private boolean stocked(LocalPlayer self) {
      return this.findHotbar(self, Items.RESPAWN_ANCHOR) >= 0 && this.findHotbar(self, Items.GLOWSTONE) >= 0 && !isRespawnFuel(self.getOffhandItem());
   }

   private Plan findSupport(ClientLevel world, BlockPos pos, Vec3 eye, double reach) {
      if (!AnchorPlacement.allowed(world,pos)) return null;
      Plan best = null;
      double bestDist = Double.MAX_VALUE;
      for (Direction dir : Direction.values()) {
         BlockPos support = pos.relative(dir);
         BlockState state = world.getBlockState(support);
         if (!AnchorPlacement.supportAllowed(world,support) || state.getCollisionShape(world, support).isEmpty() || BlockClick.isInteractable(world, support, state)) continue;

         Direction face = dir.getOpposite();
         Vec3 point = facePoint(support, face);
         Angle angle = AngleUtil.calculateAngle(point);
         BlockHitResult hit = raycast(angle, support, eye, reach);
         if (hit == null || hit.getDirection() != face) continue;

         double dist = eye.distanceToSqr(point);
         if (dist < bestDist) {
            bestDist = dist;
            best = new Plan(pos, support, face, new float[]{angle.getYaw(), angle.getPitch()}, 0.0);
         }
      }

      return best;
   }

   private boolean obstructed(AABB box, List<Entity> blockers) {
      for (Entity entity : blockers) {
         if (entity.getBoundingBox().intersects(box)) return true;
      }

      return false;
   }

   private static double blastCeiling(Vec3 center, LivingEntity target) {
      double dist = Math.sqrt(target.distanceToSqr(center)) / BLAST_DIAMETER;
      if (dist > 1.0) return 0.0;

      double impact = 1.0 - dist;
      return (impact * impact + impact) / 2.0 * 7.0 * BLAST_DIAMETER + 1.0;
   }

   private boolean selfDamageAllowed(LocalPlayer self, double selfDamage, double targetDamage) {
      if (!this.safeYourSelf.isEnabled()) return selfDamage <= MAX_SELF_DAMAGE_UNSAFE;
      if (selfDamage <= NEGLIGIBLE_SELF_DAMAGE) return true;
      if (selfDamage > this.maxSelfDamage.getValue()) return false;
      if (self.getHealth() + self.getAbsorptionAmount() - selfDamage < this.minHealthLeft.getValue()) return false;
      return targetDamage >= selfDamage;
   }

   private double minDamage(LivingEntity target) {
      return Math.min(this.minDamage.getValue(), target.getHealth() + target.getAbsorptionAmount() + 0.5);
   }

   private LivingEntity findTarget(LocalPlayer self, ClientLevel world) {
      LivingEntity best = null;
      double bestDist = TARGET_SEARCH_RANGE * TARGET_SEARCH_RANGE;
      for (Entity entity : world.entitiesForRendering()) {
         if (!(entity instanceof LivingEntity living) || entity == self) continue;

         double dist = self.distanceToSqr(living);
         if (dist < bestDist && CombatTargets.accepts(this.targets, living)) {
            bestDist = dist;
            best = living;
         }
      }

      return best;
   }

   private boolean anchorsHarmless(ClientLevel world, LocalPlayer self) {
      return world.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, self.blockPosition());
   }

   private int detonateSlot(LocalPlayer self) {
      int anchors = this.findHotbar(self, Items.RESPAWN_ANCHOR);
      if (anchors >= 0) return anchors;

      int selected = self.getInventory().getSelectedSlot();
      if (!isRespawnFuel(self.getInventory().getItem(selected))) return selected;

      for (int i = 0; i < 9; i++) {
         if (!isRespawnFuel(self.getInventory().getItem(i))) return i;
      }

      return -1;
   }

   private int findHotbar(LocalPlayer self, Item item) {
      for (int i = 0; i < 9; i++) {
         if (self.getInventory().getItem(i).is(item)) return i;
      }

      return -1;
   }

   private static boolean isRespawnFuel(ItemStack stack) {
      return stack.is(Items.GLOWSTONE);
   }

   private BlockHitResult rayHit(BlockPos block, LocalPlayer self) {
      RotationController c = RotationController.INSTANCE;
      Vec3 eye = self.getEyePosition();
      double reach = this.range.getValue();
      Angle next = c != null && c.isActive() ? c.getRotation() : new Angle(self.getYRot(), self.getXRot());
      BlockHitResult hit = raycast(next, block, eye, reach);
      if (hit != null || c == null || c.getServerAngle() == null) return hit;

      return raycast(c.getServerAngle(), block, eye, reach);
   }

   private static BlockHitResult raycast(Angle angle, BlockPos block, Vec3 eye, double reach) {
      return AABB.clip(UNIT, eye, eye.add(angle.toVector().scale(reach)), block);
   }

   private static Vec3 facePoint(BlockPos pos, Direction face) {
      return new Vec3(
         pos.getX() + 0.5 + face.getStepX() * 0.5001,
         pos.getY() + 0.5 + face.getStepY() * 0.5001,
         pos.getZ() + 0.5 + face.getStepZ() * 0.5001
      );
   }

   private void click(LocalPlayer self, BlockHitResult hit) {
      mc.gameMode.useItemOn(self, InteractionHand.MAIN_HAND, hit);
      self.swing(InteractionHand.MAIN_HAND);
   }

   private void aimAt(float[] rot) {
      RotationController c = RotationController.INSTANCE;
      if (c == null) return;

      c.rotateTo(this.rotationPlan(rot), ACTION_PRIORITY, this);
      c.update();
      this.rotating = true;
   }

   private RotationPlan rotationPlan(float[] rot) {
      Angle angle = new Angle(rot[0], rot[1]);
      Vec3 point = angle.toVector().add(mc.player.getEyePosition());
      return new RotationPlan(angle, point, null, new GrimSmoothMode(true), 3, 3.0F, true, true);
   }

   private void idle() {
      if (this.stage == Stage.IDLE) {
         this.aim = null;
         this.releaseRotation();
         CombatSync.free(this);
      }
   }

   private void releaseRotation() {
      if (!this.rotating) return;

      this.rotating = false;
      if (RotationController.INSTANCE != null) {
         RotationController.INSTANCE.release(this);
      }
   }

   private void reset() {
      if (this.stage == Stage.IDLE) this.blastPos = null;
      this.stage = Stage.IDLE;
      this.anchorPos = null;
      this.aim = null;
      this.stageTicks = 0;
      this.juggling = false;
      this.juggleTarget = null;
      CombatSync.free(this);
   }

   private void mark(BlockPos pos) {
      this.renderPos = pos;
      this.renderAt = System.currentTimeMillis();
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (!this.render.isEnabled() || this.renderPos == null) return;

      float age = (System.currentTimeMillis() - this.renderAt) / (float)RENDER_MS;
      if (age >= 1.0F) return;

      float alpha = 1.0F - age;
      int base = this.color.getIntColor() & 0xFFFFFF;
      AABB box = new AABB(this.renderPos);
      Render3DUtil.drawFilledBox(e, box, base | Math.round(48 * alpha) << 24, true);
      Render3DUtil.drawBox(e, box, base | Math.round(200 * Math.min(1.0F, alpha + 0.3F)) << 24, true);
   }

   private enum Stage {
      IDLE,
      CHARGE,
      WAIT,
      DETONATE
   }

   private record Plan(BlockPos pos, BlockPos support, Direction face, float[] rot, double damage) {
   }

   private record Landing(BlockPos pos, int ticks) {
   }
}
