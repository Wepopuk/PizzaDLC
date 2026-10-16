package pizza.dlc.client.modules.impl.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.util.game.ToolSpeed;
import pizza.dlc.api.events.impl.game.EventTick;
import pizza.dlc.api.events.impl.game.EventTrace;
import pizza.dlc.api.events.impl.game.EventWorldLoad;
import pizza.dlc.api.events.impl.game.EventUpdate;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.util.game.DestroyHold;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.api.rotation.angle.GrimSmoothMode;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ColorSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.util.game.BlockClick;
import pizza.dlc.api.util.game.ExplosionUtil;
import pizza.dlc.api.util.game.InventoryUtility;
import pizza.dlc.api.util.game.LootGuard;
import pizza.dlc.api.util.game.MultipointUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.client.modules.impl.player.AutoTool;
import pizza.dlc.mixin.client.MultiPlayerGameModeAccessor;

@ModuleInfo(name = "Crystal Aura", description = "Ставит обсидиан, кристаллы и подкапывает (furry порт)", category = ModuleCategory.COMBAT)
public class CrystalAura extends Module {
   private static final double MAX_SELF_DAMAGE_UNSAFE = 30.0D;
   private static final double NEGLIGIBLE_SELF_DAMAGE = 0.5D;
   private static final int MINE_LOOK_WAIT = 12;
   private static final double MINE_ABORT_TARGET_DIST = 8.0D;
   private static final int ROTATION_PRIORITY = 51;
   private static final int ACTION_PRIORITY = 52;
   private static final double GRIM_HIT_REACH = 2.99D;
   private static final int TOOL_SWAP_TICKS = 1;
   private static final int MAX_DIG_TICKS = 60;
   private static final int OBSIDIAN_EXTRA_TICKS = 3;
   private static final double TIME_WEIGHT = 0.75D;
   private static final int ANCHOR_CLEAR_COST = 5;
   private static final double AIM_MARGIN = 0.03D;
   private static final double[] AIM_STEPS = {0.1D, 0.25D, 0.4D, 0.5D, 0.6D, 0.75D, 0.9D};

   private final ModeSetting rotate = new ModeSetting("Ротация", this, "Vanilla", "Без ротации");
   private final MultiModeSetting targets = new MultiModeSetting(
      "Цели", this, CombatTargets.PLAYERS, CombatTargets.NAKED, CombatTargets.MOBS, CombatTargets.ANIMALS, CombatTargets.FRIENDS
   );
   private final ModeSetting speed = new ModeSetting("Скорость", this, "Быстро", "Обычно");
   private final BooleanSetting instantBreak = new BooleanSetting("Мгновенный удар", "Бьёт кристалл в момент появления, не дожидаясь тика", this)
      .setVisible(this::fast);
   private final BooleanSetting instantPlace = new BooleanSetting("Ставить сразу после удара", "Новый кристалл уходит тем же тиком, что и удар по старому", this)
      .setVisible(this::fast);
   private final BooleanSetting pauseOnUse = new BooleanSetting(
      "Пауза при еде/копании", "Пока ты держишь ПКМ (ешь, пьёшь, щит) или копаешь сам, аура не трогает слот и взгляд", this
   );
   private final BooleanSetting waterPause = new BooleanSetting(
      "Пауза в воде", "Пока ты или цель в воде, аура не пытается бахать: кристалл в воду не ставится, а копать из воды впятеро дольше", this
   );
   private final SliderSetting minDamage = new SliderSetting("Мин. урон по цели", this, 8.0F, 2.0F, 20.0F, 0.5F);
   private final ModeSetting lootGuard = new ModeSetting("Не взрывать ресы", this, LootGuard.VALUABLE, LootGuard.ALL, LootGuard.OFF);
   private final SliderSetting placeRange = new SliderSetting("Дальность кристалла", this, 4.5F, 2.0F, 6.0F, 0.1F);
   private final ModeSetting launch = new ModeSetting("Подброс ударом", this, "Когда не достать", "Всегда", "Выкл");
   @Getter
   private final BooleanSetting autoKrot = new BooleanSetting("Делать подкоп", this);
   private final BooleanSetting legitMine = new BooleanSetting("Легитное ломание", "Берёт кирку в руку, смотрит на блок и копает как игрок", this);
   private final SliderSetting maxBreakTicks = new SliderSetting("Макс. время ломания (тики)", this, 30.0F, 5.0F, 120.0F, 1.0F)
      .setVisible(() -> this.autoKrot.isEnabled() && this.legitMine.isEnabled());
   private final BooleanSetting mineThroughWalls = new BooleanSetting("Ломать сквозь стены", "Подкоп без требования видимости блока", this)
      .setVisible(() -> !this.legitMine.isEnabled());
   private final BooleanSetting selfMine = new BooleanSetting(
      "Подкоп под собой", "Ломает блок под ногами, если он ломается быстро: проваливаешься в ямку и бахаешь из неё — взрывы бьют по тебе слабее", this
   );
   private final SliderSetting selfHoleTicks = new SliderSetting("Яма: макс. тиков", this, 8.0F, 1.0F, 20.0F, 1.0F)
      .setVisible(() -> this.selfMine.isEnabled());
   private final BooleanSetting anchorClear = new BooleanSetting(
      "Пробахивать якорем", "Блоки под кристалл сначала сносит Anchor Aura взрывом якоря; копать только там, где якорем нельзя, и только киркой или лопатой", this
   ).setVisible(() -> this.autoKrot.isEnabled());
   private final BooleanSetting safeYourSelf = new BooleanSetting("Не взрывать себя", "Не ставит туда, где урон по тебе слишком большой или смертельный", this);
   private final SliderSetting maxSelfDamage = new SliderSetting("Макс. урон по себе", this, 8.0F, 1.0F, 20.0F, 0.5F)
      .setVisible(() -> this.safeYourSelf.isEnabled());
   private final SliderSetting minHealthLeft = new SliderSetting("Оставлять HP", this, 4.0F, 0.0F, 20.0F, 0.5F)
      .setVisible(() -> this.safeYourSelf.isEnabled());
   private final BooleanSetting render = new BooleanSetting("Рендер", this);
   private final ColorSetting color = new ColorSetting("Цвет", this, -1);

   private BlockHitResult pendingPlaceHit;
   private Item pendingPlaceItem;
   private BlockPos pendingVisualPos;

   private final java.util.LinkedHashSet<BlockPos> excavationSpots = new java.util.LinkedHashSet<>();
   private ClientLevel excavationWorld;
   private BlockPos minePos;
   private Direction mineFace;
   private double mineDamage;
   private int mineTicks;
   private int mineTimeout;
   private int mineLookWait;
   private int toolSwapWait;
   private int refillWait;
   private long launchBlockedUntil;
   private int launchAimTicks;
   private boolean selfHole;
   private boolean paused;

   @Getter
   private PlaceData lastData;
   private final ConcurrentLinkedQueue<VisualData> visualDatas = new ConcurrentLinkedQueue<>();

   @Getter
   private float[] rotation;
   private int rotationPriority = ROTATION_PRIORITY;

   private final Map<Integer, Long> brokenCrystals = new HashMap<>();
   private final Map<BlockPos, Long> placedCrystals = new HashMap<>();
   private static CrystalAura instance;
   private static volatile long lastActionMs;

   public BooleanSetting getAutoKrot() {
      return this.autoKrot;
   }

   public PlaceData getLastData() {
      return this.lastData;
   }

   public float[] getRotation() {
      return this.rotation;
   }

   public boolean isLegitMine() {
      return this.legitMine.isEnabled();
   }

   public CrystalAura() {
      instance = this;
      this.targets.get(CombatTargets.PLAYERS).setEnabled(true);
      this.targets.get(CombatTargets.NAKED).setEnabled(true);
      this.instantBreak.setEnabled(true);
      this.pauseOnUse.setEnabled(true);
      this.waterPause.setEnabled(true);
      this.instantPlace.setEnabled(true);
      this.autoKrot.setEnabled(true);
      this.legitMine.setEnabled(true);
      this.mineThroughWalls.setEnabled(true);
      this.selfMine.setEnabled(true);
      this.anchorClear.setEnabled(true);
      this.safeYourSelf.setEnabled(true);
      this.render.setEnabled(true);
   }

   @Override
   public void onDisable() {
      this.abortMining();
      this.releaseRotation();
      this.rotation = null;
      this.lastData = null;
      this.pendingPlaceHit = null;
      this.pendingVisualPos = null;
      this.brokenCrystals.clear();
      this.placedCrystals.clear();
      super.onDisable();
      CombatSlots.release(this);
      CombatSync.free(this);
   }

   @EventHandler
   public void onTick(EventTick e) {
      if (mc.player == null) return;

      if (this.pauseOnUse.isEnabled() && CombatSync.userBusy()) {
         this.pauseForUser();
         return;
      }

      this.preAim();
   }

   private void preAim() {
      RotationController c = RotationController.INSTANCE;
      if (c == null || this.paused || this.rotation == null || !this.rotate.is("Vanilla")) return;

      c.rotateTo(this.rotationPlan(this.rotation[0], this.rotation[1]), this.rotationPriority, this);
   }

   private void pauseForUser() {
      this.pendingPlaceHit = null;
      this.pendingVisualPos = null;
      this.lastData = null;
      if (this.minePos != null) {
         if (mc.options.keyAttack.isDown()) {
            this.minePos = null;
            this.mineFace = null;
            this.selfHole = false;
         } else {
            this.abortMining();
         }
      }
      if (!this.paused) {
         this.paused = true;
         this.releaseRotation();
         CombatSync.free(this);
         CombatSlots.restoreNow();
      }
   }

   @EventHandler
   public void onWorldLoad(EventWorldLoad e) {
      this.excavationSpots.clear();this.excavationWorld=null;
      this.minePos = null;
      this.mineFace = null;
      this.lastData = null;
      this.rotation = null;
      this.pendingPlaceHit = null;
      this.pendingVisualPos = null;
      this.selfHole = false;
      this.paused = false;
      this.brokenCrystals.clear();
      this.placedCrystals.clear();
      this.visualDatas.clear();
      CombatSlots.reset();
      CombatSync.reset();
   }

