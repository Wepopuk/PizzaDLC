package pizza.dlc.mixin.client;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.CustomSkyRenderer;

@Mixin({CloudRenderer.class})
public class CloudRendererMixin {
   @Inject(
      method = {"render"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$noClouds(int renderDistance, CloudStatus cloudStatus, float cloudHeight, int cellSize, Vec3 cameraPos, long tick, float partialTick, CallbackInfo ci) {
      if (CustomSkyRenderer.shouldReplace()) {
         ci.cancel();
      }
   }
}
