package pizza.dlc.mixin.client;

import com.mojang.authlib.minecraft.UserApiService;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftSessionAccessor {
   @Mutable
   @Accessor("userApiService")
   void pizzadlc$setUserApiService(UserApiService service);

   @Mutable
   @Accessor("profileKeyPairManager")
   void pizzadlc$setProfileKeyPairManager(ProfileKeyPairManager manager);

   @Mutable
   @Accessor("userPropertiesFuture")
   void pizzadlc$setUserPropertiesFuture(CompletableFuture<UserApiService.UserProperties> future);
}