   @EventHandler
   public void onUpdate(EventUpdate e) {
      LocalPlayer self = mc.player;
      ClientLevel world = mc.level;
      if (!this.isEnabled() || self == null || world == null || mc.gameMode == null) return;
      if (this.refillWait > 0) this.refillWait--;
      CombatSlots.tick();
      if (self.isUsingItem() || inContainer() || this.pauseOnUse.isEnabled() && CombatSync.userBusy() || this.waterPause.isEnabled() && self.isInWater()) {
         this.pauseForUser();
         return;
      }
      this.paused = false;

      if (this.pendingPlaceHit != null) {
         BlockHitResult hit = this.pendingPlaceHit;
         BlockPos vis = this.pendingVisualPos;
         this.pendingPlaceHit = null;
         this.pendingVisualPos = null;
         if (this.rotation != null && this.rotate.is("Vanilla")) {
            this.setSilent(this.rotation[0], this.rotation[1]);
         }
         Item item = this.pendingPlaceItem;
         int slot = item == null ? -1 : this.findHotbar(self, item);
         boolean aimed = !this.rotate.is("Vanilla") || this.serverAimHit(self, hit.getBlockPos()) != null;
         boolean free = !AnchorAura.clickWindowUsed() && !CombatSync.blocked(this, CombatSync.PRIORITY_CRYSTAL)
            && (item != Items.END_CRYSTAL || !this.lootAt(Vec3.atBottomCenterOf(hit.getBlockPos().above())));
         if (slot != -1 && aimed && free && mc.gameMode != null && this.findTarget(self, world) != null) {
            lastActionMs = System.currentTimeMillis();
            CombatSlots.select(this, slot, 3);
            if (mc.gameMode instanceof MultiPlayerGameModeAccessor acc) {
               acc.invokerEnsureHasSentCarriedItem();
            }
            mc.gameMode.useItemOn(self, InteractionHand.MAIN_HAND, hit);
            self.swing(InteractionHand.MAIN_HAND);
            this.placedCrystals.put(hit.getBlockPos().immutable(), System.currentTimeMillis());
         }
         if (vis != null && !this.hasVisual(vis)) {
            this.visualDatas.add(new VisualData(vis, System.currentTimeMillis(), 0.2F, 0.8F, 1.0F));
         }
         return;
      }

      long now = System.currentTimeMillis();
      long ttl = this.brokenTtl(self);
      this.brokenCrystals.values().removeIf(t -> now - t > ttl);
      this.placedCrystals.values().removeIf(t -> now - t > ttl);

      LivingEntity target = this.findTarget(self, world);
      if (target != null && this.waterPause.isEnabled() && target.isInWater()) target = null;
      if (target != null) CombatSync.shareTarget(target);

      int crysSlot = target != null ? this.refillHotbar(self, Items.END_CRYSTAL) : this.findHotbar(self, Items.END_CRYSTAL);
      int obsSlot = target != null && crysSlot != -1 ? this.refillHotbar(self, Items.OBSIDIAN) : this.findHotbar(self, Items.OBSIDIAN);
      boolean canPop = this.canPopCrystals(self);
      boolean hasCrystals = crysSlot != -1 && canPop;
      boolean hasObsidian = obsSlot != -1;
      boolean crystalsAnywhere = canPop && (hasCrystals || this.inInventory(self, Items.END_CRYSTAL));
      boolean obsidianAnywhere = hasObsidian || this.inInventory(self, Items.OBSIDIAN);
      if (!crystalsAnywhere && !this.hasCrystalNearby(self, world)) {
         this.abortMining();
         this.lastData = null;
         this.releaseRotation();
         return;
      }

      if (this.minePos != null && this.tickMining(self, world, target)) {
         return;
      }

      if (target != null) {
         List<Entity> entities = this.nearbyEntities(self, world);
         PlaceData obsidianData = this.findBestObsidian(target, world, self, entities);
         if (!hasObsidian && obsidianData.placeResult() == PlaceResult.PLACE_OBSIDIAN || !hasCrystals && obsidianData.placeResult() != PlaceResult.EXPLOSION) {
            obsidianData = new PlaceData(null, null, 0, PlaceResult.PLACE_OBSIDIAN, null);
         }
         PlaceData crystalData = hasCrystals
            ? this.findBestCrystal(target, world, self, entities)
            : new PlaceData(null, null, 0, PlaceResult.PLACE_CRYSTAL, null);
         PlaceData breakData = this.autoKrot.isEnabled() && crystalsAnywhere
            ? this.findBestBreak(target, world, self, entities, obsidianAnywhere)
            : new PlaceData(null, null, 0, PlaceResult.BREAK, null);

         PlaceData finalData = obsidianData;
         if (crystalData.damage() > finalData.damage()) finalData = crystalData;
         if (breakData.damage() > finalData.damage() && this.autoKrot.isEnabled()) finalData = breakData;
         if (obsidianData.placeResult() == PlaceResult.AWAITING) finalData = obsidianData;

         if (this.lastData == null || this.lastData.placeResult() != PlaceResult.EXPLOSION) this.lastData = finalData;
      } else {
         this.lastData = null;
      }

      if (target != null
         && this.selfMine.isEnabled()
         && (this.lastData == null || this.lastData.placeResult() != PlaceResult.EXPLOSION)
         && !CombatSync.blocked(this, CombatSync.PRIORITY_CRYSTAL)) {
         BlockPos hole = this.selfHoleBlock(self, world, target);
         if (hole != null) {
            this.lastData = null;
            this.startMining(self, world, hole, Direction.UP, 0.0D);
            this.selfHole = true;
            this.tickMining(self, world, target);
            return;
         }
      }

      if (target != null
         && !this.launch.is("Выкл")
         && (this.lastData == null || this.lastData.placeResult() != PlaceResult.EXPLOSION)
         && (this.launch.is("Всегда") || this.lastData == null || this.lastData.damage() == 0 || this.lastData.placeResult() == PlaceResult.BREAK)
         && !CombatSync.blocked(this, CombatSync.PRIORITY_CRYSTAL)
         && this.launchHit(self, target)) {
         this.lastData = null;
         return;
      }

      if (this.lastData == null || this.lastData.placeResult() == PlaceResult.AWAITING) {
         this.releaseRotation();
         return;
      }

      if (this.lastData.damage() == 0) {
         this.lastData = null;
         this.releaseRotation();
         return;
      }

      if (CombatSync.blocked(this, CombatSync.PRIORITY_CRYSTAL)) {
         this.lastData = null;
         return;
      }

      if (this.lastData.placeResult() == PlaceResult.EXPLOSION && this.fast() && this.lastData.entity() instanceof EndCrystal crystal) {
         boolean hit = this.breakCrystal(self, crystal, true);
         this.lastData = null;
         if (hit && target != null && this.instantPlace.isEnabled()) {
            this.placeBestNow(self, world, target);
         }
         if (!hit && this.rotation == null) this.releaseRotation();
         return;
      }

      if (this.lastData.placeResult() == PlaceResult.EXPLOSION) {
         BlockPos blockPos = this.lastData.blockPos();
         Direction direction = this.lastData.face();
         Entity crystal = this.lastData.entity();
         if (blockPos != null && direction != null && crystal instanceof EndCrystal) {
            float[] rot = this.calculate(this.getFacePoint(blockPos, direction), self);
            if (this.raycastBox(rot, blockPos, 5.5F, self) != null && this.rayTrace(rot, GRIM_HIT_REACH, crystal.getBoundingBox(), self)) {
               this.strikeCrystal(self, (EndCrystal)crystal, rot, true);
               this.lastData = null;
               return;
            }
         }
         if (crystal instanceof EndCrystal endCrystal) {
            Vec3 nearest = MultipointUtil.getNearestPoint(crystal, 0.0D, 0.05D);
            float[] rotCrystal = this.calculate(nearest, self);
            if (this.isLookingAtEntity(rotCrystal, crystal, GRIM_HIT_REACH, self)) {
               this.strikeCrystal(self, endCrystal, rotCrystal, true);
               this.lastData = null;
               return;
            }
         }
         this.lastData = null;
         this.releaseRotation();
         return;
      }

      BlockPos blockPos = this.lastData.blockPos();
      if (blockPos == null) {
         this.lastData = null;
         this.releaseRotation();
         return;
      }
      Direction direction = this.lastData.face();
      if (direction == null) {
         this.lastData = null;
         this.releaseRotation();
         return;
      }

      if (this.lastData.placeResult() == PlaceResult.BREAK) {
         this.startMining(self, world, blockPos, direction, this.lastData.damage());
         this.tickMining(self, world, target);
         return;
      }

      float[] rot = this.lastData.placeResult() == PlaceResult.PLACE_CRYSTAL
         ? this.crystalAim(blockPos, direction, self)
         : this.calculate(this.getFacePoint(blockPos, direction), self);

      if (this.fast() && this.lastData.placeResult() == PlaceResult.PLACE_CRYSTAL && this.placedCrystals.containsKey(blockPos)) {
         this.lastData = null;
         this.rotation = rot;
         if (this.rotate.is("Vanilla")) this.setSilent(rot[0], rot[1]);
         return;
      }

      if (this.fast() && this.raycastBox(rot, blockPos, this.placeRange(), self) != null && this.placeNow(self, blockPos, direction, this.lastData.placeResult())) {
         this.lastData = null;
         this.rotation = rot;
         if (this.rotate.is("Vanilla")) this.setSilent(rot[0], rot[1]);
         return;
      }

      if (this.raycastBox(rot, blockPos, this.placeRange(), self) != null) {
         if (this.rotate.is("Без ротации") || this.raycastBoxLook(blockPos, this.placeRange(), self) != null) {
            int slot = this.findHotbar(self, Items.OBSIDIAN);
            if (this.lastData.placeResult() == PlaceResult.PLACE_CRYSTAL) slot = this.findHotbar(self, Items.END_CRYSTAL);
            if (slot != -1 && CombatSync.claim(this, CombatSync.PRIORITY_CRYSTAL, 3, true)) {
               CombatSlots.select(this, slot, 3);
               if (mc.gameMode instanceof MultiPlayerGameModeAccessor acc) {
                  acc.invokerEnsureHasSentCarriedItem();
               }
               BlockPos vis = blockPos.relative(direction);
               if (this.lastData.placeResult() == PlaceResult.PLACE_CRYSTAL) vis = blockPos.above();
               this.pendingPlaceItem = this.lastData.placeResult() == PlaceResult.PLACE_CRYSTAL ? Items.END_CRYSTAL : Items.OBSIDIAN;
               BlockHitResult aim = this.raycastBox(rot, blockPos, this.placeRange(), self);
               this.pendingPlaceHit = aim != null
                  ? new BlockHitResult(aim.getLocation(), aim.getDirection(), blockPos, false)
                  : new BlockHitResult(Vec3.atCenterOf(blockPos), this.getFaceDirection(rot, self, blockPos), blockPos, false);
               this.pendingVisualPos = vis;
               this.lastData = null;
            }
         }
         this.rotation = rot;
      } else {
         this.releaseRotation();
         this.lastData = null;
      }

      if (this.rotation != null && this.rotate.is("Vanilla")) {
         this.setSilent(this.rotation[0], this.rotation[1]);
      }
   }

   private boolean fast() {
      return this.speed.is("Быстро");
   }

