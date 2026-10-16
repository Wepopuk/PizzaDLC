package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.ShaderESP;

@Mixin({Minecraft.class})
public class MinecraftShaderEspGlowMixin {
   @Inject(
      method = {"shouldEntityAppearGlowing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$shaderEspGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         ShaderESP esp = cc.getModuleManager().get(ShaderESP.class);
         if (esp != null && esp.isEnabled()) {
            if (esp.accept(entity)) {
               cir.setReturnValue(true);
            }
         }
      }
   }
}
