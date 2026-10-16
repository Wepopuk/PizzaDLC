package pizza.dlc.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.visuals.SeeInvisibles;

@Mixin({LivingEntityRenderer.class})
public class LivingEntityRendererSeeInvisiblesMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = {@At("RETURN")}
   )
   private void pizzadlc$seeInvisibles(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
      if (entity.isInvisible() && enabled()) {
         state.isInvisibleToPlayer = false;
      }
   }

   @ModifyConstant(
      method = {"submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"},
      constant = {@Constant(
         intValue = 0x26FFFFFF
      )}
   )
   private int pizzadlc$seeInvisiblesColor(int original) {
      SeeInvisibles m = module();
      if (m != null && m.isEnabled()) {
         float a = Mth.clamp(m.alpha.getValue(), 0.0F, 1.0F);
         return (int)(a * 255.0F) << 24 | 0xFFFFFF;
      }

      return original;
   }

   private static SeeInvisibles module() {
      PizzaDLCClient c = PizzaDLCClient.getInstance();
      return c != null && c.getModuleManager() != null ? c.getModuleManager().get(SeeInvisibles.class) : null;
   }

   private static boolean enabled() {
      SeeInvisibles m = module();
      return m != null && m.isEnabled();
   }
}
