package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import pizza.dlc.PizzaDLC;

public final class ChamsRenderType {
   private static final Identifier WHITE = Identifier.withDefaultNamespace("textures/misc/white.png");
   private static RenderType glow;
   private static RenderType solid;

   private ChamsRenderType() {
   }

   public static RenderType glow() {
      if (glow == null) {
         glow = build("glow", BlendFunction.LIGHTNING);
      }

      return glow;
   }

   public static RenderType solid() {
      if (solid == null) {
         solid = build("solid", BlendFunction.TRANSLUCENT);
      }

      return solid;
   }

   private static RenderType build(String name, BlendFunction blend) {
      try {
         RenderPipeline p = RenderPipeline.builder(new Snippet[]{RenderPipelines.ENTITY_EMISSIVE_SNIPPET})
            .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/chams_" + name))
            .withShaderDefine("NO_OVERLAY")
            .withColorTargetState(new ColorTargetState(blend))
            .withDepthStencilState(Optional.empty())
            .withCull(true)
            .build();
         return RenderType.create("pizzadlc:chams_" + name, RenderSetup.builder(p).withTexture("Sampler0", WHITE).createRenderSetup());
      } catch (Throwable ex) {
         PizzaDLC.LOGGER.warn("[chams] пайплайн недоступен, фолбэк на ванильный entity", ex);
         return RenderTypes.entityTranslucentEmissive(WHITE);
      }
   }
}
