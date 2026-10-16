package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record StarRenderState(
   Matrix3x2fc pose,
   float x0,
   float y0,
   float x1,
   float y1,
   int color,
   int size,
   int points,
   float m,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public StarRenderState(
      Matrix3x2fc pose, float x0, float y0, float x1, float y1, int color, int size, int points, float m, @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x0, y0, x1, y1, color, size, points, m, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(float x0, float y0, float x1, float y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      int ix = (int)Math.floor(x0);
      int iy = (int)Math.floor(y0);
      ScreenRectangle r = new ScreenRectangle(ix, iy, (int)Math.ceil(x1) - ix, (int)Math.ceil(y1) - iy).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int packM = Math.round(this.m * 100.0F);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.color).setUv(0.0F, 0.0F).setUv1(this.size, this.size).setUv2(this.points, packM);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(this.color).setUv(0.0F, 1.0F).setUv1(this.size, this.size).setUv2(this.points, packM);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.color).setUv(1.0F, 1.0F).setUv1(this.size, this.size).setUv2(this.points, packM);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(this.color).setUv(1.0F, 0.0F).setUv1(this.size, this.size).setUv2(this.points, packM);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.STAR;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
