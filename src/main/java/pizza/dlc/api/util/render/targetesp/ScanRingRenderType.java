package pizza.dlc.api.util.render.targetesp;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.BlendFactor;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import pizza.dlc.PizzaDLC;

public final class ScanRingRenderType {
   private static RenderType cached;

   private ScanRingRenderType() {
   }

   public static RenderType get() {
      if (cached != null) {
         return cached;
      }

      Identifier dummy = Identifier.withDefaultNamespace("textures/misc/white.png");

      try {
         RenderPipeline p = RenderPipeline.builder(new Snippet[]{RenderPipelines.WORLD_TEXT_SNIPPET})
            .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/scanring"))
            .withVertexShader("core/text")
            .withFragmentShader(Identifier.fromNamespaceAndPath("pizzadlc", "core/scanring"))
            .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .build();
         cached = RenderType.create("pizzadlc:scanring", RenderSetup.builder(p).withTexture("Sampler0", dummy).useLightmap().createRenderSetup());
      } catch (Throwable ex) {
         PizzaDLC.LOGGER.warn("[scanring] пайплайн недоступен, фолбэк textSeeThrough", ex);
         cached = RenderTypes.textSeeThrough(dummy);
      }

      return cached;
   }
}
