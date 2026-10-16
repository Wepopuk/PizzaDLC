package pizza.dlc.api.util.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import pizza.dlc.api.render.msdf.PizzaDLCFonts;
import pizza.dlc.api.render.msdf.MsdfFont;
import pizza.dlc.api.render.msdf.ReplaceSymbols;
import pizza.dlc.api.util.color.ColorRGBA;
import pizza.dlc.mixin.client.GuiGraphicsExtractorAccessor;

public final class Render2DUtil {
   private static final int PANEL_SHADOW_SPREAD = 4;
   private static float opacity = 1.0F;

   private Render2DUtil() {
   }

   public static float opacity() {
      return opacity;
   }

   public static void setOpacity(float value) {
      opacity = Math.max(0.0F, Math.min(1.0F, value));
   }

   public static int op(int argb) {
      if (opacity >= 0.999F) return argb;
      int a = Math.round((argb >>> 24) * opacity);
      return argb & 0xFFFFFF | a << 24;
   }

   public static void rect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int argb) {
      argb = op(argb);
      ctx.fill(x, y, x + w, y + h, argb);
   }

   public static void rect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, ColorRGBA color) {
      ctx.fill(x, y, x + w, y + h, op(color.getRGB()));
   }

   public static void text(GuiGraphicsExtractor ctx, String text, int x, int y, int argb, boolean shadow) {
      argb = op(argb);
      ctx.text(Minecraft.getInstance().font, text, x, y, argb, shadow);
   }

   public static int textWidth(String text) {
      return Minecraft.getInstance().font.width(text);
   }

   public static int fontHeight() {
      return 9;
   }

   public static void roundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float radius, int argb) {
      argb = op(argb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new RoundedRectRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argb, radius, ctx.scissorStack.peek()));
   }

   public static void roundedRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int argb) {
      argb = op(argb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new RoundedRectRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argb, radius, ctx.scissorStack.peek()));
   }

   public static void roundedOutline(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float radius, float thickness, int argb) {
      argb = op(argb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new RoundedOutlineRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argb, radius, thickness, ctx.scissorStack.peek()));
   }

   public static void chevron(GuiGraphicsExtractor ctx, int x, int y, int w, int h, boolean up, float thickness, int argb) {
      argb = op(argb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new ChevronRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argb, up ? 1 : 0, thickness, ctx.scissorStack.peek()));
   }

   public static void check(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float thickness, int argb) {
      argb = op(argb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new ChevronRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argb, 2, thickness, ctx.scissorStack.peek()));
   }

   public static void glow(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int glowColor, int spread) {
      glowColor = op(glowColor);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new GlowRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, glowColor, radius, spread, ctx.scissorStack.peek()));
   }

   public static void glowContour(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int radius, int glowColor, int spread) {
      glowColor = op(glowColor);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new GlowRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, glowColor, -radius, spread, ctx.scissorStack.peek()));
   }

   public static void softShadow(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float depth, float alpha) {
      if (alpha <= 0.01F || w <= 0.0F || h <= 0.0F) return;
      int ix = Math.round(x);
      int iy = Math.round(y);
      int iw = Math.round(w);
      int ih = Math.round(h);
      int r = Math.round(radius);
      int ambient = Math.round(0x2E * Math.min(1.0F, alpha));
      int key = Math.round(0x52 * Math.min(1.0F, alpha));
      glow(ctx, ix, iy, iw, ih, r, ambient << 24, Math.max(2, Math.round(depth * 1.6F)));
      glow(ctx, ix, iy + Math.max(1, Math.round(depth * 0.35F)), iw, ih, r, key << 24, Math.max(2, Math.round(depth * 0.8F)));
   }

   public static int panelShadowSpread() {
      return PANEL_SHADOW_SPREAD;
   }

   public static void panelShadow(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, float alphaMul) {
      int a = Math.round(69.0F * Math.max(0.0F, Math.min(1.0F, alphaMul)));
      if (a > 0) {
         glow(ctx, Math.round(x), Math.round(y), Math.round(w), Math.round(h), Math.round(radius), 0 | a << 24, PANEL_SHADOW_SPREAD);
      }
   }

   public static void glassRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float radius, int tintArgb) {
      tintArgb = op(tintArgb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState().addGuiElement(new GlassRectRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, tintArgb, radius, null));
   }

   public static void glassRect(GuiGraphicsExtractor ctx, float x, float y, float w, float h, float rtl, float rtr, float rbr, float rbl, int tintArgb) {
      tintArgb = op(tintArgb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState().addGuiElement(new GlassRectRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, tintArgb, rtl, rtr, rbr, rbl, null));
   }

   public static void roundedFace(GuiGraphicsExtractor ctx, Identifier skin, int x, int y, int size, int radius, int uOff, int vOff, int tint) {
      tint = op(tint);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState().addGuiElement(new FaceRenderState(new Matrix3x2f(ctx.pose()), x, y, size, radius, uOff, vOff, tint, skin, ctx.scissorStack.peek()));
   }

   public static void roundedTexture(GuiGraphicsExtractor ctx, Identifier texture, float x, float y, float w, float h, float radius, boolean linear, int tint) {
      roundedTexture(ctx, texture, x, y, w, h, radius, radius, radius, radius, linear, tint);
   }

   public static void roundedTexture(
      GuiGraphicsExtractor ctx, Identifier texture, float x, float y, float w, float h, float rtl, float rtr, float rbr, float rbl, boolean linear, int tint
   ) {
      tint = op(tint);
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(
               new RoundedTextureRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, tint, rtl, rtr, rbr, rbl, linear, texture, ctx.scissorStack.peek())
            );
      }
   }

   public static void circleTexture(GuiGraphicsExtractor ctx, Identifier texture, float x, float y, float d, boolean linear, int tint) {
      roundedTexture(ctx, texture, x, y, d, d, d / 2.0F, linear, tint);
   }

   public static void image(GuiGraphicsExtractor ctx, Identifier texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int tint) {
      tint = op(tint);
      AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(
            new BlitRenderState(
               RenderPipelines.GUI_TEXTURED,
               TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler()),
               new Matrix3x2f(ctx.pose()),
               x0,
               y0,
               x1,
               y1,
               u0,
               u1,
               v0,
               v1,
               tint,
               null
            )
         );
   }

   public static void imageGradient(
      GuiGraphicsExtractor ctx, Identifier texture, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int colorTop, int colorBottom
   ) {
      colorTop = op(colorTop);
      colorBottom = op(colorBottom);
      AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(
            new GradientBlitRenderState(
               RenderPipelines.GUI_TEXTURED,
               TextureSetup.singleTexture(tex.getTextureView(), tex.getSampler()),
               new Matrix3x2f(ctx.pose()),
               x0,
               y0,
               x1,
               y1,
               u0,
               u1,
               v0,
               v1,
               colorTop,
               colorBottom,
               colorBottom,
               colorTop,
               ctx.scissorStack.peek()
            )
         );
   }

   public static void line(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, float thick, int argb) {
      argb = op(argb);
      float dx = x2 - x1;
      float dy = y2 - y1;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(len < 0.5F)) {
         int h = Math.max(1, Math.round(thick));
         ctx.pose().pushMatrix();
         ctx.pose().translate(x1, y1);
         ctx.pose().rotate((float)Math.atan2(dy, dx));
         ctx.fill(0, -h / 2, Math.round(len), -h / 2 + h, argb);
         ctx.pose().popMatrix();
      }
   }

   public static void roundedRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float rtl, float rtr, float rbr, float rbl, int argb) {
      argb = op(argb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new RoundedRectRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argb, rtl, rtr, rbr, rbl, ctx.scissorStack.peek()));
   }

   public static void arc(GuiGraphicsExtractor ctx, float x, float y, int w, int h, float radiusN, float thickN, float sweepDeg, int argb) {
      argb = op(argb);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState().addGuiElement(new ArcRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argb, radiusN, thickN, sweepDeg, null));
   }

   public static void arcGradient(
      GuiGraphicsExtractor ctx, float x, float y, int w, int h, float radiusN, float thickN, float sweepDeg, int argbA, int argbB, float phase
   ) {
      argbA = op(argbA);
      argbB = op(argbB);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new ArcRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, argbA, argbB, radiusN, thickN, sweepDeg, phase, null));
   }

   public static void star(GuiGraphicsExtractor ctx, float cx, float cy, float size, int points, float m, float rot, int argb) {
      argb = op(argb);
      int sz = Math.max(1, Math.round(size));
      float h = sz * 0.5F;
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      if (rot != 0.0F) {
         ctx.pose().rotate(rot);
      }

      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState().addGuiElement(new StarRenderState(new Matrix3x2f(ctx.pose()), -h, -h, h, h, argb, sz, points, m, ctx.scissorStack.peek()));
      ctx.pose().popMatrix();
   }

   public static void crystal(GuiGraphicsExtractor ctx, float cx, float cy, float size, float rot, int argbTop, int argbBottom) {
      argbTop = op(argbTop);
      argbBottom = op(argbBottom);
      int sz = Math.max(1, Math.round(size));
      float h = sz * 0.5F;
      ctx.pose().pushMatrix();
      ctx.pose().translate(cx, cy);
      if (rot != 0.0F) {
         ctx.pose().rotate(rot);
      }

      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState()
         .addGuiElement(new CrystalRenderState(new Matrix3x2f(ctx.pose()), -h, -h, h, h, argbTop, argbBottom, sz, ctx.scissorStack.peek()));
      ctx.pose().popMatrix();
   }

   public static void gradientRect(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int colorLeft, int colorRight, float bottomRadius) {
      colorLeft = op(colorLeft);
      colorRight = op(colorRight);
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(bottomRadius, Math.min(w / 2.0F, (float)h))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(
               new RoundedGradientRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, colorLeft, colorRight, r, 12, ctx.scissorStack.peek())
            );
      }
   }

   public static void gradientRectFull(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int colorLeft, int colorRight, float radius) {
      colorLeft = op(colorLeft);
      colorRight = op(colorRight);
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(radius, Math.min(w / 2.0F, h / 2.0F))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(
               new RoundedGradientRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, colorLeft, colorRight, r, 15, ctx.scissorStack.peek())
            );
      }
   }

   public static void gradientRectDiag(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int colorTL, int colorBR, float radius) {
      colorTL = op(colorTL);
      colorBR = op(colorBR);
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(radius, Math.min(w / 2.0F, h / 2.0F))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(new RoundedGradientRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, colorTL, colorBR, r, 47, ctx.scissorStack.peek()));
      }
   }

   public static void gradientRectV(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int colorTop, int colorBottom, float bottomRadius) {
      colorTop = op(colorTop);
      colorBottom = op(colorBottom);
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(bottomRadius, Math.min(w / 2.0F, (float)h))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(
               new RoundedGradientRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, colorTop, colorBottom, r, 28, ctx.scissorStack.peek())
            );
      }
   }

   public static void gradientRectVTop(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int colorTop, int colorBottom, float topRadius) {
      colorTop = op(colorTop);
      colorBottom = op(colorBottom);
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(topRadius, Math.min(w / 2.0F, (float)h))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(
               new RoundedGradientRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, colorTop, colorBottom, r, 19, ctx.scissorStack.peek())
            );
      }
   }

   public static void roundedRadial(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int colorInner, int colorOuter, float radius) {
      colorInner = op(colorInner);
      colorOuter = op(colorOuter);
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(radius, Math.min(w / 2.0F, h / 2.0F))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(
               new RoundedGradientRenderState(new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, colorInner, colorOuter, r, 79, ctx.scissorStack.peek())
            );
      }
   }

   public static void svSquare(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int hueColor, float radius) {
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(radius, Math.min(w / 2.0F, h / 2.0F))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(new PickerRenderState(0, new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, hueColor, r, 15, ctx.scissorStack.peek()));
      }
   }

   public static void hueBar(GuiGraphicsExtractor ctx, int x, int y, int w, int h, float radius) {
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(radius, Math.min(w / 2.0F, h / 2.0F))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(new PickerRenderState(1, new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, 0, r, 15, ctx.scissorStack.peek()));
      }
   }

   public static void alphaBar(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color, float radius) {
      color = op(color);
      if (w > 0 && h > 0) {
         int r = Math.round(Math.max(0.0F, Math.min(radius, Math.min(w / 2.0F, h / 2.0F))));
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState()
            .addGuiElement(new PickerRenderState(2, new Matrix3x2f(ctx.pose()), x, y, x + w, y + h, color | 0xFF000000, r, 15, ctx.scissorStack.peek()));
      }
   }

   public static void menuBackground(GuiGraphicsExtractor ctx, int width, int height) {
      if (width > 0 && height > 0) {
         int timeFp = (int)(System.nanoTime() / 1.0E9 % 320.0 * 100.0);
         GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
         acc.pizzadlc$guiRenderState().addGuiElement(new MenuBgRenderState(new Matrix3x2f(ctx.pose()), 0, 0, width, height, timeFp, null));
      }
   }

   public static int lerpColor(int a, int b, float t) {
      t = t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
      int aA = a >>> 24 & 0xFF;
      int aR = a >> 16 & 0xFF;
      int aG = a >> 8 & 0xFF;
      int aB = a & 0xFF;
      int bA = b >>> 24 & 0xFF;
      int bR = b >> 16 & 0xFF;
      int bG = b >> 8 & 0xFF;
      int bB = b & 0xFF;
      return (int)(aA + (bA - aA) * t) << 24 | (int)(aR + (bR - aR) * t) << 16 | (int)(aG + (bG - aG) * t) << 8 | (int)(aB + (bB - aB) * t);
   }

   public static void msdfText(GuiGraphicsExtractor ctx, String text, float x, float y, float size, int argb) {
      msdfText(ctx, PizzaDLCFonts.regular(), text, x, y, size, argb);
   }

   public static void msdfText(GuiGraphicsExtractor ctx, MsdfFont font, String text, float x, float y, float size, int argb) {
      argb = op(argb);
      text = ReplaceSymbols.normalize(text);
      GuiGraphicsExtractorAccessor acc = (GuiGraphicsExtractorAccessor)ctx;
      acc.pizzadlc$guiRenderState().addGuiElement(new MsdfTextRenderState(new Matrix3x2f(ctx.pose()), font, text, x, y, size, argb, ctx.scissorStack.peek()));
   }

   public static float msdfWidth(String text, float size) {
      return PizzaDLCFonts.regular().width(text, size);
   }

   public static float msdfVisualWidth(String text, float size) {
      return PizzaDLCFonts.regular().visualWidth(text, size);
   }

   public static float msdfVisualWidth(MsdfFont font, String text, float size) {
      return font.visualWidth(text, size);
   }

   public static float msdfWidth(MsdfFont font, String text, float size) {
      return font.width(text, size);
   }

   @Deprecated
   public static void blurBehind(GuiGraphicsExtractor ctx) {
      ctx.blurBeforeThisStratum();
   }
}