   private boolean lootAt(Vec3 center) {
      return LootGuard.threatened(mc.level, center, ExplosionUtil.CRYSTAL_POWER, this.lootGuard.get());
   }

   private float placeRange() {
      return this.placeRange.getValue();
   }

   public static boolean recentlyActive(long windowMs) {
      CrystalAura ca = instance;
      return ca != null && ca.isEnabled() && System.currentTimeMillis() - lastActionMs <= windowMs;
   }

   private double minDamage(LivingEntity target) {
      return Math.min(this.minDamage.getValue(), target.getHealth() + target.getAbsorptionAmount() + 0.5D);
   }

   private static boolean inContainer() {
      return mc.gui.screen() instanceof AbstractContainerScreen<?>;
   }

   private int refillHotbar(LocalPlayer self, Item item) {
      int slot = this.findHotbar(self, item);
      if (slot != -1 || this.refillWait > 0) return slot;
      for (int i = 9; i < 36; i++) {
         if (!self.getInventory().getItem(i).is(item)) continue;

         int hotbar = this.hotbarSlotForTool(self);
         InventoryUtility.swap(i, hotbar + 36);
         this.refillWait = 4;
         return self.getInventory().getItem(hotbar).is(item) ? hotbar : -1;
      }
      return -1;
   }

   private boolean inInventory(LocalPlayer self, Item item) {
      for (int i = 9; i < 36; i++) {
         if (self.getInventory().getItem(i).is(item)) return true;
      }
      return false;
   }

   private boolean hasCrystalNearby(LocalPlayer self, ClientLevel world) {
      return !world.getEntitiesOfClass(EndCrystal.class, self.getBoundingBox().inflate(6.0D), Entity::isAlive).isEmpty();
   }

   private long brokenTtl(LocalPlayer self) {
      int ping = 0;
      if (mc.getConnection() != null) {
         PlayerInfo info = mc.getConnection().getPlayerInfo(self.getUUID());
         if (info != null) ping = info.getLatency();
      }
      return Math.max(150L, Math.min(700L, ping * 2L + 100L));
   }

   private List<Entity> nearbyEntities(LocalPlayer self, ClientLevel world) {
      List<Entity> list = new ArrayList<>(world.getEntitiesOfClass(Entity.class, self.getBoundingBox().inflate(16.0D), ex -> ex.isAlive() && !ex.isSpectator()));
      if (!this.brokenCrystals.isEmpty()) {
         list.removeIf(ex -> ex instanceof EndCrystal && this.brokenCrystals.containsKey(ex.getId()));
      }
      return list;
   }

   private boolean breakCrystal(LocalPlayer self, EndCrystal crystal, boolean inTick) {
      if (this.brokenCrystals.containsKey(crystal.getId())) return false;
      BlockPos base = crystal.blockPosition().below();
      float[] rot = this.calculate(this.getFacePoint(base, Direction.UP), self);
      BlockHitResult baseHit = this.raycastBox(rot, base, 5.5F, self);
      if (baseHit == null || baseHit.getDirection() != Direction.UP || !this.rayTrace(rot, GRIM_HIT_REACH, crystal.getBoundingBox(), self)) {
         rot = this.calculate(MultipointUtil.getNearestPoint(crystal, 0.0D, 0.05D), self);
         if (!this.isLookingAtEntity(rot, crystal, GRIM_HIT_REACH, self)) return false;
      }
      return this.strikeCrystal(self, crystal, rot, inTick);
   }

   private boolean strikeCrystal(LocalPlayer self, EndCrystal crystal, float[] rot, boolean inTick) {
      if (this.brokenCrystals.containsKey(crystal.getId()) || this.lootAt(crystal.position()) || !this.prepareAttack(self)) return false;
      CombatSync.claim(this, CombatSync.PRIORITY_CRYSTAL, 2, false);
      float[] held = this.sentLookAt(self, crystal);
      float[] aim = held != null ? held : rot;
      this.rotation = aim;
      this.rotationPriority = ACTION_PRIORITY;
      if (this.rotate.is("Vanilla")) this.setSilent(aim[0], aim[1], ACTION_PRIORITY);
      if (!this.grimAccepts(self, crystal, inTick)) return false;
      lastActionMs = System.currentTimeMillis();
      mc.gameMode.attack(self, crystal);
      self.swing(InteractionHand.MAIN_HAND);
      this.brokenCrystals.put(crystal.getId(), System.currentTimeMillis());
      this.placedCrystals.remove(crystal.blockPosition().below());
      return true;
   }

   private boolean grimAccepts(LocalPlayer self, Entity entity, boolean inTick) {
      AABB box = entity.getBoundingBox().deflate(0.005D);
      Vec3 eye = self.getEyePosition();
      if (box.contains(eye)) return true;

      RotationController c = RotationController.INSTANCE;
      Angle own = new Angle(self.getYRot(), self.getXRot());
      Angle sent = c != null ? c.getServerAngle() : null;
      if (this.hitsBox(eye, sent != null ? sent : own, box)) return true;
      if (!inTick) return false;

      Angle next = c != null && c.isActive() ? c.getRotation() : own;
      return this.hitsBox(eye, next, box);
   }

   private float[] sentLookAt(LocalPlayer self, Entity entity) {
      RotationController c = RotationController.INSTANCE;
      Angle sent = c != null ? c.getServerAngle() : null;
      if (sent == null) return null;

      AABB box = entity.getBoundingBox().deflate(0.005D);
      Vec3 eye = self.getEyePosition();
      return !box.contains(eye) && this.hitsBox(eye, sent, box) ? new float[]{sent.getYaw(), sent.getPitch()} : null;
   }

   private float[] crystalAim(BlockPos base, Direction face, LocalPlayer self) {
      float[] center = this.calculate(this.getFacePoint(base, face), self);
      AABB crystal = crystalBox(base.above()).deflate(AIM_MARGIN);
      if (crystal.contains(self.getEyePosition())) return center;

      float[] best = null;
      double bestOffset = Double.MAX_VALUE;
      for (double u : AIM_STEPS) {
         for (double v : AIM_STEPS) {
            double offset = (u - 0.5D) * (u - 0.5D) + (v - 0.5D) * (v - 0.5D);
            if (offset >= bestOffset) continue;
            float[] rot = this.calculate(facePoint(base, face, u, v), self);
            BlockHitResult hit = this.raycastBox(rot, base, this.placeRange(), self);
            if (hit == null || hit.getDirection() != face || !this.rayTrace(rot, GRIM_HIT_REACH - AIM_MARGIN, crystal, self)) continue;
            best = rot;
            bestOffset = offset;
         }
      }

      return best != null ? best : center;
   }

