package pizza.dlc.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.PreparedText;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer.Submit;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.api.util.render.NameTagCache;
import pizza.dlc.client.modules.impl.misc.Optimization;
import pizza.dlc.client.modules.impl.misc.StreamerMode;

@Mixin({NameTagFeatureRenderer.class})
public class NameTagFeatureRendererMixin {
   @Redirect(
      method = {"buildGroup(Lnet/minecraft/client/renderer/feature/FeatureFrameContext;Ljava/util/List;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/feature/NameTagFeatureRenderer;prepareText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/renderer/feature/NameTagFeatureRenderer$Submit;)Lnet/minecraft/client/gui/Font$PreparedText;"
      )
   )
   private PreparedText pizzadlc$cachedPrepare(Font font, Submit s) {
      Component text = StreamerMode.replace(s.text());
      return !Optimization.nameCacheOn()
         ? font.prepareText(text.getVisualOrderText(), s.x(), s.y(), s.color(), false, false, s.backgroundColor())
         : NameTagCache.get(font, text, s.x(), s.y(), s.color(), s.backgroundColor());
   }
}
