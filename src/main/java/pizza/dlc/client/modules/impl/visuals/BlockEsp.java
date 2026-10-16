package pizza.dlc.client.modules.impl.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector2f;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.events.impl.render.EventRenderWorld2D;
import pizza.dlc.api.seed.SeedStructureFinder;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.ProjectionUtil;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Block ESP",
   category = ModuleCategory.VISUALS
)
public class BlockEsp extends Module {
   private static final long RESCAN_INTERVAL_MS = 3000L;
   private static final String GROWN_PLANTS = "Выросшие растения";
   private static final String SPAWNERS = "Спавнера";
   private static final String SHULKERS = "Шалкера";
   private static final String CHESTS = "Сундуки";
   private static final String ENDER_CHESTS = "Эндер Сундуки";
   private static final String BARRELS = "Бочки";
   private static final String[] AUTO_MODES = new String[]{GROWN_PLANTS, SPAWNERS, SHULKERS, CHESTS, ENDER_CHESTS, BARRELS};
   public final SliderSetting distance = new SliderSetting("Дистанция", this, 16.0F, 4.0F, 32.0F, 1.0F);
   private final MultiModeSetting auto = new MultiModeSetting("Авто", this, AUTO_MODES);
   private static final float SEED_LABEL_SIZE = 7.5F;
   private static final String[] SEED_NAMES = SeedStructureFinder.ALL.stream().map(SeedStructureFinder.Placement::name).toArray(String[]::new);
   private final BooleanSetting seedMode = new BooleanSetting("Работать по сиду", this);
   private final SliderSetting seedRadius = new SliderSetting("Радиус сида (чанки)", this, 80.0F, 16.0F, 256.0F, 8.0F);
   private final MultiModeSetting seedStructures = new MultiModeSetting("Структуры", this, SEED_NAMES);
   private long seed;
   private boolean hasSeed;
   private boolean seedDirty = true;
   private int lastSeedChunkX = Integer.MAX_VALUE;
   private int lastSeedChunkZ = Integer.MAX_VALUE;
   private int lastSeedHash;
   private final List<SeedStructureFinder.Found> seedFound = new ArrayList<>();
   private final List<Block> blockList = new ArrayList<>();
   private final Set<Block> blockSet = new HashSet<>();
   private final List<AABB> renderBoxes = new ArrayList<>();
   private BlockPos lastCenter;
   private int lastDistance = -1;
   private int lastTargetHash;
   private long lastScanMs;
   private boolean cacheDirty = true;

   private static int color() {
      return ThemeManager.accent();
   }

   public BlockEsp() {
      for (String n : new String[]{"Деревня", "Монумент", "Особняк", "Древний город", "Испыт. камера", "Энд-сити", "Крепость/Бастион"}) {
         BooleanSetting s = this.seedStructures.get(n);
         if (s != null) {
            s.setEnabled(true);
         }
      }

      this.seedRadius.setVisible(() -> this.seedMode.isEnabled());
      this.seedStructures.setVisible(() -> this.seedMode.isEnabled());
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (mc.level == null || mc.player == null) return;

      if (this.hasTargets()) {
         this.updateRenderBoxes();

         for (AABB box : this.renderBoxes) {
            Render3DUtil.drawBox(e, box, color(), true);
         }
      }

      if (this.seedMode.isEnabled() && this.hasSeed) {
         this.updateSeedFound();

         for (SeedStructureFinder.Found f : this.seedFound) {
            AABB beam = new AABB(f.blockX() - 0.35, -64.0, f.blockZ() - 0.35, f.blockX() + 0.35, 320.0, f.blockZ() + 0.35);
            Render3DUtil.drawBox(e, beam, f.color(), true);
         }
      }
   }

   @EventHandler
   public void onRenderWorld2D(EventRenderWorld2D e) {
      if (mc.level == null || mc.player == null) return;

      if (this.seedMode.isEnabled() && this.hasSeed && !this.seedFound.isEmpty()) {
         double px = mc.player.getX();
         double py = mc.player.getY();
         double pz = mc.player.getZ();
         int drawn = 0;

         for (SeedStructureFinder.Found f : this.seedFound) {
            if (drawn >= 30) {
               break;
            }

            double cx = f.blockX() + 0.5;
            double cz = f.blockZ() + 0.5;
            Vector2f s = ProjectionUtil.worldToScreen(new Vec3(cx, py + 1.0, cz));
            if (s != null) {
               double dist = Math.sqrt((cx - px) * (cx - px) + (cz - pz) * (cz - pz));
               String label = f.name() + "  " + (int)dist + "m";
               float w = Render2DUtil.msdfWidth(label, SEED_LABEL_SIZE);
               Render2DUtil.msdfText(e.getContext(), label, s.x - w / 2.0F, s.y, SEED_LABEL_SIZE, f.color());
               drawn++;
            }
         }
      }
   }

