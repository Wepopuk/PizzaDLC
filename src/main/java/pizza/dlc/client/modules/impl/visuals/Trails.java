package pizza.dlc.client.modules.impl.visuals;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.events.impl.render.EventRender3D;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.api.theme.ThemeManager;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Trails",
   description = "Светящийся шлейф за игроком",
   category = ModuleCategory.VISUALS
)
public class Trails extends Module {
   private static final int MAX_POINTS = 512;
   private static final double MIN_STEP_SQR = 4.0E-4;
   private static final double TELEPORT_SQR = 64.0;
   private final SliderSetting lifetime = new SliderSetting("Время жизни (мс)", this, 800.0F, 250.0F, 3000.0F, 50.0F);
   private final List<Trail> trails = new ArrayList<>();
   private static RenderType trailType;
   private static boolean trailTypeFailed;

   @Override
   public void onDisable() {
      this.trails.clear();
      super.onDisable();
   }

   @EventHandler
   public void onRender3D(EventRender3D e) {
      if (mc.player == null || mc.level == null) return;

      if (!mc.options.getCameraType().isFirstPerson()) {
         RenderType type = trailType();
         if (type != null) {
            long now = System.currentTimeMillis();
            long lifetimeMs = (long)this.lifetime.getValue();
            this.trails.removeIf(t -> now - t.time() > lifetimeMs);
            float td = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Vec3 interp = new Vec3(
               Mth.lerp(td, mc.player.xo, mc.player.getX()), Mth.lerp(td, mc.player.yo, mc.player.getY()), Mth.lerp(td, mc.player.zo, mc.player.getZ())
            );
            if (this.trails.isEmpty()) {
               this.trails.add(new Trail(interp, now));
            } else {
               double moved = this.trails.get(this.trails.size() - 1).pos().distanceToSqr(interp);
               if (moved > TELEPORT_SQR) {
                  this.trails.clear();
                  this.trails.add(new Trail(interp, now));
               } else if (moved >= MIN_STEP_SQR) {
                  this.trails.add(new Trail(interp, now));
                  if (this.trails.size() > MAX_POINTS) {
                     this.trails.remove(0);
                  }
               }
            }

            int count = this.trails.size();
            if (count >= 2) {
               float bbHeight = mc.player.getBbHeight();
               Vec3 cam = e.getCamera().pos;
               int bottomBase = ThemeManager.accent();
               int topBase = ThemeManager.secondary();
               e.getCollector().submitCustomGeometry(e.getPose(), type, (pose, vc) -> {
                  for (Trail t : this.trails) {
                     float age = (float)(now - t.time()) / (float)lifetimeMs;
                     float alpha = 1.0F - Math.min(1.0F, age);
                     alpha *= alpha;
                     int a = (int)(alpha * 255.0F);
                     float x = (float)(t.pos().x - cam.x);
                     float y = (float)(t.pos().y - cam.y);
                     float z = (float)(t.pos().z - cam.z);
                     vc.addVertex(pose, x, y, z).setColor(withAlpha(bottomBase, a));
                     vc.addVertex(pose, x, y + bbHeight, z).setColor(withAlpha(topBase, a));
                  }
               });
            }
         }
      }
   }

   private static RenderType trailType() {
      if (trailType == null && !trailTypeFailed) {
         try {
            RenderPipeline p = RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
               .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/trails"))
               .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.LIGHTNING), GpuFormat.RGBA8_UNORM, 7))
               .withCull(false)
               .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
               .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
               .build();
            trailType = RenderType.create("pizzadlc:trails", RenderSetup.builder(p).createRenderSetup());
         } catch (Throwable ex) {
            trailTypeFailed = true;
            PizzaDLC.LOGGER.warn("[trails] пайплайн недоступен", ex);
         }

         return trailType;
      }

      return trailType;
   }

   private static int withAlpha(int argb, int alpha) {
      return argb & 0xFFFFFF | (alpha & 0xFF) << 24;
   }

   private record Trail(Vec3 pos, long time) {
   }
}
