package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record GradientBlitRenderState(
   RenderPipeline pipeline,
   TextureSetup textureSetup,
   Matrix3x2fc pose,
   int x0,
   int y0,
   int x1,
   int y1,
   float u0,
   float u1,
   float v0,
   float v1,
   int colorTL,
   int colorBL,
   int colorBR,
   int colorTR,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public GradientBlitRenderState(
      RenderPipeline pipeline,
      TextureSetup textureSetup,
      Matrix3x2f pose,
      int x0,
      int y0,
      int x1,
      int y1,
      float u0,
      float u1,
      float v0,
      float v1,
      int colorTL,
      int colorBL,
      int colorBR,
      int colorTR,
      @Nullable ScreenRectangle scissorArea
   ) {
      this(
         pipeline,
         textureSetup,
         pose,
         x0,
         y0,
         x1,
         y1,
         u0,
         u1,
         v0,
         v1,
         colorTL,
         colorBL,
         colorBR,
         colorTR,
         scissorArea,
         getBounds(x0, y0, x1, y1, pose, scissorArea)
      );
   }

   public void buildVertices(VertexConsumer vc) {
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setUv(this.u0, this.v0).setColor(this.colorTL);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setUv(this.u0, this.v1).setColor(this.colorBL);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setUv(this.u1, this.v1).setColor(this.colorBR);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setUv(this.u1, this.v0).setColor(this.colorTR);
   }

   @Nullable
   private static ScreenRectangle getBounds(int x0, int y0, int x1, int y1, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea) {
      ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
   }
}
