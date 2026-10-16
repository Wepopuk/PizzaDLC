package pizza.dlc.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pizza.dlc.client.modules.impl.combat.HitBox;

@Mixin({ProjectileUtil.class})
public abstract class ProjectileUtilMixin {
   @ModifyExpressionValue(
      method = {"getHitEntitiesAlong(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/component/AttackRange;Ljava/util/function/Predicate;Lnet/minecraft/world/level/ClipContext$Block;)Lcom/mojang/datafixers/util/Either;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/component/AttackRange;hitboxMargin()F"
      )}
   )
   private static float pizzadlc$hitboxMargin(float original, @Local(argsOnly = true) Entity attacker) {
      if (attacker == Minecraft.getInstance().player) {
         float extra = HitBox.extraMargin();
         if (extra > 0.0F) {
            return original + extra;
         }
      }

      return original;
   }
}
