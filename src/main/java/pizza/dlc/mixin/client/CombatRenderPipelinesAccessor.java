package pizza.dlc.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = RenderPipelines.class, remap = false)
public interface CombatRenderPipelinesAccessor {
   @Accessor(value = "DEBUG_FILLED_SNIPPET", remap = false)
   static Snippet getPositionColorSnippet() {
      throw new AssertionError();
   }

   @Accessor(value = "OUTLINE_SNIPPET", remap = false)
   static Snippet getOutlineSnippet() {
      throw new AssertionError();
   }

   @Invoker(value = "register", remap = false)
   static RenderPipeline register(RenderPipeline var0) {
      throw new AssertionError();
   }
}
