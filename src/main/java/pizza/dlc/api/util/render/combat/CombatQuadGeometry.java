package pizza.dlc.api.util.render.combat;
import pizza.dlc.api.util.render.combat.CombatPipelineBuilder;
import pizza.dlc.api.util.render.combat.CombatResources;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4f;

public final class CombatQuadGeometry {

   private CombatQuadGeometry() {
   }

   public static void m_1813(BufferBuilder var0, Matrix4f var1, float var2, int var3) {
      float var4 = var2 / 2.0F;
      var0.addVertex(var1, -var4, var4, 0.0F).setUv(0.0F, 1.0F).setColor(var3);
      var0.addVertex(var1, var4, var4, 0.0F).setUv(1.0F, 1.0F).setColor(var3);
      var0.addVertex(var1, var4, -var4, 0.0F).setUv(1.0F, 0.0F).setColor(var3);
      var0.addVertex(var1, -var4, -var4, 0.0F).setUv(0.0F, 0.0F).setColor(var3);
   }

   public static RenderPipeline m_1814(String var0, CompareOp var1) {
      return CombatPipelineBuilder.m_2718(CombatPipelineBuilder.f_7671)
         .m_2726(var0)
         .m_2734(CombatResources.m_9312("dead_chams"))
         .m_2740(CombatResources.m_9312("private_glow"))
         .m_2719(var1)
         .m_2737(false)
         .m_2725(false)
         .m_2732(BlendFunction.OVERLAY)
         .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
         .m_2741("Sampler0")
         .m_2722();
   }
}
