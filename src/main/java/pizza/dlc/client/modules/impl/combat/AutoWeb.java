package pizza.dlc.client.modules.impl.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.api.rotation.angle.GrimSmoothMode;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ColorSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.game.BlockClick;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.api.util.game.PredictUtils;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.mixin.client.MultiPlayerGameModeAccessor;

@ModuleInfo(
   name = "AutoWeb",
   description = "Ставит паутину под ноги цели (двойная, сквозь стены)",
   category = ModuleCategory.COMBAT
)
public class AutoWeb extends Module {
   private final BooleanSetting rotate = new BooleanSetting("Ротация", this);
   private final BooleanSetting doubleWeb = new BooleanSetting("Двойная паутина", this);
   private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", this);
   private final BooleanSetting ignoreSelf = new BooleanSetting("Игнорировать себя", "Не ставить паутину в себя", this);
   private final BooleanSetting webCheck = new BooleanSetting("Не вебать в паутине", "Если цель уже в паутине — не ставить, дать ауре взорвать", this);
   private final SliderSetting range = new SliderSetting("Радиус", this, 4.5F, 1.0F, 6.0F, 0.5F);
   private final SliderSetting predictTicks = new SliderSetting("Прогноз", this, 2.0F, 0.0F, 6.0F, 1.0F);
   private final BooleanSetting render = new BooleanSetting("Рендер", this);
   private final ColorSetting color = new ColorSetting("Цвет", this, -1);
   private static final int HOTBAR_SIZE = 9;
   private static final int RESTORE_TICKS = 2;
   private static final int SWAP_COOLDOWN = 4;
   private static final double MAX_PREDICT_SHIFT = 1.5D;
   private int swapWait;
   private BlockPos pendingUpperPos;
   private BlockHitResult pendingUseHit;
   private final Map<BlockPos, Long> placedWebs = new HashMap<>();

   public AutoWeb() {
      this.rotate.setEnabled(true);
      this.doubleWeb.setEnabled(true);
      this.throughWalls.setEnabled(true);
      this.ignoreSelf.setEnabled(true);
      this.webCheck.setEnabled(true);
      this.render.setEnabled(true);
   }

   @Override
   public void onDisable() {
      this.pendingUpperPos = null;
      this.pendingUseHit = null;
      this.swapWait = 0;
      this.releaseRotation();
      this.placedWebs.clear();
      super.onDisable();
      CombatSlots.release(this);
      CombatSync.free(this);
   }

   @EventHandler
   public void onTick(EventTick e) {
      if (mc.player == null || !CombatSync.userBusy()) return;

      this.pendingUseHit = null;
      this.pendingUpperPos = null;
      CombatSync.free(this);
      CombatSlots.release(this);
      CombatSlots.restoreNow();
   }

