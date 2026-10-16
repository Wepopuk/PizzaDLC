package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record RoundedOutlineRenderState(
   Matrix3x2fc pose,
   int x0,
   int y0,
   int x1,
   int y1,
   int color,
   float radius,
   float thickness,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public RoundedOutlineRenderState(
      Matrix3x2fc pose, int x0, int y0, int x1, int y1, int color, float radius, float thickness, @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x0, y0, x1, y1, color, radius, thickness, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(int x0, int y0, int x1, int y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      ScreenRectangle r = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int w = this.x1 - this.x0;
      int h = this.y1 - this.y0;
      int rr = Math.round(this.radius);
      int t10 = Math.round(this.thickness * 10.0F);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.color).setUv(0.0F, 0.0F).setUv1(w, h).setUv2(rr, t10);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(this.color).setUv(0.0F, 1.0F).setUv1(w, h).setUv2(rr, t10);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.color).setUv(1.0F, 1.0F).setUv1(w, h).setUv2(rr, t10);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(this.color).setUv(1.0F, 0.0F).setUv1(w, h).setUv2(rr, t10);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.ROUNDED_OUTLINE;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
