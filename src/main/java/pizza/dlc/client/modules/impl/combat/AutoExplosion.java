package pizza.dlc.client.modules.impl.combat;

import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.PacketEvent;
import pizza.dlc.api.events.impl.game.EventMotion;
import pizza.dlc.api.events.impl.game.EventTrace;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.api.rotation.angle.AngleSmoothMode;
import pizza.dlc.api.rotation.angle.GrimSmoothMode;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.game.ExplosionUtil;
import pizza.dlc.api.util.game.LootGuard;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "AutoExplosion",
   description = "Обсидиан → авто-кристалл; якорь → авто-заряд+детонация",
   category = ModuleCategory.COMBAT
)
public class AutoExplosion extends Module {
   private static final float ANCHOR_POWER = 5.0F;
   private final BooleanSetting switchBack = new BooleanSetting("Свап обратно", this);
   private final BooleanSetting autoAnchor = new BooleanSetting("Авто-якорь", this);
   private final ModeSetting lootGuard = new ModeSetting("Не взрывать ресы", this, LootGuard.VALUABLE, LootGuard.ALL, LootGuard.OFF);
   private final SliderSetting placeDelayMs = new SliderSetting("Задержка постановки мс", this, 0.0F, 0.0F, 200.0F, 10.0F);
   private final SliderSetting attackDelayMs = new SliderSetting("Задержка удара мс", this, 0.0F, 0.0F, 200.0F, 10.0F);
   private final BooleanSetting aura = new BooleanSetting("Крист-аура", "Сама ставит и подрывает кристаллы у цели. ПКМ по галке — бинд", this);
   private final BooleanSetting auraThroughWalls = new BooleanSetting("Сквозь стены", "Разрешить позиции без прямой видимости", this)
      .setVisible(() -> this.aura.isEnabled());
   private final SliderSetting auraTargetRange = new SliderSetting("Радиус цели", this, 8.0F, 3.0F, 14.0F, 0.5F).setVisible(() -> this.aura.isEnabled());
   private final SliderSetting auraPlaceRange = new SliderSetting("Радиус постановки", this, 4.5F, 2.0F, 6.0F, 0.1F).setVisible(() -> this.aura.isEnabled());
   private final SliderSetting auraMinDamage = new SliderSetting("Мин. урон цели", this, 6.0F, 1.0F, 20.0F, 0.5F).setVisible(() -> this.aura.isEnabled());
   private final SliderSetting auraMaxSelf = new SliderSetting("Макс. урон себе", this, 6.0F, 0.0F, 20.0F, 0.5F).setVisible(() -> this.aura.isEnabled());
   private ExplosiveType activeType = ExplosiveType.CRYSTAL;
   private boolean rotationLanded = false;
   private boolean invSwapSent = false;
   private int rotateGateTicks = 0;
   private Angle currentTargetAngle;
   private boolean pendingSlotSwitch = false;
   private int crystalTargetSlot = -1;
   private Phase phase = Phase.IDLE;
   private BlockPos pendingObsidianPos;
   private BlockPos pendingAltPos;
   private BlockPos pendingHitPos;
   private long pendingStartMs;
   private long suppressInteractBlockPacketsUntilMs;
   private BlockPos lastCrystalPlaceOnPos;
   private long lastCrystalPlaceAtMs;
   private BlockPos lastCompletedObsidianPos;
   private long lastCompletedAtMs;
   private boolean waitingForUseRelease;
   private boolean auraDriven;
   private int lastPlacedCrystalId = -1;
   private Vec3 lastPlaceHitVec;
   private long lastPlaceSentAtMs;
   private int prevSelectedSlot = -1;
   private int switchedFromSlot = -1;
   private InteractionHand crystalPlaceHand;
   private long lastPlaceAttemptMs;
   private long lastAttackAttemptMs;
   private int attackedCrystalId = -1;
   private long attackedAtMs;
   private int invCrystalSwapSlot = -1;
   private int invCrystalSwapHotbar = -1;
   private int invCrystalSwapBackDelay = 0;
   private EndCrystal pendingAttackCrystal;
   private int pendingAttackCrystalId = -1;
   private static final int ROTATION_PRIORITY = 45;

