package pizza.dlc.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.visuals.Removals;
import pizza.dlc.client.modules.impl.misc.StreamProof;

@Mixin({GameRenderer.class})
public class GameRendererRemovalsMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Unique
   private boolean pizzadlc$portalHidden;
   @Unique
   private float pizzadlc$portal;
   @Unique
   private float pizzadlc$prevPortal;

   @Inject(
      method = {"bobHurt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeHurtCam(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
      if (Removals.active("HurtCam")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"displayItemActivation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$removeTotemAnimation(ItemStack itemStack, CallbackInfo ci) {
      StreamProof streamProof = StreamProof.get();
      if (Removals.active("Анимация тотема") && (streamProof == null || !streamProof.isEnabled())) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$hidePortalEffect(DeltaTracker deltaTracker, CallbackInfo ci) {
      this.pizzadlc$portalHidden = false;
      LocalPlayer player = this.minecraft.player;
      if (player != null && Removals.active("Портал")) {
         this.pizzadlc$portal = player.portalEffectIntensity;
         this.pizzadlc$prevPortal = player.oPortalEffectIntensity;
         player.portalEffectIntensity = 0.0F;
         player.oPortalEffectIntensity = 0.0F;
         this.pizzadlc$portalHidden = true;
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$restorePortalEffect(DeltaTracker deltaTracker, CallbackInfo ci) {
      if (this.pizzadlc$portalHidden && this.minecraft.player != null) {
         this.minecraft.player.portalEffectIntensity = this.pizzadlc$portal;
         this.minecraft.player.oPortalEffectIntensity = this.pizzadlc$prevPortal;
         this.pizzadlc$portalHidden = false;
      }
   }

   @Redirect(
      method = {"tick", "renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"
      )
   )
   private float pizzadlc$removeNauseaProjection(LocalPlayer player, Holder<MobEffect> effect, float partialTicks) {
      return Removals.shouldRemoveNausea(player) ? 0.0F : player.getEffectBlendFactor(effect, partialTicks);
   }
}
