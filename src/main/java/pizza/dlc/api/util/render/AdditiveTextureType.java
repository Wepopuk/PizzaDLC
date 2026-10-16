package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import pizza.dlc.PizzaDLC;

public final class AdditiveTextureType {
   private static final Map<Identifier, RenderType> CACHE = new HashMap<>();
   private static final Map<Identifier, RenderType> CACHE_DEPTH = new HashMap<>();

   private AdditiveTextureType() {
   }

   public static RenderType image(Identifier texture) {
      return CACHE.computeIfAbsent(texture, t -> build(t, false));
   }

   public static RenderType imageDepth(Identifier texture) {
      return CACHE_DEPTH.computeIfAbsent(texture, t -> build(t, true));
   }

   private static RenderType build(Identifier texture, boolean depthTested) {
      String key = (texture.getNamespace() + "_" + texture.getPath()).replaceAll("[^a-z0-9_]", "_") + (depthTested ? "_depth" : "");

      try {
         RenderPipeline p = RenderPipeline.builder(new Snippet[]{RenderPipelines.WORLD_TEXT_SNIPPET})
            .withLocation(Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/additive_" + key))
            .withVertexShader("core/text")
            .withFragmentShader("core/text")
            .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE)))
            .withDepthStencilState(depthTested ? Optional.of(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false)) : Optional.empty())
            .withCull(false)
            .build();
         return RenderType.create("pizzadlc:additive_" + key, RenderSetup.builder(p).withTexture("Sampler0", texture).useLightmap().createRenderSetup());
      } catch (Throwable ex) {
         PizzaDLC.LOGGER.warn("[additive] пайплайн недоступен, фолбэк на ванильный текст", ex);
         return depthTested ? RenderTypes.text(texture) : RenderTypes.textSeeThrough(texture);
      }
   }
}
