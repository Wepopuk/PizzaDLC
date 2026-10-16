package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;

@Mixin({LivingEntityRenderer.class})
public class LivingEntityRendererAuraRotationMixin implements QuickImports {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$auraRotation(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
      if (entity == mc.player) {
         RotationController c = RotationController.INSTANCE;
         if (c != null && c.isActive()) {
            Angle prev = c.getPreviousRotation();
            Angle cur = c.getRotation();
            state.bodyRot = Mth.rotLerp(partialTick, prev.getYaw(), cur.getYaw());
            state.xRot = Mth.lerp(partialTick, prev.getPitch(), cur.getPitch());
            state.yRot = 0.0F;
         }
      }
   }
}