   @EventHandler
   public void onWorldLoad(EventWorldLoad e) {
      this.pendingUpperPos = null;
      this.pendingUseHit = null;
      this.swapWait = 0;
      this.placedWebs.clear();
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      if (!this.isEnabled() || mc.player == null || mc.level == null || mc.gameMode == null) return;

      long now = System.currentTimeMillis();
      this.placedWebs.values().removeIf(t -> now - t > 400L);
      if (this.swapWait > 0) this.swapWait--;
      CombatSlots.tick();

      if (mc.player.isUsingItem() || mc.gui.screen() instanceof AbstractContainerScreen<?> || CombatSync.userBusy()) {
         this.pendingUseHit = null;
         this.pendingUpperPos = null;
         CombatSync.free(this);
         CombatSlots.release(this);
         return;
      }

      if (this.pendingUseHit != null) {
         BlockHitResult hit = this.pendingUseHit;
         this.pendingUseHit = null;
         boolean placed = this.performWebUse(hit);
         if (placed && this.pendingUpperPos != null) {
            BlockPos upper = this.pendingUpperPos;
            this.pendingUpperPos = null;
            this.pendingUseHit = this.findPlaceHit(upper);
            if (this.pendingUseHit != null) {
               CombatSlots.hold(this, 2);
               CombatSync.claim(this, CombatSync.PRIORITY_WEB, 2, true);
            } else {
               CombatSync.free(this);
            }
         } else {
            this.pendingUpperPos = null;
            CombatSync.free(this);
         }
         return;
      }

      LivingEntity target = CombatSync.sharedTarget(this.range.getValue());
      if (target == null) target = this.findTarget();
      if (target == null) {
         CombatSync.free(this);
         if (this.rotate.isEnabled()) this.releaseRotation();
         return;
      }

      if (this.webCheck.isEnabled() && this.targetInWeb(target)) {
         CombatSlots.release(this);
         CombatSync.free(this);
         this.pendingUpperPos = null;
         if (this.rotate.isEnabled()) this.releaseRotation();
         return;
      }

      int webSlot = this.getWebSlot();
      if (webSlot == -1) {
         int containerSlot = this.findInventoryCobweb();
         if (containerSlot != -1 && this.swapWait == 0) {
            InventoryUtility.swap(containerSlot, this.findSwapTargetSlot() + 36);
            this.swapWait = SWAP_COOLDOWN;
         }
         if (this.rotate.isEnabled()) this.releaseRotation();
         return;
      }

      BlockPos feet = this.predictFeet(target);
      if (feet == null || CrystalAura.isPlannedCrystalSpot(feet)) {
         CombatSync.free(this);
         if (this.rotate.isEnabled()) this.releaseRotation();
         return;
      }
      if (!CombatSync.claim(this, CombatSync.PRIORITY_WEB, 3, true)) return;
      BlockHitResult lowerHit = this.findPlaceHit(feet);
      if (lowerHit == null) {
         CombatSync.free(this);
         if (this.rotate.isEnabled()) this.releaseRotation();
         return;
      }

      if (this.doubleWeb.isEnabled()) {
         this.pendingUpperPos = feet.above();
      }

      this.holdWeb(webSlot);
      this.pendingUseHit = lowerHit;
   }

   private void holdWeb(int webSlot) {
      CombatSlots.select(this, webSlot, RESTORE_TICKS + 1);
      if (mc.gameMode instanceof MultiPlayerGameModeAccessor acc) {
         try {
            acc.invokerEnsureHasSentCarriedItem();
         } catch (Throwable ignored) {
         }
      }
   }

   private BlockPos predictFeet(LivingEntity target) {
      float ticks = this.predictTicks.getValue();
      BlockPos feet;
      if (target.isFallFlying()) {
         feet = BlockPos.containing(PredictUtils.predict(target, target.position(), ticks));
      } else {
         double dx = Mth.clamp((target.getX() - target.xOld) * ticks, -MAX_PREDICT_SHIFT, MAX_PREDICT_SHIFT);
         double dz = Mth.clamp((target.getZ() - target.zOld) * ticks, -MAX_PREDICT_SHIFT, MAX_PREDICT_SHIFT);
         feet = BlockPos.containing(target.getX() + dx, target.getY(), target.getZ() + dz);
         if (!target.onGround()) {
            for (int i = 0; i < 3 && mc.level.getBlockState(feet.below()).getCollisionShape(mc.level, feet.below()).isEmpty(); i++) {
               feet = feet.below();
            }
         }
      }

      if (!this.canPlaceAt(feet) && this.canPlaceAt(feet.above())) {
         feet = feet.above();
      }
      if (!this.canPlaceAt(feet)) {
         feet = target.blockPosition();
         if (!this.canPlaceAt(feet)) feet = feet.above();
      }
      return this.canPlaceAt(feet) ? feet : null;
   }

   private void setSilent(float yaw, float pitch) {
      RotationController c = RotationController.INSTANCE;
      if (c != null && mc.player != null) {
         Angle a = new Angle(yaw, pitch);
         Vec3 target = a.toVector().add(mc.player.getEyePosition());
         RotationPlan plan = new RotationPlan(a, target, null, new GrimSmoothMode(true), 3, 3.0F, true, true);
         c.rotateTo(plan, 55, this);
         c.update();
      }
   }

   private void releaseRotation() {
      if (RotationController.INSTANCE == null) return;

      RotationController.INSTANCE.release(this);
   }

