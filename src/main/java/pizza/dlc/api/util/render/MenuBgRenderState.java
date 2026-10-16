package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record MenuBgRenderState(
   Matrix3x2fc pose, int x0, int y0, int x1, int y1, int timeFp, @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public MenuBgRenderState(Matrix3x2fc pose, int x0, int y0, int x1, int y1, int timeFp, @Nullable ScreenRectangle scissorArea) {
      this(pose, x0, y0, x1, y1, timeFp, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(int x0, int y0, int x1, int y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      ScreenRectangle r = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int w = this.x1 - this.x0;
      int h = this.y1 - this.y0;
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(-1).setUv(0.0F, 0.0F).setUv1(w, h).setUv2(this.timeFp, 0);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(-1).setUv(0.0F, 1.0F).setUv1(w, h).setUv2(this.timeFp, 0);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(-1).setUv(1.0F, 1.0F).setUv1(w, h).setUv2(this.timeFp, 0);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(-1).setUv(1.0F, 0.0F).setUv1(w, h).setUv2(this.timeFp, 0);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.MENU_BG;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.noTexture();
   }
}
