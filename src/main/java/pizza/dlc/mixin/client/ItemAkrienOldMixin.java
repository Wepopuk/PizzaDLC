package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pizza.dlc.api.interfaces.AkrienOldState;
import pizza.dlc.api.util.render.AkrienOldRenderType;
import pizza.dlc.api.util.render.AkrienOldScope;
import pizza.dlc.client.modules.impl.visuals.Esp;

@Mixin(value = ItemFeatureRenderer.class, remap = false)
public class ItemAkrienOldMixin {
   @WrapMethod(method = "prepareSubmit")
   private void pizzadlc$akrienItem(ItemFeatureRenderer.Submit item, boolean outline, Operation<Void> original) {
      boolean active = (Object)item instanceof AkrienOldState flag && flag.pizzadlc$akrienOld() && Esp.isAkrienOldActive();
      try (var scope = new AkrienOldScope(active)) { original.call(item, outline); }
   }

   @ModifyArg(method = {"prepareMainSubmit", "prepareOutlineSubmit", "getFoilBuffer"}, at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index = 0)
   private RenderType pizzadlc$akrienType(RenderType original) {
      return AkrienOldScope.active() ? AkrienOldRenderType.of(original) : original;
   }
}
