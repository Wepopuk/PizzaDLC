package pizza.dlc.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MultiPlayerGameMode.class, remap = false)
public interface MultiPlayerGameModeAccessor {
   @Accessor("destroyProgress")
   float getDestroyProgress();

   @Accessor("destroyProgress")
   void setDestroyProgress(float progress);

   @Accessor("destroyDelay")
   int getDestroyDelay();

   @Accessor("destroyDelay")
   void setDestroyDelay(int delay);

   @org.spongepowered.asm.mixin.gen.Invoker(value = "ensureHasSentCarriedItem", remap = false)
   void invokerEnsureHasSentCarriedItem();

   @Accessor("carriedIndex")
   void setCarriedIndex(int index);
}
