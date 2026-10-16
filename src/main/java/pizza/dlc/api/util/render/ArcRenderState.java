package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record ArcRenderState(
   Matrix3x2fc pose,
   float x0,
   float y0,
   float x1,
   float y1,
   int color,
   int colorB,
   float radiusN,
   float thickN,
   float sweepDeg,
   float phase,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public ArcRenderState(
      Matrix3x2fc pose, float x0, float y0, float x1, float y1, int color, float radiusN, float thickN, float sweepDeg, @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x0, y0, x1, y1, color, color, radiusN, thickN, sweepDeg, 0.0F, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   public ArcRenderState(
      Matrix3x2fc pose,
      float x0,
      float y0,
      float x1,
      float y1,
      int color,
      int colorB,
      float radiusN,
      float thickN,
      float sweepDeg,
      float phase,
      @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x0, y0, x1, y1, color, colorB, radiusN, thickN, sweepDeg, phase, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(float x0, float y0, float x1, float y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      int ix = (int)Math.floor(x0);
      int iy = (int)Math.floor(y0);
      ScreenRectangle r = new ScreenRectangle(ix, iy, (int)Math.ceil(x1) - ix, (int)Math.ceil(y1) - iy).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int r = this.colorB >> 16 & 0xFF;
      int g = this.colorB >> 8 & 0xFF;
      int b = this.colorB & 0xFF;
      int a = this.colorB >>> 24 & 0xFF;
      int uv1x = r | g << 8;
      int uv1y = b | a << 8;
      int rN = clampByte(Math.round(this.radiusN * 255.0F));
      int tN = clampByte(Math.round(this.thickN * 255.0F));
      int swB = clampByte(Math.round(Math.min(Math.max(this.sweepDeg, 0.0F), 360.0F) * 0.7083333F));
      int phB = clampByte(Math.round((this.phase % 1.0F + 1.0F) % 1.0F * 255.0F));
      int uv2x = rN | tN << 8;
      int uv2y = swB | phB << 8;
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.color).setUv(0.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(this.color).setUv(0.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.color).setUv(1.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(this.color).setUv(1.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
   }

   private static int clampByte(int v) {
      return v < 0 ? 0 : (v > 255 ? 255 : v);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.ARC;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
