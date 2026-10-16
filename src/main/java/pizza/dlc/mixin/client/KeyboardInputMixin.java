package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import pizza.dlc.api.events.impl.game.EventInputMove;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.rotation.RotationPlan;
import pizza.dlc.api.util.game.SwapSystem;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;
import pizza.dlc.client.modules.impl.movement.InventoryMove;

@Mixin({KeyboardInput.class})
public class KeyboardInputMixin implements QuickImports {
   @Unique
   private static float pizzadlc$mult(boolean positive, boolean negative) {
      return positive == negative ? 0.0F : (positive ? 1.0F : -1.0F);
   }

   @ModifyExpressionValue(
      method = {"tick"},
      at = {@At(
         value = "NEW",
         target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"
      )}
   )
   private Input pizzadlc$tickHook(Input original) {
      if (StreamProofRenderer.isVanillaGameplayActive()) return original;
      if (SwapSystem.grimStallInput()) {
         return Input.EMPTY;
      }

      if (pizza.dlc.api.util.game.InventorySwapQueue.blocksSprint() && mc.player!=null) mc.player.setSprinting(false);
      Input manual=original;
      var neuro=pizza.dlc.api.neuro.NeuroSystem.get();
      if(neuro!=null)neuro.observeInput(manual);
      original = InventoryMove.override(original);
      float forward = pizzadlc$mult(original.forward(), original.backward());
      float strafe = pizzadlc$mult(original.left(), original.right());
      EventInputMove event = new EventInputMove(forward, strafe, original.jump(), original.shift(), original.sprint());
      event.call();
      if (event.isCancelled()) {
         return original;
      }

      Input afterEvent = new Input(
         event.getForward() > 0.0F,
         event.getForward() < 0.0F,
         event.getStrafe() > 0.0F,
         event.getStrafe() < 0.0F,
         event.isJump(),
         event.isSneaking(),
         event.isSprint() && !pizza.dlc.api.util.game.InventorySwapQueue.blocksSprint()
      );
      if(neuro!=null)afterEvent=neuro.movement(manual,afterEvent);
      return this.pizzadlc$applyFreeMove(afterEvent);
   }

   @Unique
   private Input pizzadlc$applyFreeMove(Input input) {
      RotationController controller = RotationController.INSTANCE;
      if (controller != null && mc.player != null && controller.isActive()) {
         RotationPlan plan = controller.getCurrentRotationPlan();
         if (plan != null && plan.isMoveCorrection() && plan.isFreeCorrection()) {
            Angle angle = controller.getRotation();
            float deltaYaw = mc.player.getYRot() - angle.getYaw();
            float z = pizzadlc$mult(input.forward(), input.backward());
            float x = pizzadlc$mult(input.left(), input.right());
            float rad = deltaYaw * (float) (Math.PI / 180.0);
            float cos = Mth.cos(rad);
            float sin = Mth.sin(rad);
            int sideways = Math.round(x * cos - z * sin);
            int forward = Math.round(z * cos + x * sin);
            return new Input(forward > 0, forward < 0, sideways > 0, sideways < 0, input.jump(), input.shift(), input.sprint());
         }

         return input;
      }

      return input;
   }
}
