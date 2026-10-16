package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record RoundedGradientRenderState(
   Matrix3x2fc pose,
   int x0,
   int y0,
   int x1,
   int y1,
   int colorLeft,
   int colorRight,
   int radius,
   int cornerMask,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public RoundedGradientRenderState(
      Matrix3x2fc pose, int x0, int y0, int x1, int y1, int colorLeft, int colorRight, int radius, int cornerMask, @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x0, y0, x1, y1, colorLeft, colorRight, radius, cornerMask, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(int x0, int y0, int x1, int y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      ScreenRectangle r = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int w = this.x1 - this.x0;
      int h = this.y1 - this.y0;
      int cr = this.colorRight >> 16 & 0xFF;
      int cg = this.colorRight >> 8 & 0xFF;
      int cb = this.colorRight & 0xFF;
      int ca = this.colorRight >>> 24 & 0xFF;
      int uv1x = cr | cg << 8;
      int uv1y = cb | ca << 8;
      int uv2x = Math.min(w, 255) & 0xFF | (Math.min(h, 255) & 0xFF) << 8;
      int uv2y = Math.min(this.radius, 255) & 0xFF | (this.cornerMask & 0xFF) << 8;
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.colorLeft).setUv(0.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(this.colorLeft).setUv(0.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.colorLeft).setUv(1.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(this.colorLeft).setUv(1.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.ROUNDED_GRAD;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
