package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.interfaces.IChamsState;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.Chams;

@Mixin({EntityRenderer.class})
public class EntityRendererChamsMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$chamsMark(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      if (state instanceof pizza.dlc.api.interfaces.AkrienOldState old) {
         old.pizzadlc$setAkrienOld(pizza.dlc.client.modules.impl.visuals.Esp.shouldRenderAkrienOld(entity));
      }
      if (state instanceof IChamsState holder) {
         PizzaDLCClient cc = PizzaDLCClient.getInstance();
         Chams chams = cc != null && cc.getModuleManager() != null ? cc.getModuleManager().get(Chams.class) : null;
         if (chams != null && chams.isEnabled()) {
            int solid = chams.solidColor(entity);
            int glow = chams.glowColor(entity);
            holder.pizzadlc$setChams(solid, glow, (solid != 0 || glow != 0) && chams.hidesSkin());
         } else {
            holder.pizzadlc$setChams(0, 0, false);
         }
      }
   }
}
