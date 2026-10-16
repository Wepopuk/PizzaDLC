package pizza.dlc.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.visuals.Ambience;
import pizza.dlc.client.modules.impl.visuals.Removals;

@Mixin({FogRenderer.class})
public class FogRendererMixin {
   @Inject(
      method = {"setupFog"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$removeFog(
      Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> cir
   ) {
      if (Removals.shouldRemoveFog(camera.entity())) {
         FogData fog = (FogData)cir.getReturnValue();
         if (fog != null) {
            float far = 1000000.0F;
            fog.environmentalStart = far;
            fog.environmentalEnd = far;
            fog.renderDistanceStart = far;
            fog.renderDistanceEnd = far;
            fog.skyEnd = far;
            fog.cloudEnd = far;
         }
      }
   }

   @Inject(
      method = {"setupFog"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$ambienceFog(
      Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> cir
   ) {
      if (Ambience.fogOn()) {
         FogData fog = (FogData)cir.getReturnValue();
         if (fog != null) {
            int c = Ambience.fogColor();
            fog.color.set((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, 1.0F);
            float dist = Ambience.fogDistance();
            float start = dist * 0.25F;
            fog.environmentalStart = start;
            fog.environmentalEnd = dist;
            fog.renderDistanceStart = start;
            fog.renderDistanceEnd = dist;
         }
      }
   }
}
