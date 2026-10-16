package pizza.dlc.api.util.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice.MappedView;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;

public final class SaturationRenderer {
   private static TextureTarget temp;
   private static int lastW = -1;
   private static int lastH = -1;
   private static MappableRingBuffer ubo;

   private SaturationRenderer() {
   }

   public static void apply(float sat) {
      Minecraft mc = Minecraft.getInstance();
      RenderTarget main = mc.gameRenderer.mainRenderTarget();
      if (main != null) {
         ensure(main.width, main.height);
         if (ubo == null) {
            ubo = new MappableRingBuffer(() -> "pizzadlc_sat_ubo", 130, 16);
         }

         MappedView view = ubo.currentBuffer().map(false, true);

         try {
            Std140Builder.intoBuffer(view.data()).putFloat(sat);
         } catch (Throwable ex) {
            if (view != null) {
               try {
                  view.close();
               } catch (Throwable ex2) {
                  ex.addSuppressed(ex2);
               }
            }

            throw ex;
         }

         if (view != null) {
            view.close();
         }

         CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
         RenderPass p = enc.createRenderPass(() -> "pizzadlc_sat_copy", temp.getColorTextureView(), Optional.empty());

         try {
            p.setPipeline(PizzaDLCPipelines.SATURATION_COPY);
            RenderSystem.bindDefaultUniforms(p);
            p.bindTexture("InSampler", main.getColorTextureView(), PizzaDLCSamplers.linear());
            p.draw(3, 1, 0, 0);
         } catch (Throwable ex) {
            if (p != null) {
               try {
                  p.close();
               } catch (Throwable ex2) {
                  ex.addSuppressed(ex2);
               }
            }

            throw ex;
         }

         if (p != null) {
            p.close();
         }

         p = enc.createRenderPass(() -> "pizzadlc_sat", main.getColorTextureView(), Optional.empty());

         try {
            p.setPipeline(PizzaDLCPipelines.SATURATION);
            RenderSystem.bindDefaultUniforms(p);
            p.bindTexture("InSampler", temp.getColorTextureView(), PizzaDLCSamplers.linear());
            p.setUniform("SatConfig", ubo.currentBuffer());
            p.draw(3, 1, 0, 0);
         } catch (Throwable ex) {
            if (p != null) {
               try {
                  p.close();
               } catch (Throwable ex2) {
                  ex.addSuppressed(ex2);
               }
            }

            throw ex;
         }

         if (p != null) {
            p.close();
         }

         ubo.rotate();
      }
   }

   private static void ensure(int w, int h) {
      if (temp != null && lastW == w && lastH == h) return;

      if (temp != null) {
         temp.destroyBuffers();
      }

      temp = new TextureTarget("pizzadlc_sat_temp", w, h, false, GpuFormat.RGBA8_UNORM);
      lastW = w;
      lastH = h;
   }
}
