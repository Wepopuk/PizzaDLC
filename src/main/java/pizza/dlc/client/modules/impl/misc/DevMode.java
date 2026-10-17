package pizza.dlc.client.modules.impl.misc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Locale;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;
import pizza.dlc.api.events.impl.render.EventRender2D;
import pizza.dlc.api.util.game.ChatUtility;
import pizza.dlc.api.util.render.PizzaDLCPipelines;
import pizza.dlc.api.util.render.Render2DUtil;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;
import pizza.dlc.mixin.client.GuiGraphicsExtractorAccessor;

@ModuleInfo(
   name = "DevMode",
   description = "Тест рендера: 15000 ректов каждый кадр, статистика FPS в чат",
   category = ModuleCategory.OTHER
)
public class DevMode extends Module {
   private static final int RECT_COUNT = 15000;
   private static final int SIZE = 2;
   private static final int COLUMNS = 455;
   private static final int START_X = 30;
   private static final long REPORT_INTERVAL_NANOS = 3_000_000_000L;
   private long lastReportAt;
   private double renderMs;

   @Override
   public void onEnable() {
      this.lastReportAt = System.nanoTime();
      this.renderMs = 0.0;
      super.onEnable();
   }

   @EventHandler
   public void onRender(EventRender2D event) {
      if (!this.isEnabled() || mc.player == null || mc.level == null) return;

      var context = event.getContext();
      long submissionStartedAt = System.nanoTime();
      Matrix3x2fc pose = new Matrix3x2f(context.pose());
      ScreenRectangle scissor = context.scissorStack.peek();
      int rows = (RECT_COUNT + COLUMNS - 1) / COLUMNS;
      ScreenRectangle bounds = new ScreenRectangle(START_X, 0, COLUMNS * SIZE, rows * SIZE).transformMaxBounds(pose);
      if (scissor != null) bounds = scissor.intersection(bounds);
      var state = new RectGridRenderState(pose, Render2DUtil.op(-1), Render2DUtil.op(0xFF0000FF), scissor, bounds, this);
      ((GuiGraphicsExtractorAccessor)context).pizzadlc$guiRenderState().addGuiElement(state);
      state.submissionNanos = System.nanoTime() - submissionStartedAt;

      long now = System.nanoTime();
      if (now - this.lastReportAt >= REPORT_INTERVAL_NANOS) {
         // Vertex building is deferred; report the last completed frame's CPU time.
         ChatUtility.send(String.format(Locale.ROOT,
            "FPS: %d | Render: %.3f ms | Objects: %d", mc.getFps(), this.renderMs, RECT_COUNT));
         this.lastReportAt = now;
      }
   }

   // One batch, with the same positions, colors and radii as the reference loop.
   private static final class RectGridRenderState implements GuiElementRenderState {
      private final Matrix3x2fc pose;
      private final int white;
      private final int blue;
      private final @Nullable ScreenRectangle scissorArea;
      private final @Nullable ScreenRectangle bounds;
      private final DevMode owner;
      private long submissionNanos;

      private RectGridRenderState(Matrix3x2fc pose, int white, int blue,
         @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds, DevMode owner) {
         this.pose = pose;
         this.white = white;
         this.blue = blue;
         this.scissorArea = scissorArea;
         this.bounds = bounds;
         this.owner = owner;
      }

      @Override
      public void buildVertices(VertexConsumer vertices) {
         long startedAt = System.nanoTime();
         int column = 0;
         int x = START_X;
         int y = 0;
         for (int i = 0; i < RECT_COUNT; i++) {
            int color = (i & 1) == 0 ? this.white : this.blue;
            int radius = i & 3;
            int packedRadius = radius | radius << 8;
            vertices.addVertexWith2DPose(this.pose, x, y).setColor(color).setUv(0.0F, 0.0F).setUv1(SIZE, SIZE).setUv2(packedRadius, packedRadius);
            vertices.addVertexWith2DPose(this.pose, x, y + SIZE).setColor(color).setUv(0.0F, 1.0F).setUv1(SIZE, SIZE).setUv2(packedRadius, packedRadius);
            vertices.addVertexWith2DPose(this.pose, x + SIZE, y + SIZE).setColor(color).setUv(1.0F, 1.0F).setUv1(SIZE, SIZE).setUv2(packedRadius, packedRadius);
            vertices.addVertexWith2DPose(this.pose, x + SIZE, y).setColor(color).setUv(1.0F, 0.0F).setUv1(SIZE, SIZE).setUv2(packedRadius, packedRadius);
            column++;
            x += SIZE;
            if (column == COLUMNS) {
               column = 0;
               x = START_X;
               y += SIZE;
            }
         }
         this.owner.renderMs = (this.submissionNanos + System.nanoTime() - startedAt) / 1_000_000.0;
      }

      @Override
      public RenderPipeline pipeline() {
         return PizzaDLCPipelines.ROUNDED;
      }

      @Override
      public TextureSetup textureSetup() {
         return TextureSetup.noTexture();
      }

      @Override
      public @Nullable ScreenRectangle scissorArea() {
         return this.scissorArea;
      }

      @Override
      public @Nullable ScreenRectangle bounds() {
         return this.bounds;
      }
   }
}
