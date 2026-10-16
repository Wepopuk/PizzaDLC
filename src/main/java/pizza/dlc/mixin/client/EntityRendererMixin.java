package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.util.game.BoatLimiter;
import pizza.dlc.client.modules.impl.misc.Optimization;
import pizza.dlc.client.modules.impl.visuals.Esp;

@Mixin({EntityRenderer.class})
public class EntityRendererMixin {
   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$hideBoats(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (BoatLimiter.shouldCull(entity)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$hideVanillaNameForEsp(
      Entity entity, EntityRenderState state, float partialTicks, double nameTagDistance, double belowNameDistance, CallbackInfo ci
   ) {
      if (Esp.shouldHideVanillaName(entity)) {
         state.nameTag = null;
      }

      if (Optimization.singlePass(entity)) {
         state.isDiscrete = true;
      }
   }
}
