package pizza.dlc.mixin.client;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.combat.CombatHudText;


import net.minecraft.client.Camera;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.misc.FreeCam;
import pizza.dlc.client.modules.impl.misc.Zoom;
import pizza.dlc.client.modules.impl.visuals.Removals;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   protected abstract void setPosition(Vec3 position);

   @Shadow
   protected abstract void setRotation(float yaw, float pitch);

   @Inject(
      method = {"update"},
      at = {@At("TAIL")}
   )
   private void pizzadlc$freecam(DeltaTracker tracker, CallbackInfo ci) {
      FreeCam fc = FreeCam.get();
      if (fc != null && fc.isEnabled()) {
         this.setPosition(new Vec3(fc.getFakeX(), fc.getFakeY(), fc.getFakeZ()));
         this.setRotation(fc.getFakeYaw(), fc.getFakePitch());
      }
   }

   @Inject(
      method = {"calculateFov"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void pizzadlc$zoomFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
      if (CleanRenderScope.isActive()) return;
      Zoom zoom = Zoom.get();
      if (zoom != null && zoom.shouldApply()) {
         cir.setReturnValue(Mth.clamp((Float)cir.getReturnValue() * zoom.getAnimatedMultiplier(), 1.0F, 179.0F));
      }
   }

   @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
   private float pizzadlc$cleanFeedYaw(float original, float partialTick) {
      if (!CleanRenderScope.isActive() && ((Camera)(Object)this).entity() == net.minecraft.client.Minecraft.getInstance().player) {
         original = pizza.dlc.api.util.combat.CombatRuntime.camera(original, ((Camera)(Object)this).xRot()).getYaw();
      }
      return ((Camera)(Object)this).entity() == net.minecraft.client.Minecraft.getInstance().player
         ? CleanRenderScope.yaw(original, partialTick) : original;
   }

   @ModifyExpressionValue(method = "alignWithEntity", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
   private float pizzadlc$cleanFeedPitch(float original, float partialTick) {
      if (!CleanRenderScope.isActive() && ((Camera)(Object)this).entity() == net.minecraft.client.Minecraft.getInstance().player) {
         original = pizza.dlc.api.util.combat.CombatRuntime.camera(((Camera)(Object)this).yRot(), original).getPitch();
      }
      return ((Camera)(Object)this).entity() == net.minecraft.client.Minecraft.getInstance().player
         ? CleanRenderScope.pitch(original, partialTick) : original;
   }

   @Inject(
      method = {"getMaxZoom"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeCameraClip(float cameraDistance, CallbackInfoReturnable<Float> cir) {
      if (Removals.active("Камера")) {
         cir.setReturnValue(cameraDistance);
      }
   }
}