   private Player findTarget() {
      double range = this.range.getValue();
      double rangeSq = range * range;
      Player best = null;
      double bestDist = Double.MAX_VALUE;
      for (Player p : mc.level.players()) {
         if (p == mc.player || !p.isAlive() || p.isRemoved()) continue;
         if (p.isSpectator() || p.isCreative()) continue;
         if (isFriend(p) || AntiBot.isBot(p)) continue;
         double d = mc.player.distanceToSqr(p);
         if (d <= rangeSq && d < bestDist) {
            bestDist = d;
            best = p;
         }
      }
      return best;
   }

   private static boolean isFriend(Player p) {
      return PizzaDLC.getInstance() != null
         && PizzaDLC.getInstance().getFriendManager() != null
         && PizzaDLC.getInstance().getFriendManager().isFriend(p.getName().getString());
   }

   private boolean targetInWeb(LivingEntity target) {
      if (mc.level == null) return false;
      BlockPos feet = target.blockPosition();
      return mc.level.getBlockState(feet).is(Blocks.COBWEB)
         || mc.level.getBlockState(feet.above()).is(Blocks.COBWEB);
   }

   private BlockHitResult findPlaceHit(BlockPos placePos) {
      if (!this.canPlaceAt(placePos)) return null;
      PlaceData placeData = this.findPlaceData(placePos);
      if (placeData == null) return null;
      if (this.rotate.isEnabled()) {
         float[] rot = this.getRotations(placeData.hitVec());
         this.setSilent(rot[0], rot[1]);
      }
      return new BlockHitResult(placeData.hitVec(), placeData.side(), placeData.neighbourPos(), false);
   }

   private boolean performWebUse(BlockHitResult hit) {
      if (mc.gameMode == null || mc.player == null) return false;
      BlockPos placePos = hit.getBlockPos().relative(hit.getDirection());
      if (!this.canPlaceAt(placePos) || mc.player.getEyePosition().distanceTo(hit.getLocation()) > mc.player.blockInteractionRange()) return false;
      int webSlot = this.getWebSlot();
      if (webSlot == -1) return false;
      if (mc.player.getInventory().getSelectedSlot() != webSlot) this.holdWeb(webSlot);
      mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
      mc.player.swing(InteractionHand.MAIN_HAND);
      CombatSlots.hold(this, RESTORE_TICKS);
      this.placedWebs.put(placePos.immutable(), System.currentTimeMillis());
      return true;
   }

   private boolean canPlaceAt(BlockPos pos) {
      BlockState state = mc.level.getBlockState(pos);
      if (!state.isAir() && !state.canBeReplaced()) return false;
      return !this.ignoreSelf.isEnabled() || !mc.player.getBoundingBox().intersects(new AABB(pos));
   }

   private PlaceData findPlaceData(BlockPos placePos) {
      Vec3 eye = mc.player.getEyePosition();
      double reach = mc.player.blockInteractionRange();
      PlaceData best = null;
      double bestDist = Double.MAX_VALUE;
      for (Direction side : Direction.values()) {
         BlockPos neighbour = placePos.relative(side);
         Direction hitSide = side.getOpposite();
         BlockState neighbourState = mc.level.getBlockState(neighbour);
         if (!this.isValidSupport(neighbour, neighbourState, hitSide)) continue;
         Vec3 faceCenter = new Vec3(
            neighbour.getX() + 0.5 + hitSide.getStepX() * 0.5,
            neighbour.getY() + 0.5 + hitSide.getStepY() * 0.5,
            neighbour.getZ() + 0.5 + hitSide.getStepZ() * 0.5);
         double dist = eye.distanceTo(faceCenter);
         if (dist > reach || dist >= bestDist) continue;
         if (!this.throughWalls.isEnabled() && !this.blockHit(this.getRotations(faceCenter), neighbour, reach)) continue;
         best = new PlaceData(neighbour, hitSide, faceCenter);
         bestDist = dist;
      }
      return best;
   }

   private boolean isValidSupport(BlockPos pos, BlockState state, Direction face) {
      if (state.isAir()) return false;
      if (!state.getFluidState().isEmpty()) return false;
      Block block = state.getBlock();
      if (block == Blocks.COBWEB) return true;
      if (!state.isFaceSturdy(mc.level, pos, face)) return false;
      if (BlockClick.isInteractable(mc.level, pos, state)) return false;
      if (block instanceof ButtonBlock) return false;
      if (block instanceof LeverBlock) return false;
      if (block instanceof TorchBlock) return false;
      if (block instanceof WallTorchBlock) return false;
      if (block instanceof RedstoneTorchBlock) return false;
      if (block instanceof FenceBlock) return false;
      if (block instanceof FenceGateBlock) return false;
      if (block instanceof DoorBlock) return false;
      if (block instanceof TrapDoorBlock) return false;
      if (block instanceof CarpetBlock) return false;
      if (block instanceof VineBlock) return false;
      if (block instanceof LadderBlock) return false;
      return true;
   }

