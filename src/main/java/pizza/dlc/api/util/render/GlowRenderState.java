package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record GlowRenderState(
   Matrix3x2fc pose, int x0, int y0, int x1, int y1, int color, int radius, int spread, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public GlowRenderState(Matrix3x2fc pose, int x0, int y0, int x1, int y1, int color, int radius, int spread, @Nullable ScreenRectangle scissorArea) {
      this(pose, x0, y0, x1, y1, color, radius, spread, scissorArea, computeBounds(x0, y0, x1, y1, spread, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(int x0, int y0, int x1, int y1, int spread, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      int ex0 = x0 - spread;
      int ey0 = y0 - spread;
      int ex1 = x1 + spread;
      int ey1 = y1 + spread;
      ScreenRectangle r = new ScreenRectangle(ex0, ey0, ex1 - ex0, ey1 - ey0).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int w = this.x1 - this.x0;
      int h = this.y1 - this.y0;
      int ex0 = this.x0 - this.spread;
      int ey0 = this.y0 - this.spread;
      int ex1 = this.x1 + this.spread;
      int ey1 = this.y1 + this.spread;
      vc.addVertexWith2DPose(this.pose, ex0, ey0).setColor(this.color).setUv(0.0F, 0.0F).setUv1(w, h).setUv2(this.radius, this.spread);
      vc.addVertexWith2DPose(this.pose, ex0, ey1).setColor(this.color).setUv(0.0F, 1.0F).setUv1(w, h).setUv2(this.radius, this.spread);
      vc.addVertexWith2DPose(this.pose, ex1, ey1).setColor(this.color).setUv(1.0F, 1.0F).setUv1(w, h).setUv2(this.radius, this.spread);
      vc.addVertexWith2DPose(this.pose, ex1, ey0).setColor(this.color).setUv(1.0F, 0.0F).setUv1(w, h).setUv2(this.radius, this.spread);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.GLOW;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
