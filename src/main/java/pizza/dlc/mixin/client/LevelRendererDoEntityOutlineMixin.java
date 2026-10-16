package pizza.dlc.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.ShaderEspGlow;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.ShaderESP;

@Mixin({LevelRenderer.class})
public class LevelRendererDoEntityOutlineMixin {
   @Shadow
   @Final
   private RenderTarget entityOutlineTarget;

   @Inject(
      method = {"doEntityOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$shaderEspGlow(CallbackInfo ci) {
      ShaderESP esp = null;
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      if (cc != null && cc.getModuleManager() != null) {
         esp = cc.getModuleManager().get(ShaderESP.class);
      }

      if (esp != null && esp.isEnabled()) {
         ShaderEspGlow.render(this.entityOutlineTarget, Math.round(esp.radius.getValue()), esp.contour.isEnabled(), esp.direction.getIndex());
         ci.cancel();
      } else {
         ShaderEspGlow.edgeInPlace(this.entityOutlineTarget);
      }
   }
}
