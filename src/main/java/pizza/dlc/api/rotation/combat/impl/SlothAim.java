package pizza.dlc.api.rotation.combat.impl;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import pizza.dlc.api.util.combat.MouseSensitivity;

import pizza.dlc.client.modules.impl.combat.AttackAura;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class SlothAim extends CombatRotationPlan {
   private float f_6780;

   private static final double f_6799 = Math.PI;

   private static final double f_6805 = Math.PI;

   @Override
   public Vector2f rotate(Vector2f var1, Vector2f var2) {
      AttackAura var3 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      double var4 = Mth.wrapDegrees(var2.x - var1.x);
      double var6 = var2.y - var1.y;
      float var11 = (0.08F + ((float)Math.random() * 0.02F - 0.01F)) * 0.01F;
      this.f_6780 += var11;
      this.f_6780 = Math.clamp(this.f_6780, 0.0F, 1.0F);
      float var10 = this.f_6780;
      float var12 = 0.0F;
      float var13 = 0.0F;
      LivingEntity var14 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class).getTarget();
      if (mc.hitResult instanceof EntityHitResult var15 && var15.getEntity() == var14 && !this.returning) {
         this.f_6780 = 0.0F;
         var10 = 0.0F;
      } else {
         var12 = (float)(Math.sin(System.currentTimeMillis() * 0.001) * 0.5);
         var13 = (float)(Math.cos(System.currentTimeMillis() * 0.0015) * 0.3);
         if (Math.abs(var4) > 1.0 || Math.abs(var6) > 1.0) {
            var12 += ((float)Math.random() - 0.5F) * 0.4F;
            var13 += ((float)Math.random() - 0.5F) * 0.2F;
         }
      }

      float var36 = 90.0F;
      float var37 = 90.0F;
      float var17 = new Random().nextFloat(5.0F);
      double var18 = Math.sqrt(var4 * var4 + var6 * var6);
      if (var18 == 0.0) {
         return new Vector2f(var1.x, var1.y);
      }

      double var20 = Math.abs(var4 / var18);
      double var22 = Math.abs(var6 / var18);
      double var24 = (var36 + var17) * var20;
      double var26 = (var37 + var17) * var22;
      float var28 = (float)Math.max(Math.min(var4, var24), -var24) + var12;
      float var29 = (float)Math.max(Math.min(var6, var26), -var26) + var13;
      float var8 = var1.x + var28 * var10;
      float var9 = var1.y + var29 * var10;
      float var30 = (float)(System.currentTimeMillis() % 10000L) / 375.0F;
      float var31 = (float)Math.sin(var30 * 3.14F * f_6799) * 7.23F * 0.083333336F;
      var8 -= !this.returning ? var31 : 0.0F;
      float var32 = (float)(System.currentTimeMillis() % 10000L) / 425.0F;
      float var33 = (float)Math.sin(var32 * 3.14F * f_6805) * 15.2F * 0.083333336F;
      var9 += !this.returning ? var33 : 0.0F;
      return new Vector2f(MouseSensitivity.m_9128(var8), MouseSensitivity.m_9128(var9));
   }

   public SlothAim(Vec3 var1) {
      super(var1, CombatRotationPlan.UpdateRate.FAST);
   }
}
