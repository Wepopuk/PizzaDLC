package pizza.dlc.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({RenderPipelines.class})
public interface RenderPipelinesAccessor {
   @Accessor("GUI_SNIPPET")
   static Snippet pizzadlc$guiSnippet() {
      throw new AssertionError();
   }

   @Accessor("GUI_TEXTURED_SNIPPET")
   static Snippet pizzadlc$guiTexturedSnippet() {
      throw new AssertionError();
   }
}
