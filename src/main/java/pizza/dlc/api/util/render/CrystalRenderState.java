package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record CrystalRenderState(
   Matrix3x2fc pose,
   float x0,
   float y0,
   float x1,
   float y1,
   int colorTop,
   int colorBottom,
   int size,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public CrystalRenderState(
      Matrix3x2fc pose, float x0, float y0, float x1, float y1, int colorTop, int colorBottom, int size, @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x0, y0, x1, y1, colorTop, colorBottom, size, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(float x0, float y0, float x1, float y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      int ix = (int)Math.floor(x0);
      int iy = (int)Math.floor(y0);
      ScreenRectangle r = new ScreenRectangle(ix, iy, (int)Math.ceil(x1) - ix, (int)Math.ceil(y1) - iy).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int cr = this.colorBottom >> 16 & 0xFF;
      int cg = this.colorBottom >> 8 & 0xFF;
      int cb = this.colorBottom & 0xFF;
      int ca = this.colorBottom >>> 24 & 0xFF;
      int uv1x = cr | cg << 8;
      int uv1y = cb | ca << 8;
      int sz = Math.min(this.size, 255) & 0xFF;
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.colorTop).setUv(0.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(sz, 0);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(this.colorTop).setUv(0.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(sz, 0);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.colorTop).setUv(1.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(sz, 0);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(this.colorTop).setUv(1.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(sz, 0);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.CRYSTAL;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
