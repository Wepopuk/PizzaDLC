package pizza.dlc.api.rotation.combat.impl;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import pizza.dlc.api.util.combat.MouseSensitivity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class HvhAim extends CombatRotationPlan {

   @Override
   public Vector2f rotate(Vector2f var1, Vector2f var2) {
      float var3 = var2.x;
      float var4 = var2.y;
      float var5 = var1.x;
      float var6 = var1.y;
      double var7 = Mth.wrapDegrees(var3 - var5);
      double var9 = var4 - var6;
      double var11 = 999.0;
      double var13 = 999.0;
      float var15 = (float)Math.max(Math.min(var7, 999.0), -999.0);
      float var16 = (float)Math.max(Math.min(var9, 999.0), -999.0);
      var3 = var5 + var15;
      var4 = var6 + var16;
      Vector2f var17 = new Vector2f(var3, var4);
      var3 = var17.x;
      var4 = Math.max(-90.0F, Math.min(90.0F, var17.y));
      return new Vector2f(MouseSensitivity.m_9133(var3), MouseSensitivity.m_9133(var4));
   }

   public HvhAim(Vec3 var1) {
      super(var1, CombatRotationPlan.UpdateRate.FAST);
   }
}
