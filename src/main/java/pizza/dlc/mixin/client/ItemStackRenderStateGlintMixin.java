package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.EspIconScope;

@Mixin({ItemStackRenderState.class})
public class ItemStackRenderStateGlintMixin {
   @Inject(
      method = {"setAnimated"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$staticGlintForEspIcons(CallbackInfo ci) {
      if (EspIconScope.isActive()) {
         ci.cancel();
      }
   }
}