   private static Vec3 facePoint(BlockPos pos, Direction face, double u, double v) {
      Direction.Axis axis = face.getAxis();
      double plane = 0.5D + (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0.5001D : -0.5001D);
      double x = axis == Direction.Axis.X ? plane : u;
      double y = axis == Direction.Axis.Y ? plane : axis == Direction.Axis.X ? u : v;
      double z = axis == Direction.Axis.Z ? plane : v;
      return new Vec3(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
   }

   private boolean hitsBox(Vec3 eye, Angle angle, AABB box) {
      return box.clip(eye, eye.add(angle.toVector().scale(GRIM_HIT_REACH))).isPresent();
   }

   private boolean placeBestNow(LocalPlayer self, ClientLevel world, LivingEntity target) {
      PlaceData d = this.findBestCrystal(target, world, self, this.nearbyEntities(self, world));
      return d.blockPos() != null && d.face() != null && d.damage() > 0 && this.placeNow(self, d.blockPos(), d.face(), PlaceResult.PLACE_CRYSTAL);
   }

   private boolean placeNow(LocalPlayer self, BlockPos pos, Direction face, PlaceResult kind) {
      if (kind != PlaceResult.PLACE_CRYSTAL && kind != PlaceResult.PLACE_OBSIDIAN) return false;
      if (kind == PlaceResult.PLACE_CRYSTAL && this.placedCrystals.containsKey(pos)) return false;
      BlockHitResult hit;
      if (this.rotate.is("Без ротации")) {
         float[] rot = this.calculate(this.getFacePoint(pos, face), self);
         hit = new BlockHitResult(Vec3.atCenterOf(pos), this.getFaceDirection(rot, self, pos), pos, false);
      } else {
         hit = this.serverAimHit(self, pos);
         if (hit == null || kind == PlaceResult.PLACE_OBSIDIAN && hit.getDirection() != face) return false;
      }
      int slot = this.findHotbar(self, kind == PlaceResult.PLACE_CRYSTAL ? Items.END_CRYSTAL : Items.OBSIDIAN);
      if (slot == -1 || AnchorAura.clickWindowUsed() || !CombatSync.claim(this, CombatSync.PRIORITY_CRYSTAL, 1, false)) return false;
      lastActionMs = System.currentTimeMillis();
      CombatSlots.select(this, slot, 3);
      if (mc.gameMode instanceof MultiPlayerGameModeAccessor acc) {
         acc.invokerEnsureHasSentCarriedItem();
      }
      mc.gameMode.useItemOn(self, InteractionHand.MAIN_HAND, hit);
      self.swing(InteractionHand.MAIN_HAND);
      if (kind == PlaceResult.PLACE_CRYSTAL) this.placedCrystals.put(pos.immutable(), System.currentTimeMillis());
      BlockPos vis = kind == PlaceResult.PLACE_CRYSTAL ? pos.above() : pos.relative(face);
      if (!this.hasVisual(vis)) {
         this.visualDatas.add(new VisualData(vis, System.currentTimeMillis(), 0.2F, 0.8F, 1.0F));
      }
      return true;
   }

   private boolean prepareAttack(LocalPlayer self) {
      if (!self.hasEffect(MobEffects.WEAKNESS) || attackDamage(self, self.getMainHandItem()) > 0.0D) return true;
      int best = -1;
      double bestDamage = 0.0D;
      for (int i = 0; i < 9; i++) {
         double d = attackDamage(self, self.getInventory().getItem(i));
         if (d > bestDamage) {
            bestDamage = d;
            best = i;
         }
      }
      if (best == -1) return false;
      CombatSlots.select(this, best, 3);
      return true;
   }

   private boolean canPopCrystals(LocalPlayer self) {
      if (!self.hasEffect(MobEffects.WEAKNESS)) return true;
      for (int i = 0; i < 9; i++) {
         if (attackDamage(self, self.getInventory().getItem(i)) > 0.0D) return true;
      }
      return false;
   }

   private static double attackDamage(LocalPlayer self, ItemStack stack) {
      double damage = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
         .compute(Attributes.ATTACK_DAMAGE, 1.0D, EquipmentSlot.MAINHAND);
      MobEffectInstance strength = self.getEffect(MobEffects.STRENGTH);
      if (strength != null) damage += 3.0D * (strength.getAmplifier() + 1);
      MobEffectInstance weakness = self.getEffect(MobEffects.WEAKNESS);
      if (weakness != null) damage -= 4.0D * (weakness.getAmplifier() + 1);
      return damage;
   }

   private BlockHitResult serverAimHit(LocalPlayer self, BlockPos pos) {
      RotationController c = RotationController.INSTANCE;
      Angle a = c != null ? c.getServerAngle() : null;
      return a == null ? null : this.raycastBox(new float[]{a.getYaw(), a.getPitch()}, pos, this.placeRange(), self);
   }

   private BlockPos selfHoleBlock(LocalPlayer self, ClientLevel world, LivingEntity target) {
      if (!self.onGround() || self.isPassenger() || self.isInWater() || self.isInLava() || self.isFallFlying()) return null;
      if (self.distanceToSqr(target) > 6.0D * 6.0D) return null;
      BlockPos feet = self.blockPosition();
      BlockPos under = feet.below();
      if (this.solidSides(world, feet) == 4) return null;
      AABB box = self.getBoundingBox();
      if (box.minX < under.getX() - 1.0E-3 || box.maxX > under.getX() + 1.001 || box.minZ < under.getZ() - 1.0E-3 || box.maxZ > under.getZ() + 1.001) return null;
      BlockState state = world.getBlockState(under);
      if (state.isAir() || !state.getFluidState().isEmpty() || BlockClick.isInteractable(world, under, state)) return null;
      if (!this.canBreak(self, world, under, state)) return null;
      int ticks = this.breakTicks(self, world, under, state);
      if (ticks > this.maxDigTicks() || this.digCost(self, world, under, state, ticks) > Math.round(this.selfHoleTicks.getValue())) return null;
      BlockPos floor = under.below();
      BlockState floorState = world.getBlockState(floor);
      if (floorState.getCollisionShape(world, floor).isEmpty() || !floorState.getFluidState().isEmpty()) return null;
      if (this.solidSides(world, under) < 4) return null;
      return under.immutable();
   }

   private int solidSides(ClientLevel world, BlockPos pos) {
      int n = 0;
      for (Direction d : Direction.Plane.HORIZONTAL) {
         BlockPos side = pos.relative(d);
         BlockState st = world.getBlockState(side);
         if (!st.getCollisionShape(world, side).isEmpty() && st.getFluidState().isEmpty()) n++;
      }
      return n;
   }

   public static boolean isReservedForExcavation(BlockPos pos) {
      CrystalAura ca=instance;
      if (ca==null || mc.level==null) return false;
      if (ca.excavationWorld!=null && ca.excavationWorld!=mc.level) { ca.excavationSpots.clear();ca.excavationWorld=null; }
      if (ca.isEnabled() && pos.equals(ca.minePos)) return true;
      if (ca.isEnabled() && ca.lastData!=null && ca.lastData.placeResult()==PlaceResult.BREAK && pos.equals(ca.lastData.blockPos())) return true;
      if (!ca.excavationSpots.contains(pos)) return false;
      BlockState state=mc.level.getBlockState(pos);
      if (!state.canBeReplaced() && !state.is(Blocks.RESPAWN_ANCHOR)) { ca.excavationSpots.remove(pos);return false; }
      return true;
   }

   public static boolean isPlannedAnchorConflict(BlockPos pos) {
      if (isPlannedCrystalSpot(pos)) return true;
      CrystalAura ca=instance;
      if (ca==null || !ca.isEnabled()) return false;
      PlaceData data=ca.lastData;
      if (data!=null && data.placeResult()==PlaceResult.PLACE_OBSIDIAN && data.blockPos()!=null
          && (pos.equals(data.blockPos()) || pos.equals(data.blockPos().above()))) return true;
      BlockHitResult hit=ca.pendingPlaceHit;
      if (hit!=null && ca.pendingPlaceItem==Items.OBSIDIAN) {
         BlockPos base=hit.getBlockPos().relative(hit.getDirection());
         return pos.equals(base) || pos.equals(base.above());
      }
      return false;
   }

   public static boolean isPlannedCrystalSpot(BlockPos pos) {
      CrystalAura ca = instance;
      if (ca == null || !ca.isEnabled()) return false;
      PlaceData d = ca.lastData;
      if (d != null && d.blockPos() != null && d.placeResult() == PlaceResult.PLACE_CRYSTAL && d.blockPos().above().equals(pos)) return true;
      BlockHitResult pending = ca.pendingPlaceHit;
      if (pending != null && ca.pendingPlaceItem == Items.END_CRYSTAL && pending.getBlockPos().above().equals(pos)) return true;
      for (BlockPos base : ca.placedCrystals.keySet()) {
         if (base.above().equals(pos)) return true;
      }
      return false;
   }

   public static void onEntityAdded(Entity entity) {
      CrystalAura ca = instance;
      if (ca != null && entity instanceof EndCrystal crystal) {
         ca.placedCrystals.remove(crystal.blockPosition().below());
      }
      if (ca != null && entity instanceof EndCrystal crystal && ca.isEnabled() && ca.fast() && ca.instantBreak.isEnabled()) {
         try {
            ca.onCrystalSpawn(crystal);
         } catch (Throwable ignored) {
         }
      }
   }

   private void onCrystalSpawn(EndCrystal crystal) {
      LocalPlayer self = mc.player;
      ClientLevel world = mc.level;
      if (self == null || world == null || mc.gameMode == null || self.isUsingItem() || this.minePos != null || inContainer()) return;
      if (this.pauseOnUse.isEnabled() && CombatSync.userBusy()) return;
      if (this.waterPause.isEnabled() && self.isInWater()) return;
      if (CombatSync.blocked(this, CombatSync.PRIORITY_CRYSTAL)) return;
      LivingEntity target = this.findTarget(self, world);
      if (target == null || this.waterPause.isEnabled() && target.isInWater()) return;
      Vec3 source = crystal.position();
      double targetDamage = this.explosionDamage(source, target, world);
      if (targetDamage < this.minDamage(target) * 0.5D) return;
      double selfDamage = this.explosionDamage(source, self, world);
      if (!this.selfDamageAllowed(self, selfDamage, targetDamage)) return;
      if (this.breakCrystal(self, crystal, false) && this.instantPlace.isEnabled()) {
         this.placeBestNow(self, world, target);
      }
   }

   private void startMining(LocalPlayer self, ClientLevel world, BlockPos pos, Direction face, double damage) {
      if (this.excavationWorld!=world) { this.excavationSpots.clear();this.excavationWorld=world; }
      this.excavationSpots.add(pos.immutable());
      if (this.excavationSpots.size()>128) this.excavationSpots.removeFirst();
      this.minePos = pos.immutable();
      this.mineFace = face;
      this.mineDamage = damage;
      this.mineTicks = 0;
      this.mineLookWait = 0;
      this.toolSwapWait = 0;
      int est = this.breakTicks(self, world, pos, world.getBlockState(pos));
      this.mineTimeout = est == Integer.MAX_VALUE ? 200 : Math.min(600, est * 6 + 20);
   }

   private boolean tickMining(LocalPlayer self, ClientLevel world, LivingEntity target) {
      BlockPos pos = this.minePos;
      BlockState state = world.getBlockState(pos);
      if (state.isAir()) {
         this.finishMining();
         return false;
      }

      double reach = self.blockInteractionRange() + 1.0D;
      if (this.selfHole && !pos.equals(self.blockPosition().below())) {
         this.abortMining();
         return false;
      }
      if (state.liquid()
         || state.getDestroySpeed(world, pos) < 0.0F
         || target == null
         || !target.isAlive()
         || target.distanceToSqr(Vec3.atCenterOf(pos)) > MINE_ABORT_TARGET_DIST * MINE_ABORT_TARGET_DIST
         || new AABB(pos).distanceToSqr(self.getEyePosition()) > reach * reach
         || ++this.mineTicks > this.mineTimeout) {
         this.abortMining();
         return false;
      }

      if (!this.selfHole && this.remainingDigTicks(self, world, pos, state) > 1 && this.anchorClears(pos) && AnchorAura.requestClear(pos)) {
         DestroyHold.release(this);
         this.lastData = new PlaceData(pos, this.mineFace, this.mineDamage, PlaceResult.BREAK, null);
         return true;
      }

      if (this.toolSwapWait > 0) this.toolSwapWait--;
      CombatSync.claim(this, CombatSync.PRIORITY_CRYSTAL, 2, true);
      boolean legit = this.legitMine.isEnabled();
      AutoTool autoTool = legit ? null : this.autoTool();
      if (legit || autoTool == null || !autoTool.isEnabled()) {
         int slot = this.ensureToolInHotbar(self, world, pos, state);
         if (slot == -1) {
            this.abortMining();
            return false;
         }
         if (slot == -2) {
            return true;
         }
         CombatSlots.select(this, slot, 3);
      } else {
         if (!this.canBreak(self, world, pos, state)) {
            this.abortMining();
            return false;
         }
         autoTool.swapForBlock(state);
      }

      Direction face = this.mineFace;
      if (legit) {
         Direction visible = this.legitFace(pos, self, world);
         if (visible != null) face = visible;
      }
      this.mineFace = face;
      this.lastData = new PlaceData(pos, face, this.mineDamage, PlaceResult.BREAK, null);
      float[] rot = this.calculate(legit ? this.faceAimPoint(pos, face) : this.getFacePoint(pos, face), self);
      this.rotation = rot;
      boolean vanillaRot = this.rotate.is("Vanilla");
      if (vanillaRot) {
         this.setSilent(rot[0], rot[1]);
      }

      if (legit && vanillaRot) {
         BlockHitResult hit = this.lookHit(pos, self, world);
         if (hit == null) {
            if (++this.mineLookWait > MINE_LOOK_WAIT) this.abortMining();
            return true;
         }
         this.mineLookWait = 0;
         face = hit.getDirection();
      }

      DestroyHold.hold(this);
      if (mc.gameMode.continueDestroyBlock(pos, face)) {
         self.swing(InteractionHand.MAIN_HAND);
      }
      if (!this.hasVisual(pos)) {
         this.visualDatas.add(new VisualData(pos, System.currentTimeMillis(), 0.8F, 0.1F, 0.1F));
      }
      return true;
   }

   private void finishMining() {
      DestroyHold.release(this);
      CombatSync.free(this);
      this.selfHole = false;
      this.minePos = null;
      this.mineFace = null;
      this.lastData = null;
   }

   private void abortMining() {
      if (this.minePos == null) return;

      DestroyHold.release(this);
      CombatSync.free(this);
      this.selfHole = false;
      this.minePos = null;
      this.mineFace = null;
      this.lastData = null;
      if (mc.gameMode != null && mc.gameMode.isDestroying()) {
         try {
            mc.gameMode.stopDestroyBlock();
         } catch (Throwable ignored) {
         }
      }
      this.releaseRotation();
   }

   private int ensureToolInHotbar(LocalPlayer self, ClientLevel world, BlockPos pos, BlockState state) {
      int selected = self.getInventory().getSelectedSlot();
      if (state.getDestroySpeed(world, pos) == 0.0F) {
         return selected;
      }
      int slot = this.toolSlot(self, world, pos, state);
      if (slot == -1) return this.canBreak(self, world, pos, state) ? selected : -1;
      if (slot < 9) return slot;
      if (this.toolSwapWait > 0) return -2;
      ItemStack tool = self.getInventory().getItem(slot).copy();
      int hotbar = this.hotbarSlotForTool(self);
      InventoryUtility.swap(slot, hotbar + 36);
      this.toolSwapWait = 4;
      return ItemStack.isSameItemSameComponents(self.getInventory().getItem(hotbar), tool) ? hotbar : -2;
   }

   private int hotbarSlotForTool(LocalPlayer self) {
      int selected = self.getInventory().getSelectedSlot();
      for (int i = 0; i < 9; i++) {
         if (self.getInventory().getItem(i).isEmpty()) return i;
      }
      for (int i = 8; i >= 0; i--) {
         ItemStack stack = self.getInventory().getItem(i);
         if (i != selected && !isImportantHotbarItem(stack) && !stack.has(DataComponents.TOOL)) return i;
      }
      for (int i = 8; i >= 0; i--) {
         if (i != selected && !isImportantHotbarItem(self.getInventory().getItem(i))) return i;
      }
      return selected;
   }

   private static boolean isImportantHotbarItem(ItemStack stack) {
      Item item = stack.getItem();
      return item == Items.OBSIDIAN
         || item == Items.END_CRYSTAL
         || item == Items.COBWEB
         || item == Items.TOTEM_OF_UNDYING
         || item == Items.GOLDEN_APPLE
         || item == Items.ENCHANTED_GOLDEN_APPLE
         || item == Items.ENDER_PEARL;
   }

   private int toolSlot(LocalPlayer self, ClientLevel world, BlockPos pos, BlockState state) {
      float hardness = state.getDestroySpeed(world, pos);
      int best = -1;
      long bestCost = Long.MAX_VALUE;
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = self.getInventory().getItem(slot);
         if (!isDigTool(stack) || stack.getDestroySpeed(state) <= 1.0F) continue;
         int ticks = this.ticksWith(self, world, state, hardness, stack);
         if (ticks == Integer.MAX_VALUE) continue;
         long cost = ticks + (slot >= 9 ? TOOL_SWAP_TICKS : 0);
         if (cost < bestCost) {
            bestCost = cost;
            best = slot;
         }
      }
      return best;
   }