   private void updateSeedFound() {
      int cx = mc.player.blockPosition().getX() >> 4;
      int cz = mc.player.blockPosition().getZ() >> 4;
      int radius = Math.round(this.seedRadius.getValue());
      int hash = this.seedStructHash() * 31 + radius;
      boolean moved = Math.abs(cx - this.lastSeedChunkX) >= 2 || Math.abs(cz - this.lastSeedChunkZ) >= 2;
      if (this.seedDirty || moved || hash != this.lastSeedHash) {
         this.lastSeedChunkX = cx;
         this.lastSeedChunkZ = cz;
         this.lastSeedHash = hash;
         this.seedDirty = false;
         this.seedFound.clear();
         this.seedFound.addAll(SeedStructureFinder.find(this.seed, cx, cz, radius, n -> {
            BooleanSetting s = this.seedStructures.get(n);
            return s != null && s.isEnabled();
         }, 400));
         long bx = (long)cx << 4;
         long bz = (long)cz << 4;
         this.seedFound.sort(Comparator.comparingLong(f -> {
            long dx = f.blockX() - bx;
            long dz = f.blockZ() - bz;
            return dx * dx + dz * dz;
         }));
      }
   }

   private int seedStructHash() {
      int h = 1;

      for (SeedStructureFinder.Placement p : SeedStructureFinder.ALL) {
         BooleanSetting s = this.seedStructures.get(p.name());
         h = 31 * h + (s != null && s.isEnabled() ? 1 : 0);
      }

      return h;
   }

   public void setSeed(long value) {
      this.seed = value;
      this.hasSeed = true;
      this.seedDirty = true;
   }

   public void clearSeed() {
      this.hasSeed = false;
      this.seedDirty = true;
   }

   public boolean hasSeed() {
      return this.hasSeed;
   }

   public long getSeed() {
      return this.seed;
   }

   public boolean isSeedMode() {
      return this.seedMode.isEnabled();
   }

   public boolean addBlock(Block block) {
      if (block == null || block == Blocks.AIR) {
         return false;
      }

      if (this.blockSet.contains(block)) {
         return false;
      }

      this.blockList.add(block);
      this.blockSet.add(block);
      this.cacheDirty = true;
      return true;
   }

   public boolean removeBlock(Block block) {
      if (!this.blockSet.contains(block)) {
         return false;
      }

      this.blockList.remove(block);
      this.blockSet.remove(block);
      this.cacheDirty = true;
      return true;
   }

   public List<Block> getBlockList() {
      return Collections.unmodifiableList(this.blockList);
   }

   public void clearBlocks() {
      this.blockList.clear();
      this.blockSet.clear();
      this.renderBoxes.clear();
      this.cacheDirty = true;
   }

