package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiRuntime;

@Mixin(UseDuration.class)
public abstract class ItemModelPropertyHmiMixin {
   @Inject(method = "get", at = @At("RETURN"), cancellable = true)
   private void pizzadlc$hmiUseDuration(ItemStack stack, ClientLevel level, ItemOwner owner, int seed, CallbackInfoReturnable<Float> cir) {
      if (!AnimatedHands.active()) return;
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null || player.getUseItem() != stack) return;
      Float value = HmiRuntime.useDuration(stack);
      if (value != null) cir.setReturnValue(value);
   }
}
