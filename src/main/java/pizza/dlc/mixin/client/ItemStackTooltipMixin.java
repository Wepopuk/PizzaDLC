package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pizza.dlc.api.util.ItemNbtViewer;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

@Mixin({ItemStack.class})
public class ItemStackTooltipMixin {
   @ModifyReturnValue(
      method = {"getTooltipLines"},
      at = {@At("RETURN")}
   )
   private List<Component> pizzadlc$appendNbt(List<Component> original) {
      if (CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive()) return original;
      return ItemNbtViewer.appendNbt((ItemStack)(Object)this, original);
   }
}
