package pizza.dlc.client.modules.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.mixin.client.FireworkRocketAccessor;

public final class ElytraPredict {
   private static final int DEFAULT_BOOST_TICKS = 20;

   private ElytraPredict() {
   }

   public static Vec3 predict(LivingEntity target, int ticks) {
      Vec3 pos = ElytraSample.serverPos(target);
      Vec3 vel = ElytraSample.getVelocity(target);
      int boost = boostTicks(target);
      for (int i = 0; i < ticks; i++) {
         vel = step(target, vel, i < boost);
         pos = pos.add(vel);
      }
      return pos;
   }

   public static Vec3 interceptOffset(LivingEntity target, int maxTicks) {
      LocalPlayer self = Minecraft.getInstance().player;
      if (self == null || maxTicks <= 0) return Vec3.ZERO;
      Vec3 eye = self.getEyePosition();
      double speed = Math.max(self.getDeltaMovement().length(), boostTicks(self) > 0 ? 1.6D : 0.8D);
      double reach = 3.0D;
      Vec3 pos = ElytraSample.serverPos(target);
      Vec3 vel = ElytraSample.getVelocity(target);
      int boost = boostTicks(target);
      double half = target.getBbHeight() * 0.5D;
      for (int t = 1; t <= maxTicks; t++) {
         vel = step(target, vel, t - 1 < boost);
         pos = pos.add(vel);
         double need = pos.add(0.0D, half, 0.0D).distanceTo(eye) - reach;
         if (need <= speed * t) {
            break;
         }
      }
      return pos.subtract(target.position());
   }

   private static Vec3 step(LivingEntity e, Vec3 vel, boolean boosting) {
      Vec3 look = e.getLookAngle();
      if (boosting) {
         vel = vel.add(look.x * 0.1D + (look.x * 1.5D - vel.x) * 0.5D, look.y * 0.1D + (look.y * 1.5D - vel.y) * 0.5D, look.z * 0.1D + (look.z * 1.5D - vel.z) * 0.5D);
      }
      float pitch = e.getXRot() * (float)(Math.PI / 180.0);
      double horizLook = Math.sqrt(look.x * look.x + look.z * look.z);
      double horizVel = vel.horizontalDistance();
      double gravity = vel.y <= 0.0D && e.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(e.getGravity(), 0.01D) : e.getGravity();
      double cos2 = Mth.square(Math.cos(pitch));
      vel = vel.add(0.0D, gravity * (-1.0D + cos2 * 0.75D), 0.0D);
      if (vel.y < 0.0D && horizLook > 0.0D) {
         double d = vel.y * -0.1D * cos2;
         vel = vel.add(look.x * d / horizLook, d, look.z * d / horizLook);
      }
      if (pitch < 0.0F && horizLook > 0.0D) {
         double d = horizVel * -Mth.sin(pitch) * 0.04D;
         vel = vel.add(-look.x * d / horizLook, d * 3.2D, -look.z * d / horizLook);
      }
      if (horizLook > 0.0D) {
         vel = vel.add((look.x / horizLook * horizVel - vel.x) * 0.1D, 0.0D, (look.z / horizLook * horizVel - vel.z) * 0.1D);
      }
      return vel.multiply(0.99F, 0.98F, 0.99F);
   }

   private static int boostTicks(LivingEntity e) {
      if (e.level() == null) return 0;
      int best = 0;
      for (FireworkRocketEntity rocket : e.level().getEntitiesOfClass(FireworkRocketEntity.class, e.getBoundingBox().inflate(2.0D))) {
         FireworkRocketAccessor acc = (FireworkRocketAccessor)rocket;
         if (acc.pizzadlc$getAttachedToEntity() != e) continue;
         int lifetime = acc.pizzadlc$getLifetime();
         int left = lifetime > 0 ? lifetime - acc.pizzadlc$getLife() : DEFAULT_BOOST_TICKS - rocket.tickCount;
         best = Math.max(best, Math.max(0, left));
      }
      return best;
   }
}
