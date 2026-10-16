package pizza.dlc.api.util.render.targetesp;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import pizza.dlc.api.util.render.AdditiveTextureType;

public final class TargetEspRenderType {
   private TargetEspRenderType() {
   }

   public static RenderType image(Identifier texture) {
      return AdditiveTextureType.image(texture);
   }
}
