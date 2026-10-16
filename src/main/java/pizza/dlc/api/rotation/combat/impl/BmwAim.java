package pizza.dlc.api.rotation.combat.impl;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import pizza.dlc.api.util.combat.AimNoise;
import pizza.dlc.api.util.combat.CombatTickTimer;
import pizza.dlc.api.util.combat.MouseSensitivity;

import pizza.dlc.client.modules.impl.combat.AttackAura;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class BmwAim extends CombatRotationPlan {
   private final AimNoise f_11050;
   private final CombatTickTimer f_11051;
   private Vec3 f_11052;
   private float f_11053;
   private float f_11054;
   private float f_11055;
   private boolean f_11056;
   private int f_11057;
   private float f_11058;

   @Override
   public Vector2f targetAngles() {
      LivingEntity var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class).getTarget();
      float var2 = (float)(Math.random() - 0.5) * mc.player.getBbWidth() * 0.6F;
      float var3 = (float)(Math.random() - 0.3) * mc.player.getBbHeight() * 0.5F;
      float var4 = (float)(Math.random() - 0.5) * mc.player.getBbWidth() * 0.6F;
      Vec3 var5 = new Vec3(var2, var3, var4);
      this.f_11052 = this.f_11052.lerp(var5, 0.12 + Math.random() * 0.08);
      Vec3 var6 = mc.player.getEyePosition();
      Vec3 var7 = this.targetPoint.add(this.f_11052);
      Vec3 var8 = var7.subtract(var6).normalize();
      float var9 = (float)Math.toDegrees(Math.atan2(-var8.x, var8.z));
      float var10 = (float)Math.toDegrees(Math.asin(var8.y));
      return new Vector2f(var9, -var10);
   }

   @Override
   public Vector2f rotate(Vector2f var1, Vector2f var2) {
      AttackAura var3 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
      Minecraft var4 = Minecraft.getInstance();
      float var5 = var2.x;
      float var6 = var2.y;
      float var7 = 0.003F + (float)Math.random() * 0.008F;
      var5 += (float)((Math.random() - 0.5) * var7);
      var6 += (float)((Math.random() - 0.5) * var7 * 0.6F);
      float var8 = var1.x;
      float var9 = var1.y;
      double var10 = Mth.wrapDegrees(var5 - var8);
      double var12 = var6 - var9;
      double var14 = Math.sqrt(var10 * var10 + var12 * var12);
      if (var14 == 0.0) {
         return new Vector2f(var1.x, var1.y);
      }

      float var16 = 80.0F + (float)Math.random() * 20.0F;
      float var17 = var16 * (1.0F + 0.2F * (float)Math.min(var14 / 20.0, 1.0));
      float var18 = var16 * 0.6F * (1.0F + 0.2F * (float)Math.min(var14 / 20.0, 1.0));
      double var19 = Math.abs(var10 / var14);
      double var21 = Math.abs(var12 / var14);
      double var23 = var17 * var19;
      double var25 = var18 * var21;
      float var27 = (float)Math.clamp(var10, -var23, var23);
      float var28 = (float)Math.clamp(var12, -var25, var25);
      float var29 = (float)Math.min(var14 / 15.0, 1.0);
      float var30 = var29 * var29 * (3.0F - 2.0F * var29);
      var30 = Math.min(var30 * 1.2F, 0.98F);
      var27 *= var30;
      var28 *= var30;
      this.f_11053 = this.f_11053 * 0.5F + 0.5F;
      this.f_11053 = Math.clamp(this.f_11053, 0.4F, 0.9F);
      float var31 = 0.985F + (float)Math.random() * 0.01F;
      float var32 = 0.98F + (float)Math.random() * 0.015F;
      var27 *= var31 * (0.8F + this.f_11053 * 0.15F);
      var28 *= var32 * (0.75F + this.f_11053 * 0.15F);
      this.f_11054 = this.f_11054 * 0.3F + var27 * 0.7F;
      this.f_11055 = this.f_11055 * 0.3F + var28 * 0.7F;
      float var33 = Math.abs((float)var10) * 1.02F;
      float var34 = Math.abs((float)var12) * 1.02F;
      this.f_11054 = Math.clamp(this.f_11054, -var33, var33);
      this.f_11055 = Math.clamp(this.f_11055, -var34, var34);
      var5 = var8 + this.f_11054;
      var6 = var9 + this.f_11055;
      float var35 = 0.002F + (float)Math.random() * 0.005F;
      var5 += (float)((Math.random() - 0.5) * var35);
      var6 += (float)((Math.random() - 0.5) * var35 * 0.8F);
      if (Math.random() < 0.001) {
         var5 += (float)((Math.random() - 0.5) * 0.1);
         var6 += (float)((Math.random() - 0.5) * 0.08);
      }

      var6 = Math.max(-90.0F, Math.min(90.0F, var6));
      return new Vector2f(MouseSensitivity.m_9128(var5), MouseSensitivity.m_9128(var6));
   }

   public BmwAim(Vec3 var1) {
      super(var1);
      this.f_11050 = new AimNoise(1.0F, 15.0F, 1.0F);
      this.f_11051 = new CombatTickTimer();
      this.f_11052 = Vec3.ZERO;
      this.f_11053 = 0.0F;
      this.f_11054 = 0.0F;
      this.f_11055 = 0.0F;
      this.f_11056 = false;
      this.f_11057 = 0;
      this.f_11058 = 0.0F;
   }

   @Override
   public void prepare() {
      this.f_11050.m_7967();
   }

   @Override
   public boolean usesPlayerCamera() {
      return false;
   }
}
