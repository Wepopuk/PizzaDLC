package pizza.dlc.api.rotation.combat.impl;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import pizza.dlc.api.util.combat.MouseSensitivity;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class SmoothAim extends CombatRotationPlan {

   public SmoothAim(Vec3 var1) {
      super(var1, CombatRotationPlan.UpdateRate.SLOW);
   }

   @Override
   public Vector2f rotate(Vector2f var1, Vector2f var2) {
      float var3 = var2.x;
      float var4 = var2.y;
      float var5 = var1.x;
      float var6 = var1.y;
      double var7 = Mth.wrapDegrees(var3 - var5);
      double var9 = var4 - var6;
      double var11 = Math.sqrt(var7 * var7 + var9 * var9);
      if (var11 == 0.0) {
         return new Vector2f(var1.x, var1.y);
      }

      double var13 = Math.abs(var7 / var11);
      double var15 = Math.abs(var9 / var11);
      double var17 = 360.0 * var13;
      double var19 = 360.0 * var15;
      float var21 = (float)Math.max(Math.min(var7, var17), -var17);
      float var22 = (float)Math.max(Math.min(var9, var19), -var19);
      float var23 = !this.returning ? pizza.dlc.api.util.combat.CombatMath.m_1302(-5.0F, 5.0F) : 0.0F;
      float var24 = !this.returning ? pizza.dlc.api.util.combat.CombatMath.m_1302(-4.0F, 4.0F) : 0.0F;
      var3 = var5 + var21 + var23;
      var4 = var6 + var22 + var24;

      for (int var25 = 1; var25 <= (int)(Minecraft.getInstance().getFps() / 20.0F + Math.random() * 20.0); var25++) {
         if (Math.abs(var21) + Math.abs(var22) > 1.0F) {
            var3 += (float)((Math.random() - 0.5) / 900.0);
            var4 -= (float)(Math.random() / 100.0);
         }

         Vector2f var26 = new Vector2f(var3, var4);
         var3 = var26.x;
         var4 = Math.max(-90.0F, Math.min(90.0F, var26.y));
      }

      return new Vector2f(MouseSensitivity.m_9133(var3), MouseSensitivity.m_9133(var4));
   }
}
