package pizza.dlc.mixin.client;

import java.util.Map;
import net.minecraft.client.renderer.rendertype.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RenderSetup.class, remap = false)
public interface AkrienRenderSetupAccessor {
   @Accessor("textures") Map<String, RenderSetup.TextureBinding> pizzadlc$textures();
   @Accessor("useLightmap") boolean pizzadlc$lightmap();
   @Accessor("useOverlay") boolean pizzadlc$overlay();
   @Accessor("affectsCrumbling") boolean pizzadlc$crumbling();
   @Accessor("sortOnUpload") boolean pizzadlc$sort();
   @Accessor("layeringTransform") LayeringTransform pizzadlc$layering();
   @Accessor("textureTransform") TextureTransform pizzadlc$textureTransform();
   @Accessor("outlineProperty") RenderSetup.OutlineProperty pizzadlc$outline();
}
