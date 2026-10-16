package pizza.dlc.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketAccessor {
   @Accessor("attachedToEntity")
   LivingEntity pizzadlc$getAttachedToEntity();

   @Accessor("life")
   int pizzadlc$getLife();

   @Accessor("lifetime")
   int pizzadlc$getLifetime();
}
