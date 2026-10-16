package pizza.dlc.client.modules.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ChorusRadius",
   category = ModuleCategory.VISUALS,
   description = "Показывает, куда может телепортировать хорус"
)
public class ChorusRadius extends Module {
   private static final double DIAMETER = 16.0;
   private static final double R = 8.0;
   private static final int DEEP_SCAN = 32;
   private static final int MAX_SPOTS = 1024;
   private static final long RECALC_MS = 250L;
   private static final double INSET = 0.06;
   private static final double TILE_H = 0.03;
   public final BooleanSetting onlyWithFruit = new BooleanSetting("Только с хорусом", this);
   public final BooleanSetting chanceAlpha = new BooleanSetting("Шанс прозрачностью", this);
   public final BooleanSetting colorByHeight = new BooleanSetting("Цвет по высоте", this);
   public final BooleanSetting showBounds = new BooleanSetting("Границы области", this);
   public final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", this);
   private final List<Spot> spots = new ArrayList<>();
   private BlockPos lastPos;
   private long lastMs;

   public ChorusRadius() {
      this.onlyWithFruit.setEnabled(true);
      this.chanceAlpha.setEnabled(true);
      this.colorByHeight.setEnabled(true);
      this.showBounds.setEnabled(true);
      this.throughWalls.setEnabled(true);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.lastPos = null;
      this.spots.clear();
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (mc.level == null || mc.player == null) return;

      if (this.onlyWithFruit.isEnabled() && !this.holdingChorus()) {
         this.spots.clear();
         this.lastPos = null;
      } else {
         this.recomputeIfNeeded();
         boolean tw = this.throughWalls.isEnabled();
         double py = mc.player.getY();
         int rgbUp = ThemeManager.accent() & 0xFFFFFF;
         int rgbDown = ThemeManager.secondary() & 0xFFFFFF;

         for (Spot s : this.spots) {
            int rgb = this.colorByHeight.isEnabled() && s.y() < py ? rgbDown : rgbUp;
            int alpha = this.chanceAlpha.isEnabled() ? Mth.clamp(Math.round(45.0F + 165.0F * s.weight()), 30, 255) : 120;
            AABB tile = new AABB(s.x() + INSET, s.y(), s.z() + INSET, s.x() + 1 - INSET, s.y() + TILE_H, s.z() + 1 - INSET);
            Render3DUtil.drawFilledBox(e, tile, alpha << 24 | rgb, tw);
         }

         if (this.showBounds.isEnabled()) {
            double px = mc.player.getX();
            double pz = mc.player.getZ();
            Render3DUtil.drawBox(e, new AABB(px - R, py - R, pz - R, px + R, py + R, pz + R), 0x40000000 | rgbUp, tw);
         }
      }
   }

   private boolean holdingChorus() {
      return mc.player.getMainHandItem().is(Items.CHORUS_FRUIT) || mc.player.getOffhandItem().is(Items.CHORUS_FRUIT);
   }

   private void recomputeIfNeeded() {
      BlockPos now = mc.player.blockPosition();
      long ms = System.currentTimeMillis();
      if (this.lastPos == null || !now.equals(this.lastPos) || ms - this.lastMs >= RECALC_MS) {
         this.lastPos = now;
         this.lastMs = ms;
         this.recompute();
      }
   }

   private void recompute() {
      this.spots.clear();
      LocalPlayer p = mc.player;
      ClientLevel lvl = mc.level;
      double px = p.getX();
      double py = p.getY();
      double pz = p.getZ();
      int worldMin = lvl.getMinY();
      int worldMax = worldMin + lvl.getHeight() - 1;
      double yLo = Math.max((double)worldMin, py - R);
      double yHi = Math.min((double)worldMax, py + R);
      if (!(yHi <= yLo)) {
         int cHi = Mth.floor(yHi);
         int bottom = Math.max(worldMin, Mth.floor(yLo) - DEEP_SCAN);
         AABB bb = p.getBoundingBox();
         MutableBlockPos m = new MutableBlockPos();

         for (int bx = Mth.floor(px - R); bx <= Mth.floor(px + R); bx++) {
            double cx = bx + 0.5;
            if (!(cx < px - R) && !(cx > px + R)) {
               for (int bz = Mth.floor(pz - R); bz <= Mth.floor(pz + R); bz++) {
                  double cz = bz + 0.5;
                  if (!(cz < pz - R) && !(cz > pz + R)) {
                     double aboveBound = yHi;

                     for (int y = cHi; y >= bottom; y--) {
                        m.set(bx, y - 1, bz);
                        if (lvl.getBlockState(m).blocksMotion()) {
                           double lo = Math.max((double)y, yLo);
                           double hi = Math.min(aboveBound, yHi);
                           double span = hi - lo;
                           if (span <= 0.0) {
                              break;
                           }

                           aboveBound = y;
                           AABB at = bb.move(cx - px, y - py, cz - pz);
                           if (lvl.noCollision(p, at) && !lvl.containsAnyLiquid(at)) {
                              this.spots.add(new Spot(bx, y, bz, (float)(span / DIAMETER)));
                              if (this.spots.size() >= MAX_SPOTS) {
                                 return;
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

   private record Spot(int x, int y, int z, float weight) {
   }
}