   private boolean blockHit(float[] rot, BlockPos pos, double reach) {
      Vec3 eye = mc.player.getEyePosition();
      Vec3 dir = Vec3.directionFromRotation(rot[1], rot[0]);
      Vec3 end = eye.add(dir.scale(reach + 0.5D));
      HitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
      return hit.getType() == HitResult.Type.BLOCK && pos.equals(((BlockHitResult) hit).getBlockPos());
   }

   private float[] getRotations(Vec3 point) {
      Vec3 eyes = mc.player.getEyePosition();
      Vec3 delta = point.subtract(eyes);
      double distXZ = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float pitch = (float) Math.toDegrees(Math.atan2(-delta.y, distXZ));
      if (pitch > 90.0F) pitch = 90.0F;
      if (pitch < -90.0F) pitch = -90.0F;
      return new float[]{yaw, pitch};
   }

   private int getWebSlot() {
      for (int i = 0; i < HOTBAR_SIZE; i++) {
         if (mc.player.getInventory().getItem(i).getItem() != Items.COBWEB) continue;

         return i;
      }
      return -1;
   }

   private int findInventoryCobweb() {
      var inv = mc.player.getInventory().getNonEquipmentItems();
      for (int i = 9; i < inv.size(); i++) {
         if (!inv.get(i).is(Items.COBWEB)) continue;

         return i;
      }
      return -1;
   }

   private int findSwapTargetSlot() {
      int selected = mc.player.getInventory().getSelectedSlot();
      for (int i = 0; i < HOTBAR_SIZE; i++) {
         if (i != selected && mc.player.getInventory().getItem(i).isEmpty()) return i;
      }
      for (int i = HOTBAR_SIZE - 1; i >= 0; i--) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (i != selected && !isImportant(stack) && !stack.has(DataComponents.TOOL)) return i;
      }
      for (int i = HOTBAR_SIZE - 1; i >= 0; i--) {
         if (i != selected && !isImportant(mc.player.getInventory().getItem(i))) return i;
      }
      return selected;
   }

   private static boolean isImportant(ItemStack stack) {
      Item item = stack.getItem();
      return item == Items.COBWEB
         || item == Items.END_CRYSTAL
         || item == Items.OBSIDIAN
         || item == Items.TOTEM_OF_UNDYING
         || item == Items.GOLDEN_APPLE
         || item == Items.ENCHANTED_GOLDEN_APPLE
         || item == Items.ENDER_PEARL;
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (!this.render.isEnabled() || this.placedWebs.isEmpty()) return;

      long now = System.currentTimeMillis();
      int base = this.color.getIntColor();
      for (Entry<BlockPos, Long> en : this.placedWebs.entrySet()) {
         long age = now - en.getValue();
         if (age <= 400L) {
            float in = age < 200L ? eoc((float) age / 200.0F) : 1.0F - eoc(((float) age - 200.0F) / 200.0F);
            if (!(in < 0.01F)) {
               Vec3 c = Vec3.atCenterOf((Vec3i) en.getKey());
               double h = 0.5 * in;
               AABB box = new AABB(c.x - h, c.y - h, c.z - h, c.x + h, c.y + h, c.z + h);
               Render3DUtil.drawFilledBox(e, box, withAlpha(base, (int) (48.0F * in)), true);
               Render3DUtil.drawBox(e, box, withAlpha(base, (int) (200.0F * in)), true);
            }
         }
      }
   }

   private static float eoc(float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      float u = 1.0F - t;
      return 1.0F - u * u * u;
   }

   private static int withAlpha(int argb, int a) {
      return argb & 0xFFFFFF | (Math.max(0, Math.min(255, a)) & 0xFF) << 24;
   }

   private record PlaceData(BlockPos neighbourPos, Direction side, Vec3 hitVec) {
   }
}
