package pizza.dlc.mixin.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.RotationController;

@Mixin({Player.class})
public abstract class PlayerSwimAimMixin implements QuickImports {
   @Unique
   private float pizzadlc$savedPitch;
   @Unique
   private boolean pizzadlc$swapped;

   @Inject(
      method = {"travel"},
      at = {@At("HEAD")}
   )
   private void pizzadlc$swimAimHead(Vec3 travelVector, CallbackInfo ci) {
      this.pizzadlc$swapped = false;
      if ((Object)this == mc.player) {
         Player self = (Player)(Object)this;
         if (self.isSwimming()) {
            RotationController c = RotationController.INSTANCE;
            if (c != null && c.isActive()) {
               Angle aim = c.getRotation();
               this.pizzadlc$savedPitch = self.getXRot();
               self.setXRot(Mth.clamp(aim.getPitch(), -90.0F, 90.0F));
               this.pizzadlc$swapped = true;
            }
         }
      }
   }

   @Inject(
      method = {"travel"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$swimAimReturn(Vec3 travelVector, CallbackInfo ci) {
      if (this.pizzadlc$swapped) {
         this.pizzadlc$swapped = false;
         ((Player)(Object)this).setXRot(this.pizzadlc$savedPitch);
      }
   }
}
