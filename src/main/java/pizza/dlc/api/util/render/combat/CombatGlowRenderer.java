package pizza.dlc.api.util.render.combat;
import pizza.dlc.api.util.render.combat.CombatPipelineBuilder;
import pizza.dlc.api.util.render.combat.CombatResources;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class CombatGlowRenderer implements AutoCloseable, pizza.dlc.api.interfaces.QuickImports {

   private static final RenderPipeline f_30061 = CombatPipelineBuilder.m_2718()
      .m_2726("dimasik/entity_esp_other_mask")
      .m_2734(CombatResources.m_9312("hands_fullscreen"))
      .m_2740(CombatResources.m_9312("entity_esp_mask"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2727()
      .m_2739(CombatPipelineBuilder.f_7670, PrimitiveTopology.TRIANGLES)
      .m_2741("InSampler")
      .m_2722();
   private static final RenderPipeline f_30062 = CombatPipelineBuilder.m_2718()
      .m_2726("dimasik/entity_esp_other_blur")
      .m_2734(CombatResources.m_9312("hands_fullscreen"))
      .m_2740(CombatResources.m_9312("entity_esp_gaussian_blur"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2727()
      .m_2739(CombatPipelineBuilder.f_7670, PrimitiveTopology.TRIANGLES)
      .m_2729("EspBlur", UniformType.UNIFORM_BUFFER)
      .m_2741("InSampler")
      .m_2722();
   private static final RenderPipeline f_30063 = CombatPipelineBuilder.m_2718()
      .m_2726("dimasik/entity_esp_other_glow")
      .m_2734(CombatResources.m_9312("hands_fullscreen"))
      .m_2740(CombatResources.m_9312("entity_esp_outer_glow"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(BlendFunction.LIGHTNING)
      .m_2739(CombatPipelineBuilder.f_7670, PrimitiveTopology.TRIANGLES)
      .m_2729("EspOuterGlow", UniformType.UNIFORM_BUFFER)
      .m_2741("BloomSampler")
      .m_2741("MaskSampler")
      .m_2722();
   private static TextureTarget f_30064;
   private static TextureTarget f_30065;
   private static GpuBuffer f_30066;
   private static GpuBuffer f_30067;
   private static GpuBuffer f_30068;

   private static void m_8377(CommandEncoder var0, RenderTarget var1, int var2, float var3, float var4, boolean var5, float var6, int var7, float var8) {
      f_30068 = m_8378(f_30068, 80, "entityEspOtherGlowUniforms");
      float var9 = m_8392(var2);
      float var10 = m_8387(var2);
      float var11 = m_8379(var2);
      MemoryStack var12 = MemoryStack.stackPush();

      try {
         ByteBuffer var13 = Std140Builder.onStack(var12, 80)
            .putVec4(var3, var5 ? 1.0F : 0.0F, var4, var6)
            .putVec4(var9, var10, var11, 1.0F)
            .putVec4(var9 * 0.5F, var10 * 0.5F, var11 * 0.5F, var8)
            .putVec4(m_8392(var7), m_8387(var7), m_8379(var7), m_8388(var7))
            .putVec4(var1.width, var1.height, var8, 0.0F)
            .get();
         var0.writeToBuffer(f_30068.slice(), var13);
      } catch (Throwable var16) {
         if (var12 != null) {
            try {
               var12.close();
            } catch (Throwable var15) {
               var16.addSuppressed(var15);
            }
         }

         throw var16;
      }

      if (var12 != null) {
         var12.close();
      }
   }

   private static GpuBuffer m_8378(GpuBuffer var0, int var1, String var2) {
      return var0 != null && !var0.isClosed() ? var0 : RenderSystem.getDevice().createBuffer(() -> var2, 136, var1);
   }

   private static float m_8379(int var0) {
      return (var0 & 0xFF) / 255.0F;
   }

   private static boolean m_8380(RenderTarget var0) {
      return var0 != null && var0.getColorTextureView() != null;
   }

   private static void m_8383(int var0, int var1) {
      if (f_30064 == null || f_30065 == null) {
         f_30064 = new TextureTarget("entity-esp-other-blur-horizontal", var0, var1, false, GpuFormat.RGBA8_UNORM);
         f_30065 = new TextureTarget("entity-esp-other-blur-vertical", var0, var1, false, GpuFormat.RGBA8_UNORM);
      } else if (f_30064.width != var0 || f_30064.height != var1) {
         f_30064.resize(var0, var1);
         f_30065.resize(var0, var1);
      }
   }

   public static void m_8385(RenderTarget var0, RenderTarget var1, int var2, float var3, float var4, float var5, boolean var6, float var7, int var8, float var9) {
      if (m_8380(var0) && m_8380(var1)) {
         int var10 = Math.max(1, var0.width / 2);
         int var11 = Math.max(1, var0.height / 2);
         float var12 = Math.clamp(var3, 1.0F, 12.0F);
         m_8383(var10, var11);
         CommandEncoder var13 = RenderSystem.getDevice().createCommandEncoder();
         m_8390(var13, var10, var11, var12);
         m_8377(var13, var0, var2, var4, var5, var6, var7, var8, var9);
         RenderPass var14 = var13.createRenderPass(() -> "entityEspOtherMask", f_30064.getColorTextureView(), Optional.of(new Vector4f(0.0F)));

         try {
            var14.setPipeline(f_30061);
            var14.bindTexture("InSampler", var0.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            var14.draw(3, 1, 0, 0);
         } catch (Throwable var20) {
            if (var14 != null) {
               try {
                  var14.close();
               } catch (Throwable var18) {
                  var20.addSuppressed(var18);
               }
            }

            throw var20;
         }

         if (var14 != null) {
            var14.close();
         }

         m_8386(var13, f_30064, f_30065, f_30066);
         m_8386(var13, f_30065, f_30064, f_30067);
         var14 = var13.createRenderPass(() -> "entityEspOtherGlow", var1.getColorTextureView(), Optional.empty());

         try {
            var14.setPipeline(f_30063);
            var14.bindTexture("BloomSampler", f_30064.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            var14.bindTexture("MaskSampler", var0.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            var14.setUniform("EspOuterGlow", f_30068);
            var14.draw(3, 1, 0, 0);
         } catch (Throwable var19) {
            if (var14 != null) {
               try {
                  var14.close();
               } catch (Throwable var17) {
                  var19.addSuppressed(var17);
               }
            }

            throw var19;
         }

         if (var14 != null) {
            var14.close();
         }
      }
   }

   private static void m_8386(CommandEncoder var0, RenderTarget var1, RenderTarget var2, GpuBuffer var3) {
      RenderPass var4 = var0.createRenderPass(() -> "entityEspOtherBlur", var2.getColorTextureView(), Optional.of(new Vector4f(0.0F)));

      try {
         var4.setPipeline(f_30062);
         var4.bindTexture("InSampler", var1.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
         var4.setUniform("EspBlur", var3);
         var4.draw(3, 1, 0, 0);
      } catch (Throwable var8) {
         if (var4 != null) {
            try {
               var4.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (var4 != null) {
         var4.close();
      }
   }

   private static float m_8387(int var0) {
      return (var0 >>> 8 & 0xFF) / 255.0F;
   }

   private static float m_8388(int var0) {
      return (var0 >>> 24 & 0xFF) / 255.0F;
   }

   @Override
   public void close() {
      if (f_30064 != null) {
         f_30064.destroyBuffers();
         f_30064 = null;
      }

      if (f_30065 != null) {
         f_30065.destroyBuffers();
         f_30065 = null;
      }

      if (f_30066 != null) {
         f_30066.close();
         f_30066 = null;
      }

      if (f_30067 != null) {
         f_30067.close();
         f_30067 = null;
      }

      if (f_30068 != null) {
         f_30068.close();
         f_30068 = null;
      }
   }

   public static void m_8389(RenderTarget var0, RenderTarget var1, int var2, float var3, float var4, float var5) {
      m_8385(var0, var1, var2, var3, var4, var5, false, 0.0F, 0, 0.0F);
   }

   private static void m_8390(CommandEncoder var0, int var1, int var2, float var3) {
      f_30066 = m_8378(f_30066, 32, "entityEspOtherBlurHorizontalUniforms");
      f_30067 = m_8378(f_30067, 32, "entityEspOtherBlurVerticalUniforms");
      MemoryStack var4 = MemoryStack.stackPush();

      try {
         ByteBuffer var5 = Std140Builder.onStack(var4, 32).putVec2(1.0F / var1, 1.0F / var2).putVec2(1.0F, 0.0F).putFloat(var3).align(16).get();
         var0.writeToBuffer(f_30066.slice(), var5);
         ByteBuffer var6 = Std140Builder.onStack(var4, 32).putVec2(1.0F / var1, 1.0F / var2).putVec2(0.0F, 1.0F).putFloat(var3).align(16).get();
         var0.writeToBuffer(f_30067.slice(), var6);
      } catch (Throwable var8) {
         if (var4 != null) {
            try {
               var4.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }
         }

         throw var8;
      }

      if (var4 != null) {
         var4.close();
      }
   }

   private static float m_8392(int var0) {
      return (var0 >>> 16 & 0xFF) / 255.0F;
   }
}
