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

public final class ShaderEspGlow {
   public static final int DIR_OUT = 0;
   public static final int DIR_IN = 1;
   public static final int DIR_BOTH = 2;
   private static TextureTarget edges;
   private static TextureTarget ping;
   private static TextureTarget pong;
   private static int lastFW = -1;
   private static int lastFH = -1;
   private static int lastHW = -1;
   private static int lastHH = -1;

   private ShaderEspGlow() {
   }

   public static void render(RenderTarget outline, int steps, boolean contour, int dir) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      if (main != null && outline != null) {
         steps = Math.max(1, steps);
         int fw = Math.max(1, outline.width);
         int fh = Math.max(1, outline.height);
         int hw = Math.max(1, fw / 2);
         int hh = Math.max(1, fh / 2);
         ensure(fw, fh, hw, hh);
         CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
         run(enc, PizzaDLCPipelines.SHADERESP_EDGE, outline.getColorTextureView(), edges.getColorTextureView());
         run(enc, PizzaDLCPipelines.SHADERESP_BLUR, edges.getColorTextureView(), ping.getColorTextureView());
         TextureTarget src = ping;
         TextureTarget dst = pong;

         for (int i = 1; i < steps; i++) {
            run(enc, PizzaDLCPipelines.SHADERESP_BLUR, src.getColorTextureView(), dst.getColorTextureView());
            TextureTarget t = src;
            src = dst;
            dst = t;
         }

         GpuTextureView mainColor = main.getColorTextureView();
         GpuTextureView glow = src.getColorTextureView();
         if (dir == 0) {
            run2(enc, PizzaDLCPipelines.SHADERESP_DIR_OUT, glow, outline.getColorTextureView(), mainColor);
         } else if (dir == 1) {
            run2(enc, PizzaDLCPipelines.SHADERESP_DIR_IN, glow, outline.getColorTextureView(), mainColor);
         } else {
            run2(enc, PizzaDLCPipelines.SHADERESP_DIR_BOTH, glow, outline.getColorTextureView(), mainColor);
         }

         if (contour) {
            run(enc, PizzaDLCPipelines.SHADERESP_GLOW, edges.getColorTextureView(), mainColor);
         }
      }
   }

   public static void edgeInPlace(RenderTarget outline) {
      if (outline == null) return;

      int fw = Math.max(1, outline.width);
      int fh = Math.max(1, outline.height);
      ensure(fw, fh, Math.max(1, fw / 2), Math.max(1, fh / 2));
      CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
      run(enc, PizzaDLCPipelines.SHADERESP_EDGE, outline.getColorTextureView(), edges.getColorTextureView());
      run(enc, PizzaDLCPipelines.SATURATION_COPY, edges.getColorTextureView(), outline.getColorTextureView());
   }

   private static void run(CommandEncoder enc, RenderPipeline pipe, GpuTextureView src, GpuTextureView dst) {
      RenderPass pass = enc.createRenderPass(() -> "pizzadlc_shaderesp", dst, Optional.empty());

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

   private static void run2(CommandEncoder enc, RenderPipeline pipe, GpuTextureView s0, GpuTextureView s1, GpuTextureView dst) {
      RenderPass pass = enc.createRenderPass(() -> "pizzadlc_shaderesp2", dst, Optional.empty());

      try {
         pass.setPipeline(pipe);
         RenderSystem.bindDefaultUniforms(pass);
         pass.bindTexture("Sampler0", s0, PizzaDLCSamplers.linear());
         pass.bindTexture("Sampler1", s1, PizzaDLCSamplers.linear());
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

   private static void ensure(int fw, int fh, int hw, int hh) {
      if (edges != null && lastFW == fw && lastFH == fh && lastHW == hw && lastHH == hh) return;

      destroy();
      edges = new TextureTarget("pizzadlc_shaderesp_edges", fw, fh, false, GpuFormat.RGBA8_UNORM);
      ping = new TextureTarget("pizzadlc_shaderesp_ping", hw, hh, false, GpuFormat.RGBA8_UNORM);
      pong = new TextureTarget("pizzadlc_shaderesp_pong", hw, hh, false, GpuFormat.RGBA8_UNORM);
      lastFW = fw;
      lastFH = fh;
      lastHW = hw;
      lastHH = hh;
   }

   private static void destroy() {
      if (edges != null) {
         edges.destroyBuffers();
      }

      if (ping != null) {
         ping.destroyBuffers();
      }

      if (pong != null) {
         pong.destroyBuffers();
      }

      pong = null;
      ping = null;
      edges = null;
      lastHH = -1;
      lastHW = -1;
      lastFH = -1;
      lastFW = -1;
   }
}
