package pizza.dlc.mixin.client;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pizza.dlc.api.neuro.NeuroSystem;
@Mixin(MultiPlayerGameMode.class)
public abstract class NeuroInteractionMixin {
 @Inject(method="attack",at=@At("HEAD"),cancellable=true)
 private void pizzadlc$localTrainingHit(Player player,Entity victim,CallbackInfo ci){
  if(NeuroSystem.get()!=null&&NeuroSystem.get().localAttack(victim))ci.cancel();
 }
}
