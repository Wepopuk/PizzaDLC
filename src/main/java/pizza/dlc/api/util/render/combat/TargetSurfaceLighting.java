package pizza.dlc.api.util.render.combat;
import pizza.dlc.api.util.render.combat.CombatPipelineBuilder;
import pizza.dlc.api.util.render.combat.CombatResources;
import pizza.dlc.api.util.render.combat.CombatUniformBuffers;
import pizza.dlc.api.util.render.combat.TargetLightingSampler;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

public final class TargetSurfaceLighting implements pizza.dlc.api.interfaces.QuickImports {

   private static final RenderPipeline f_34240 = CombatPipelineBuilder.m_2718()
      .m_2726("dimasik/target_surface_light")
      .m_2734(CombatResources.m_9312("hands_fullscreen"))
      .m_2740(CombatResources.m_9312("target_surface_light"))
      .m_2719(CompareOp.ALWAYS_PASS)
      .m_2737(false)
      .m_2725(false)
      .m_2732(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE))
      .m_2739(CombatPipelineBuilder.f_7670, PrimitiveTopology.TRIANGLES)
      .m_2729("TargetLighting", UniformType.UNIFORM_BUFFER)
      .m_2741("DepthSampler")
      .m_2741("SceneSampler")
      .m_2722();
   private static final Vector3f[] f_34241 = new Vector3f[4];
   private static final Vector3f[] f_34242 = new Vector3f[4];
   private static final float[] f_34243 = new float[4];
   private static final float[] f_34244 = new float[4];
   private static final float[] f_34245 = new float[4];
   private static final Matrix4f f_34246 = new Matrix4f();
   private static final Matrix4f f_34247 = new Matrix4f();
   private static final Matrix4f f_34248 = new Matrix4f();
   private static final TargetLightingSampler f_34249 = new TargetLightingSampler();
   private static final Vector3f f_34250 = new Vector3f();
   private static final Vector4f f_34251 = new Vector4f();
   private static GpuTexture f_34252;
   private static GpuTextureView f_34253;
   private static GpuTexture f_34254;
   private static GpuTextureView f_34255;
   private static boolean f_34256;
   private static boolean f_34257;
   private static int f_34258;

   public static void m_9270() {
      f_34256 = false;
      f_34257 = false;
      f_34258 = 0;
   }

   public static void m_9271() {
      m_9270();
      if (f_34253 != null && !f_34253.isClosed()) {
         f_34253.close();
      }

      if (f_34252 != null && !f_34252.isClosed()) {
         f_34252.close();
      }

      if (f_34255 != null && !f_34255.isClosed()) {
         f_34255.close();
      }

      if (f_34254 != null && !f_34254.isClosed()) {
         f_34254.close();
      }

      f_34253 = null;
      f_34252 = null;
      f_34255 = null;
      f_34254 = null;
      f_34249.m_3634();
   }

   public static void m_9272(boolean var0, Matrix4f var1) {
      f_34257 = var0 && f_34256;
      f_34258 = 0;
      f_34246.set(var1);
   }

   public static void m_9273(float var0, float var1, float var2, float var3, int var4) {
      m_9276(f_34246, var0, var1, var2, var3, var4);
   }

   public static void m_9274(Matrix4f var0) {
      f_34257 = false;
      if (f_34256 && f_34258 != 0 && mc.level != null && var0 != null) {
         f_34256 = false;
         RenderTarget var1 = mc.gameRenderer.mainRenderTarget();
         if (var1 != null
            && var1.getColorTextureView() != null
            && f_34252 != null
            && !f_34252.isClosed()
            && f_34253 != null
            && !f_34253.isClosed()
            && f_34254 != null
            && !f_34254.isClosed()
            && f_34255 != null
            && !f_34255.isClosed()
            && f_34252.getWidth(0) == var1.width
            && f_34252.getHeight(0) == var1.height) {
            f_34247.set(var0).invert();
            f_34248.set(f_34246).invert();
            if (f_34247.isFinite() && f_34248.isFinite()) {
               float var2 = 1.0F;
               float var3 = 1.0F;
               float var4 = 0.0F;
               float var5 = 0.0F;

               for (int var6 = 0; var6 < f_34258; var6++) {
                  Vector3f var7 = f_34241[var6];
                  float var8 = f_34245[var6];
                  if (!(var7.z - var8 >= 0.0F)) {
                     for (int var9 = 0; var9 < 8; var9++) {
                        f_34251.set(
                              var7.x + ((var9 & 1) == 0 ? -var8 : var8),
                              var7.y + ((var9 & 2) == 0 ? -var8 : var8),
                              var7.z + ((var9 & 4) == 0 ? -var8 : var8),
                              1.0F
                           )
                           .mul(var0);
                        if (f_34251.w <= 0.01F) {
                           var3 = 0.0F;
                           var2 = 0.0F;
                           var5 = 1.0F;
                           var4 = 1.0F;
                           break;
                        }

                        float var10 = f_34251.x / f_34251.w * 0.5F + 0.5F;
                        float var11 = f_34251.y / f_34251.w * 0.5F + 0.5F;
                        var2 = Math.min(var2, var10);
                        var3 = Math.min(var3, var11);
                        var4 = Math.max(var4, var10);
                        var5 = Math.max(var5, var11);
                     }
                  }
               }

               int var22 = Math.max(0, (int)Math.floor(var2 * var1.width) - 2);
               int var23 = Math.max(0, (int)Math.floor(var3 * var1.height) - 2);
               int var24 = Math.min(var1.width, (int)Math.ceil(var4 * var1.width) + 2);
               int var25 = Math.min(var1.height, (int)Math.ceil(var5 * var1.height) + 2);
               if (var24 > var22 && var25 > var23) {
                  Vec3 var26 = mc.gameRenderer.mainCamera().position();
                  f_34249.m_3635(f_34248, f_34241, f_34245, f_34258, var26);
                  MemoryStack var27 = MemoryStack.stackPush();

                  try {
                     Std140Builder var12 = Std140Builder.onStack(var27, 4448)
                        .putMat4f(var0)
                        .putMat4f(f_34247)
                        .putVec4(1.0F / var1.width, 1.0F / var1.height, f_34258, 4.5F);

                     for (int var13 = 0; var13 < 4; var13++) {
                        Vector3f var14 = f_34241[var13];
                        var12.putVec4(var14.x, var14.y, var14.z, f_34245[var13]);
                     }

                     for (int var28 = 0; var28 < 4; var28++) {
                        Vector3f var30 = f_34242[var28];
                        var12.putVec4(var30.x, var30.y, var30.z, f_34244[var28]);
                     }

                     var12.putMat4f(f_34248).putVec4(f_34249.m_3637(var26), f_34249.m_3636(var26), f_34249.m_3631(var26), 2.0F);
                     int[] var29 = f_34249.m_3633();

                     for (int var31 = 0; var31 < var29.length; var31 += 4) {
                        var12.putIVec4(var29[var31], var29[var31 + 1], var29[var31 + 2], var29[var31 + 3]);
                     }

                     GpuBufferSlice var32 = CombatUniformBuffers.m_2945(var12.get());
                     RenderPass var15 = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Target surface lighting", var1.getColorTextureView(), Optional.empty());

                     try {
                        var15.setPipeline(f_34240);
                        var15.setUniform("TargetLighting", var32);
                        var15.bindTexture("DepthSampler", f_34253, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                        var15.bindTexture("SceneSampler", f_34255, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                        var15.enableScissor(var22, var23, var24 - var22, var25 - var23);
                        var15.draw(3, 1, 0, 0);
                     } catch (Throwable var20) {
                        if (var15 != null) {
                           try {
                              var15.close();
                           } catch (Throwable var19) {
                              var20.addSuppressed(var19);
                           }
                        }

                        throw var20;
                     }

                     if (var15 != null) {
                        var15.close();
                     }
                  } catch (Throwable var21) {
                     if (var27 != null) {
                        try {
                           var27.close();
                        } catch (Throwable var18) {
                           var21.addSuppressed(var18);
                        }
                     }

                     throw var21;
                  }

                  if (var27 != null) {
                     var27.close();
                  }
               }
            }
         }
      }
   }

   private TargetSurfaceLighting() {
   }

   static {
      for (int var0 = 0; var0 < 4; var0++) {
         f_34241[var0] = new Vector3f();
         f_34242[var0] = new Vector3f();
      }
   }

   public static void m_9276(Matrix4f var0, float var1, float var2, float var3, float var4, int var5) {
      if (f_34257 && !(var4 <= 0.02F)) {
         float var6 = (var5 >>> 24) / 255.0F;
         if (!(var6 <= 0.025F)) {
            var0.transformPosition(var1, var2, var3, f_34250);
            if (f_34250.isFinite() && !(f_34250.lengthSquared() > 6400.0F)) {
               float var7 = Math.min(var4, 1.2F) * var6;
               int var8 = -1;
               float var9 = Float.POSITIVE_INFINITY;

               for (int var10 = 0; var10 < f_34258; var10++) {
                  float var11 = f_34241[var10].distanceSquared(f_34250);
                  if (var11 < var9) {
                     var8 = var10;
                     var9 = var11;
                  }
               }

               float var17 = (var5 >> 16 & 0xFF) / 255.0F;
               float var18 = (var5 >> 8 & 0xFF) / 255.0F;
               float var12 = (var5 & 0xFF) / 255.0F;
               float var13 = Math.clamp(1.6F + var4 * 1.6F, 1.8F, 3.8F);
               if (var8 >= 0 && (!(var9 > 0.64F) || f_34258 >= 4)) {
                  float var19 = f_34243[var8] + var7;
                  float var15 = var7 / var19;
                  f_34241[var8].lerp(f_34250, var15);
                  Vector3f var16 = f_34242[var8];
                  var16.set(var16.x + (var17 - var16.x) * var15, var16.y + (var18 - var16.y) * var15, var16.z + (var12 - var16.z) * var15);
                  f_34243[var8] = var19;
                  f_34244[var8] = Math.max(f_34244[var8], var7);
                  f_34245[var8] = Math.max(f_34245[var8], var13);
               } else {
                  int var14 = f_34258++;
                  f_34241[var14].set(f_34250);
                  f_34242[var14].set(var17, var18, var12);
                  f_34243[var14] = f_34244[var14] = var7;
                  f_34245[var14] = var13;
               }
            }
         }
      }
   }

   public static void m_9277() {
      if (!f_34256 && mc.level != null) {
         RenderTarget var0 = mc.gameRenderer.mainRenderTarget();
         if (var0 != null) {
            GpuTexture var1 = var0.getDepthTexture();
            GpuTexture var2 = var0.getColorTexture();
            if (var1 != null && !var1.isClosed() && var2 != null && !var2.isClosed()) {
               int var3 = var1.getWidth(0);
               int var4 = var1.getHeight(0);
               if (var3 > 0 && var4 > 0) {
                  if (f_34252 == null
                     || f_34252.isClosed()
                     || f_34253 == null
                     || f_34253.isClosed()
                     || f_34252.getWidth(0) != var3
                     || f_34252.getHeight(0) != var4
                     || f_34252.getFormat() != var1.getFormat()
                     || f_34254 == null
                     || f_34254.isClosed()
                     || f_34255 == null
                     || f_34255.isClosed()
                     || f_34254.getFormat() != var2.getFormat()) {
                     m_9271();
                     f_34252 = RenderSystem.getDevice().createTexture("dimasik-target-lighting-depth", 5, var1.getFormat(), var3, var4, 1, 1);
                     f_34253 = RenderSystem.getDevice().createTextureView(f_34252);
                     f_34254 = RenderSystem.getDevice().createTexture("dimasik-target-lighting-color", 5, var2.getFormat(), var3, var4, 1, 1);
                     f_34255 = RenderSystem.getDevice().createTextureView(f_34254);
                  }

                  CommandEncoder var5 = RenderSystem.getDevice().createCommandEncoder();
                  var5.copyTextureToTexture(var1, f_34252, 0, 0, 0, 0, 0, var3, var4);
                  var5.copyTextureToTexture(var2, f_34254, 0, 0, 0, 0, 0, var3, var4);
                  f_34256 = true;
               }
            }
         }
      }
   }
}
