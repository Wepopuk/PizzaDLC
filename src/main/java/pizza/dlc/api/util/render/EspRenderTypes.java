package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import pizza.dlc.PizzaDLC;

public final class EspRenderTypes {
   private static RenderType linesNoDepth;
   private static RenderType filledNoDepth;

   private EspRenderTypes() {
   }

   public static RenderType linesThroughWalls() {
      if (linesNoDepth == null) {
         try {
            RenderPipeline p = RenderPipeline.builder(new Snippet[]{RenderPipelines.LINES_SNIPPET})
               .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/lines_nodepth"))
               .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
               .build();
            linesNoDepth = RenderType.create("pizzadlc:lines_nodepth", RenderSetup.builder(p).createRenderSetup());
         } catch (Throwable ex) {
            PizzaDLC.LOGGER.warn("[esp] lines no-depth недоступен, фолбэк depth-on", ex);
            linesNoDepth = RenderTypes.LINES;
         }
      }

      return linesNoDepth;
   }

   public static RenderType filledThroughWalls() {
      if (filledNoDepth == null) {
         try {
            RenderPipeline p = RenderPipeline.builder(new Snippet[]{RenderPipelines.DEBUG_FILLED_SNIPPET})
               .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/filled_nodepth"))
               .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
               .build();
            filledNoDepth = RenderType.create("pizzadlc:filled_nodepth", RenderSetup.builder(p).sortOnUpload().createRenderSetup());
         } catch (Throwable ex) {
            PizzaDLC.LOGGER.warn("[esp] filled no-depth недоступен, фолбэк depth-on", ex);
            filledNoDepth = RenderTypes.debugFilledBox();
         }
      }

      return filledNoDepth;
   }
}
