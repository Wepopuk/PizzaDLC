package pizza.dlc.client.modules.impl.visuals;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.api.util.render.Render3DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Highlight",
   description = "Подсветка наводимого блока",
   category = ModuleCategory.VISUALS
)
public class Highlight extends Module {
   private static final String FILL = "Заливка";
   private static final String OUTLINE = "Контур";
   private static final String BOTH = "Оба";
   public final ModeSetting style = new ModeSetting("Стиль", this, BOTH, FILL, OUTLINE);
   public final SliderSetting animSpeed = new SliderSetting("Скорость", this, 0.3F, 0.05F, 1.0F, 0.05F);
   public final BooleanSetting throughBlocks = new BooleanSetting("Сквозь стены", this);
   private AABB current = null;
   private long lastNanos = 0L;

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (mc.level == null || mc.player == null) return;

      if (this.isCameraInsideBlock(e)) {
         this.current = null;
      } else {
         AABB target = null;
         if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getShape(mc.level, pos, CollisionContext.of(mc.player));
            if (!shape.isEmpty()) {
               target = shape.bounds().move(pos);
            }
         }

         if (target == null) {
            this.current = null;
         } else {
            long now = System.nanoTime();
            float dt = this.lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.lastNanos) / 1.0E9F);
            this.lastNanos = now;
            if (this.current == null) {
               this.current = target;
            } else {
               float f = 1.0F - (float)Math.exp(-this.animSpeed.getValue() * 16.0F * dt);
               this.current = lerpBox(this.current, target, f);
            }

            boolean tw = this.throughBlocks.isEnabled();
            int accent = ThemeManager.accent();
            if (!this.style.is(OUTLINE)) {
               Render3DUtil.drawFilledBox(e, this.current, accent & 0xFFFFFF | 0x40000000, tw);
            }

            if (!this.style.is(FILL)) {
               Render3DUtil.drawBox(e, this.current, accent, tw);
            }
         }
      }
   }

   private boolean isCameraInsideBlock(EventRender3D e) {
      Vec3 cam = e.getCamera().pos;
      BlockPos pos = BlockPos.containing(cam.x, cam.y, cam.z);
      BlockState state = mc.level.getBlockState(pos);
      if (state.isAir()) {
         return false;
      }

      VoxelShape shape = state.getCollisionShape(mc.level, pos, CollisionContext.of(mc.player));
      if (shape.isEmpty()) {
         return false;
      }

      double lx = cam.x - pos.getX();
      double ly = cam.y - pos.getY();
      double lz = cam.z - pos.getZ();

      for (AABB b : shape.toAabbs()) {
         if (!b.inflate(1.0E-6).contains(lx, ly, lz)) continue;

         return true;
      }

      return false;
   }

   private static AABB lerpBox(AABB a, AABB b, float f) {
      return new AABB(
         Mth.lerp(f, a.minX, b.minX),
         Mth.lerp(f, a.minY, b.minY),
         Mth.lerp(f, a.minZ, b.minZ),
         Mth.lerp(f, a.maxX, b.maxX),
         Mth.lerp(f, a.maxY, b.maxY),
         Mth.lerp(f, a.maxZ, b.maxZ)
      );
   }
}