   public AutoExplosion() {
      this.switchBack.setEnabled(true);
   }

   @Override
   public void onDisable() {
      this.clearState();
      this.lastCompletedObsidianPos = null;
      this.lastCompletedAtMs = 0L;
      super.onDisable();
   }

   @EventHandler
   public void onPacketSend(PacketEvent.Send event) {
      if (!this.isEnabled()) return;

      if (mc.player != null && mc.level != null) {
         if (event.getPacket() instanceof ServerboundUseItemOnPacket packet) {
            long now = System.currentTimeMillis();
            if (now >= this.suppressInteractBlockPacketsUntilMs) {
               if (!this.waitingForUseRelease) {
                  if (mc.options != null && mc.options.keyUse.isDown()) {
                     InteractionHand hand = packet.getHand();
                     ItemStack inHand = mc.player.getItemInHand(hand);
                     if (inHand != null && !inHand.isEmpty()) {
                        boolean isObsidian = inHand.getItem() == Items.OBSIDIAN;
                        boolean isAnchor = this.autoAnchor.isEnabled() && inHand.getItem() == Items.RESPAWN_ANCHOR;
                        if (isObsidian || isAnchor) {
                           this.activeType = isAnchor ? ExplosiveType.ANCHOR : ExplosiveType.CRYSTAL;
                           BlockHitResult hit = packet.getHitResult();
                           if (hit != null) {
                              if (this.lastCrystalPlaceOnPos == null
                                 || hit.getDirection() != Direction.UP
                                 || !hit.getBlockPos().equals(this.lastCrystalPlaceOnPos)
                                 || now - this.lastCrystalPlaceAtMs >= 750L) {
                                 BlockPos placedPos = new BlockPlaceContext(mc.player, hand, inHand, hit).getClickedPos();
                                 BlockPos altPos = hit.getBlockPos().relative(hit.getDirection());
                                 BlockPos hitPos = hit.getBlockPos();
                                 if (this.lastCompletedObsidianPos == null
                                    || !placedPos.equals(this.lastCompletedObsidianPos)
                                    || now - this.lastCompletedAtMs >= 1500L) {
                                    if (this.pendingObsidianPos == null || !placedPos.equals(this.pendingObsidianPos) || now - this.pendingStartMs >= 500L) {
                                       this.pendingObsidianPos = placedPos;
                                       this.pendingAltPos = altPos;
                                       this.pendingHitPos = hitPos;
                                       this.pendingStartMs = now;
                                       this.waitingForUseRelease = true;
                                       this.prevSelectedSlot = mc.player.getInventory().getSelectedSlot();
                                       this.lastPlaceAttemptMs = 0L;
                                       this.lastAttackAttemptMs = 0L;
                                       this.lastPlacedCrystalId = -1;
                                       this.lastPlaceHitVec = null;
                                       this.lastPlaceSentAtMs = 0L;
                                       this.pendingAttackCrystal = null;
                                       this.pendingAttackCrystalId = -1;
                                       this.pendingSlotSwitch = false;
                                       this.crystalTargetSlot = -1;
                                       this.auraDriven = false;
                                       this.phase = Phase.PREPARE_PLACE;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (!this.isEnabled()) return;

      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         if (mc.options != null && !mc.options.keyUse.isDown()) {
            this.waitingForUseRelease = false;
         }

         if (this.invCrystalSwapSlot != -1) {
            if (this.invCrystalSwapBackDelay > 0) {
               this.invCrystalSwapBackDelay--;
            } else {
               SwapSystem.swapInventoryToHotbar(this.invCrystalSwapSlot, this.invCrystalSwapHotbar);
               this.invCrystalSwapSlot = -1;
               this.invCrystalSwapHotbar = -1;
            }
         }

         long now = System.currentTimeMillis();
         if (this.phase == Phase.IDLE && this.aura.isEnabled()) {
            this.tryAura(now);
         }

         if (this.phase != Phase.IDLE) {
            if (now - this.pendingStartMs > 1750L) {
               this.clearState();
            } else {
               if (!this.auraDriven) {
                  BlockPos basePos = this.activeType == ExplosiveType.ANCHOR ? this.resolveAnchorBasePos() : this.resolveObsidianBasePos();
                  if (basePos != null && !basePos.equals(this.pendingObsidianPos)) {
                     this.pendingObsidianPos = basePos;
                  }
               }

               if (this.activeType == ExplosiveType.CRYSTAL
                  && this.lastPlacedCrystalId == -1
                  && this.lastPlaceHitVec != null
                  && now - this.lastPlaceSentAtMs < 500L) {
                  EndCrystal placed = this.findCrystalClosestTo(this.lastPlaceHitVec, 1.25);
                  if (placed != null) {
                     this.lastPlacedCrystalId = placed.getId();
                  }
               }

               switch (this.phase) {
                  case PREPARE_PLACE:
                     if (!this.auraDriven && mc.options != null && mc.options.keyUse.isDown()) {
                        return;
                     }

                     long delay = (long)this.placeDelayMs.getValue();
                     if (now - this.pendingStartMs < delay || now - this.lastPlaceAttemptMs < 25L) {
                        return;
                     }

                     if (this.activeType == ExplosiveType.ANCHOR) {
                        this.prepareAnchorCharge();
                        return;
                     }

                     if (!this.canPlaceOn(this.pendingObsidianPos)) {
                        return;
                     }

                     if (this.findCrystalAtBase(this.pendingObsidianPos) != null) {
                        return;
                     }

                     this.crystalPlaceHand = this.pickCrystalHand();
                     if (this.crystalPlaceHand == null) {
                        return;
                     }

                     Vec3 hitPos = new Vec3(
                        this.pendingObsidianPos.getX() + 0.5, this.pendingObsidianPos.getY() + 0.999999, this.pendingObsidianPos.getZ() + 0.5
                     );
                     this.lastPlaceHitVec = hitPos;
                     this.switchedFromSlot = -1;
                     this.pendingSlotSwitch = false;
                     this.crystalTargetSlot = -1;
                     if (this.crystalPlaceHand == InteractionHand.MAIN_HAND && mc.player.getMainHandItem().getItem() != Items.END_CRYSTAL) {
                        int slot = this.findHotbarSlot(Items.END_CRYSTAL);
                        if (slot == -1) {
                           return;
                        }

                        this.crystalTargetSlot = slot;
                        this.switchedFromSlot = mc.player.getInventory().getSelectedSlot();
                        this.pendingSlotSwitch = true;
                     }

                     this.requestRotation(hitPos);
                     this.rotationLanded = false;
                     this.invSwapSent = false;
                     this.rotateGateTicks = 0;
                     this.phase = Phase.ROTATE_PLACE;
                     break;
                  case PREPARE_ATTACK:
                     long attackDelay = (long)this.attackDelayMs.getValue();
                     if (now - this.lastAttackAttemptMs < Math.max(0L, attackDelay)) {
                        return;
                     }

                     if (this.activeType == ExplosiveType.ANCHOR) {
                        this.prepareAnchorDetonate();
                        return;
                     }

                     EndCrystal crystal = this.findOurCrystal();
                     if (crystal == null) {
                        return;
                     }

                     this.pendingAttackCrystal = crystal;
                     this.pendingAttackCrystalId = crystal.getId();
                     Vec3 crystalPos = crystal.position().add(0.0, 0.5, 0.0);
                     this.requestRotation(crystalPos);
                     this.rotationLanded = false;
                     this.rotateGateTicks = 0;
                     this.phase = Phase.ROTATE_ATTACK;
                     break;
                  case WAIT_EXPLODE:
                     boolean gone = this.activeType == ExplosiveType.ANCHOR
                        ? !this.isAnchorBlock(this.pendingObsidianPos)
                        : this.attackedCrystalId > 0 && mc.level.getEntity(this.attackedCrystalId) == null;
                     boolean timedOut = now - this.attackedAtMs > 750L;
                     if (gone || timedOut) {
                        this.lastCompletedObsidianPos = this.pendingObsidianPos;
                        this.lastCompletedAtMs = now;
                        this.clearState();
                     }
               }
            }
         }
      }
   }

   private void tryAura(long now) {
      if (mc.player.isUsingItem() || CombatSync.userBusy() || mc.player.containerMenu != mc.player.inventoryMenu) return;
      if (!mc.player.isSpectator() && !mc.player.isDeadOrDying()) {
         if (this.pickCrystalHand() != null || this.findHotbarSlot(Items.END_CRYSTAL) != -1) {
            Player target = this.findAuraTarget();
            if (target != null) {
               BlockPos best = this.findAuraPlacement(target);
               if (best != null) {
                  this.activeType = ExplosiveType.CRYSTAL;
                  this.pendingObsidianPos = best;
                  this.pendingAltPos = best.above();
                  this.pendingHitPos = best;
                  this.pendingStartMs = now;
                  this.prevSelectedSlot = mc.player.getInventory().getSelectedSlot();
                  this.lastPlaceAttemptMs = 0L;
                  this.lastAttackAttemptMs = 0L;
                  this.lastPlacedCrystalId = -1;
                  this.lastPlaceHitVec = null;
                  this.lastPlaceSentAtMs = 0L;
                  this.pendingAttackCrystal = null;
                  this.pendingAttackCrystalId = -1;
                  this.pendingSlotSwitch = false;
                  this.crystalTargetSlot = -1;
                  this.waitingForUseRelease = false;
                  this.auraDriven = true;
                  this.phase = Phase.PREPARE_PLACE;
               }
            }
         }
      }
   }

   private Player findAuraTarget() {
      double range = this.auraTargetRange.getValue();
      double rangeSq = range * range;
      Player best = null;
      double bestDist = Double.MAX_VALUE;

      for (Player p : mc.level.players()) {
         if (p == mc.player || p.isSpectator() || p.isCreative() || p.isDeadOrDying() || isFriend(p) || AntiBot.isBot(p)) continue;

         double d = mc.player.distanceToSqr(p);
         if (!(d > rangeSq) && !(d >= bestDist)) {
            best = p;
            bestDist = d;
         }
      }

      return best;
   }

   private static boolean isFriend(Player p) {
      return PizzaDLC.getInstance() != null
         && PizzaDLC.getInstance().getFriendManager() != null
         && PizzaDLC.getInstance().getFriendManager().isFriend(p.getName().getString());
   }

   private BlockPos findAuraPlacement(Player target) {
      double placeRange = this.auraPlaceRange.getValue();
      double placeRangeSq = placeRange * placeRange;
      float minDamage = this.auraMinDamage.getValue();
      float maxSelf = this.auraMaxSelf.getValue();
      boolean throughWalls = this.auraThroughWalls.isEnabled();
      float ownHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
      Vec3 eye = mc.player.getEyePosition();
      BlockPos center = target.blockPosition();
      BlockPos best = null;
      float bestDamage = 0.0F;
      MutableBlockPos pos = new MutableBlockPos();

      for (int dx = -4; dx <= 4; dx++) {
         for (int dy = -3; dy <= 3; dy++) {
            for (int dz = -4; dz <= 4; dz++) {
               pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
               if (this.canPlaceCrystalOn(pos)) {
                  Vec3 blast = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                  if (!(eye.distanceToSqr(blast) > placeRangeSq) && (throughWalls || this.hasLineOfSight(eye, pos))) {
                     float damage = ExplosionUtil.crystalDamage(blast, target);
                     if (!(damage < minDamage) && !(damage <= bestDamage) && !this.lootAt(blast, ExplosionUtil.CRYSTAL_POWER)) {
                        float self = ExplosionUtil.crystalDamage(blast, mc.player);
                        if (!(self > maxSelf) && !(self >= ownHp)) {
                           bestDamage = damage;
                           best = pos.immutable();
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean canPlaceCrystalOn(BlockPos support) {
      BlockState state = mc.level.getBlockState(support);
      if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
         return false;
      }

      BlockPos above = support.above();
      if (!mc.level.isEmptyBlock(above)) {
         return false;
      }

      AABB box = new AABB(above.getX(), above.getY(), above.getZ(), above.getX() + 1.0, above.getY() + 2.0, above.getZ() + 1.0);
      return mc.level.getEntities((Entity)null, box, e -> true).isEmpty();
   }

   private boolean hasLineOfSight(Vec3 eye, BlockPos support) {
      Vec3 aim = new Vec3(support.getX() + 0.5, support.getY() + 1.0, support.getZ() + 0.5);
      BlockHitResult hit = mc.level.clip(new ClipContext(eye, aim, Block.COLLIDER, Fluid.NONE, mc.player));
      return hit.getType() == Type.MISS || hit.getBlockPos().equals(support);
   }

   private void requestRotation(Vec3 target) {
      RotationController controller = RotationController.INSTANCE;
      if (controller != null && mc.player != null) {
         Angle targetAngle = AngleUtil.calculateAngle(target);
         this.currentTargetAngle = targetAngle;
         AngleSmoothMode smooth = new GrimSmoothMode(true);
         RotationPlan plan = new RotationPlan(targetAngle, target, null, smooth, 3, 3.0F, true, true);
         controller.rotateTo(plan, ROTATION_PRIORITY, this);
      }
   }

   private boolean rotationDelivered() {
      RotationController c = RotationController.INSTANCE;
      if (c != null && this.currentTargetAngle != null) {
         float q = (float)Angle.computeGcd();
         float tol = Math.max(6.0F, 5.0F * q);
         return RotationController.computeRotationDifference(c.getRotation(), this.currentTargetAngle) < tol;
      }

      return true;
   }

   @EventHandler
   public void onTrace(EventTrace e) {
      if (this.phase == Phase.IDLE) return;

      RotationController controller = RotationController.INSTANCE;
      if (controller != null && controller.isActive()) {
         Angle rot = controller.getRotation();
         e.setYaw(rot.getYaw());
         e.setPitch(rot.getPitch());
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void onMotion(EventMotion e) {
      if (this.phase == Phase.IDLE) return;

      RotationController controller = RotationController.INSTANCE;
      if (controller != null && controller.isActive()) {
         Angle rot = controller.getRotation();
         e.setYaw(rot.getYaw());
         e.setPitch(rot.getPitch());
      }

      switch (this.phase) {
         case ROTATE_PLACE:
            e.setForceFlying(true);
            if (this.pendingSlotSwitch) {
               this.sendSelectedSlot(this.crystalTargetSlot);
               this.pendingSlotSwitch = false;
            } else if (this.invCrystalSwapSlot != -1 && !this.invSwapSent) {
               SwapSystem.swapInventoryToHotbar(this.invCrystalSwapSlot, this.invCrystalSwapHotbar);
               this.invSwapSent = true;
            } else if (!this.rotationLanded) {
               if (this.rotationDelivered() || ++this.rotateGateTicks > 5) {
                  this.rotationLanded = true;
               }
            } else if (this.activeType == ExplosiveType.ANCHOR) {
               this.doChargeAnchor();
            } else {
               this.doPlaceCrystal();
            }
            break;
         case SWITCH_BACK:
            e.setForceFlying(true);
            boolean mustSwitch = this.switchBack.isEnabled() || this.activeType == ExplosiveType.ANCHOR;
            if (mustSwitch && this.switchedFromSlot >= 0) {
               this.sendSelectedSlot(this.switchedFromSlot);
               this.switchedFromSlot = -1;
            }

            if (this.invCrystalSwapSlot != -1) {
               this.invCrystalSwapBackDelay = 1;
            }

            this.phase = Phase.PREPARE_ATTACK;
         case PREPARE_ATTACK:
         default:
            break;
         case ROTATE_ATTACK:
            e.setForceFlying(true);
            if (!this.rotationLanded) {
               if (this.rotationDelivered() || ++this.rotateGateTicks > 5) {
                  this.rotationLanded = true;
               }
            } else if (this.activeType == ExplosiveType.ANCHOR) {
               this.doDetonateAnchor();
            } else {
               this.doAttackCrystal();
            }
      }
   }

   private void doPlaceCrystal() {
      if (this.pendingObsidianPos != null && this.lootAt(Vec3.atBottomCenterOf(this.pendingObsidianPos.above()), ExplosionUtil.CRYSTAL_POWER)) {
         this.clearState();
         return;
      }

      long now = System.currentTimeMillis();
      this.lastPlaceAttemptMs = now;
      if (this.crystalPlaceHand != null && this.lastPlaceHitVec != null) {
         BlockHitResult bhr = new BlockHitResult(this.lastPlaceHitVec, Direction.UP, this.pendingObsidianPos, false);
         this.suppressInteractBlockPacketsUntilMs = now + 250L;
         this.lastCrystalPlaceOnPos = this.pendingObsidianPos;
         this.lastCrystalPlaceAtMs = now;
         this.lastPlaceSentAtMs = now;
         this.lastPlacedCrystalId = -1;
         mc.gameMode.useItemOn(mc.player, this.crystalPlaceHand, bhr);
         mc.player.swing(this.crystalPlaceHand);
         this.phase = Phase.SWITCH_BACK;
      } else {
         this.phase = Phase.PREPARE_PLACE;
      }
   }

   private void doAttackCrystal() {
      if (this.pendingAttackCrystal != null && this.lootAt(this.pendingAttackCrystal.position(), ExplosionUtil.CRYSTAL_POWER)) {
         this.clearState();
         return;
      }

      if (this.pendingAttackCrystal != null && !this.pendingAttackCrystal.isRemoved()) {
         long now = System.currentTimeMillis();
         this.lastAttackAttemptMs = now;
         mc.gameMode.attack(mc.player, this.pendingAttackCrystal);
         mc.player.swing(InteractionHand.MAIN_HAND);
         this.attackedCrystalId = this.pendingAttackCrystalId;
         this.attackedAtMs = now;
         this.pendingAttackCrystal = null;
         this.pendingAttackCrystalId = -1;
         this.phase = Phase.WAIT_EXPLODE;
      } else {
         this.phase = Phase.PREPARE_ATTACK;
      }
   }

   private void prepareAnchorCharge() {
      if (!this.isAnchorBlock(this.pendingObsidianPos)) return;

      this.switchedFromSlot = -1;
      this.pendingSlotSwitch = false;
      this.crystalTargetSlot = -1;
      ItemStack main = mc.player.getMainHandItem();
      if (main != null && main.getItem() == Items.GLOWSTONE) {
         this.crystalPlaceHand = InteractionHand.MAIN_HAND;
      } else {
         int hotbar = this.findHotbarSlot(Items.GLOWSTONE);
         if (hotbar != -1) {
            this.crystalPlaceHand = InteractionHand.MAIN_HAND;
            this.crystalTargetSlot = hotbar;
            this.switchedFromSlot = mc.player.getInventory().getSelectedSlot();
            this.pendingSlotSwitch = true;
         } else {
            int inv = this.findInventorySlot(Items.GLOWSTONE);
            if (inv == -1) {
               return;
            }

            this.crystalPlaceHand = InteractionHand.MAIN_HAND;
            this.invCrystalSwapSlot = inv;
            this.invCrystalSwapHotbar = mc.player.getInventory().getSelectedSlot();
         }
      }

      this.lastPlaceHitVec = this.anchorHit(this.pendingObsidianPos);
      this.requestRotation(this.lastPlaceHitVec);
      this.rotationLanded = false;
      this.invSwapSent = false;
      this.rotateGateTicks = 0;
      this.phase = Phase.ROTATE_PLACE;
   }

   private void doChargeAnchor() {
      if (this.pendingObsidianPos != null && this.lootAt(Vec3.atCenterOf(this.pendingObsidianPos), ANCHOR_POWER)) {
         this.clearState();
         return;
      }

      long now = System.currentTimeMillis();
      this.lastPlaceAttemptMs = now;
      if (this.crystalPlaceHand != null && this.lastPlaceHitVec != null) {
         BlockHitResult bhr = new BlockHitResult(this.lastPlaceHitVec, Direction.UP, this.pendingObsidianPos, false);
         this.suppressInteractBlockPacketsUntilMs = now + 250L;
         mc.gameMode.useItemOn(mc.player, this.crystalPlaceHand, bhr);
         mc.player.swing(this.crystalPlaceHand);
         this.phase = Phase.SWITCH_BACK;
      } else {
         this.phase = Phase.PREPARE_PLACE;
      }
   }

   private void prepareAnchorDetonate() {
      if (!this.isAnchorBlock(this.pendingObsidianPos)) {
         this.clearState();
      } else {
         this.lastPlaceHitVec = this.anchorHit(this.pendingObsidianPos);
         this.requestRotation(this.lastPlaceHitVec);
         this.rotationLanded = false;
         this.rotateGateTicks = 0;
         this.phase = Phase.ROTATE_ATTACK;
      }
   }

   private void doDetonateAnchor() {
      if (this.pendingObsidianPos != null && this.lootAt(Vec3.atCenterOf(this.pendingObsidianPos), ANCHOR_POWER)) {
         this.clearState();
         return;
      }

      long now = System.currentTimeMillis();
      this.lastAttackAttemptMs = now;
      if (this.lastPlaceHitVec == null) {
         this.phase = Phase.PREPARE_ATTACK;
      } else if (mc.player.getMainHandItem().getItem() != Items.GLOWSTONE) {
         BlockHitResult bhr = new BlockHitResult(this.lastPlaceHitVec, Direction.UP, this.pendingObsidianPos, false);
         this.suppressInteractBlockPacketsUntilMs = now + 250L;
         mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
         mc.player.swing(InteractionHand.MAIN_HAND);
         this.attackedAtMs = now;
         this.phase = Phase.WAIT_EXPLODE;
      }
   }

   private Vec3 anchorHit(BlockPos pos) {
      return new Vec3(pos.getX() + 0.5, pos.getY() + 0.999999, pos.getZ() + 0.5);
   }

   private boolean isAnchorBlock(BlockPos pos) {
      return mc.level != null && pos != null && mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR);
   }

   private BlockPos resolveAnchorBasePos() {
      if (mc.level == null) {
         return null;
      }

      BlockPos[] seeds = new BlockPos[]{this.pendingObsidianPos, this.pendingHitPos, this.pendingAltPos};

      for (BlockPos seed : seeds) {
         if (seed == null || !this.isAnchorBlock(seed)) continue;

         return seed;
      }

      for (BlockPos seedx : seeds) {
         if (seedx == null) continue;

         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               for (int dz = -1; dz <= 1; dz++) {
                  if (dx == 0 && dy == 0 && dz == 0) continue;

                  BlockPos p = seedx.offset(dx, dy, dz);
                  if (this.isAnchorBlock(p)) {
                     return p;
                  }
               }
            }
         }
      }

      return null;
   }

   private InteractionHand pickCrystalHand() {
      if (mc.player == null) {
         return null;
      }

      ItemStack main = mc.player.getMainHandItem();
      ItemStack off = mc.player.getOffhandItem();
      boolean mainHas = main != null && main.getItem() == Items.END_CRYSTAL;
      boolean offHas = off != null && off.getItem() == Items.END_CRYSTAL;
      if (mainHas) {
         return InteractionHand.MAIN_HAND;
      }

      if (offHas) {
         return InteractionHand.OFF_HAND;
      }

      if (this.findHotbarSlot(Items.END_CRYSTAL) != -1) {
         return InteractionHand.MAIN_HAND;
      }

      int invSlot = this.findInventorySlot(Items.END_CRYSTAL);
      if (invSlot != -1) {
         this.invCrystalSwapSlot = invSlot;
         this.invCrystalSwapHotbar = mc.player.getInventory().getSelectedSlot();
         return InteractionHand.MAIN_HAND;
      }

      return null;
   }

   private void sendSelectedSlot(int slot) {
      SwapSystem.selectSlot(slot);
   }

   private int findHotbarSlot(Item item) {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack s = mc.player.getInventory().getItem(i);
         if (s != null && !s.isEmpty() && s.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private int findInventorySlot(Item item) {
      if (mc.player == null) {
         return -1;
      }

      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getItem(i).getItem() != item) continue;

         return i;
      }

      return -1;
   }

   private boolean lootAt(Vec3 center, float power) {
      return LootGuard.threatened(mc.level, center, power, this.lootGuard.get());
   }

   private void clearState() {
      this.phase = Phase.IDLE;
      this.auraDriven = false;
      this.rotationLanded = false;
      this.invSwapSent = false;
      this.rotateGateTicks = 0;
      this.currentTargetAngle = null;
      this.pendingSlotSwitch = false;
      this.crystalTargetSlot = -1;
      if (RotationController.INSTANCE != null) {
         RotationController.INSTANCE.release(this);
      }

      this.pendingObsidianPos = null;
      this.pendingAltPos = null;
      this.pendingHitPos = null;
      this.pendingStartMs = 0L;
      this.suppressInteractBlockPacketsUntilMs = 0L;
      this.lastCrystalPlaceOnPos = null;
      this.lastCrystalPlaceAtMs = 0L;
      this.waitingForUseRelease = false;
      this.lastPlacedCrystalId = -1;
      this.lastPlaceHitVec = null;
      this.lastPlaceSentAtMs = 0L;
      this.prevSelectedSlot = -1;
      this.switchedFromSlot = -1;
      this.crystalPlaceHand = null;
      this.lastPlaceAttemptMs = 0L;
      this.lastAttackAttemptMs = 0L;
      this.attackedCrystalId = -1;
      this.attackedAtMs = 0L;
      this.pendingAttackCrystal = null;
      this.pendingAttackCrystalId = -1;
   }

   private BlockPos resolveObsidianBasePos() {
      if (mc.level == null) {
         return null;
      }

      BlockPos[] seeds = new BlockPos[]{this.pendingObsidianPos, this.pendingHitPos, this.pendingAltPos};

      for (BlockPos seed : seeds) {
         if (seed == null || !this.canPlaceOn(seed)) continue;

         return seed;
      }

      for (BlockPos seedx : seeds) {
         if (seedx == null) continue;

         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               for (int dz = -1; dz <= 1; dz++) {
                  if (dx == 0 && dy == 0 && dz == 0) continue;

                  BlockPos checkPos = seedx.offset(dx, dy, dz);
                  if (this.canPlaceOn(checkPos)) {
                     return checkPos;
                  }
               }
            }
         }
      }

      return null;
   }

   private boolean canPlaceOn(BlockPos basePos) {
      return mc.level != null && basePos != null
         ? mc.level.getBlockState(basePos).is(Blocks.OBSIDIAN) && mc.level.getBlockState(basePos.above()).isAir()
         : false;
   }

   private EndCrystal findOurCrystal() {
      if (mc.level != null && this.lastPlacedCrystalId > 0) {
         return mc.level.getEntity(this.lastPlacedCrystalId) instanceof EndCrystal c ? c : null;
      }

      return null;
   }

   private EndCrystal findCrystalClosestTo(Vec3 point, double maxDist) {
      if (mc.level != null && point != null) {
         double maxDistSq = maxDist * maxDist;
         AABB box = new AABB(point.x - maxDist, point.y - maxDist, point.z - maxDist, point.x + maxDist, point.y + maxDist, point.z + maxDist);
         List<EndCrystal> list = mc.level.getEntitiesOfClass(EndCrystal.class, box, cx -> true);
         EndCrystal best = null;
         double bestD2 = Double.POSITIVE_INFINITY;

         for (EndCrystal c : list) {
            double d2 = c.position().distanceToSqr(point);
            if (d2 <= maxDistSq && d2 < bestD2) {
               bestD2 = d2;
               best = c;
            }
         }

         return best;
      }

      return null;
   }

   private EndCrystal findCrystalAtBase(BlockPos basePos) {
      if (mc.level != null && basePos != null) {
         Vec3 center = new Vec3(basePos.getX() + 0.5, basePos.getY() + 1.0, basePos.getZ() + 0.5);
         return this.findCrystalClosestTo(center, 0.75);
      }

      return null;
   }

   private static enum ExplosiveType {
      CRYSTAL,
      ANCHOR;
   }

   private static enum Phase {
      IDLE,
      PREPARE_PLACE,
      ROTATE_PLACE,
      SWITCH_BACK,
      PREPARE_ATTACK,
      ROTATE_ATTACK,
      WAIT_EXPLODE;
   }
}
