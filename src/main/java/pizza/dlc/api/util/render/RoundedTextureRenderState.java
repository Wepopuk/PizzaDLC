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

public record RoundedTextureRenderState(
   Matrix3x2fc pose,
   float x0,
   float y0,
   float x1,
   float y1,
   int color,
   float rtl,
   float rtr,
   float rbr,
   float rbl,
   boolean linear,
   Identifier texture,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public RoundedTextureRenderState(
      Matrix3x2fc pose,
      float x0,
      float y0,
      float x1,
      float y1,
      int color,
      float rtl,
      float rtr,
      float rbr,
      float rbl,
      boolean linear,
      Identifier texture,
      @Nullable ScreenRectangle scissorArea
   ) {
      this(pose, x0, y0, x1, y1, color, rtl, rtr, rbr, rbl, linear, texture, scissorArea, computeBounds(x0, y0, x1, y1, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(float x0, float y0, float x1, float y1, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      int ix = (int)Math.floor(x0);
      int iy = (int)Math.floor(y0);
      ScreenRectangle r = new ScreenRectangle(ix, iy, (int)Math.ceil(x1) - ix, (int)Math.ceil(y1) - iy).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      int w = Math.round(this.x1 - this.x0);
      int h = Math.round(this.y1 - this.y0);
      int packX = Math.round(this.rtl) & 0xFF | (Math.round(this.rtr) & 0xFF) << 8;
      int packY = Math.round(this.rbr) & 0xFF | (Math.round(this.rbl) & 0xFF) << 8;
      vc.addVertexWith2DPose(this.pose, this.x0, this.y0).setColor(this.color).setUv(0.0F, 0.0F).setUv1(w, h).setUv2(packX, packY);
      vc.addVertexWith2DPose(this.pose, this.x0, this.y1).setColor(this.color).setUv(0.0F, 1.0F).setUv1(w, h).setUv2(packX, packY);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y1).setColor(this.color).setUv(1.0F, 1.0F).setUv1(w, h).setUv2(packX, packY);
      vc.addVertexWith2DPose(this.pose, this.x1, this.y0).setColor(this.color).setUv(1.0F, 0.0F).setUv1(w, h).setUv2(packX, packY);
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.ROUNDED_TEXTURE;
   }

   public TextureSetup textureSetup() {
      try {
         GpuTextureView view = Minecraft.getInstance().getTextureManager().getTexture(this.texture).getTextureView();
         return TextureSetup.singleTexture(view, this.linear ? PizzaDLCSamplers.linear() : PizzaDLCSamplers.nearest());
      } catch (Throwable ignored) {
         return TextureSetup.noTexture();
      }
   }
}
