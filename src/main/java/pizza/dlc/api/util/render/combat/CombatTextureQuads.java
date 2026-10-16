package pizza.dlc.api.util.render.combat;
import pizza.dlc.api.util.render.combat.CombatPipelineBuilder;
import pizza.dlc.api.util.render.combat.CombatResources;
import pizza.dlc.api.util.render.combat.CombatVertexAllocator;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class CombatTextureQuads implements pizza.dlc.api.interfaces.QuickImports {
   public static final Identifier f_33138 = Identifier.fromNamespaceAndPath("pizzadlc", "textures/nightix/particles_1.png");
   public static final Identifier f_33139 = Identifier.fromNamespaceAndPath("pizzadlc", "textures/nightix/particles_2.png");
   private static final RenderPipeline[] f_33140 = new RenderPipeline[]{m_9216(false, false, false), m_9216(false, false, true)};
   private static final RenderPipeline[] f_33141 = new RenderPipeline[]{m_9216(true, false, false), m_9216(true, false, true)};
   private static final RenderPipeline[] f_33142 = new RenderPipeline[]{m_9216(false, true, false), m_9216(false, true, true)};

   private CombatTextureQuads() {
   }

   public static void m_9211(BufferBuilder var0, Matrix4f var1, float var2, int var3) {
      for (int var4 = -1; var4 <= 1; var4 += 2) {
         m_9212(var0, var1, -var2, var4 * var2, -var2, var2, var4 * var2, -var2, var3);
         m_9212(var0, var1, var2, var4 * var2, -var2, var2, var4 * var2, var2, var3);
         m_9212(var0, var1, var2, var4 * var2, var2, -var2, var4 * var2, var2, var3);
         m_9212(var0, var1, -var2, var4 * var2, var2, -var2, var4 * var2, -var2, var3);
      }

      for (int var6 = -1; var6 <= 1; var6 += 2) {
         for (int var5 = -1; var5 <= 1; var5 += 2) {
            m_9212(var0, var1, var6 * var2, -var2, var5 * var2, var6 * var2, var2, var5 * var2, var3);
         }
      }
   }

   private static void m_9212(BufferBuilder var0, Matrix4f var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      var0.addVertex(var1, var2, var3, var4).setColor(var8);
      var0.addVertex(var1, var5, var6, var7).setColor(var8);
   }

   public static BufferBuilder m_9213(boolean var0, boolean var1) {
      return CombatVertexAllocator.m_4161()
         .m_4162(
            var0 ? PrimitiveTopology.DEBUG_LINES : PrimitiveTopology.QUADS, var1 ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR
         );
   }

   private static void m_9214(
      BufferBuilder var0,
      Matrix4f var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      int var14
   ) {
      var0.addVertex(var1, var2, var3, var4).setColor(var14);
      var0.addVertex(var1, var5, var6, var7).setColor(var14);
      var0.addVertex(var1, var8, var9, var10).setColor(var14);
      var0.addVertex(var1, var11, var12, var13).setColor(var14);
   }

   public static void m_9215(BufferBuilder var0, Matrix4f var1, float var2, int var3) {
      var0.addVertex(var1, -var2, -var2, 0.0F).setColor(var3).setUv(0.0F, 1.0F);
      var0.addVertex(var1, var2, -var2, 0.0F).setColor(var3).setUv(1.0F, 1.0F);
      var0.addVertex(var1, var2, var2, 0.0F).setColor(var3).setUv(1.0F, 0.0F);
      var0.addVertex(var1, -var2, var2, 0.0F).setColor(var3).setUv(0.0F, 0.0F);
   }

   private static RenderPipeline m_9216(boolean var0, boolean var1, boolean var2) {
      CombatPipelineBuilder var3 = CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
         .m_2726("" + var0 + var1 + var2)
         .m_2734(CombatResources.m_9312(var1 ? "nightix_cube_glow" : "nightix_cube"))
         .m_2740(CombatResources.m_9312(var1 ? "nightix_cube_glow" : "nightix_cube"))
         .m_2739(
            var1 ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR, var0 ? PrimitiveTopology.DEBUG_LINES : PrimitiveTopology.QUADS
         )
         .m_2719(var2 ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS)
         .m_2737(false)
         .m_2725(false)
         .m_2732(BlendFunction.LIGHTNING);
      if (var1) {
         var3.m_2741("Sampler0");
      }

      return var3.m_2722();
   }

   public static void m_9217(BufferBuilder var0, Matrix4f var1, float var2, int var3) {
      m_9214(var0, var1, -var2, var2, -var2, var2, var2, -var2, var2, var2, var2, -var2, var2, var2, var3);
      m_9214(var0, var1, -var2, -var2, var2, var2, -var2, var2, var2, -var2, -var2, -var2, -var2, -var2, var3);
      m_9214(var0, var1, var2, -var2, -var2, var2, -var2, var2, var2, var2, var2, var2, var2, -var2, var3);
      m_9214(var0, var1, -var2, -var2, var2, -var2, -var2, -var2, -var2, var2, -var2, -var2, var2, var2, var3);
      m_9214(var0, var1, -var2, -var2, var2, var2, -var2, var2, var2, var2, var2, -var2, var2, var2, var3);
      m_9214(var0, var1, var2, -var2, -var2, -var2, -var2, -var2, -var2, var2, -var2, var2, var2, -var2, var3);
   }

   public static void m_9218(BufferBuilder var0, boolean var1, Identifier var2, boolean var3) {
      MeshData var4 = var0.build();
      if (var4 != null) {
         MeshData var5 = var4;

         try {
            GpuBuffer var6 = RenderSystem.getDevice().createBuffer(() -> "Nightix cube vertices", 32, var4.vertexBuffer());

            try {
               DrawState var7 = var4.drawState();
               AutoStorageIndexBuffer var8 = RenderSystem.getSequentialBuffer(var7.primitiveTopology());
               GpuBuffer var9 = var8.getBuffer(var7.indexCount());
               RenderTarget var10 = mc.gameRenderer.mainRenderTarget();
               RenderPass var11 = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(() -> "Nightix cubes", var10.getColorTextureView(), Optional.empty(), var10.getDepthTextureView(), OptionalDouble.empty());

               try {
                  RenderSystem.bindDefaultUniforms(var11);
                  var11.setPipeline((var2 != null ? f_33142 : (var1 ? f_33141 : f_33140))[var3 ? 1 : 0]);
                  if (var2 != null) {
                     AbstractTexture var12 = mc.getTextureManager().getTexture(var2);
                     var11.bindTexture("Sampler0", var12.getTextureView(), var12.getSampler());
                  }

                  var11.setVertexBuffer(0, var6.slice());
                  var11.setIndexBuffer(var9, var8.type());
                  var11.drawIndexed(var7.indexCount(), 1, 0, 0, 0);
               } catch (Throwable var17) {
                  if (var11 != null) {
                     try {
                        var11.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (var11 != null) {
                  var11.close();
               }
            } catch (Throwable var18) {
               if (var6 != null) {
                  try {
                     var6.close();
                  } catch (Throwable var15) {
                     var18.addSuppressed(var15);
                  }
               }

               throw var18;
            }

            if (var6 != null) {
               var6.close();
            }
         } catch (Throwable var19) {
            if (var5 != null) {
               try {
                  var5.close();
               } catch (Throwable var14) {
                  var19.addSuppressed(var14);
               }
            }

            throw var19;
         }

         if (var5 != null) {
            var5.close();
         }
      }
   }
}