   private boolean canBreak(LocalPlayer self, ClientLevel world, BlockPos pos, BlockState state) {
      float hardness = state.getDestroySpeed(world, pos);
      if (hardness < 0.0F || state.liquid()) return false;
      return hardness == 0.0F || this.toolSlot(self, world, pos, state) != -1;
   }

   private static boolean isDigTool(ItemStack stack) {
      return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS);
   }

   private boolean anchorClears(BlockPos pos) {
      return this.autoKrot.isEnabled() && this.anchorClear.isEnabled() && AnchorAura.canClear(pos);
   }

   private int remainingDigTicks(LocalPlayer self, ClientLevel world, BlockPos pos, BlockState state) {
      int total = this.breakTicks(self, world, pos, state);
      if (total == Integer.MAX_VALUE || !mc.gameMode.isDestroying()) return total;

      float progress = ((MultiPlayerGameModeAccessor)mc.gameMode).getDestroyProgress();
      return (int)Math.ceil(total * (1.0F - Math.min(1.0F, progress)));
   }

   private int maxDigTicks() {
      return this.legitMine.isEnabled() ? Math.round(this.maxBreakTicks.getValue()) : MAX_DIG_TICKS;
   }

   private int breakTicks(LocalPlayer self, ClientLevel world, BlockPos pos, BlockState state) {
      float hardness = state.getDestroySpeed(world, pos);
      if (hardness < 0.0F) return Integer.MAX_VALUE;
      if (hardness == 0.0F) return 1;
      int slot = this.toolSlot(self, world, pos, state);
      return this.ticksWith(self, world, state, hardness, slot == -1 ? ItemStack.EMPTY : self.getInventory().getItem(slot));
   }

   private int ticksWith(LocalPlayer self, ClientLevel world, BlockState state, float hardness, ItemStack tool) {
      if (hardness < 0.0F) return Integer.MAX_VALUE;
      if (hardness == 0.0F) return 1;
      float speed = ToolSpeed.speed(tool, state, world);
      if (MobEffectUtil.hasDigSpeed(self)) {
         speed *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(self) + 1) * 0.2F;
      }
      MobEffectInstance fatigue = self.getEffect(MobEffects.MINING_FATIGUE);
      if (fatigue != null) {
         speed *= (float) Math.pow(0.3D, Math.min(4, fatigue.getAmplifier() + 1));
      }
      boolean correct = !state.requiresCorrectToolForDrops() || !tool.isEmpty() && tool.isCorrectToolForDrops(state);
      float perTick = speed / hardness / (correct ? 30.0F : 100.0F);
      return perTick <= 0.0F ? Integer.MAX_VALUE : (int) Math.ceil(1.0F / perTick);
   }

   private Direction breakFace(BlockPos pos, LocalPlayer self, ClientLevel world) {
      if (this.legitMine.isEnabled()) return this.legitFace(pos, self, world);
      if (this.mineThroughWalls.isEnabled()) return this.closestFace(pos, self);
      Direction face = this.getBestVisibleFace(pos, self);
      if (face == null) return null;
      BlockHitResult raycast = this.raycastBox(this.calculate(this.getFacePoint(pos, face), self), pos, 5.5F, self);
      return raycast != null && raycast.getDirection() == face ? face : null;
   }

   private Direction legitFace(BlockPos pos, LocalPlayer self, ClientLevel world) {
      Vec3 eye = self.getEyePosition();
      double range = self.blockInteractionRange();
      Direction best = null;
      double bestDist = Double.MAX_VALUE;
      for (Direction face : Direction.values()) {
         Vec3 point = this.faceAimPoint(pos, face);
         double dist = eye.distanceToSqr(point);
         if (dist > range * range || dist >= bestDist) continue;
         BlockHitResult hit = world.clip(new ClipContext(eye, point, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, self));
         if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) && hit.getDirection() == face) {
            best = face;
            bestDist = dist;
         }
      }
      return best;
   }

   private BlockHitResult lookHit(BlockPos pos, LocalPlayer self, ClientLevel world) {
      Vec3 eye = self.getEyePosition();
      RotationController c = RotationController.INSTANCE;
      Vec3 dir = this.rotate.is("Vanilla") && c != null ? c.getServerAngle().toVector() : this.lookVec(self);
      Vec3 end = eye.add(dir.scale(self.blockInteractionRange()));
      BlockHitResult hit = world.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, self));
      return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos) ? hit : null;
   }

   private Vec3 faceAimPoint(BlockPos pos, Direction face) {
      return new Vec3(pos.getX() + 0.5 + face.getStepX() * 0.49, pos.getY() + 0.5 + face.getStepY() * 0.49, pos.getZ() + 0.5 + face.getStepZ() * 0.49);
   }

   private static boolean isCrystalBase(BlockState state) {
      return state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.BEDROCK;
   }

   private boolean launchHit(LocalPlayer self, LivingEntity target) {
      long now = System.currentTimeMillis();
      if (now < this.launchBlockedUntil) return false;
      if (!target.onGround() || target.hurtTime > 0 || target.isBlocking()) return false;
      if (CombatTarget.current() == target) return false;
      Vec3 aim = MultipointUtil.getNearestPoint(target, 0.0D, 0.05D);
      if (aim.distanceToSqr(self.getEyePosition()) > 9.0D) {
         this.launchAimTicks = 0;
         return false;
      }
      if (!this.canPopCrystals(self) && attackDamage(self, self.getMainHandItem()) <= 0.0D) return false;
      float[] rot = this.calculate(aim, self);
      CombatSync.claim(this, CombatSync.PRIORITY_CRYSTAL, 2, false);
      this.rotation = rot;
      if (this.rotate.is("Vanilla")) {
         this.setSilent(rot[0], rot[1]);
         Angle sent = RotationController.INSTANCE != null ? RotationController.INSTANCE.getServerAngle() : null;
         if (sent == null || !this.rayTrace(new float[]{sent.getYaw(), sent.getPitch()}, 3.0D, target.getBoundingBox(), self)) {
            if (++this.launchAimTicks > 6) {
               this.launchAimTicks = 0;
               this.launchBlockedUntil = now + 1000L;
            }
            return true;
         }
      }
      if (!this.prepareAttack(self)) return false;
      this.launchAimTicks = 0;
      mc.gameMode.attack(self, target);
      self.swing(InteractionHand.MAIN_HAND);
      this.launchBlockedUntil = now + 600L;
      return true;
   }

   private AttackAura attackAura() {
      try {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         if (cc != null && cc.getModuleManager() != null) return cc.getModuleManager().get(AttackAura.class);
      } catch (Throwable ignored) {
      }
      return null;
   }

   private AutoTool autoTool() {
      try {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         if (cc != null && cc.getModuleManager() != null) return cc.getModuleManager().get(AutoTool.class);
      } catch (Throwable ignored) {
      }
      return null;
   }

   private void setSilent(float yaw, float pitch) {
      this.rotationPriority = ROTATION_PRIORITY;
      this.setSilent(yaw, pitch, ROTATION_PRIORITY);
   }

   private void setSilent(float yaw, float pitch, int priority) {
      RotationController c = RotationController.INSTANCE;
      if (c != null && mc.player != null) {
         c.rotateTo(this.rotationPlan(yaw, pitch), priority, this);
         c.update();
      }
   }

   private RotationPlan rotationPlan(float yaw, float pitch) {
      Angle a = new Angle(yaw, pitch);
      Vec3 target = a.toVector().add(mc.player.getEyePosition());
      return new RotationPlan(a, target, null, new GrimSmoothMode(true), 3, 3.0F, true, true);
   }

   private void releaseRotation() {
      this.rotation = null;
      this.rotationPriority = ROTATION_PRIORITY;
      if (RotationController.INSTANCE != null) {
         RotationController.INSTANCE.release(this);
      }
   }

   @EventHandler
   public void onTrace(EventTrace e) {
      RotationController c = RotationController.INSTANCE;
      if (c != null && c.isActive() && this.isEnabled() && (this.lastData != null || this.pendingPlaceHit != null || this.rotation != null)) {
         Angle rot = c.getRotation();
         e.setYaw(rot.getYaw());
         e.setPitch(rot.getPitch());
      }
   }

   private LivingEntity findTarget(LocalPlayer self, ClientLevel world) {
      LivingEntity best = null;
      double bestDist = 8.0D * 8.0D;
      for (Entity e : world.entitiesForRendering()) {
         if (!(e instanceof LivingEntity living)) continue;
         double d = self.distanceToSqr(living);
         if (d > bestDist) continue;
         if (!CombatTargets.accepts(this.targets, living)) continue;
         bestDist = d;
         best = living;
      }
      return best;
   }

   private PlaceData findBestObsidian(LivingEntity entity, ClientLevel world, LocalPlayer self, List<Entity> entities) {
      BlockPos bestPanPos = null;
      double bestPanDamage = 0;
      Direction bestFace = null;
      BlockPos basePos = self.blockPosition();
      for (int x = -5; x <= 5; x++) {
         for (int y = -5; y <= 5; y++) {
            for (int z = -5; z <= 5; z++) {
               BlockPos checkPos = basePos.offset(x, y, z);
               BlockState state = world.getBlockState(checkPos);
               if (!state.getBlock().defaultBlockState().isSolid()) continue;
               if (BlockClick.isInteractable(world, checkPos, state)) continue;
               for (Direction face : Direction.values()) {
                  BlockPos placePos = checkPos.relative(face);
                  if (placePos.getY() >= Mth.floor(entity.getY())) continue;
                  if (!world.getBlockState(placePos).isAir()) continue;
                  BlockPos crystalPos = placePos.above();
                  if (!world.getBlockState(crystalPos).isAir()) continue;
                  float[] rot = this.calculate(this.getFacePoint(checkPos, face), self);
                  BlockHitResult raycast = this.raycastBox(rot, checkPos, this.placeRange(), self);
                  if (raycast == null || raycast.getDirection() != face) continue;
                  if (this.blockedObsidian(entities, placePos) == ReturnResult.PLAYER) continue;
                  if (this.blockedObsidian(entities, crystalPos) == ReturnResult.PLAYER) continue;
                  if (!this.canReachBox(crystalBox(placePos), GRIM_HIT_REACH, self)) continue;
                  double damage = this.ghostDamage(Vec3.atBottomCenterOf(crystalPos), entity, placePos, world);
                  double score = damage + 19 - 6;
                  if (damage < this.minDamage(entity) || score <= bestPanDamage) continue;
                  double selfDamage = this.ghostDamage(Vec3.atBottomCenterOf(crystalPos), self, placePos, world);
                  if (!this.selfDamageAllowed(self, selfDamage, damage) || this.lootAt(Vec3.atBottomCenterOf(crystalPos))) continue;
                  if (this.blockedObsidian(entities, crystalPos) == ReturnResult.ITEM) continue;
                  bestPanDamage = score;
                  bestPanPos = checkPos;
                  bestFace = face;
               }
            }
         }
      }
      BlockPos bestCrystalPos = null;
      Direction bestCrystalFace = null;
      EndCrystal bestCrystal = null;
      double bestCrystalDamage = 0;
      for (Entity entity1 : entities) {
         if (!(entity1 instanceof EndCrystal crystal)) continue;
         double targetDamage = this.explosionDamage(crystal.position(), entity, world);
         double damage = targetDamage + 30;
         if (damage <= bestCrystalDamage) continue;
         double selfDamage = this.explosionDamage(crystal.position(), self, world);
         if (!this.selfDamageAllowed(self, selfDamage, targetDamage) || this.lootAt(crystal.position())) continue;
         if (targetDamage <= 0.0D && selfDamage > NEGLIGIBLE_SELF_DAMAGE) continue;
         BlockPos pos = crystal.blockPosition().below();
         Direction face = Direction.UP;
         float[] rotBlock = this.calculate(this.getFacePoint(pos, face), self);
         BlockHitResult raycast = this.raycastBox(rotBlock, pos, 5.5F, self);
         boolean viaBase = raycast != null && raycast.getDirection() == face && this.rayTrace(rotBlock, GRIM_HIT_REACH, crystal.getBoundingBox(), self);
         float[] rot = this.calculate(MultipointUtil.getNearestPoint(crystal, 0.0D, 0.05D), self);
         if (!viaBase && !this.isLookingAtEntity(rot, crystal, GRIM_HIT_REACH, self)) continue;
         bestCrystalPos = viaBase ? pos : null;
         bestCrystalFace = viaBase ? face : null;
         bestCrystalDamage = damage;
         bestCrystal = crystal;
      }
      if (bestCrystalDamage > bestPanDamage) {
         return new PlaceData(bestCrystalPos, bestCrystalFace, bestCrystalDamage, PlaceResult.EXPLOSION, bestCrystal);
      }
      return new PlaceData(bestPanPos, bestFace, bestPanDamage, PlaceResult.PLACE_OBSIDIAN, null);
   }

   private PlaceData findBestCrystal(LivingEntity entity, ClientLevel world, LocalPlayer self, List<Entity> entities) {
      BlockPos bestPanPos = null;
      double bestPanDamage = 0;
      Direction bestFace = null;
      BlockPos basePos = self.blockPosition();
      for (int x = -5; x <= 5; x++) {
         for (int y = -5; y <= 5; y++) {
            for (int z = -5; z <= 5; z++) {
               BlockPos checkPos = basePos.offset(x, y, z);
               if (checkPos.getY() >= Mth.floor(entity.getY())) continue;
               BlockState state = world.getBlockState(checkPos);
               if (state.getBlock() != Blocks.OBSIDIAN && state.getBlock() != Blocks.BEDROCK) continue;
               Direction face = this.getBestVisibleFace(checkPos, self);
               if (face == null) continue;
               BlockPos placePos = checkPos.above();
               if (!world.getBlockState(placePos).isAir()) continue;
               float[] rot = this.calculate(this.getFacePoint(checkPos, face), self);
               BlockHitResult raycast = this.raycastBox(rot, checkPos, this.placeRange(), self);
               if (raycast == null || raycast.getDirection() != face) continue;
               float[] rot2 = this.calculate(this.getFacePoint(checkPos, Direction.UP), self);
               BlockHitResult raycast2 = this.raycastBox(rot2, checkPos, this.placeRange(), self);
               if (raycast2 != null && raycast2.getDirection() == Direction.UP) face = Direction.UP;
               if (this.blockedCrystal(entities, placePos)) continue;
               if (!this.canReachBox(crystalBox(placePos), GRIM_HIT_REACH, self)) continue;
               double targetDamage = this.explosionDamage(Vec3.atBottomCenterOf(placePos), entity, world);
               double damage = targetDamage + 19;
               if (targetDamage < this.minDamage(entity) || damage <= bestPanDamage) continue;
               double selfDamage = this.explosionDamage(Vec3.atBottomCenterOf(placePos), self, world);
               if (!this.selfDamageAllowed(self, selfDamage, targetDamage) || this.lootAt(Vec3.atBottomCenterOf(placePos))) continue;
               bestPanDamage = damage;
               bestPanPos = checkPos;
               bestFace = face;
            }
         }
      }
      return new PlaceData(bestPanPos, bestFace, bestPanDamage, PlaceResult.PLACE_CRYSTAL, null);
   }

   private PlaceData findBestBreak(LivingEntity entity, ClientLevel world, LocalPlayer self, List<Entity> entities, boolean hasObsidian) {
      BlockPos bestPos = null;
      Direction bestFace = null;
      double bestDamage = 0;
      double bestRank = -Double.MAX_VALUE;
      BlockPos basePos = self.blockPosition();
      for (int x = -5; x <= 5; x++) {
         for (int y = -5; y <= 5; y++) {
            for (int z = -5; z <= 5; z++) {
               BlockPos checkPos = basePos.offset(x, y, z);
               boolean nearSelf = this.selfMine.isEnabled()
                  && Math.abs(checkPos.getX() - self.getX()) < 2.5D
                  && Math.abs(checkPos.getZ() - self.getZ()) < 2.5D
                  && checkPos.getY() <= self.blockPosition().getY() + 1;
               if (checkPos.getY() >= Mth.floor(entity.getY()) && !nearSelf) continue;
               BlockState base = world.getBlockState(checkPos);
               if (!base.getBlock().defaultBlockState().isSolid()) continue;
               BlockPos placePos = checkPos.above();
               if (this.blockedBreak(entities, placePos)) continue;
               if (!this.canReachBox(crystalBox(placePos), GRIM_HIT_REACH, self)) continue;
               BlockState above = world.getBlockState(placePos);
               boolean clearTop = !above.isAir();
               BlockPos breakPos = clearTop ? placePos : checkPos;
               BlockState breakState = clearTop ? above : base;
               if (!clearTop && isCrystalBase(base)) continue;
               if (!isCrystalBase(base)) {
                  if (!hasObsidian || clearTop) continue;
                  if (checkPos.getY() >= Mth.floor(entity.getY()) || checkPos.getY() >= self.blockPosition().getY()) continue;
               }
               int ticks = this.breakTicks(self, world, breakPos, breakState);
               boolean viaAnchor = this.anchorClears(breakPos);
               if (!viaAnchor && (!this.canBreak(self, world, breakPos, breakState) || ticks > this.maxDigTicks())) continue;
               int total = viaAnchor ? ANCHOR_CLEAR_COST : this.digCost(self, world, breakPos, breakState, ticks);
               if (!isCrystalBase(base)) {
                  total += OBSIDIAN_EXTRA_TICKS;
               }
               double penalty = TIME_WEIGHT * total;
               if (40.0D - penalty <= bestRank) continue;
               Direction face = this.breakFace(breakPos, self, world);
               if (face == null && viaAnchor) face = this.closestFace(breakPos, self);
               if (face == null) continue;
               double targetDamage = clearTop
                  ? this.ignoreDamage(Vec3.atBottomCenterOf(placePos), entity, placePos, world)
                  : this.explosionDamage(Vec3.atBottomCenterOf(placePos), entity, world);
               double rank = targetDamage - penalty;
               if (targetDamage < this.minDamage(entity) || rank <= bestRank) continue;
               double selfDamage = clearTop
                  ? this.ignoreDamage(Vec3.atBottomCenterOf(placePos), self, placePos, world)
                  : this.explosionDamage(Vec3.atBottomCenterOf(placePos), self, world);
               if (!this.selfDamageAllowed(self, selfDamage, targetDamage) || this.lootAt(Vec3.atBottomCenterOf(placePos))) continue;
               bestRank = rank;
               bestPos = breakPos;
               bestFace = face;
               bestDamage = Math.max(0.5D, targetDamage + 19 - 12 - Math.min(6.0D, penalty));
            }
         }
      }
      return new PlaceData(bestPos, bestFace, bestPos == null ? 0 : bestDamage, PlaceResult.BREAK, null);
   }

   private int digCost(LocalPlayer self, ClientLevel world, BlockPos pos, BlockState state, int ticks) {
      int slot = state.getDestroySpeed(world, pos) == 0.0F ? -1 : this.toolSlot(self, world, pos, state);
      return ticks + (slot >= 9 ? TOOL_SWAP_TICKS : 0);
   }

   private boolean selfDamageAllowed(LocalPlayer self, double selfDamage, double targetDamage) {
      if (!this.safeYourSelf.isEnabled()) return selfDamage <= MAX_SELF_DAMAGE_UNSAFE;
      if (selfDamage <= NEGLIGIBLE_SELF_DAMAGE) return true;
      if (selfDamage > this.maxSelfDamage.getValue()) return false;
      double health = self.getHealth() + self.getAbsorptionAmount();
      if (health - selfDamage < this.minHealthLeft.getValue()) return false;
      return targetDamage >= selfDamage;
   }

   private ReturnResult blockedObsidian(List<Entity> entities, BlockPos crystalPos) {
      for (Entity entity : entities) {
         AABB blockBox = new AABB(crystalPos.getX(), crystalPos.getY() - 0.2F, crystalPos.getZ(), crystalPos.getX() + 1.0, crystalPos.getY() + 2.2, crystalPos.getZ() + 1.0);
         if (entity instanceof ItemEntity && !entity.onGround() && entity.getBoundingBox().intersects(blockBox)) return ReturnResult.ITEM;
         if ((entity.blocksBuilding || entity instanceof EndCrystal) && this.intersectsBox(crystalPos, entity)) return ReturnResult.PLAYER;
      }
      return ReturnResult.NONE;
   }

   private boolean blockedCrystal(List<Entity> entities, BlockPos placePos) {
      for (Entity entity : entities) {
         if (this.intersectsBox(placePos, entity)) return true;
      }
      return false;
   }

   private boolean blockedBreak(List<Entity> entities, BlockPos placePos) {
      for (Entity entity : entities) {
         if (this.intersectsBox(placePos, entity) && !(entity instanceof ItemEntity)) return true;
      }
      return false;
   }

   private boolean intersectsBox(BlockPos pos, Entity entity) {
      AABB crystalBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
      return crystalBox.intersects(entity.getBoundingBox());
   }

   private boolean hasVisual(BlockPos blockPos) {
      for (VisualData visualData : this.visualDatas) {
         if (visualData.blockPos.equals(blockPos)) return true;
      }
      return false;
   }

   private float[] calculate(Vec3 target, LocalPlayer self) {
      Vec3 eye = self.getEyePosition();
      Vec3 delta = target.subtract(eye);
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
      float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, horizontal));
      return new float[]{yaw, pitch};
   }

   private Vec3 getFacePoint(BlockPos pos, Direction face) {
      return new Vec3(pos.getX() + 0.5 + face.getStepX() * 0.5001, pos.getY() + 0.5 + face.getStepY() * 0.5001, pos.getZ() + 0.5 + face.getStepZ() * 0.5001);
   }

   private Vec3 lookVec(LocalPlayer self) {
      RotationController c = RotationController.INSTANCE;
      if (this.rotate.is("Vanilla") && c != null && c.isActive()) {
         return c.getRotation().toVector();
      }
      Camera cam = mc.gameRenderer.mainCamera();
      if (cam != null) return Vec3.directionFromRotation(cam.xRot(), cam.yRot());
      return Vec3.directionFromRotation(self.getXRot(), self.getYRot());
   }

   private BlockHitResult raycastBox(float[] rot, BlockPos targetPos, double maxDistance, LocalPlayer self) {
      Vec3 eye = self.getEyePosition();
      Vec3 dir = Vec3.directionFromRotation(rot[1], rot[0]);
      Vec3 end = eye.add(dir.scale(maxDistance));
      AABB blockBox = new AABB(targetPos);
      Optional<Vec3> hitOpt = blockBox.clip(eye, end);
      if (hitOpt.isEmpty()) return null;
      Vec3 hitVec = hitOpt.get();
      return new BlockHitResult(hitVec, this.getHitSide(hitVec, targetPos), targetPos, false);
   }

   private BlockHitResult raycastBoxLook(BlockPos targetPos, double maxDistance, LocalPlayer self) {
      Vec3 eye = self.getEyePosition();
      Vec3 dir = this.lookVec(self);
      Vec3 end = eye.add(dir.scale(maxDistance));
      AABB blockBox = new AABB(targetPos);
      Optional<Vec3> hitOpt = blockBox.clip(eye, end);
      if (hitOpt.isEmpty()) return null;
      Vec3 hitVec = hitOpt.get();
      return new BlockHitResult(hitVec, this.getHitSide(hitVec, targetPos), targetPos, false);
   }

   private Direction getHitSide(Vec3 hitVec, BlockPos pos) {
      double dx = hitVec.x - (pos.getX() + 0.5);
      double dy = hitVec.y - (pos.getY() + 0.5);
      double dz = hitVec.z - (pos.getZ() + 0.5);
      double absDx = Math.abs(dx);
      double absDy = Math.abs(dy);
      double absDz = Math.abs(dz);
      if (absDx > absDy && absDx > absDz) return dx > 0 ? Direction.EAST : Direction.WEST;
      else if (absDy > absDz) return dy > 0 ? Direction.UP : Direction.DOWN;
      else return dz > 0 ? Direction.SOUTH : Direction.NORTH;
   }

   private Direction getBestVisibleFace(BlockPos pos, LocalPlayer self) {
      Vec3 eye = self.getEyePosition();
      Direction best = null;
      double bestDist = Double.MAX_VALUE;
      for (Direction face : Direction.values()) {
         float[] rot = this.calculate(this.getFacePoint(pos, face), self);
         if (this.raycastBox(rot, pos, this.placeRange(), self) == null) continue;
         double dist = eye.distanceToSqr(this.getFacePoint(pos, face));
         if (dist < bestDist) {
            bestDist = dist;
            best = face;
         }
      }
      return best;
   }

   private Direction closestFace(BlockPos pos, LocalPlayer self) {
      Vec3 eye = self.getEyePosition();
      Direction best = null;
      double bestDist = Double.MAX_VALUE;
      for (Direction face : Direction.values()) {
         double dist = eye.distanceToSqr(this.getFacePoint(pos, face));
         if (dist < bestDist) {
            bestDist = dist;
            best = face;
         }
      }
      return best;
   }

   private boolean rayTrace(float[] rot, double reach, AABB box, LocalPlayer self) {
      Vec3 eye = self.getEyePosition();
      Vec3 dir = Vec3.directionFromRotation(rot[1], rot[0]);
      Vec3 end = eye.add(dir.scale(reach));
      return box.contains(eye) || box.clip(eye, end).isPresent();
   }

   private boolean isLookingAtEntity(float[] rot, Entity entity, double reach, LocalPlayer self) {
      return this.rayTrace(rot, reach, entity.getBoundingBox(), self);
   }

   private static AABB crystalBox(BlockPos placePos) {
      return new AABB(placePos.getX() - 0.5, placePos.getY(), placePos.getZ() - 0.5, placePos.getX() + 1.5, placePos.getY() + 2.0, placePos.getZ() + 1.5);
   }

   private boolean canReachBox(AABB box, double reach, LocalPlayer self) {
      return box.distanceToSqr(self.getEyePosition()) <= reach * reach;
   }

   private Direction getFaceDirection(float[] rot, LocalPlayer self, BlockPos targetPos) {
      Vec3 eyePos = self.getEyePosition();
      Vec3 lookVec = Vec3.directionFromRotation(rot[1], rot[0]);
      double minX = targetPos.getX();
      double minY = targetPos.getY();
      double minZ = targetPos.getZ();
      double maxX = targetPos.getX() + 1.0;
      double maxY = targetPos.getY() + 1.0;
      double maxZ = targetPos.getZ() + 1.0;
      Direction closestFace = null;
      double closestT = Double.MAX_VALUE;
      double t = tryHitPlaneY(eyePos, lookVec, maxY, minX, maxX, minZ, maxZ);
      if (t > 0 && t < closestT) { closestT = t; closestFace = Direction.UP; }
      t = tryHitPlaneY(eyePos, lookVec, minY, minX, maxX, minZ, maxZ);
      if (t > 0 && t < closestT) { closestT = t; closestFace = Direction.DOWN; }
      t = tryHitPlaneZ(eyePos, lookVec, minZ, minX, maxX, minY, maxY);
      if (t > 0 && t < closestT) { closestT = t; closestFace = Direction.NORTH; }
      t = tryHitPlaneZ(eyePos, lookVec, maxZ, minX, maxX, minY, maxY);
      if (t > 0 && t < closestT) { closestT = t; closestFace = Direction.SOUTH; }
      t = tryHitPlaneX(eyePos, lookVec, minX, minY, maxY, minZ, maxZ);
      if (t > 0 && t < closestT) { closestT = t; closestFace = Direction.WEST; }
      t = tryHitPlaneX(eyePos, lookVec, maxX, minY, maxY, minZ, maxZ);
      if (t > 0 && t < closestT) { closestT = t; closestFace = Direction.EAST; }
      if (closestFace == null) {
         double cx = targetPos.getX() + 0.5;
         double cy = targetPos.getY() + 0.5;
         double cz = targetPos.getZ() + 0.5;
         closestFace = fallbackDirection(eyePos, cx, cy, cz);
      }
      return closestFace;
   }

   private static double tryHitPlaneY(Vec3 eye, Vec3 dir, double yVal, double minX, double maxX, double minZ, double maxZ) {
      if (dir.y == 0) return -1;
      double t = (yVal - eye.y) / dir.y;
      if (t <= 0) return -1;
      double hitX = eye.x + t * dir.x;
      double hitZ = eye.z + t * dir.z;
      if (hitX >= minX && hitX <= maxX && hitZ >= minZ && hitZ <= maxZ) return t;
      return -1;
   }

   private static double tryHitPlaneZ(Vec3 eye, Vec3 dir, double zVal, double minX, double maxX, double minY, double maxY) {
      if (dir.z == 0) return -1;
      double t = (zVal - eye.z) / dir.z;
      if (t <= 0) return -1;
      double hitX = eye.x + t * dir.x;
      double hitY = eye.y + t * dir.y;
      if (hitX >= minX && hitX <= maxX && hitY >= minY && hitY <= maxY) return t;
      return -1;
   }

   private static double tryHitPlaneX(Vec3 eye, Vec3 dir, double xVal, double minY, double maxY, double minZ, double maxZ) {
      if (dir.x == 0) return -1;
      double t = (xVal - eye.x) / dir.x;
      if (t <= 0) return -1;
      double hitY = eye.y + t * dir.y;
      double hitZ = eye.z + t * dir.z;
      if (hitY >= minY && hitY <= maxY && hitZ >= minZ && hitZ <= maxZ) return t;
      return -1;
   }

   private static Direction fallbackDirection(Vec3 eye, double cx, double cy, double cz) {
      double dx = eye.x - cx;
      double dy = eye.y - cy;
      double dz = eye.z - cz;
      double ax = Math.abs(dx);
      double ay = Math.abs(dy);
      double az = Math.abs(dz);
      if (ay >= ax && ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
      else if (ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
      else return dz > 0 ? Direction.SOUTH : Direction.NORTH;
   }

   private int findHotbar(LocalPlayer self, net.minecraft.world.item.Item item) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = self.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getItem() == item) return i;
      }
      return -1;
   }

   private static final double DIAMETER = 12.0D;

   private double explosionDamage(Vec3 source, LivingEntity target, ClientLevel world) {
      if (world.getDifficulty() == Difficulty.PEACEFUL || target == null) return 0;
      if (!new AABB(Mth.floor(source.x - 11.0D), Mth.floor(source.y - 11.0D), Mth.floor(source.z - 11.0D), Mth.floor(source.x + 13.0D), Mth.floor(source.y + 13.0D), Mth.floor(source.z + 13.0D)).intersects(target.getBoundingBox())) return 0;
      if (target.isInvulnerable()) return 0;
      double distExposure = Math.sqrt(target.position().distanceToSqr(source)) / DIAMETER;
      if (distExposure > 1.0D) return 0;
      double finalExposure = (1.0D - distExposure) * this.exposureOverride(source, target.getBoundingBox(), world, null, null);
      float toDamage = (float) ((finalExposure * finalExposure + finalExposure) / 2.0D * 7.0D * DIAMETER + 1.0D);
      return this.reductions(toDamage, target, world);
   }

   private double ghostDamage(Vec3 source, LivingEntity target, BlockPos ghost, ClientLevel world) {
      if (world.getDifficulty() == Difficulty.PEACEFUL) return 0;
      if (!new AABB(Mth.floor(source.x - DIAMETER - 1.0D), Mth.floor(source.y - DIAMETER - 1.0D), Mth.floor(source.z - DIAMETER - 1.0D), Mth.floor(source.x + DIAMETER + 1.0D), Mth.floor(source.y + DIAMETER + 1.0D), Mth.floor(source.z + DIAMETER + 1.0D)).intersects(target.getBoundingBox())) return 0;
      if (target.isInvulnerable()) return 0;
      double distExposure = Math.sqrt(target.position().distanceToSqr(source)) / DIAMETER;
      if (distExposure > 1.0D) return 0;
      double finalExposure = (1.0D - distExposure) * this.exposureOverride(source, target.getBoundingBox(), world, ghost, null);
      float toDamage = (float) ((finalExposure * finalExposure + finalExposure) / 2.0D * 7.0D * DIAMETER + 1.0D);
      return this.reductions(toDamage, target, world);
   }

   private double ignoreDamage(Vec3 source, LivingEntity target, BlockPos ignore, ClientLevel world) {
      if (world.getDifficulty() == Difficulty.PEACEFUL || target == null || ignore == null) return 0;
      if (!new AABB(Mth.floor(source.x - DIAMETER - 1.0D), Mth.floor(source.y - DIAMETER - 1.0D), Mth.floor(source.z - DIAMETER - 1.0D), Mth.floor(source.x + DIAMETER + 1.0D), Mth.floor(source.y + DIAMETER + 1.0D), Mth.floor(source.z + DIAMETER + 1.0D)).intersects(target.getBoundingBox())) return 0;
      if (target.isInvulnerable()) return 0;
      double distExposure = Math.sqrt(target.position().distanceToSqr(source)) / DIAMETER;
      if (distExposure > 1.0D) return 0;
      double finalExposure = (1.0D - distExposure) * this.exposureOverride(source, target.getBoundingBox(), world, null, ignore);
      float toDamage = (float) ((finalExposure * finalExposure + finalExposure) / 2.0D * 7.0D * DIAMETER + 1.0D);
      return this.reductions(toDamage, target, world);
   }

   private float reductions(float toDamage, LivingEntity target, ClientLevel world) {
      if (target instanceof Player) {
         if (world.getDifficulty() == Difficulty.EASY) toDamage = Math.min(toDamage / 2.0F + 1.0F, toDamage);
         else if (world.getDifficulty() == Difficulty.HARD) toDamage = toDamage * 3.0F / 2.0F;
      }
      float armor = (float) Math.floor(target.getArmorValue());
      float toughness = 0.0F;
      var attr = target.getAttribute(Attributes.ARMOR_TOUGHNESS);
      if (attr != null) toughness = (float) attr.getValue();
      toDamage = getDamageAfterArmor(toDamage, armor, toughness);
      MobEffectInstance resistance = target.getEffect(MobEffects.RESISTANCE);
      if (resistance != null) {
         float r = 25.0F - (resistance.getAmplifier() + 1) * 5.0F;
         toDamage = Math.max(toDamage * r / 25.0F, 0.0F);
      }
      if (toDamage <= 0.0F) return 0.0F;
      int protAmount = this.getProtectionTotal(target, world);
      if (protAmount > 0) toDamage = toDamage * (1.0F - Mth.clamp(protAmount, 0.0F, 20.0F) / 25.0F);
      return toDamage;
   }

   private static float getDamageAfterArmor(float damage, float armor, float toughness) {
      float i = 2.0F + toughness / 4.0F;
      float j = Mth.clamp(armor - damage / i, armor * 0.2F, 20.0F);
      return damage * (1.0F - j / 25.0F);
   }

   private int getProtectionTotal(LivingEntity target, ClientLevel world) {
      int amount = 0;
      amount += this.protectionFor(target.getItemBySlot(EquipmentSlot.HEAD), world);
      amount += this.protectionFor(target.getItemBySlot(EquipmentSlot.CHEST), world);
      amount += this.protectionFor(target.getItemBySlot(EquipmentSlot.LEGS), world);
      amount += this.protectionFor(target.getItemBySlot(EquipmentSlot.FEET), world);
      return amount;
   }

   private int protectionFor(ItemStack stack, ClientLevel world) {
      return this.enchantLevel(Enchantments.BLAST_PROTECTION, stack, world) * 2 + this.enchantLevel(Enchantments.PROTECTION, stack, world);
   }

   private int enchantLevel(ResourceKey<Enchantment> key, ItemStack stack, ClientLevel world) {
      return ToolSpeed.enchantLevel(key, stack, world);
   }

   private double exposureOverride(Vec3 source, AABB box, ClientLevel world, BlockPos ghost, BlockPos ignore) {
      double d = 1.0D / ((box.maxX - box.minX) * 2.0D + 1.0D);
      double e = 1.0D / ((box.maxY - box.minY) * 2.0D + 1.0D);
      double f = 1.0D / ((box.maxZ - box.minZ) * 2.0D + 1.0D);
      if (d < 0.0D || e < 0.0D || f < 0.0D) return 0.0D;
      double g = (1.0D - Math.floor(1.0D / d) * d) / 2.0D;
      double h = (1.0D - Math.floor(1.0D / f) * f) / 2.0D;
      int i = 0;
      int j = 0;
      for (double k = 0.0D; k <= 1.0D; k += d) {
         for (double l = 0.0D; l <= 1.0D; l += e) {
            for (double m = 0.0D; m <= 1.0D; m += f) {
               double n = Mth.lerp(k, box.minX, box.maxX);
               double o = Mth.lerp(l, box.minY, box.maxY);
               double p = Mth.lerp(m, box.minZ, box.maxZ);
               if (this.isVisible(new Vec3(n + g, o, p + h), source, world, ghost, ignore)) i++;
               j++;
            }
         }
      }
      return (double) i / (double) j;
   }

   private boolean isVisible(Vec3 start, Vec3 end, ClientLevel world, BlockPos ghost, BlockPos ignore) {
      return this.raycastShapes(start, end, world, ghost, ignore) == null;
   }

   private BlockHitResult raycastShapes(Vec3 start, Vec3 end, ClientLevel world, BlockPos ghost, BlockPos ignore) {
      return BlockGetter.traverseBlocks(start, end, world, (ctx, pos) -> {
         BlockState state;
         if (ghost != null && pos.equals(ghost)) state = Blocks.OBSIDIAN.defaultBlockState();
         else if (ignore != null && pos.equals(ignore)) state = Blocks.AIR.defaultBlockState();
         else state = world.getBlockState(pos);
         return state.getCollisionShape(world, pos).clip(start, end, pos);
      }, ctx -> null);
   }

   @EventHandler
   public void onRender3D(EventRender3D event) {
      if (!this.render.isEnabled() || mc.level == null) return;
      if (this.visualDatas.isEmpty() && this.pendingVisualPos == null && this.lastData == null) return;
      long now = System.currentTimeMillis();
      this.visualDatas.removeIf(v -> now - v.createdAt > 350L);
      int base = this.color.getIntColor();
      List<VisualData> all = new ArrayList<>(this.visualDatas);
      if (this.lastData != null && this.lastData.blockPos() != null) {
         all.add(new VisualData(this.lastData.blockPos(), now, 0.2F, 0.8F, 1.0F));
      }
      for (VisualData v : all) {
         float age = (now - v.createdAt) / 300.0F;
         if (age >= 1.0F) continue;
         float alpha = 1.0F - age;
         AABB box = new AABB(v.blockPos);
         Render3DUtil.drawFilledBox(event, box, withAlpha(base, (int) (48 * alpha)), true);
         Render3DUtil.drawBox(event, box, withAlpha(base, (int) (200 * Math.min(1.0F, alpha + 0.3F))), true);
      }
   }

   private static int withAlpha(int argb, int a) {
      return argb & 0xFFFFFF | (Math.max(0, Math.min(255, a)) & 0xFF) << 24;
   }

   public record PlaceData(BlockPos blockPos, Direction face, double damage, PlaceResult placeResult, Entity entity) {
   }

   @Getter
   public static class VisualData {
      private final BlockPos blockPos;
      private final long createdAt;
      private float r;
      private float g;
      private float b;

      public VisualData(BlockPos blockPos, long createdAt, float r, float g, float b) {
         this.blockPos = blockPos;
         this.createdAt = createdAt;
         this.r = r;
         this.g = g;
         this.b = b;
      }
   }

   public enum PlaceResult {
      PLACE_OBSIDIAN,
      PLACE_CRYSTAL,
      EXPLOSION,
      BREAK,
      AWAITING
   }

   public enum ReturnResult {
      ITEM,
      PLAYER,
      NONE
   }
}
