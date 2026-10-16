package pizza.dlc.api.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public final class KawaseBlur {
   private static final int DOWNSAMPLE = 4;
   private static TextureTarget[] pyramid;
   private static int lastBaseW = -1;
   private static int lastBaseH = -1;
   private static int lastSteps = -1;
   private static boolean requested;
   private static boolean requestedPrev;

   private KawaseBlur() {
   }

   public static void request() {
      requested = true;
   }

   public static boolean pollAndRoll() {
      boolean any = requested || requestedPrev;
      requestedPrev = requested;
      requested = false;
      return any;
   }

   public static GpuTextureView blurView() {
      return pyramid != null && pyramid.length > 0 && pyramid[0] != null ? pyramid[0].getColorTextureView() : null;
   }

   public static void prepare() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      if (main != null) {
         int baseW = Math.max(1, main.width / DOWNSAMPLE);
         int baseH = Math.max(1, main.height / DOWNSAMPLE);
         ensurePyramid(baseW, baseH, 1);
         CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
         GpuTextureView src = main.getColorTextureView();

         for (int i = 0; i <= 1; i++) {
            runPass(enc, PizzaDLCPipelines.KAWASE_DOWN, src, pyramid[i]);
            src = pyramid[i].getColorTextureView();
         }

         for (int i = 0; i >= 0; i--) {
            runPass(enc, PizzaDLCPipelines.KAWASE_UP, src, pyramid[i]);
            src = pyramid[i].getColorTextureView();
         }
      }
   }

   public static void renderMenuBackground() {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      if (main != null) {
         CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
         RenderPass pass = enc.createRenderPass(() -> "pizzadlc_menubg_pass", main.getColorTextureView(), Optional.empty());

         try {
            pass.setPipeline(PizzaDLCPipelines.MENU_BG_PASS);
            RenderSystem.bindDefaultUniforms(pass);
            pass.draw(3, 1, 0, 0);
         } catch (Throwable ex) {
            if (pass != null) {
               try {
                  pass.close();
               } catch (Throwable ex2) {
                  ex.addSuppressed(ex2);
               }
            }

            throw ex;
         }

         if (pass != null) {
            pass.close();
         }
      }
   }

   private static void runPass(CommandEncoder enc, RenderPipeline pipe, GpuTextureView src, TextureTarget dst) {
      RenderPass pass = enc.createRenderPass(() -> "pizzadlc_kawase", dst.getColorTextureView(), Optional.empty());

      try {
         pass.setPipeline(pipe);
         RenderSystem.bindDefaultUniforms(pass);
         pass.bindTexture("InSampler", src, PizzaDLCSamplers.linear());
         pass.draw(3, 1, 0, 0);
      } catch (Throwable ex) {
         if (pass != null) {
            try {
               pass.close();
            } catch (Throwable ex2) {
               ex.addSuppressed(ex2);
            }
         }

         throw ex;
      }

      if (pass != null) {
         pass.close();
      }
   }

   private static void ensurePyramid(int baseW, int baseH, int steps) {
      if (pyramid != null && lastBaseW == baseW && lastBaseH == baseH && lastSteps == steps) return;

      destroyPyramid();
      pyramid = new TextureTarget[steps + 1];

      for (int i = 0; i <= steps; i++) {
         int w = Math.max(1, baseW >> i);
         int h = Math.max(1, baseH >> i);
         pyramid[i] = new TextureTarget("pizzadlc_blur_" + i, w, h, false, GpuFormat.RGBA8_UNORM);
      }

      lastBaseW = baseW;
      lastBaseH = baseH;
      lastSteps = steps;
   }

   private static void destroyPyramid() {
      if (pyramid != null) {
         for (TextureTarget t : pyramid) {
            if (t == null) continue;

            t.destroyBuffers();
         }

         pyramid = null;
      }

      lastSteps = -1;
      lastBaseH = -1;
      lastBaseW = -1;
   }
}
