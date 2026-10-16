package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import pizza.dlc.api.interfaces.AkrienOldState;
import pizza.dlc.api.util.render.AkrienOldScope;
import pizza.dlc.client.modules.impl.visuals.Esp;

@Mixin(value = LivingEntityRenderer.class, remap = false)
public class LivingEntityAkrienOldMixin {
   @WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
   private void pizzadlc$akrienOld(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
      boolean enabled = state instanceof AkrienOldState flag && flag.pizzadlc$akrienOld() && Esp.isAkrienOldActive();
      try (var scope = new AkrienOldScope(enabled)) { original.call(state, pose, collector, camera); }
   }
}
