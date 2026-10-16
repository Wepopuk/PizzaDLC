package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.client.modules.impl.misc.FreeCam;

@Mixin({LevelExtractor.class})
public class LevelExtractorFreeCamMixin {
   @Redirect(
      method = {"extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"
      )
   )
   private boolean pizzadlc$freeCamShowSelf(LivingEntity cameraEntity) {
      FreeCam fc = FreeCam.get();
      return fc != null && fc.isEnabled() && fc.showBody() && cameraEntity == Minecraft.getInstance().player || cameraEntity.isSleeping();
   }
}
