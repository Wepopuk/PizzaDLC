package pizza.dlc.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.client.modules.impl.visuals.animatedhands.AnimatedHands;
import pizza.dlc.client.modules.impl.visuals.animatedhands.HmiRuntime;

@Mixin(IsUsingItem.class)
public abstract class UsingItemPropertyHmiMixin {
   @Inject(method = "get", at = @At("RETURN"), cancellable = true)
   private void pizzadlc$hmiUsingItem(ItemStack stack, ClientLevel level, LivingEntity owner, int seed, ItemDisplayContext context, CallbackInfoReturnable<Boolean> cir) {
      if (!AnimatedHands.active() || owner == null || owner.getUseItem() != stack) return;
      Boolean value = HmiRuntime.usingItem(stack);
      if (value != null) cir.setReturnValue(value);
   }
}
