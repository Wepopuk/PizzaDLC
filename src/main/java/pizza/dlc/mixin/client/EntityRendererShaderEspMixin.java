package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.ShaderESP;

@Mixin({EntityRenderer.class})
public class EntityRendererShaderEspMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$shaderEspMark(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         ShaderESP esp = cc.getModuleManager().get(ShaderESP.class);
         if (esp != null && esp.isEnabled()) {
            if (esp.accept(entity)) {
               state.outlineColor = esp.coloredForOutline();
            }
         }
      }
   }
}