   private void updateRenderBoxes() {
      BlockPos center = mc.player.blockPosition();
      int range = Math.round(this.distance.getValue());
      int targetHash = this.targetHash();
      long now = System.currentTimeMillis();
      if (this.shouldRescan(center, range, targetHash, now)) {
         this.renderBoxes.clear();
         this.lastCenter = center;
         this.lastDistance = range;
         this.lastTargetHash = targetHash;
         this.lastScanMs = now;
         this.cacheDirty = false;
         boolean grownPlants = this.auto.get(GROWN_PLANTS).isEnabled();
         boolean spawners = this.auto.get(SPAWNERS).isEnabled();
         boolean shulkers = this.auto.get(SHULKERS).isEnabled();
         boolean chests = this.auto.get(CHESTS).isEnabled();
         boolean enderChests = this.auto.get(ENDER_CHESTS).isEnabled();
         boolean barrels = this.auto.get(BARRELS).isEnabled();
         int rangeSq = range * range;
         BlockPos min = center.offset(-range, -range, -range);
         BlockPos max = center.offset(range, range, range);

         for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz <= rangeSq) {
               BlockState state = mc.level.getBlockState(pos);
               if (!state.isAir() && this.matches(state, grownPlants, spawners, shulkers, chests, enderChests, barrels)) {
                  VoxelShape shape = state.getShape(mc.level, pos);
                  if (shape.isEmpty()) {
                     this.renderBoxes.add(new AABB(pos));
                  } else {
                     for (AABB part : shape.toAabbs()) {
                        this.renderBoxes.add(part.move(pos));
                     }
                  }
               }
            }
         }
      }
   }

   private boolean hasTargets() {
      if (!this.blockSet.isEmpty()) {
         return true;
      }

      for (String mode : AUTO_MODES) {
         if (!this.auto.get(mode).isEnabled()) continue;

         return true;
      }

      return false;
   }

   private boolean shouldRescan(BlockPos center, int range, int targetHash, long now) {
      if (!this.cacheDirty && this.lastCenter != null && range == this.lastDistance && targetHash == this.lastTargetHash) {
         return now - this.lastScanMs >= RESCAN_INTERVAL_MS
            ? true
            : Math.abs(center.getX() - this.lastCenter.getX()) >= 4
            || Math.abs(center.getY() - this.lastCenter.getY()) >= 4
            || Math.abs(center.getZ() - this.lastCenter.getZ()) >= 4;
      } else {
         return true;
      }
   }

   private int targetHash() {
      int hash = this.blockSet.hashCode();

      for (String mode : AUTO_MODES) {
         hash = 31 * hash + (this.auto.get(mode).isEnabled() ? 1 : 0);
      }

      return hash;
   }

   private boolean matches(BlockState state, boolean grownPlants, boolean spawners, boolean shulkers, boolean chests, boolean enderChests, boolean barrels) {
      Block block = state.getBlock();
      if (this.blockSet.contains(block)) {
         return true;
      }

      if (grownPlants && this.isGrownPlant(state)) {
         return true;
      }

      if (spawners && this.isSpawner(block)) {
         return true;
      }

      if (shulkers && block instanceof ShulkerBoxBlock) {
         return true;
      }

      if (chests && this.isChest(block)) {
         return true;
      }

      return enderChests && block instanceof EnderChestBlock || barrels && block instanceof BarrelBlock;
   }

   private boolean isGrownPlant(BlockState state) {
      Block block = state.getBlock();
      if (block instanceof CropBlock crop) {
         return crop.isMaxAge(state);
      }

      if (block instanceof NetherWartBlock) {
         return (Integer)state.getValue(NetherWartBlock.AGE) >= 3;
      }

      if (block instanceof CocoaBlock) {
         return (Integer)state.getValue(CocoaBlock.AGE) >= 2;
      }

      if (block instanceof SweetBerryBushBlock) {
         return (Integer)state.getValue(SweetBerryBushBlock.AGE) >= 2;
      }

      return block instanceof CaveVines ? CaveVines.hasGlowBerries(state) : block == Blocks.MELON || block == Blocks.PUMPKIN;
   }

   private boolean isSpawner(Block block) {
      if (!(block instanceof SpawnerBlock) && block != Blocks.TRIAL_SPAWNER) {
         Identifier id = BuiltInRegistries.BLOCK.getKey(block);
         return id != null && id.getPath().endsWith("spawner");
      }

      return true;
   }

   private boolean isChest(Block block) {
      if (block instanceof EnderChestBlock || block instanceof ShulkerBoxBlock) {
         return false;
      }

      if (block instanceof ChestBlock) {
         return true;
      }

      Identifier id = BuiltInRegistries.BLOCK.getKey(block);
      return id != null && id.getPath().endsWith("chest") && !id.getPath().contains("ender");
   }

   @Override
   public JsonObject save() {
      JsonObject object = super.save();
      JsonArray blocksArray = new JsonArray();

      for (Block block : this.blockList) {
         Identifier id = BuiltInRegistries.BLOCK.getKey(block);
         if (id != null) {
            blocksArray.add(id.toString());
         }
      }

      object.add("BlockList", blocksArray);
      object.addProperty("Seed", this.seed);
      object.addProperty("HasSeed", this.hasSeed);
      return object;
   }

   @Override
   public void load(JsonObject object) {
      super.load(object);
      this.blockList.clear();
      this.blockSet.clear();
      this.renderBoxes.clear();
      if (object.has("BlockList")) {
         JsonArray array = object.getAsJsonArray("BlockList");

         for (int i = 0; i < array.size(); i++) {
            String id = array.get(i).getAsString();
            Identifier identifier = Identifier.tryParse(id);
            if (identifier != null) {
               Block block = (Block)BuiltInRegistries.BLOCK.getValue(identifier);
               if (block != Blocks.AIR) {
                  this.blockList.add(block);
                  this.blockSet.add(block);
               }
            }
         }
      }

      if (object.has("Seed")) {
         this.seed = object.get("Seed").getAsLong();
      }

      if (object.has("HasSeed")) {
         this.hasSeed = object.get("HasSeed").getAsBoolean();
      }

      this.seedDirty = true;
      this.cacheDirty = true;
   }
}
