package pizza.dlc.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.client.modules.impl.movement.NoWeb;

@Mixin(value = WebBlock.class, remap = false)
public class WebBlockMixin {
   @Inject(remap = false, method = {"entityInside"}, at = {@At(value = "HEAD", remap = false)}, cancellable = true)
   private void pizzadlc$onEntityCollision(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl, CallbackInfo ci) {
      ((pizza.dlc.api.interfaces.WebStateAccess)entity).setInWeb(true);
      if (NoWeb.isIgnoreActive() && entity == Minecraft.getInstance().player) {
         ci.cancel();
         NoWeb.onEntityCollideCobweb(pos);
      }
   }
}
