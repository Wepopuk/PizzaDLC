package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin(Minecraft.class)
public class MinecraftStreamProofMixin {
   @Inject(method = "runTick", at = @At("HEAD"))
   private void pizzadlc$pollWindows(boolean renderLevel, CallbackInfo ci) {
      StreamProofRenderer.poll((Minecraft)(Object)this);
   }
   @Inject(method = "renderFrame", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/renderer/GameRenderer;update(Lnet/minecraft/client/DeltaTracker;)V"))
   private void pizzadlc$cleanFeed(boolean renderLevel, CallbackInfo ci) {
      Minecraft minecraft = (Minecraft)(Object)this;
      StreamProofRenderer.render(minecraft, minecraft.getDeltaTracker(), renderLevel);
   }

   @Inject(method = "renderFrame", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V", shift = At.Shift.AFTER))
   private void pizzadlc$oneWindowOutput(boolean renderLevel, CallbackInfo ci) {
      StreamProofRenderer.finishFrame((Minecraft)(Object)this);
   }

   @Inject(method = "close", at = @At("HEAD"))
   private void pizzadlc$closeCleanFeed(CallbackInfo ci) {
      StreamProofRenderer.shutdown();
   }
}
