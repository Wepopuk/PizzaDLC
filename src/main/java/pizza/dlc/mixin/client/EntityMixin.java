package pizza.dlc.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pizza.dlc.api.events.impl.game.EventRotationVector;
import pizza.dlc.api.events.impl.game.EventTrace;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.client.modules.impl.player.LoginFix;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;

@Mixin({Entity.class})
public abstract class EntityMixin implements QuickImports {
   @Shadow
   private int id;

   @Shadow
   protected static Vec3 getInputVector(Vec3 movementInput, float speed, float yaw) {
      throw new AssertionError();
   }

   @Inject(
      method = {"getId"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void pizzadlc$tolerateZeroId(CallbackInfoReturnable<Integer> cir) {
      if (this.id == 0 && LoginFix.active()) {
         cir.setReturnValue(0);
      }
   }

   @Redirect(
      method = {"moveRelative"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private Vec3 pizzadlc$rotationVector(Vec3 movementInput, float speed, float yaw) {
      if ((Object)this == mc.player) {
         EventRotationVector event = new EventRotationVector(yaw, mc.player.getXRot()).call();
         return getInputVector(movementInput, speed, event.getYaw());
      }

      return getInputVector(movementInput, speed, yaw);
   }

   @ModifyArgs(
      method = {"getViewVector"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private void pizzadlc$trace(Args args, float partialTick) {
      if ((Object)this == mc.player) {
         if (CleanRenderScope.isActive()) {
            args.set(0, CleanRenderScope.pitch(args.<Float>get(0), partialTick));
            args.set(1, CleanRenderScope.yaw(args.<Float>get(1), partialTick));
            return;
         }
         EventTrace event = new EventTrace(mc.player.getYRot(), mc.player.getXRot()).call();
         if (event.isCancelled()) {
            args.set(0, event.getPitch());
            args.set(1, event.getYaw());
         }
      }
   }
}
