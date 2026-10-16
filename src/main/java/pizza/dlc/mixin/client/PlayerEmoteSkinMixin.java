package pizza.dlc.mixin.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.util.render.streamproof.CleanRenderScope;
import pizza.dlc.api.util.render.streamproof.StreamProofRenderer;

/** getSkin also selects the wide/slim renderer and supplies first-person hands. */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerEmoteSkinMixin {
   @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
   private void pizzadlc$emoteSkin(CallbackInfoReturnable<PlayerSkin> cir) {
      if (CleanRenderScope.isActive() || StreamProofRenderer.isVanillaGameplayActive()) return;
      AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
      cir.setReturnValue(EmoteEngine.INSTANCE.skinFor(player.getUUID(), cir.getReturnValue()));
   }
}
