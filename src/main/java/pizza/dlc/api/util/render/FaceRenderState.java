package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

public record FaceRenderState(
   Matrix3x2fc pose,
   int x,
   int y,
   int size,
   int radius,
   int uOff,
   int vOff,
   int color,
   Identifier skin,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public FaceRenderState(
      Matrix3x2fc pose, int x, int y, int size, int radius, int uOff, int vOff, int color, Identifier skin, @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x, y, size, radius, uOff, vOff, color, skin, scissorArea, computeBounds(x, y, size, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(int x, int y, int size, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      ScreenRectangle r = new ScreenRectangle(x, y, size, size).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int x1 = this.x + this.size;
      int y1 = this.y + this.size;
      int uv1x = this.size;
      int uv1y = this.radius;
      vc.addVertexWith2DPose(this.pose, this.x, this.y).setColor(this.color).setUv(0.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(this.uOff, this.vOff);
      vc.addVertexWith2DPose(this.pose, this.x, y1).setColor(this.color).setUv(0.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(this.uOff, this.vOff);
      vc.addVertexWith2DPose(this.pose, x1, y1).setColor(this.color).setUv(1.0F, 1.0F).setUv1(uv1x, uv1y).setUv2(this.uOff, this.vOff);
      vc.addVertexWith2DPose(this.pose, x1, this.y).setColor(this.color).setUv(1.0F, 0.0F).setUv1(uv1x, uv1y).setUv2(this.uOff, this.vOff);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.FACE;
   }

   public TextureSetup textureSetup() {
      try {
         GpuTextureView view = Minecraft.getInstance().getTextureManager().getTexture(this.skin).getTextureView();
         return TextureSetup.singleTexture(view, PizzaDLCSamplers.nearest());
      } catch (Throwable ignored) {
         return TextureSetup.noTexture();
      }
   }
}
