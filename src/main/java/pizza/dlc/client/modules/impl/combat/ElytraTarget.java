package pizza.dlc.client.modules.impl.combat;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.client.modules.impl.combat.AttackAura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.impl.movement.ElytraBooster;
import pizza.dlc.api.util.combat.CombatMath;
import pizza.dlc.api.util.render.combat.CombatRenderTypes;
import pizza.dlc.api.events.impl.combat.GlidingVelocityEvent;
import pizza.dlc.api.util.combat.ElytraPrediction;
import pizza.dlc.api.util.combat.CombatTimer;
import pizza.dlc.api.events.impl.combat.CombatWorldRenderEvent;
import pizza.dlc.api.events.impl.combat.RocketBoostEvent;
import pizza.dlc.api.events.impl.combat.CombatTickEvent;
import pizza.dlc.api.util.render.combat.CombatBoxGeometry;
import pizza.dlc.api.util.combat.CombatInventory;
import pizza.dlc.api.interfaces.ResolvedRotationAccess;
import lombok.Generated;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElytraTarget extends Module {
   public final BooleanSetting groundRetarget;
   public final CombatSliderSetting escapeDistance;
   private final CombatSliderSetting attackRange;
   private final CombatSliderSetting detectionRange;
   private final BooleanSetting lagFlight;
   private final CombatSliderSetting retreatDistance;
   private final BooleanSetting overtake;
   private final ModeSetting predictionMode;
   private final CombatSliderSetting predictionStrength;
   private final ModeSetting attackTiming;
   private final CombatSliderSetting predictionBps;
   private final BooleanSetting automaticFirework;
   private final BooleanSetting instantResponse;
   private final BooleanSetting renderPrediction;
   public final CombatTimer rocketTimer;
   private boolean restoreRocketVelocity;

   private static final float f_14497 = (float) (Math.PI / 180.0);

   public ElytraTarget() {
      super("ElytraTarget", "Преследует игрока на элитрах", ModuleCategory.COMBAT);
      this.groundRetarget = CombatSettings.bool("Смена на наземную цель", this, true);
      this.escapeDistance = new CombatSliderSetting("Дистанция смены цели", this, 12, 6, 60).withStep(1).setVisible(groundRetarget::isEnabled);
      this.attackRange = new CombatSliderSetting("Дистанция атаки", this, 3.0F, 1.0F, 5.0F).withStep(0.1F);
      this.detectionRange = new CombatSliderSetting("Дистанция обнаружения", this, 30.0F, 15.0F, 120.0F).withStep(0.1F);
      this.lagFlight = CombatSettings.bool("Элитра лаг", this, false);
      this.retreatDistance = new CombatSliderSetting("Дистанция отлета", this, 7.5F, 5.0F, 15.0F).withStep(0.5F).setVisible(() -> this.lagFlight.isEnabled());
      this.overtake = CombatSettings.bool("Элитра перегон", this, false);
      this.predictionMode = new ModeSetting("Режим предикта", this, "От ротации", "От скорости")
         .setVisible(() -> this.overtake.isEnabled());
      this.predictionStrength = new CombatSliderSetting("Сила предикта", this, 3.0F, 1.5F, 10.0F).withStep(0.5F).setVisible(() -> this.overtake.isEnabled());
      this.attackTiming = new ModeSetting("Бить когда", this, "Перегнал", "Всегда");
      this.predictionBps = new CombatSliderSetting("Предиктить от бпс", this, 15.0F, 10.0F, 30.0F).withStep(1.0F);
      this.automaticFirework = CombatSettings.bool("Автоматически использовать фейверк", this, true);
      this.instantResponse = CombatSettings.bool("Моментальный отклик", this, true);
      this.renderPrediction = CombatSettings.bool("Рендерить предикт", this, true);
      this.rocketTimer = new CombatTimer();

      this.getSettings().clear();
      this.addSettings(
         this.groundRetarget,
         this.escapeDistance,
         this.attackRange,
         this.detectionRange,
         this.lagFlight,
         this.retreatDistance,
         this.overtake,
         this.predictionMode,
         this.predictionStrength,
         this.attackTiming,
         this.predictionBps,
         this.automaticFirework,
         this.instantResponse,
         this.renderPrediction
      );
   }

   @Generated
   public CombatTimer getRocketTimer() {
      return this.rocketTimer;
   }

   @Generated
   public CombatSliderSetting getRetreatDistance() {
      return this.retreatDistance;
   }

   @Generated
   public boolean getRestoreRocketVelocity() {
      return this.restoreRocketVelocity;
   }

   @Generated
   public CombatSliderSetting getPredictionBps() {
      return this.predictionBps;
   }

   @Generated
   public ModeSetting getAttackTiming() {
      return this.attackTiming;
   }

   @Generated
   public BooleanSetting getLagFlight() {
      return this.lagFlight;
   }

   @Generated
   public ModeSetting onTick() {
      return this.predictionMode;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void m_5081(CombatTickEvent var1) {
      if (mc.player != null) {
         if (!mc.player.isFallFlying()) {
            this.restoreRocketVelocity = false;
         }

         AttackAura var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
         if (var2.isEnabled() && mc.player.isFallFlying() && var2.getTarget() != null && this.automaticFirework.isEnabled() && this.rocketTimer.m_4458(1000.0)) {
            pizza.dlc.api.util.game.SwapSystem.useItemTracked(Items.FIREWORK_ROCKET,null,this,"automatic-firework",
               pizza.dlc.api.util.game.InventorySwapQueue.configuredMode(),this.rocketTimer::m_4459);
         }
      }
   }

   @Generated
   public BooleanSetting getRenderPrediction() {
      return this.renderPrediction;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   private void onGlidingVelocity(GlidingVelocityEvent var1) {
      if (mc.player != null && this.instantResponse.isEnabled()) {
         LivingEntity var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class).getTarget();
         if (this.restoreRocketVelocity) {
            float var3 = 1.7F;
            ElytraBooster var4 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraBooster.class);
            if (var4.isEnabled()) {
               var3 = var4.horizontalBoost() + 0.17F;
            }

            if (var2 != null && mc.player.getEyePosition().distanceTo(this.m_5088(var2)) < 2.0) {
               var3 = (float)var2.getDeltaMovement().length();
            }

            var1.setVelocity(this.m_5095(Vec3.directionFromRotation(mc.player.getXRot(), mc.player.getYRot()).scale(var3), 1.0));
            this.restoreRocketVelocity = false;
         } else if (var2 != null) {
            Vec3 var5 = this.m_5095(mc.player.getDeltaMovement(), 3.2);
            Vec3 var6 = new Vec3(var2.getDeltaMovement().x * 1.0E-5F, var2.getDeltaMovement().y * 1.0E-5F, var2.getDeltaMovement().z * 1.0E-5F);
            if (mc.player.getEyePosition().distanceTo(this.m_5088(var2)) < 4.5
               && var2.isFallFlying()
               && Math.hypot(var2.getDeltaMovement().x, var2.getDeltaMovement().z) != 0.0) {
               var1.setVelocity(var5.add(var6));
            }
         }
      }
   }

   public boolean m_5084(LivingEntity var1) {
      return mc.player.isFallFlying();
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   private void onWorldRender(CombatWorldRenderEvent var1) {
      if (mc.player != null && mc.level != null) {
         if (this.renderPrediction.isEnabled()) {
            LivingEntity var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class).getTarget();
            if (var2 != null) {
               AttackAura var3 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class);
               if (var3.isEnabled() && mc.player.isFallFlying() && var2.isFallFlying()) {
                  PoseStack var4 = var1.getPose();
                  VertexConsumer var5 = var1.getBuffer().m_881(CombatRenderTypes.f_3816);
                  Vec3 var6 = var1.getCameraPosition();
                  Vec3 var7 = var2.position().add(0.0, var2.getBbHeight() / 2.0F + (mc.player.isFallFlying() ? 0.5F : 0.0F), 0.0);
                  Vec3 var8 = this.predictTarget(var2, var7);
                  AABB var9 = AABB.ofSize(var8, 0.75, 0.75, 0.75);
                  CombatBoxGeometry.m_6087(var4, var5, var6, var9.minX, var9.minY, var9.minZ, var9.maxX, var9.maxY, var9.maxZ, -1);
               }
            }
         }
      }
   }

   @Generated
   public BooleanSetting getOvertake() {
      return this.overtake;
   }

   @Generated
   public CombatSliderSetting getDetectionRange() {
      return this.detectionRange;
   }

   private Vec3 m_5088(LivingEntity var1) {
      return var1.getEyePosition().add(var1.getDeltaMovement());
   }

   @Generated
   public CombatSliderSetting getPredictionStrength() {
      return this.predictionStrength;
   }

   @Generated
   public CombatSliderSetting getAttackRange() {
      return this.attackRange;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   private void onRocketBoost(RocketBoostEvent var1) {
      if (mc.player != null && this.instantResponse.isEnabled()) {
         LivingEntity var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(AttackAura.class).getTarget();
         Vec3 var3 = var2 != null ? CombatMath.m_1307(mc.player.getEyePosition(), var2.getBoundingBox()) : Vec3.ZERO;
         if (var1.getEntity() == mc.player
            && (var2 == null || !mc.player.isFallFlying() || var2.isFallFlying() || !(mc.player.getEyePosition().distanceTo(var3) < 2.6F))) {
            this.restoreRocketVelocity = true;
         }
      }
   }

   public void resetState() {
      this.restoreRocketVelocity = false;
   }

   @Generated
   public BooleanSetting getAutomaticFirework() {
      return this.automaticFirework;
   }

   private Vec3 m_5095(Vec3 var1, double var2) {
      Vec3 var4 = mc.player.getLookAngle();
      float var5 = mc.player.getXRot() * f_14497;
      double var6 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
      double var8 = var1.horizontalDistance();
      boolean var10 = mc.player.getDeltaMovement().y <= 0.0;
      double var11 = var10 && mc.player.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(mc.player.getGravity(), 0.01) : mc.player.getGravity();
      double var13 = Mth.square(Math.cos(var5));
      var1 = var1.add(0.0, var11 * (-1.0 + var13 * 0.75), 0.0);
      if (var1.y < 0.0 && var6 > 0.0) {
         double var15 = var1.y * -0.1 * var13;
         var1 = var1.add(var4.x * var15 / var6, var15, var4.z * var15 / var6);
      }

      if (var5 < 0.0F && var6 > 0.0) {
         double var18 = var8 * -Mth.sin(var5) * 0.04;
         var1 = var1.add(-var4.x * var18 / var6, var18 * var2, -var4.z * var18 / var6);
      }

      if (var6 > 0.0) {
         var1 = var1.add((var4.x / var6 * var8 - var1.x) * 0.1, 0.0, (var4.z / var6 * var8 - var1.z) * 0.1);
      }

      return var1.multiply(0.99F, 0.98F, 0.99F);
   }

   @Generated
   public BooleanSetting getInstantResponse() {
      return this.instantResponse;
   }

   public Vec3 predictTarget(LivingEntity var1, Vec3 var2) {
      if (this.isEnabled() && var1 != null && mc.player != null) {
         if (!this.m_5084(var1)
            || !this.overtake.isEnabled()
            || !var1.isFallFlying()
            || !(CombatMath.blocksPerSecond(var1) > this.predictionBps.getValue())) {
            return var2;
         } else {
            return this.predictionMode.is("От ротации")
               ? var2.add(
                  var1.calculateViewVector(((ResolvedRotationAccess)var1).getResolvedPitch(), ((ResolvedRotationAccess)var1).getResolvedYaw())
                     .scale(this.predictionStrength.getValue())
               )
               : var2.add(ElytraPrediction.m_3885(var1, this.predictionStrength.getValue()));
         }
      } else {
         return var2;
      }
   }

   @Override public void onEnable() {
      super.onEnable();
      CombatRuntime.enabled(this);
   }
   @Override public void onDisable() {
      try { resetState(); } finally { CombatRuntime.disabled(this); super.onDisable(); }
   }
}
