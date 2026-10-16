package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.events.impl.game.EventJump;
import pizza.dlc.api.events.impl.input.EventTravel;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.client.modules.impl.player.NoPush;
import pizza.dlc.client.modules.impl.visuals.ViewModel;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin implements QuickImports {
   @Shadow
   protected abstract float getJumpPower();

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onTravel(Vec3 movementInput, CallbackInfo ci) {
      EventTravel event = new EventTravel((LivingEntity)(Object)this).call();
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"jumpFromGround"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$onJump(CallbackInfo ci) {
      if ((Object)this == mc.player) {
         EventJump event = new EventJump(this.getJumpPower(), mc.player.getYRot()).call();
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @ModifyExpressionValue(
      method = {"jumpFromGround"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
      )}
   )
   private float pizzadlc$jumpBoostYaw(float original) {
      if ((Object)this != mc.player) {
         return original;
      }

      RotationController controller = RotationController.INSTANCE;
      if (controller != null && controller.isActive()) {
         RotationPlan plan = controller.getCurrentRotationPlan();
         if (plan != null && plan.isMoveCorrection()) {
            return controller.getMoveRotation().getYaw();
         }
      }

      return original;
   }

   @Inject(
      method = {"isPushable"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$noPushEntities(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == mc.player && NoPush.noEntities()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"getCurrentSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$swingDuration(CallbackInfoReturnable<Integer> cir) {
      if ((Object)this == mc.player) {
         ViewModel vm = ViewModel.INSTANCE;
         if (vm != null && vm.isEnabled()) {
            cir.setReturnValue(vm.getSwingDuration());
         }
      }
   }
}
