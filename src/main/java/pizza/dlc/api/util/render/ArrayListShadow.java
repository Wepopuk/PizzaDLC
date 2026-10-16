package pizza.dlc.api.util.render;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.GpuBufferSlice.MappedView;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;

public final class ArrayListShadow {
   private static final int MAXN = 32;
   private static final int UBO_SIZE = 544;
   private static final float[] rects = new float[128];
   private static int count = 0;
   private static float maxV = 0.0F;
   private static float sRadius = 2.0F;
   private static boolean sBarLeft = false;
   private static MappableRingBuffer ubo;

   private ArrayListShadow() {
   }

   public static void submit(float x, float y, float w, float h, float rad, boolean barLeft, float v) {
      if (count < MAXN) {
         int o = count * 4;
         rects[o] = x;
         rects[o + 1] = y;
         rects[o + 2] = w;
         rects[o + 3] = h;
         sRadius = rad;
         sBarLeft = barLeft;
         if (v > maxV) {
            maxV = v;
         }

         count++;
      }
   }

   public static void render() {
      int n = count;
      float mv = maxV;
      count = 0;
      maxV = 0.0F;
      if (n > 0 && !(mv <= 0.001F)) {
         Minecraft mc = Minecraft.getInstance();
         RenderTarget main = mc.gameRenderer.mainRenderTarget();
         if (main != null) {
            double gs = mc.getWindow().getGuiScale();
            float alpha = 0.27058825F * mv;
            if (ubo == null) {
               ubo = new MappableRingBuffer(() -> "pizzadlc_arrayshadow_ubo", 130, UBO_SIZE);
            }

            MappedView view = ubo.currentBuffer().map(false, true);

            try {
               Std140Builder b = Std140Builder.intoBuffer(view.data());
               float rad = sRadius;
               float rtl = sBarLeft ? 0.0F : rad;
               float rtr = sBarLeft ? rad : 0.0F;
               float rbr = sBarLeft ? rad : 0.0F;
               float rbl = sBarLeft ? 0.0F : rad;
               b.putVec4(n, (float)(4.0 * gs), main.height, alpha);
               b.putVec4((float)(rtl * gs), (float)(rtr * gs), (float)(rbr * gs), (float)(rbl * gs));

               for (int i = 0; i < MAXN; i++) {
                  if (i < n) {
                     int o = i * 4;
                     b.putVec4((float)(rects[o] * gs), (float)(rects[o + 1] * gs), (float)(rects[o + 2] * gs), (float)(rects[o + 3] * gs));
                  } else {
                     b.putVec4(0.0F, 0.0F, 0.0F, 0.0F);
                  }
               }
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
            RenderPass p = enc.createRenderPass(() -> "pizzadlc_arrayshadow", main.getColorTextureView(), Optional.empty());

            try {
               p.setPipeline(PizzaDLCPipelines.ARRAYSHADOW);
               RenderSystem.bindDefaultUniforms(p);
               p.setUniform("ShadowConfig", ubo.currentBuffer());
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
   }
}
