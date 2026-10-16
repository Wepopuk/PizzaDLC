package pizza.dlc.api.util.render.combat;
import pizza.dlc.api.util.render.combat.CombatPipelineBuilder;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import pizza.dlc.mixin.client.CombatRenderPipelinesAccessor;
import pizza.dlc.mixin.client.CombatRenderTypeAccessor;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class CombatRenderTypes {
   public static final RenderPipeline f_3815 = CombatRenderPipelinesAccessor.register(
      CombatPipelineBuilder.m_2718(CombatRenderPipelinesAccessor.getPositionColorSnippet())
         .m_2742(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/xray_lines"))
         .m_2719(CompareOp.ALWAYS_PASS)
         .m_2737(false)
         .m_2725(false)
         .m_2739(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.DEBUG_LINES)
         .m_2722()
   );
   public static final RenderType f_3816 = CombatRenderTypeAccessor.create("dimasik_xray_lines", RenderSetup.builder(f_3815).createRenderSetup());
   public static final RenderPipeline f_3817 = CombatRenderPipelinesAccessor.register(
      CombatPipelineBuilder.m_2718(CombatRenderPipelinesAccessor.getPositionColorSnippet())
         .m_2742(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/xray_quads"))
         .m_2719(CompareOp.ALWAYS_PASS)
         .m_2737(false)
         .m_2725(false)
         .m_2732(BlendFunction.TRANSLUCENT)
         .m_2739(DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS)
         .m_2722()
   );
   public static final RenderType f_3818 = CombatRenderTypeAccessor.create("dimasik_xray_quads", RenderSetup.builder(f_3817).createRenderSetup());

   private CombatRenderTypes() {
   }
}
