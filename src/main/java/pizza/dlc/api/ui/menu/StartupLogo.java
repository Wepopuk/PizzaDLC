package pizza.dlc.api.ui.menu;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuSampler;
import java.io.InputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import pizza.dlc.api.util.render.PizzaDLCSamplers;

public final class StartupLogo {
   private static final Identifier ID = Identifier.fromNamespaceAndPath("pizzadlc", "startup_logo_runtime");
   private static final String PATH = "/assets/pizzadlc/textures/gui/startup_logo.png";
   private static final float LOGO_FRACTION = 256.0F / 316.0F;
   private static final int BG_TOP = 0xFF101012;
   private static final int BG_BOTTOM = 0xFF08080A;
   private static boolean loaded;
   private static boolean failed;
   private static int texW;
   private static int texH;

   private StartupLogo() {
   }

   private static boolean ensureLoaded() {
      if (loaded || failed) return loaded;
      try (InputStream in = StartupLogo.class.getResourceAsStream(PATH)) {
         if (in == null) {
            failed = true;
            return false;
         }
         NativeImage image = NativeImage.read(in);
         texW = image.getWidth();
         texH = image.getHeight();
         DynamicTexture texture = new DynamicTexture(() -> "pizzadlc startup logo", image) {
            @Override
            public GpuSampler getSampler() {
               return PizzaDLCSamplers.linear();
            }
         };
         Minecraft.getInstance().getTextureManager().register(ID, texture);
         loaded = true;
      } catch (Throwable t) {
         failed = true;
      }
      return loaded;
   }

   public static void background(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int alpha) {
      g.fillGradient(x0, y0, x1, y1, ARGB.color(alpha, BG_TOP), ARGB.color(alpha, BG_BOTTOM));
   }

   public static boolean logo(GuiGraphicsExtractor g, RenderPipeline pipeline, float cx, float cy, int h, int color) {
      if (!ensureLoaded()) return false;
      int dh = Math.round(h / LOGO_FRACTION);
      int dw = Math.round(dh * (float)texW / texH);
      g.blit(pipeline, ID, Math.round(cx - dw / 2.0F), Math.round(cy - dh / 2.0F), 0.0F, 0.0F, dw, dh, texW, texH, texW, texH, color);
      return true;
   }

   public static void progressBar(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, float progress, float opacity) {
      int a = Math.round(Math.max(0.0F, Math.min(1.0F, opacity)) * 255.0F);
      int cy = (y0 + y1) / 2;
      int top = cy - 1;
      int track = ARGB.color(a, MenuKit.MODE_SEL);
      int fill = ARGB.color(a, MenuKit.TEXT);
      pill(g, x0, top, x1, track);
      int fx = x0 + Math.round((x1 - x0) * Math.max(0.0F, Math.min(1.0F, progress)));
      if (fx - x0 >= 3) {
         pill(g, x0, top, fx, fill);
      }
   }

   private static void pill(GuiGraphicsExtractor g, int x0, int y, int x1, int color) {
      g.fill(x0 + 1, y, x1 - 1, y + 1, color);
      g.fill(x0, y + 1, x1, y + 2, color);
      g.fill(x0 + 1, y + 2, x1 - 1, y + 3, color);
   }
}
