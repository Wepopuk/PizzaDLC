package pizza.dlc.api.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import pizza.dlc.mixin.client.AkrienRenderSetupAccessor;
import pizza.dlc.mixin.client.CombatRenderTypeAccessor;

public final class AkrienOldRenderType {
   private static final Map<RenderType, RenderType> TYPES = new WeakHashMap<>();
   private static final Map<RenderPipeline, RenderPipeline> PIPELINES = new IdentityHashMap<>();
   private static int typeId;

   private AkrienOldRenderType() { }

   public static RenderPipeline copyPipeline(RenderPipeline original, Identifier location) {
      var colors = original.getColorTargetStates();
      int activeColors = colors.length;
      while (activeColors > 0 && colors[activeColors - 1] == null) activeColors--;
      var snippet = new RenderPipeline.Snippet(Optional.of(original.getVertexShader()), Optional.of(original.getFragmentShader()),
         Optional.of(original.getShaderDefines()), Optional.of(original.getBindGroupLayouts()), colors.clone(), activeColors,
         Optional.empty(), Optional.of(original.getPolygonMode()), Optional.of(original.isCull()),
         original.getVertexFormatBindings().clone(), Optional.of(original.getPrimitiveTopology()));
      return RenderPipeline.builder(snippet).withLocation(location).withDepthStencilState(Optional.empty()).build();
   }

   public static RenderType of(RenderType original) {
      if (original.isOutline()) return original;
      return TYPES.computeIfAbsent(original, type -> {
         RenderPipeline pipeline = PIPELINES.computeIfAbsent(type.pipeline(), source ->
            copyPipeline(source, Identifier.fromNamespaceAndPath("pizzadlc", "pipeline/akrien_old_" + PIPELINES.size())));
         var source = ((CombatRenderTypeAccessor)type).getRenderSetup();
         var state = (AkrienRenderSetupAccessor)(Object)source;
         var builder = RenderSetup.builder(pipeline).setOutputTarget(OutputTarget.MAIN_TARGET)
            .setLayeringTransform(state.pizzadlc$layering()).setTextureTransform(state.pizzadlc$textureTransform()).setOutline(state.pizzadlc$outline());
         state.pizzadlc$textures().forEach((name, binding) -> builder.withTexture(name, binding.location(), binding.sampler()));
         if (state.pizzadlc$lightmap()) builder.useLightmap();
         if (state.pizzadlc$overlay()) builder.useOverlay();
         if (state.pizzadlc$crumbling()) builder.affectsCrumbling();
         if (state.pizzadlc$sort()) builder.sortOnUpload();
         return RenderType.create("pizzadlc:akrien_old_" + typeId++, builder.createRenderSetup());
      });
   }
}
