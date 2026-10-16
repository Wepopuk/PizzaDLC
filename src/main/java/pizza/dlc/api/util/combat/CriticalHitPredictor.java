package pizza.dlc.api.util.combat;
import pizza.dlc.api.interfaces.WebStateAccess;
import pizza.dlc.api.util.combat.CombatMath;
import pizza.dlc.api.util.combat.CombatRaytrace;
import pizza.dlc.api.util.combat.CombatRuntime;

import pizza.dlc.client.modules.impl.combat.ElytraTarget;
import lombok.Generated;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class CriticalHitPredictor implements pizza.dlc.api.interfaces.QuickImports {
   private static int f_10211 = 0;

   public static boolean m_3380(LivingEntity var0, boolean var1, boolean var2, boolean var3, boolean var4, float var5, boolean var6, boolean var7) {
      ElytraTarget var8 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
      if (CombatRuntime.moduleEnabled("FreeCam") || CombatRuntime.moduleEnabled("AirStuck")) {
         var5 = 6.0F;
      }

      boolean var9 = var4 && var0.isFallFlying() && var8.getAttackTiming().is("Перегнал") && CombatMath.blocksPerSecond(var0) > var8.getPredictionBps().getValue()
         ? Math.sqrt(mc.player.distanceToSqr(var8.predictTarget(var0, var0.position().add(0.0, var0.getBbHeight() / 2.0F, 0.0)))) <= var5
         : mc.player.distanceTo(var0) <= var5;
      boolean var10 = !var7 || CombatRuntime.worldTicks >= f_10211;
      boolean var11 = var9;
      boolean var12 = mc.player.getAttackStrengthScale(1.5F) > 0.92F;
      boolean var13 = mc.player.isInWater() && !mc.options.keyJump.isDown();
      boolean var14 = CombatRuntime.moduleEnabled("AirStuck") || CombatRuntime.moduleEnabled("FreeCam");
      boolean var15 = mc.player.fallDistance > 0.0
         || ((WebStateAccess)mc.player).isInWeb()
         || !var1
         || var2 && mc.player.onGround() && !mc.options.keyJump.isDown();
      boolean var16 = mc.player.hasEffect(MobEffects.LEVITATION) || mc.player.hasEffect(MobEffects.BLINDNESS);
      boolean var17 = mc.player.isInLava() || mc.player.onClimbable() || mc.player.getAbilities().flying;
      boolean var18 = !var6
         || CombatRaytrace.m_6784(
            mc.player.getViewYRot(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
            mc.player.getViewXRot(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)),
            var5,
            var0,
            false
         );
      boolean var19 = var10
         && var11
         && var12
         && var18
         && (mc.player.isEyeInFluid(FluidTags.WATER) || var15 || var17 || var16 || var13 || var14 || mc.player.isFallFlying());
      if (var19 && var3) {
         f_10211 = CombatRuntime.worldTicks + 10;
      }

      return var19;
   }

   @Generated
   private CriticalHitPredictor() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static int m_3381(LivingEntity var0, boolean var1, boolean var2, boolean var3, float var4, boolean var5, int var6) {
      if (var0 != null && mc.player != null && var6 >= 0) {
         ElytraTarget var7 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
         boolean var8 = CombatRuntime.moduleEnabled("AirStuck") || CombatRuntime.moduleEnabled("FreeCam");
         if (var8) {
            var4 = 6.0F;
         }

         boolean var9 = mc.player.hasEffect(MobEffects.LEVITATION) || mc.player.hasEffect(MobEffects.BLINDNESS);
         boolean var10 = mc.player.isInLava() || mc.player.onClimbable() || mc.player.getAbilities().flying;
         boolean var11 = mc.player.isInWater() && !mc.options.keyJump.isDown();
         Vec3 var12 = mc.player.position();
         Vec3 var13 = var0.position();
         Vec3 var14 = mc.player.getDeltaMovement();
         Vec3 var15 = var0.getDeltaMovement();
         double var16 = var14.y;
         boolean var18 = mc.player.onGround();
         float var19 = (float)mc.player.fallDistance;

         for (int var20 = 0; var20 <= var6; var20++) {
            boolean var21 = var3
               && var7.getAttackTiming().is("Перегнал")
               && var0.isFallFlying()
               && CombatMath.blocksPerSecond(var0) > var7.getPredictionBps().getValue();
            Vec3 var22 = var21 ? var7.predictTarget(var0, var13.add(0.0, var0.getBbHeight() / 2.0, 0.0)) : var13;
            boolean var23 = var12.distanceTo(var22) <= var4;
            boolean var24 = !var5 || CombatRuntime.worldTicks + var20 >= f_10211;
            boolean var25 = mc.player.getAttackStrengthScale(1.5F + var20) > 0.92F;
            boolean var26 = var19 > 0.0F || !var18 && var16 < -1.0E-5;
            boolean var27 = var26 || !var1 || var2 && var18 && !mc.options.keyJump.isDown();
            boolean var28 = ((WebStateAccess)mc.player).isInWeb() && var26;
            boolean var29 = mc.player.isEyeInFluid(FluidTags.WATER) || var27 || var10 || var9 || var11 || var8 || var28 || mc.player.isFallFlying();
            if (var23 && var24 && var25 && var29) {
               return var20;
            }

            if (var20 == var6) {
               break;
            }

            var12 = var12.add(var14);
            var13 = var13.add(var15);
            if (!var18) {
               var16 = (var16 - 0.08) * 0.98;
               var14 = new Vec3(var14.x, var16, var14.z);
               if (var16 < 0.0) {
                  var19 += (float)(-var16);
               }
            }

            var15 = var15.multiply(0.99, 0.98, 0.99);
         }

         return -1;
      } else {
         return -1;
      }
   }
}
