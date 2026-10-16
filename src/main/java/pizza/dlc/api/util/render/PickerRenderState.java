package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record PickerRenderState(
   int mode,
   Matrix3x2fc pose,
   int x0,
   int y0,
   int x1,
   int y1,
   int color,
   int radius,
   int cornerMask,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public static final int MODE_SV = 0;
   public static final int MODE_HUE = 1;
   public static final int MODE_ALPHA = 2;

   public PickerRenderState(
      int mode, Matrix3x2fc pose, int x0, int y0, int x1, int y1, int color, int radius, int cornerMask, @Nullable ScreenRectangle scissorArea
   ) {
      this(mode, pose, x0, y0, x1, y1, color, radius, cornerMask, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(int x0, int y0, int x1, int y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      ScreenRectangle r = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int w = this.x1 - this.x0;
      int h = this.y1 - this.y0;
      int cr = this.color >> 16 & 0xFF;
      int cg = this.color >> 8 & 0xFF;
      int cb = this.color & 0xFF;
      int ca = this.color >>> 24 & 0xFF;
      int uv1x = cr | cg << 8;
      int uv1y = cb | ca << 8;
      int uv2x = Math.min(w, 255) & 0xFF | (Math.min(h, 255) & 0xFF) << 8;
      int uv2y = Math.min(this.radius, 255) & 0xFF | (this.cornerMask & 0xFF) << 8;
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(-1).setUv(0.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(-1).setUv(0.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(-1).setUv(1.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(-1).setUv(1.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(uv2x, uv2y);
   }

   public RenderPipeline pipeline() {
      return this.mode == 1 ? PizzaDLCPipelines.HUE_BAR : (this.mode == 2 ? PizzaDLCPipelines.ALPHA_BAR : PizzaDLCPipelines.SV_SQUARE);
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
