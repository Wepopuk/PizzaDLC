package pizza.dlc.mixin.client;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.api.util.render.combat.CombatHudText;


import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pizza.dlc.client.PizzaDLCClient;
import pizza.dlc.client.modules.impl.movement.SuperFirework;

@Mixin({FireworkRocketEntity.class})
public class FireworkRocketEntityMixin {
   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      ),
      require = 0
   )
   private void pizzadlc$superFirework(LivingEntity boosted, Vec3 vanillaVel) {
      if (pizza.dlc.api.util.combat.CombatRuntime.rocketModulesActive(boosted)) {
         boosted.setDeltaMovement(pizza.dlc.api.util.combat.CombatRuntime.rocketVelocity(boosted));
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      PizzaDLCClient cc = PizzaDLCClient.getInstance();
      SuperFirework booster = cc != null && cc.getModuleManager() != null ? cc.getModuleManager().get(SuperFirework.class) : null;
      if (booster != null && booster.isEnabled() && mc.player != null && boosted == mc.player && mc.player.isFallFlying()) {
         float power = booster.getBoostPower(mc.player.getYRot(), mc.player.getXRot());
         Vec3 dir = boosted.getLookAngle();
         Vec3 vel = boosted.getDeltaMovement();
         boosted.setDeltaMovement(
            vel.add(dir.x * 0.1 + (dir.x * power - vel.x) * 0.5, dir.y * 0.1 + (dir.y * power - vel.y) * 0.5, dir.z * 0.1 + (dir.z * power - vel.z) * 0.5)
         );
      } else {
         boosted.setDeltaMovement(vanillaVel);
      }
   }
}
