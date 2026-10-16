package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.render.msdf.MsdfGlyph;

public record MsdfTextRenderState(
   Matrix3x2fc pose,
   MsdfFont font,
   String text,
   float x,
   float y,
   float size,
   int color,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
   public MsdfTextRenderState(Matrix3x2fc pose, MsdfFont font, String text, float x, float y, float size, int color, @Nullable ScreenRectangle scissorArea) {
      this(pose, font, text, x, y, size, color, scissorArea, computeBounds(font, text, x, y, size, pose, scissorArea));
   }

   @Nullable
   private static ScreenRectangle computeBounds(MsdfFont font, String text, float x, float y, float size, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
      float width = font.width(text, size);
      int bx = (int)Math.floor(x - 0.25F * size);
      int by = (int)Math.floor(y - 0.4F * size);
      int bw = (int)Math.ceil(width + 0.5F * size);
      int bh = (int)Math.ceil(1.5F * size);
      ScreenRectangle r = new ScreenRectangle(bx, by, bw, bh).transformMaxBounds(pose);
      return scissor != null ? scissor.intersection(r) : r;
   }

   public void buildVertices(VertexConsumer vc) {
      float penX = this.x;
      float baseline = this.y + 0.7F * this.size;

      for (int i = 0; i < this.text.length(); i++) {
         MsdfGlyph g = this.font.glyph(this.text.charAt(i));
         if (g != null) {
            if (g.width > 0.0F && g.height > 0.0F) {
               float gy0 = baseline - g.topPos * this.size;
               float gx1 = penX + g.width * this.size;
               float gy1 = gy0 + g.height * this.size;
               vc.addVertexWith2DPose(this.pose, penX, gy0).setColor(this.color).setUv(g.minU, g.minV);
               vc.addVertexWith2DPose(this.pose, penX, gy1).setColor(this.color).setUv(g.minU, g.maxV);
               vc.addVertexWith2DPose(this.pose, gx1, gy1).setColor(this.color).setUv(g.maxU, g.maxV);
               vc.addVertexWith2DPose(this.pose, gx1, gy0).setColor(this.color).setUv(g.maxU, g.minV);
            }

            penX += g.advance * this.size;
         }
      }
   }

   public RenderPipeline pipeline() {
      return PizzaDLCPipelines.MSDF;
   }

   public TextureSetup textureSetup() {
      return TextureSetup.singleTexture(this.font.atlasView(), PizzaDLCSamplers.linear());
   }
}
