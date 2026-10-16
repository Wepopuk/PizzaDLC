package pizza.dlc.client.modules.impl.combat;
import pizza.dlc.api.rotation.combat.CombatRotations;
import pizza.dlc.api.settings.CombatSettings;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ColorSetting;
import pizza.dlc.api.settings.impl.CombatSliderSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.MultiModeSetting;
import pizza.dlc.api.util.combat.CombatRuntime;
import pizza.dlc.client.modules.impl.combat.ElytraTarget;

import com.google.gson.JsonObject;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.Module;
import pizza.dlc.api.util.combat.CombatPackets;
import pizza.dlc.api.util.combat.CombatMath;
import pizza.dlc.api.rotation.combat.impl.FuntimeSnapAim;
import pizza.dlc.api.events.impl.combat.CombatKeyEvent;
import pizza.dlc.api.events.impl.combat.FastAimEvent;
import pizza.dlc.api.rotation.combat.CombatRotationState;
import pizza.dlc.api.util.combat.CriticalHitPredictor;
import pizza.dlc.api.util.combat.CombatMovement;
import pizza.dlc.api.events.impl.combat.CombatWorldRenderEvent;
import pizza.dlc.api.rotation.combat.CombatRotationPlans;
import pizza.dlc.api.events.impl.combat.CombatRotationEvent;
import pizza.dlc.api.events.impl.combat.CombatTickEvent;
import pizza.dlc.api.util.combat.CombatRaytrace;
import pizza.dlc.api.util.combat.CombatInventory;
import pizza.dlc.api.rotation.combat.CombatRotationPlan;
import pizza.dlc.api.util.combat.TargetSelection;
import pizza.dlc.api.events.impl.combat.CombatInputEvent;
import pizza.dlc.api.util.render.combat.CombatTargetEffects;
import pizza.dlc.api.events.impl.combat.MediumAimEvent;
import pizza.dlc.api.util.combat.MouseSensitivity;
import java.util.HashSet;
import java.util.List;
import lombok.Generated;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public class AttackAura extends Module {
   public final CombatSliderSetting attackRange; 
   public final CombatSliderSetting rotationRange;
   private final ModeSetting rotationMode;
   private final CombatSliderSetting heightFactor;
   private final CombatSliderSetting generationSpeed;
   private final ModeSetting rotationStyle;
   private final CombatSliderSetting prediction;
   private final CombatSliderSetting aimSpeed;
   private final CombatSliderSetting aimFov;
   private final CombatSliderSetting speedMultiplier;
   private final CombatSliderSetting verticalShare;
   private final CombatSliderSetting aimSharpness;
   private final CombatSliderSetting smoothing;
   private final CombatSliderSetting horizontalJitter;
   private final CombatSliderSetting verticalJitter;
   private final ModeSetting aimPoint;
   private final ModeSetting sortMode;
   private final CombatSliderSetting acceleration;
   private final CombatSliderSetting accelerationTicks;
   public MultiModeSetting attackOptions;
   private final ModeSetting movementCorrection;
   private final MultiModeSetting targetTypes;
   private final BooleanSetting clientLook;
   private final ModeSetting targetDisplay;
   private final BooleanSetting jellyThroughWalls;
   private final CombatSliderSetting jellySize;
   private final CombatSliderSetting jellyLayers;
   private final ModeSetting ringType;
   private final ModeSetting squareType;
   private final ModeSetting ghostType;
   private final ModeSetting crystalType;
   private final BooleanSetting crystalThroughWalls;
   private final ModeSetting lightningType;
   private final BooleanSetting lightningThroughWalls;
   private final BooleanSetting surfaceLighting;
   private final CombatSliderSetting ringSpeed;
   private final CombatSliderSetting soulCount;
   private final BooleanSetting hitColor;
   private final BooleanSetting themeColors;
   private final ColorSetting targetColor;
   private final ColorSetting gradientColor;
   private final BooleanSetting glow;
   private final BooleanSetting ghostThroughWalls;
   private final BooleanSetting distortion;
   private final CombatSliderSetting distortionStrength;
   private final AttackAura.ClientLookState clientLookState;
   private CombatRotationPlan f_765;
   private LivingEntity target;
   private boolean f_767;

   private int f_775;
   private long f_776;
   private long f_777;
   private int f_778;
   private long f_779;
   private float f_780;
   private float f_781;
   private float f_782;
   private float f_783;
   private long f_784;
   private float f_785;
   private float f_786;
   private float f_787;
   private float f_788;
   private boolean f_789;
   private long f_790;
   private boolean f_791;
   private boolean f_792;

   private static final double f_1046 = Double.MAX_VALUE;

   private static final double f_1100 = Double.MAX_VALUE;

   public LivingEntity m_1008(float var1) {
      if (mc.player != null && mc.level != null && !(var1 <= 0.0F)) {
         List var2 = TargetSelection.m_7570(
            var1, this.targetTypes.get("Игроки").isEnabled(), this.targetTypes.get("Голые").isEnabled(), this.targetTypes.get("Друзья").isEnabled(), this.targetTypes.get("Мобы").isEnabled()
         );
         Vec3 var3 = mc.player.position();
         Vec3 var4 = mc.player.getDeltaMovement();
         LivingEntity var5 = null;
         double var6 = f_1046;

         for (LivingEntity var9 : (Iterable<LivingEntity>) (Iterable) var2) {
            Vec3 var10 = var9.position().subtract(var3);
            Vec3 var11 = var9.getDeltaMovement().subtract(var4);
            double var12 = var11.lengthSqr();
            double var14 = var12 < 1.0E-6 ? 0.0 : -var10.dot(var11) / var12;
            var14 = Mth.clamp(var14, 0.0, 80.0);
            Vec3 var16 = var10.add(var11.scale(var14));
            double var17 = var10.dot(var4);
            double var19 = var16.lengthSqr() + var14 * 0.01 + (var17 <= 0.0 ? 10000.0 : 0.0);
            if (var19 < var6) {
               var6 = var19;
               var5 = var9;
            }
         }

         return var5;
      } else {
         return null;
      }
   }

   private void m_1009() {
      long var1 = System.nanoTime();
      if (this.f_777 == 0L) {
         this.f_777 = var1;
      }

      long var3 = var1 - this.f_777;
      this.f_778 = this.f_778 + (int)(var3 / 4166666L);
      this.f_777 = this.f_777 + this.f_778 * 4166666L;

      for (this.f_778 = Math.min(this.f_778, 240); this.f_778 > 0; this.f_778--) {
         if (this.rotationMode.is("Legit") && !this.f_767) {
            this.m_1113();
         }

         if (this.rotationMode.is("Spooky") && !this.f_767) {
            this.m_1018();
         }
      }
   }

   public double[] m_1011(double var1, double var3) {
      if (mc.player != null && this.target != null) {
         double var5 = this.target.getX() - mc.player.getX();
         double var7 = this.target.getZ() - mc.player.getZ();
         double var9 = Math.hypot(var5, var7);
         if (!(var9 < 1.0E-4) && !(var9 > this.attackRange.getValue() + 1.25)) {
            double var11 = var5 / var9;
            double var13 = var7 / var9;
            double var15 = var1 * var11 + var3 * var13;
            if (var15 < 0.0) {
               var1 -= var11 * var15;
               var3 -= var13 * var15;
            }

            return new double[]{var1, var3};
         } else {
            return new double[]{var1, var3};
         }
      } else {
         return new double[]{var1, var3};
      }
   }

   @Generated
   public BooleanSetting getHitColor() {
      return this.hitColor;
   }

   public boolean m_1013() {
      return this.isEnabled() && (this.rotationMode.is("HvH") || this.rotationMode.is("Smooth")) && this.target != null;
   }

   private void m_1014() {
      CombatRotationPlans var1 = CombatRotations.plans();
      if (this.f_765 == var1.legit() || var1.getActive() == var1.legit()) {
         CombatRotationState var2 = CombatRotations.state();
         var2.getClientRotation().set(var2.getRotation());
         if (mc.player != null) {
            mc.player.setYRot(var2.getRotation().x);
            mc.player.setXRot(var2.getRotation().y);
         }
      }
   }

   @Generated
   public ModeSetting getMovementCorrection() {
      return this.movementCorrection;
   }

   @Generated
   public void m_1017(long var1) {
      this.f_779 = var1;
   }

   private void m_1018() {
      if (this.target != null && mc.gui.screen() == null) {
         long var1 = System.nanoTime();
         float var3 = this.f_779 == 0L ? 0.0F : (float)(var1 - this.f_779) / 1.0E9F;
         this.f_779 = var1;
         var3 = Mth.clamp(var3, 0.0F, 0.05F);
         if (!(var3 <= 0.0F)) {
            Vec3 var4 = this.m_1118(this.target);
            float var5 = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(var4.z, var4.x)) - 90.0);
            float var6 = (float)(-Math.toDegrees(Math.atan2(var4.y, Math.hypot(var4.x, var4.z))));
            long var7 = 140000000L;
            if (var1 - this.f_784 >= var7) {
               this.f_785 = CombatMath.m_1302(-1.1F, 1.1F);
               this.f_786 = CombatMath.m_1302(-0.9F, 0.9F);
               this.f_784 = var1;
            }

            float var9 = mc.player.getYRot();
            float var10 = mc.player.getXRot();
            float var11;
            float var12;
            if (CombatRaytrace.m_6784(var9, var10, this.attackRange.getValue(), this.target, false)) {
               var11 = 0.0F;
               var12 = 0.0F;
            } else {
               var11 = Mth.wrapDegrees(var5 + this.f_785 - var9);
               var12 = Mth.wrapDegrees(var6 + this.f_786 - var10);
               var12 = Mth.clamp(var10 + var12, -90.0F, 90.0F) - var10;
            }

            float var13 = 650.0F;
            float var14 = 11.0F;
            float var15 = Mth.clamp(Math.abs(var11) * var14, 0.0F, var13) * Math.signum(var11);
            float var16 = Mth.clamp(Math.abs(var12) * var14, 0.0F, var13) * Math.signum(var12);
            float var17 = 1.0F - (float)Math.exp(-9.5F * var3);
            this.f_780 = this.f_780 + (var15 - this.f_780) * var17;
            this.f_781 = this.f_781 + (var16 - this.f_781) * var17;
            float var18 = this.f_780 * var3;
            float var19 = this.f_781 * var3;
            if (Math.abs(var18) > Math.abs(var11)) {
               var18 = var11;
            }

            if (Math.abs(var19) > Math.abs(var12)) {
               var19 = var12;
            }

            float var20 = MouseSensitivity.m_9139();
            if (var20 > 1.0E-5F) {
               float var21 = var18 + this.f_782;
               float var22 = Math.round(var21 / var20) * var20;
               this.f_782 = var21 - var22;
               var18 = var22;
               float var23 = var19 + this.f_783;
               float var24 = Math.round(var23 / var20) * var20;
               this.f_783 = var23 - var24;
               var19 = var24;
            }

            float var27 = var9 + var18;
            float var28 = Mth.clamp(var10 + var19, -90.0F, 90.0F);
            this.clientLookState.setTargetPoint(this.target.position().add(0.0, this.target.getBbHeight() * 0.6, 0.0));
            this.clientLookState.m_1005(var27, var28);
            if (CombatRotations.plans().schedule(this.clientLookState, 1, 6)) {
               CombatRotations.state().getRotation().set(var27, var28);
               mc.player.setYRot(var27);
               mc.player.setXRot(var28);
            }
         }
      } else {
         this.f_779 = System.nanoTime();
         this.f_780 = 0.0F;
         this.f_781 = 0.0F;
         this.f_782 = 0.0F;
         this.f_783 = 0.0F;
      }
   }

   @Generated
   public CombatSliderSetting getVerticalJitter() {
      return this.verticalJitter;
   }

   @Generated
   public CombatSliderSetting getRotationRange() {
      return this.rotationRange;
   }

   @Generated
   public float m_1022() {
      return this.f_781;
   }

   public void resetState() {
      this.f_767 = false;
      this.m_1014();
      this.m_1071();
      this.target = null;
      this.f_765 = null;
      this.f_775 = 0;
      this.f_776 = 0L;
      this.f_777 = 0L;
      this.f_778 = 0;
      this.f_779 = 0L;
      this.f_780 = 0.0F;
      this.f_781 = 0.0F;
      this.f_782 = 0.0F;
      this.f_783 = 0.0F;
      this.f_784 = 0L;
      this.f_785 = 0.0F;
      this.f_786 = 0.0F;
      this.f_789 = false;
      this.f_790 = 0L;
      CombatRotations.plans().human().reset();
      if (this.rotationMode.is("BMW")) {
         CombatRotations.plans().setActive(null);
      }
   }

   @Generated
   public CombatSliderSetting getSmoothing() {
      return this.smoothing;
   }

   private boolean m_1025(float var1) {
      CombatRotationPlans var2 = CombatRotations.plans();
      FuntimeSnapAim var3 = var2.funtimeSnap();
      if (!this.m_1075()) {
         if (var3.m_2016()) {
            return true;
         }

         var2.clear(var3);
         CombatRotations.state().cancelPlan(var3);
         this.f_792 = true;
         return false;
      } else {
         if (this.f_792) {
            var3.m_2039();
            this.f_792 = false;
         }

         if (this.target != null
            && !CombatRuntime.moduleEnabled("WindHop")
            && (!mc.player.isUsingItem() || mc.player.isBlocking() || !this.attackOptions.get("Не бить когда ешь").isEnabled())) {
            ElytraTarget var4 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
            float var5 = mc.player.isFallFlying()
                  && var4.isEnabled()
                  && this.target.isFallFlying()
                  && CombatMath.blocksPerSecond(this.target) > var4.getPredictionBps().getValue()
                  && var4.getOvertake().isEnabled()
               ? var4.getAttackRange().getValue()
               : var1;
            int var6 = CriticalHitPredictor.m_3381(this.target, this.attackOptions.get("Только криты").isEnabled(), this.attackOptions.get("Только в прыжке").isEnabled(), true, var5, this.attackOptions.get("Проверять кулдаун").isEnabled(), 5);
            var3.m_2017(this.target, var6);
            return true;
         } else {
            var3.m_2017(this.target, -1);
            return true;
         }
      }
   }

   @Generated
   public void m_1026(boolean var1) {
      this.f_792 = var1;
   }

   private boolean m_1027() {
      return this.target != null && this.attackOptions.get("Обход через стены").isEnabled() && !mc.player.hasLineOfSight(this.target)
         && !CombatRuntime.moduleEnabled("AirStuck") && !CombatRuntime.moduleEnabled("FreeCam");
   }

   @Generated
   public void m_1028(boolean var1) {
      this.f_789 = var1;
   }

   @Generated
   public CombatSliderSetting getHeightFactor() {
      return this.heightFactor;
   }

   @Generated
   public CombatSliderSetting getPrediction() {
      return this.prediction;
   }

   @Generated
   public ModeSetting getSortMode() {
      return this.sortMode;
   }

   @Generated
   public boolean m_1033() {
      return this.f_767;
   }

   @Generated
   public void m_1035(float var1) {
      this.f_787 = var1;
   }

   @Generated
   public BooleanSetting getDistortion() {
      return this.distortion;
   }

   @Generated
   public CombatSliderSetting getAimSpeed() {
      return this.aimSpeed;
   }

   @Generated
   public float m_1038() {
      return this.f_785;
   }

   @Generated
   public BooleanSetting getCrystalThroughWalls() {
      return this.crystalThroughWalls;
   }

   @Generated
   public void setTarget(LivingEntity var1) {
      this.target = var1;
   }

   @Generated
   public ColorSetting getGradientColor() {
      return this.gradientColor;
   }

   public void m_1043(boolean var1) {
      this.f_767 = var1;
      if (var1) {
         this.m_1071();
      }
   }

   @Generated
   public void m_1044(long var1) {
      this.f_790 = var1;
   }

   private void m_1045(CombatInputEvent var1) {
      float var2 = var1.getForward();
      float var3 = var1.getStrafe();
      if (var2 != 0.0F || var3 != 0.0F) {
         double var4 = this.target.getX() - mc.player.getX();
         double var6 = this.target.getZ() - mc.player.getZ();
         float var8 = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(var6, var4)) - 90.0);
         double var9 = Mth.wrapDegrees(Math.toDegrees(CombatMovement.m_4262(var8, var2, var3)));
         float var11 = var2;
         float var12 = var3;
         double var13 = f_1100;

         for (float var15 = -1.0F; var15 <= 1.0F; var15++) {
            for (float var16 = -1.0F; var16 <= 1.0F; var16++) {
               if (var15 != 0.0F || var16 != 0.0F) {
                  double var17 = Mth.wrapDegrees(Math.toDegrees(CombatMovement.m_4262(mc.player.getYRot(), var15, var16)));
                  double var19 = Math.abs(Mth.wrapDegrees(var9 - var17));
                  if (var19 < var13) {
                     var13 = var19;
                     var11 = var15;
                     var12 = var16;
                  }
               }
            }
         }

         var1.setForward(var11);
         var1.setStrafe(var12);
      }
   }

   @Generated
   public BooleanSetting getGhostThroughWalls() {
      return this.ghostThroughWalls;
   }

   @Generated
   public void m_1049(float var1) {
      this.f_781 = var1;
   }

   @Generated
   public CombatSliderSetting getSpeedMultiplier() {
      return this.speedMultiplier;
   }

   @Generated
   public void m_1051(int var1) {
      this.f_778 = var1;
   }

   private void m_1053() {
      if (this.f_765 != null && this.target != null && !this.f_767) {
         ElytraTarget var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
         this.f_765
            .setTargetPoint(
               var1.predictTarget(this.target, this.target.position().add(0.0, this.target.getBbHeight() / 2.0F + (mc.player.isFallFlying() ? 0.5F : 0.0F), 0.0))
            );
         CombatRotations.plans().setActive(this.f_765);
      }
   }

   @Generated
   public CombatSliderSetting getJellySize() {
      return this.jellySize;
   }

   @Generated
   public float m_1055() {
      return this.f_787;
   }

   private void m_1057() {
      LivingEntity previousTarget = this.target;
      ElytraTarget var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
      float var2 = this.attackRange.getValue();
      if (var1.isEnabled()) {
         var2 = var1.getAttackRange().getValue();
      }

      if (CombatRuntime.moduleEnabled("AirStuck") || CombatRuntime.moduleEnabled("FreeCam")) {
         var2 = 6.0F;
      }

      if (mc.player.isFallFlying() && var1.isEnabled()) {
         this.target = TargetSelection.m_7574(
            this.target,
            var2 + var1.getDetectionRange().getValue(),
            this.targetTypes.get("Игроки").isEnabled(),
            this.targetTypes.get("Голые").isEnabled(),
            this.targetTypes.get("Друзья").isEnabled(),
            this.targetTypes.get("Мобы").isEnabled(),
            this.sortMode
         );
         if (var1.groundRetarget.isEnabled()) {
            this.target = TargetSelection.groundReplacement(previousTarget,this.target,
               TargetSelection.m_7570(var2+var1.getDetectionRange().getValue(),
                  targetTypes.get("Игроки").isEnabled(),targetTypes.get("Голые").isEnabled(),
                  targetTypes.get("Друзья").isEnabled(),targetTypes.get("Мобы").isEnabled()),mc.player,var1.escapeDistance.getValue());
            if (this.target != previousTarget && this.target != null && this.target.onGround()) {
               var motion = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(pizza.dlc.client.modules.impl.movement.ElytraMotion.class);
               if (motion != null) motion.preserveFlightForTargetSwitch();
            }
         }
      } else {
         this.target = TargetSelection.m_7574(
            this.target,
            var2 + this.rotationRange.getValue(),
            this.targetTypes.get("Игроки").isEnabled(),
            this.targetTypes.get("Голые").isEnabled(),
            this.targetTypes.get("Друзья").isEnabled(),
            this.targetTypes.get("Мобы").isEnabled(),
            this.sortMode
         );
      }
   }

   public void m_1058(float var1) {
      if (!this.m_1128()) {
         this.f_789 = false;
         this.f_790 = 0L;
      } else {
         Vec3 var2 = mc.player.getEyePosition(var1);
         Vec3 var3 = this.target.getPosition(var1).add(0.0, this.target.getBbHeight() * 0.5, 0.0);
         Vec3 var4 = var3.subtract(var2);
         double var5 = Math.sqrt(var4.x * var4.x + var4.z * var4.z);
         float var7 = (float)Math.toDegrees(Math.atan2(-var4.x, var4.z));
         float var8 = (float)Math.toDegrees(-Math.atan2(var4.y, Math.max(var5, 1.0E-6)));
         var8 = Mth.clamp(var8, -90.0F, 90.0F);
         long var9 = System.nanoTime();
         CombatRotationState var11 = CombatRotations.state();
         if (!this.f_789) {
            Vector2f var12 = var11.getClientRotation();
            if (var11.getActivePlan() != null) {
               this.f_787 = var12.x;
               this.f_788 = var12.y;
            } else {
               this.f_787 = mc.player.getYRot();
               this.f_788 = mc.player.getXRot();
            }

            this.f_789 = true;
            this.f_790 = var9;
         }

         float var16 = (float)(var9 - this.f_790) * 1.0E-9F;
         this.f_790 = var9;
         var16 = Mth.clamp(var16, 0.0F, 0.05F);
         float var13 = 7.5F;
         float var14 = 1.0F - (float)Math.exp(-7.5F * var16);
         this.f_787 = this.f_787 + Mth.wrapDegrees(var7 - this.f_787) * var14;
         this.f_788 = this.f_788 + (var8 - this.f_788) * var14;
         this.f_788 = Mth.clamp(this.f_788, -90.0F, 90.0F);
         var11.getClientRotation().set(this.f_787, this.f_788);
      }
   }

   @Generated
   public void m_1059(float var1) {
      this.f_782 = var1;
   }

   @Generated
   public ModeSetting getTargetDisplay() {
      return this.targetDisplay;
   }

   public AttackAura() {
      super("Attack Aura", "Автоматически наводится и бьет таргетов", ModuleCategory.COMBAT);
      this.attackRange = new CombatSliderSetting("Дистанция атаки", this, 3.0F, 1.0F, 5.0F).withStep(0.1F);
      this.rotationRange = new CombatSliderSetting("Дистанция ротации", this, 1.5F, 0.0F, 5.0F).withStep(0.1F);
      this.rotationMode = new ModeSetting("Тип ротации", this, "Smooth", "Snap", "Sloth", "FuntimeSnap", "BMW", "Legit", "HvH", "Neuro");
      this.heightFactor = new CombatSliderSetting("Фактор высоты", this, 0.5F, 0.0F, 1.0F).withStep(0.05F).setVisible(() -> false);
      this.generationSpeed = new CombatSliderSetting("Скорость Генерации", this, 1.5F, 1.0F, 6.0F)
         .withStep(0.1F)
         .setVisible(() -> false);
      this.rotationStyle = new ModeSetting("Вид ротации", this, "Обычная", "Дефенсив", "Джиттер")
         .setVisible(() -> this.rotationMode.is("HvH"));
      this.prediction = new CombatSliderSetting("Предсказание", this, 0.15F, 0.0F, 1.0F).withStep(0.01F).setVisible(() -> this.rotationMode.is("HvH") && this.rotationStyle.is("Обычная"));
      this.aimSpeed = new CombatSliderSetting("Скорость аима", this, 3.5F, 0.5F, 20.0F).withStep(0.1F).setVisible(() -> this.rotationMode.is("Legit"));
      this.aimFov = new CombatSliderSetting("FOV аима", this, 40.0F, 5.0F, 120.0F).withStep(1.0F).setVisible(() -> this.rotationMode.is("Legit"));
      this.speedMultiplier = new CombatSliderSetting("Множитель скорости", this, 4.3F, Math.min(4.3F, 5.0F), 60.0F).withStep(0.1F).setVisible(() -> this.rotationMode.is("Legit"));
      this.verticalShare = new CombatSliderSetting("Доля вертикали", this, 0.1F, 0.1F, 1.5F).withStep(0.05F).setVisible(() -> this.rotationMode.is("Legit"));
      this.aimSharpness = new CombatSliderSetting("Резкость доводки", this, 12.0F, 1.0F, 15.0F).withStep(0.5F).setVisible(() -> this.rotationMode.is("Legit"));
      this.smoothing = new CombatSliderSetting("Плавность", this, 0.45F, 0.05F, 0.5F).withStep(0.01F).setVisible(() -> this.rotationMode.is("Legit"));
      this.horizontalJitter = new CombatSliderSetting("Дрожание гориз.", this, 1.0F, 0.0F, 3.0F).withStep(0.1F).setVisible(() -> this.rotationMode.is("Legit"));
      this.verticalJitter = new CombatSliderSetting("Дрожание верт.", this, 3.0F, 0.0F, 3.0F).withStep(0.1F).setVisible(() -> this.rotationMode.is("Legit"));
      this.aimPoint = CombatSettings.mode(new ModeSetting("Точка прицела", this, "Проекция", "Центр тела", "Голова", "Ближайшая точка"), 2)
         .setVisible(() -> this.rotationMode.is("Legit"));
      this.sortMode = new ModeSetting("Тип сортировки", this, "Умная", "Дистанция", "Здоровье", "Броня");
      this.acceleration = new CombatSliderSetting("Ускорение", this, 1.0F, 1.0F, 6.0F).withStep(0.1F).setVisible(() -> this.rotationMode.is("Sloth"));
      this.accelerationTicks = new CombatSliderSetting("Тики", this, 2.0F, 1.0F, 8.0F).withStep(1.0F).setVisible(() -> this.rotationMode.is("Snap"));
      this.attackOptions = CombatSettings.multi("Настройки", this, new CombatSettings.Option("Только криты", true), new CombatSettings.Option("Только в прыжке", true).visible(() -> this.attackOptions.get("Только криты").isEnabled()), new CombatSettings.Option("Проверять кулдаун", true), new CombatSettings.Option("Проверять наводку", false), new CombatSettings.Option("Ломать щит", true), new CombatSettings.Option("Отжимать щит", true), new CombatSettings.Option("Не бить когда ешь", false), new CombatSettings.Option("Обход через стены", false), new CombatSettings.Option("Тпс синх", false));
      this.movementCorrection = new ModeSetting("Коррекция движения", this, "Полноценная", "Сфокусированная", "Незаметная");
      this.targetTypes = CombatSettings.multi("Таргеты", this, new CombatSettings.Option("Игроки", true), new CombatSettings.Option("Голые", true), new CombatSettings.Option("Друзья", false), new CombatSettings.Option("Мобы", true));
      this.clientLook = CombatSettings.bool("Включить ClientLook", this, false);
      this.targetDisplay = new ModeSetting("Отображение цели", this, "Квадрат", "Кольцо", "Кристалики", "Куб", "Молнии", "Обруч", "Пентограмма", "Призраки", "Ромбики", "Треугольник");
      this.jellyThroughWalls = CombatSettings.bool("Сквозь стены", this, true).setVisible(this::m_1077);
      this.jellySize = new CombatSliderSetting("Размер частиц желе", this, 0.2F, 0.08F, 0.45F).withStep(0.01F).setVisible(this::m_1077);
      this.jellyLayers = new CombatSliderSetting("Слои желе", this, 14.0F, 6.0F, 22.0F).withStep(1.0F).setVisible(this::m_1077);
      this.ringType = new ModeSetting("Тип кольца", this, "Тип 1", "Тип 2", "Тип 3")
         .setVisible(() -> this.targetDisplay.is("Кольцо"));
      this.squareType = new ModeSetting("Тип квадрата", this, "Тип 1", "Тип 2").setVisible(() -> this.targetDisplay.is("Квадрат"));
      this.ghostType = new ModeSetting("Тип призраков", this, "Тип 1", "Тип 2", "Тип 3", "Тип 4", "Тип 5", "Тип 6", "Тип 7", "Тип 8")
         .setVisible(() -> this.targetDisplay.is("Призраки"));
      this.crystalType = new ModeSetting("Тип кристалликов", this, "Тип 1", "Тип 2", "Тип 3", "Тип 4")
         .setVisible(() -> this.targetDisplay.is("Кристалики"));
      this.crystalThroughWalls = CombatSettings.bool("Просвечивать кристаллы", this, true).setVisible(() -> this.targetDisplay.is("Кристалики"));
      this.lightningType = new ModeSetting("Тип молний", this, "Тип 1", "Тип 2", "Тип 3")
         .setVisible(() -> this.targetDisplay.is("Молнии"));
      this.lightningThroughWalls = CombatSettings.bool("Просвечивать", this, false).setVisible(() -> this.targetDisplay.is("Молнии"));
      this.surfaceLighting = CombatSettings.bool("Освещение", this, true).setVisible(() -> this.targetDisplay.is("Призраки") || this.targetDisplay.is("Кристалики") || this.targetDisplay.is("Молнии"));
      this.ringSpeed = new CombatSliderSetting("Скорость кольца", this, 1.2F, 0.1F, 5.0F).withStep(0.1F).setVisible(() -> this.targetDisplay.is("Кольцо") && !this.m_1077());
      this.soulCount = new CombatSliderSetting("Кол-во душ", this, 3.0F, 2.0F, 4.0F).withStep(1.0F).setVisible(() -> this.targetDisplay.is("Призраки") && this.ghostType.is("Тип 6"));
      this.hitColor = CombatSettings.bool("Красный при ударе", this, true);
      this.themeColors = CombatSettings.bool("Авто цвет под тему", this, true);
      this.targetColor = new ColorSetting("Цвет отображения", this, -65400).setVisible(() -> !this.themeColors.isEnabled());
      this.gradientColor = new ColorSetting("Цвет градиента кристаллов", this, -11486721)
         .setVisible(
            () -> (
                  this.targetDisplay.is("Ромбики")
                     || this.targetDisplay.is("Кольцо") && this.ringType.is("Тип 2")
                     || this.targetDisplay.is("Призраки")
                        && (this.ghostType.is("Тип 2") || this.ghostType.is("Тип 3") || this.ghostType.is("Тип 4") || this.ghostType.is("Тип 5"))
               )
               && !this.themeColors.isEnabled()
         );
      this.glow = CombatSettings.bool("Свечение", this, false).setVisible(() -> this.targetDisplay.is("Призраки") && this.ghostType.is("Тип 2"));
      this.ghostThroughWalls = CombatSettings.bool("Просвечивать", this, true)
         .setVisible(() -> this.targetDisplay.is("Призраки") && (this.ghostType.is("Тип 2") || this.ghostType.is("Тип 4") || this.ghostType.is("Тип 5")));
      this.distortion = CombatSettings.bool("Искажение", this, true)
         .setVisible(() -> !this.m_1077() && (!this.targetDisplay.is("Призраки") || !this.ghostType.is("Тип 2") && !this.ghostType.is("Тип 4") && !this.ghostType.is("Тип 5")));
      this.distortionStrength = new CombatSliderSetting("Сила искажения", this, 12.0F, 3.0F, 30.0F)
         .withStep(1.0F)
         .setVisible(
            () -> !this.m_1077()
               && (!this.targetDisplay.is("Призраки") || !this.ghostType.is("Тип 2") && !this.ghostType.is("Тип 4") && !this.ghostType.is("Тип 5"))
               && this.distortion.isEnabled()
         );
      this.clientLookState = new AttackAura.ClientLookState();

      this.f_791 = false;

      this.rotationRange.withMax(() -> !this.rotationMode.is("HvH") && !this.rotationMode.is("Smooth") ? 5.0F : 20.0F);
      this.lightningThroughWalls.group("Молнии");
      this.ghostThroughWalls.group("Призраки");
      this.getSettings().clear();
      this.addSettings(
         this.rotationMode,
         this.rotationStyle,
         this.generationSpeed,
         this.heightFactor,
         this.prediction,
         this.aimSpeed,
         this.aimFov,
         this.speedMultiplier,
         this.verticalShare,
         this.aimSharpness,
         this.smoothing,
         this.horizontalJitter,
         this.verticalJitter,
         this.aimPoint,
         this.sortMode,
         this.attackOptions,
         this.attackRange,
         this.rotationRange,
         this.acceleration,
         this.accelerationTicks,
         this.movementCorrection,
         this.targetTypes,
         this.clientLook,
         this.targetDisplay,
         this.squareType,
         this.ringType,
         this.ghostType,
         this.crystalType,
         this.crystalThroughWalls,
         this.lightningType,
         this.lightningThroughWalls,
         this.surfaceLighting,
         this.ringSpeed,
         this.soulCount,
         this.themeColors,
         this.targetColor,
         this.gradientColor,
         this.glow,
         this.ghostThroughWalls,
         this.distortion,
         this.distortionStrength,
         this.hitColor,
         this.jellyThroughWalls,
         this.jellySize,
         this.jellyLayers
      );
   }

   @Generated
   public ModeSetting getCrystalType() {
      return this.crystalType;
   }

   @Generated
   public void m_1064(long var1) {
      this.f_784 = var1;
   }

   public boolean m_1065() {
      return false;
   }

   public boolean m_1066(Entity var1) {
      return this.m_1013() && var1 == this.target;
   }

   @Generated
   public CombatSliderSetting getSoulCount() {
      return this.soulCount;
   }

   @Generated
   public long m_1070() {
      return this.f_777;
   }

   private void m_1071() {
      CombatRotationPlans var1 = CombatRotations.plans();
      if (this.f_765 == this.clientLookState || var1.getActive() == this.clientLookState || var1.hasExpired(this.clientLookState)) {
         this.m_1158();
      } else if (this.f_765 != var1.funtimeSnap() && var1.getActive() != var1.funtimeSnap() && !var1.hasExpired(var1.funtimeSnap())) {
         if (this.f_765 == var1.human() || var1.getActive() == var1.human()) {
            var1.human().reset();
         }

         var1.setActive(null);
      } else {
         this.m_1098();
      }
   }

   @Generated
   public boolean m_1072() {
      return this.f_792;
   }

   @Generated
   public boolean m_1073() {
      return this.f_789;
   }

   @Generated
   public CombatSliderSetting getDistortionStrength() {
      return this.distortionStrength;
   }

   private boolean m_1075() {
      if (!this.rotationMode.is("FuntimeSnap")) {
         return true;
      }

      if (mc.player != null && this.target != null) {
         Vec3 var1 = this.target.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());
         if (var1.x * var1.x + var1.z * var1.z < 1.0E-8) {
            return true;
         }

         float var2 = (float)Math.toDegrees(Math.atan2(-var1.x, var1.z));
         float var3 = CombatRotations.state().getClientRotation().x;
         return Math.abs(Mth.wrapDegrees(var2 - var3)) <= 90.0F;
      } else {
         return false;
      }
   }

   @Generated
   public void m_1076(int var1) {
      this.f_775 = var1;
   }

   private boolean m_1077() {
      return this.targetDisplay.is("Кольцо") && this.ringType.is("Тип 3");
   }

   @Generated
   public void m_1078(float var1) {
      this.f_783 = var1;
   }

   @Generated
   public long m_1082() {
      return this.f_784;
   }

   @Generated
   public ColorSetting getTargetColor() {
      return this.targetColor;
   }

   @Generated
   public ModeSetting getGhostType() {
      return this.ghostType;
   }

   @Generated
   public long m_1085() {
      return this.f_779;
   }

   @Generated
   public BooleanSetting getClientLook() {
      return this.clientLook;
   }

   private void m_1088() {
      if (this.m_1027()) {
         Vec3 var1 = mc.player.getEyePosition();
         Vec3 var2 = this.target.position().add(0.0, this.target.getBbHeight() / 2.0F, 0.0);
         double var3 = var1.distanceTo(var2);
         HashSet<BlockPos> var5 = new HashSet<>();

         for (float var6 = 0.0F; var6 <= 1.0F; var6 += 0.02F) {
            Vec3 var7 = CombatMath.m_1303(var1, var2, var6);
            BlockPos var8 = BlockPos.containing(var7);
            if (!mc.level.getBlockState(var8).isAir()) {
               var5.add(var8);
            }
         }

         BlockPos var11 = null;

         for (BlockPos var14 : var5) {
            if (var11 != null && var11.equals(var14)) {
               var5.remove(var14);
               var11 = var14;
            } else if (var11 == null) {
               var11 = var14;
            }
         }

         for (BlockPos var15 : var5) {
            Vec3 var9 = var1.subtract(var2);
            Direction var10 = Direction.getApproximateNearest(var9);
            CombatPackets.m_1295(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, var15, var10));
            CombatPackets.m_1295(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, var15, var10));
         }
      }
   }

   @Generated
   public CombatSliderSetting getHorizontalJitter() {
      return this.horizontalJitter;
   }

   @Generated
   public float m_1093() {
      return this.f_782;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onMediumFrame(MediumAimEvent var1) {
      if (this.rotationMode.is("Neuro")) { this.m_1057(); return; }
      if (!this.rotationMode.is("FuntimeSnap")) {
         if (this.f_765 != null
            && this.f_765.getUpdateRate() == CombatRotationPlan.UpdateRate.MEDIUM
            && !this.rotationMode.is("BMW")
            && !this.rotationMode.is("Snap")) {
            this.m_1152();
         }
      } else {
         CombatRotationPlans var2 = CombatRotations.plans();
         if (this.f_765 != null
            && (this.f_765.getUpdateRate() == CombatRotationPlan.UpdateRate.FAST || this.f_765 == var2.hvh())
            && this.m_1025(this.attackRange.getValue())) {
            this.m_1053();
         }
      }
   }

   @Generated
   public long m_1095() {
      return this.f_790;
   }

   @Generated
   public CombatSliderSetting getGenerationSpeed() {
      return this.generationSpeed;
   }

   @Generated
   public MultiModeSetting getAttackOptions() {
      return this.attackOptions;
   }

   private void m_1098() {
      CombatRotationPlans var1 = CombatRotations.plans();
      CombatRotationState var2 = CombatRotations.state();
      Vector2f var3 = mc.player == null ? new Vector2f(var2.getRotation()) : new Vector2f(mc.player.getYRot(), mc.player.getXRot());
      var2.getClientRotation().set(var3);
      FuntimeSnapAim var4 = var1.funtimeSnap();
      var1.clear(var4);
      var2.cancelPlan(var4);
      var2.getRotation().set(var3);
      var4.reset();
      if (mc.player != null) {
         mc.player.setYRot(var3.x);
         mc.player.setXRot(var3.y);
      }
   }

   @Generated
   public long m_1099() {
      return this.f_776;
   }

   @Generated
   public void m_1100(long var1) {
      this.f_777 = var1;
   }

   @Generated
   public void m_1101(boolean var1) {
      this.f_791 = var1;
   }

   @Override
   public void load(JsonObject var1) {
      super.load(var1);
      if (var1.has("Settings") && var1.getAsJsonObject("Settings").has(this.targetDisplay.configKey()) && var1.getAsJsonObject("Settings").get(this.targetDisplay.configKey()).isJsonObject() && "Желе".equals(var1.getAsJsonObject("Settings").getAsJsonObject(this.targetDisplay.configKey()).get("currentMode").getAsString())) {
         this.targetDisplay.setCurrentMode("Кольцо");
         this.ringType.setCurrentMode("Тип 3");
      }
   }

   @Generated
   public ModeSetting getRotationStyle() {
      return this.rotationStyle;
   }

   @Generated
   public CombatSliderSetting getAccelerationTicks() {
      return this.accelerationTicks;
   }

   @Generated
   public float m_1104() {
      return this.f_780;
   }

   @Generated
   public BooleanSetting getJellyThroughWalls() {
      return this.jellyThroughWalls;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onPostTick(CombatTickEvent.PostTick var1) {
      if (CombatRuntime.moduleEnabled("Criticals")) {
         this.m_1163();
      }
   }

   @Generated
   public float m_1107() {
      return this.f_783;
   }

   @Generated
   public BooleanSetting getGlow() {
      return this.glow;
   }

   @Generated
   public CombatSliderSetting getAimFov() {
      return this.aimFov;
   }

   @Generated
   public CombatSliderSetting getJellyLayers() {
      return this.jellyLayers;
   }

   public boolean m_1111() {
      return this.target != null
         && this.m_1075()
         && CriticalHitPredictor.m_3380(
            this.target, this.attackOptions.get("Только криты").isEnabled(), this.attackOptions.get("Только в прыжке").isEnabled(), false, true, this.attackRange.getValue(), this.m_1140(), this.attackOptions.get("Проверять кулдаун").isEnabled()
         );
   }

   @Generated
   public void setAttackOptions(MultiModeSetting var1) {
      this.attackOptions = var1;
   }

   private void m_1113() {
      if (this.target != null && mc.gui.screen() == null) {
         Vec3 var1 = this.m_1133(this.target);
         float var2 = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(var1.z, var1.x)) - 90.0);
         float var3 = (float)(-Math.toDegrees(Math.atan2(var1.y, Math.hypot(var1.x, var1.z))));
         float var4 = Mth.wrapDegrees(var2 - mc.player.getYRot());
         float var5 = Mth.wrapDegrees(var3 - mc.player.getXRot());
         if ((float)Math.hypot(var4, var5) > this.aimFov.getValue()) {
            this.f_776 = System.nanoTime();
         } else {
            long var6 = System.nanoTime();
            float var8 = this.f_776 == 0L ? 0.0F : (float)(var6 - this.f_776) / 1.0E9F;
            this.f_776 = var6;
            var8 = Mth.clamp(var8, 0.0F, 0.05F);
            if (!(var8 <= 0.0F)) {
               if (this.horizontalJitter.getValue() > 0.0F) {
                  var4 += CombatMath.m_1302(-this.horizontalJitter.getValue(), this.horizontalJitter.getValue());
               }

               if (this.verticalJitter.getValue() > 0.0F) {
                  var5 += CombatMath.m_1302(-this.verticalJitter.getValue(), this.verticalJitter.getValue());
               }

               float var9 = this.aimSpeed.getValue() * this.speedMultiplier.getValue();
               float var10 = var9 * this.verticalShare.getValue();
               float var11 = this.aimSharpness.getValue();
               float var12 = Mth.clamp(var4 * var11, -var9, var9);
               float var13 = Mth.clamp(var5 * var11, -var10, var10);
               float var14 = this.smoothing.getValue();
               mc.player.turn(var12 * var8 / var14, var13 * var8 / var14);
            }
         }
      } else {
         this.f_776 = System.nanoTime();
      }
   }

   @Generated
   public LivingEntity getTarget() {
      return this.target;
   }

   @Generated
   public ModeSetting getRotationMode() {
      return this.rotationMode;
   }

   @Generated
   public boolean m_1116() {
      return this.f_791;
   }

   private Vec3 m_1118(LivingEntity var1) {
      Vec3 var2 = mc.player.getEyePosition();
      AABB var3 = var1.getBoundingBox();
      double var4 = Mth.clamp(var2.x, var3.minX, var3.maxX);
      double var6 = Mth.clamp(var2.z, var3.minZ, var3.maxZ);
      double var8 = var3.minY + (var3.maxY - var3.minY) * 0.6;
      return new Vec3(var4, var8, var6).subtract(var2);
   }

   @Generated
   public int m_1119() {
      return this.f_775;
   }

   @Generated
   public CombatSliderSetting getAttackRange() {
      return this.attackRange;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   private void onWorldRender(CombatWorldRenderEvent var1) {
      this.m_1009();
      CombatTargetEffects.m_7905(this.distortion.isEnabled(), this.distortionStrength.getValue());
      CombatTargetEffects.m_7919(this.glow.isEnabled(), this.ghostThroughWalls.isEnabled());
      CombatTargetEffects.m_7889(this.jellyThroughWalls.isEnabled(), this.jellySize.getValue(), (int)this.jellyLayers.getValue());
      CombatTargetEffects.m_7921(
         this.ringSpeed.getValue(),
         this.ringType.get(),
         (int)this.soulCount.getValue(),
         this.hitColor.isEnabled(),
         this.ghostType.get(),
         this.crystalType.get(),
         this.crystalThroughWalls.isEnabled(),
         this.lightningType.get(),
         this.lightningThroughWalls.isEnabled(),
         this.surfaceLighting.isEnabled(),
         this.squareType.get()
      );
      int var2 = this.themeColors.isEnabled() ? pizza.dlc.api.theme.ThemeManager.accent() : this.targetColor.getArgb();
      int var3 = this.themeColors.isEnabled() ? pizza.dlc.api.theme.ThemeManager.secondary() : this.gradientColor.getArgb();
      CombatTargetEffects.m_7914(var1, this.target, this.targetDisplay.get(), var2, var3);
   }

   @Generated
   public AttackAura.ClientLookState getClientLookState() {
      return this.clientLookState;
   }

   @Generated
   public CombatSliderSetting getVerticalShare() {
      return this.verticalShare;
   }

   public boolean m_1128() {
      if (this.rotationMode.is("Neuro")) return false;
      if (!this.isEnabled() || !this.clientLook.isEnabled() || this.target == null || mc.player == null) {
         return false;
      } else {
         return this.rotationMode.is("FuntimeSnap") && !this.m_1075() ? false : !CombatRuntime.moduleEnabled("FreeCam");
      }
   }

   private void m_1130(Vec3 var1) {
      if (mc.player != null && this.target != null && var1 != null) {
         double var2 = var1.x;
         double var4 = var1.z;
         double[] var6 = this.m_1011(var2, var4);
         mc.player.setDeltaMovement(var6[0], mc.player.getDeltaMovement().y, var6[1]);
      }
   }

   @Generated
   public void m_1131(float var1) {
      this.f_785 = var1;
   }

   @Generated
   public float m_1132() {
      return this.f_788;
   }

   private Vec3 m_1133(LivingEntity var1) {
      Vec3 var2 = mc.player.getEyePosition();
      AABB var3 = var1.getBoundingBox();
      if (this.aimPoint.is("Центр тела")) {
         return var3.getCenter().subtract(var2);
      } else if (this.aimPoint.is("Голова")) {
         Vec3 var16 = new Vec3((var3.minX + var3.maxX) * 0.5, var3.maxY - var1.getEyeHeight(var1.getPose()) * 0.15, (var3.minZ + var3.maxZ) * 0.5);
         return var16.subtract(var2);
      } else if (this.aimPoint.is("Ближайшая точка")) {
         double var15 = Mth.clamp(var2.x, var3.minX, var3.maxX);
         double var17 = Mth.clamp(var2.y, var3.minY, var3.maxY);
         double var18 = Mth.clamp(var2.z, var3.minZ, var3.maxZ);
         return new Vec3(var15, var17, var18).subtract(var2);
      } else {
         double var4 = Mth.clamp(var2.x, var3.minX, var3.maxX);
         double var6 = Mth.clamp(var2.z, var3.minZ, var3.maxZ);
         Vec3 var8 = mc.player.getViewVector(1.0F);
         double var9 = Math.hypot(var4 - var2.x, var6 - var2.z);
         double var11 = Math.hypot(var8.x, var8.z);
         double var13 = var11 > 1.0E-4 ? var2.y + var8.y * (var9 / var11) : var3.minY + (var3.maxY - var3.minY) * 0.5;
         var13 = Mth.clamp(var13, var3.minY, var3.maxY);
         return new Vec3(var4, var13, var6).subtract(var2);
      }
   }

   @Generated
   public BooleanSetting getThemeColors() {
      return this.themeColors;
   }

   @Generated
   public ModeSetting getLightningType() {
      return this.lightningType;
   }

   @Generated
   public void m_1136(long var1) {
      this.f_776 = var1;
   }

   private void m_1137() {
      if (!this.rotationMode.is("Spooky") && this.f_765 == this.clientLookState) {
         this.m_1158();
         this.f_765 = null;
      }

      CombatRotationPlans var1 = CombatRotations.plans();
      if (!this.rotationMode.is("FuntimeSnap") && (this.f_765 == var1.funtimeSnap() || var1.getActive() == var1.funtimeSnap())) {
         this.m_1098();
         this.f_765 = null;
      }

      if (!this.rotationMode.is("HvH") && (this.f_765 == var1.human() || var1.getActive() == var1.human())) {
         var1.human().reset();
         if (var1.getActive() == var1.human()) {
            var1.setActive(null);
         }

         this.f_765 = null;
      }

      if (this.rotationMode.is("HvH")) {
         this.f_765 = var1.human();
      } else if ((this.rotationMode.is("Smooth") || this.rotationMode.is("Snap")) && !mc.player.isFallFlying()) {
         this.f_765 = CombatRotations.plans().smooth();
      } else if (this.rotationMode.is("FuntimeSnap")) {
         this.f_765 = CombatRotations.plans().funtimeSnap();
      } else if (this.rotationMode.is("Sloth")) {
         this.f_765 = CombatRotations.plans().snap();
      } else if (this.rotationMode.is("Funtime")) {
         this.f_765 = CombatRotations.plans().bmw();
      } else if (this.rotationMode.is("Human")) {
         this.f_765 = CombatRotations.plans().legit();
      } else if ((this.rotationMode.is("Smooth") || this.rotationMode.is("Snap")) && mc.player.isFallFlying()) {
         this.f_765 = CombatRotations.plans().hvh();
      } else if (this.rotationMode.is("Neuro")) {
         this.f_765 = CombatRotations.plans().neuro();
      }
   }

   @Generated
   public ModeSetting getSquareType() {
      return this.squareType;
   }

   @Generated
   public void m_1139(float var1) {
      this.f_780 = var1;
   }

   private boolean m_1140() {
      ElytraTarget var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
      return this.attackOptions.get("Проверять наводку").isEnabled() && (!var1.isEnabled() || mc.player == null || !mc.player.isFallFlying());
   }

   @Generated
   public BooleanSetting getLightningThroughWalls() {
      return this.lightningThroughWalls;
   }

   @Generated
   public void m_1142(float var1) {
      this.f_786 = var1;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onInput(CombatInputEvent var1) {
      if (!this.f_767) {
         CombatRotationPlan var2 = CombatRotations.plans().getActive();
         if (var2 == null || !var2.hasMovementCorrection()) {
            if (this.rotationMode.is("Legit") && this.movementCorrection.is("Сфокусированная") && this.target != null) {
               this.m_1045(var1);
            } else if (this.movementCorrection.is("Незаметная")) {
               CombatMovement.correctInput(var1);
            } else if (this.movementCorrection.is("Полноценная") && this.target != null) {
               Vec3 var3 = mc.player.getEyePosition();
               Vec3 var4 = this.target.position().add(0.0, this.target.getBbHeight() / 2.0F, 0.0);
               Vec3 var5 = var4.subtract(var3).normalize();
               float var6 = (float)Math.toDegrees(Math.atan2(-var5.x, var5.z));
               CombatMovement.m_4265(var1, var6);
            }
         }
      }
   }

   @Generated
   public CombatSliderSetting getAimSharpness() {
      return this.aimSharpness;
   }

   @Generated
   public MultiModeSetting getTargetTypes() {
      return this.targetTypes;
   }

   @Generated
   public float m_1147() {
      return this.f_786;
   }

   @Generated
   public CombatRotationPlan m_1148() {
      return this.f_765;
   }

   public int m_1149(int var1) {
      if (this.target != null && var1 >= 0 && this.m_1075()) {
         ElytraTarget var2 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
         float var3 = mc.player.isFallFlying()
               && var2.isEnabled()
               && this.target.isFallFlying()
               && CombatMath.blocksPerSecond(this.target) > var2.getPredictionBps().getValue()
               && var2.getOvertake().isEnabled()
            ? var2.getAttackRange().getValue()
            : this.attackRange.getValue();
         return CriticalHitPredictor.m_3381(this.target, this.attackOptions.get("Только криты").isEnabled(), this.attackOptions.get("Только в прыжке").isEnabled(), true, var3, this.attackOptions.get("Проверять кулдаун").isEnabled(), var1);
      } else {
         return -1;
      }
   }

   private void m_1152() {
      if (this.f_765 != null && this.target != null && !this.f_767) {
         ElytraTarget var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
         this.f_765
            .setTargetPoint(
               var1.predictTarget(this.target, this.target.position().add(0.0, this.target.getBbHeight() / 2.0F + (mc.player.isFallFlying() ? 0.5F : 0.0F), 0.0))
            );
         CombatRotations.plans().setActive(this.f_765);
      }
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onTick(CombatTickEvent var1) {
      if (this.rotationMode.is("Neuro")) { this.m_1057(); this.f_765=CombatRotations.plans().neuro(); this.m_1163(); return; }
      if (this.rotationMode.is("FuntimeSnap")) {
         this.m_1057();
      }

      if (this.target == null) {
         this.m_1014();
         this.m_1071();
      } else if (this.f_767) {
         this.m_1071();
         if (!CombatRuntime.moduleEnabled("Criticals")) {
            this.m_1163();
         }
      } else {
         this.m_1137();
         if (this.f_765 != null) {
            float var2 = this.attackRange.getValue();
            if (CombatRuntime.moduleEnabled("AirStuck") || CombatRuntime.moduleEnabled("FreeCam")) {
               var2 = 6.0F;
            }

            boolean var3 = !this.rotationMode.is("FuntimeSnap") || this.m_1025(var2);
            if (this.rotationMode.is("Legit")) {
               this.m_1071();
               this.f_765 = null;
            } else if (this.rotationMode.is("Spooky")) {
               this.f_765 = this.clientLookState;
            } else if (this.f_765.getUpdateRate() == CombatRotationPlan.UpdateRate.SLOW && !this.rotationMode.is("BMW") && !this.rotationMode.is("Snap") && var3) {
               this.m_1152();
            }

            if (this.rotationMode.is("Snap") && !mc.player.isFallFlying()) {
               if (CriticalHitPredictor.m_3380(
                  this.target, this.attackOptions.get("Только криты").isEnabled(), this.attackOptions.get("Только в прыжке").isEnabled(), false, true, var2, this.m_1140(), this.attackOptions.get("Проверять кулдаун").isEnabled()
               )) {
                  this.f_775 = (int)this.accelerationTicks.getValue();
               }

               if (this.f_775 > 0) {
                  this.m_1152();
                  this.f_775--;
               } else {
                  this.m_1071();
               }
            }
         }

         if (!CombatRuntime.moduleEnabled("Criticals")) {
            this.m_1163();
         }
      }
   }

   @Generated
   public void m_1155(float var1) {
      this.f_788 = var1;
   }

   private void m_1158() {
      CombatRotationPlans var1 = CombatRotations.plans();
      CombatRotationState var2 = CombatRotations.state();
      Vector2f var3 = mc.player == null ? new Vector2f(var2.getRotation()) : new Vector2f(mc.player.getYRot(), mc.player.getXRot());
      var2.getClientRotation().set(var3);
      var1.clear(this.clientLookState);
      var2.cancelPlan(this.clientLookState);
      var2.getRotation().set(var3);
      if (mc.player != null) {
         mc.player.setYRot(var3.x);
         mc.player.setXRot(var3.y);
      }
   }

   @Generated
   public CombatSliderSetting getAcceleration() {
      return this.acceleration;
   }

   @Generated
   public ModeSetting getAimPoint() {
      return this.aimPoint;
   }

   @Generated
   public void m_1162(CombatRotationPlan var1) {
      this.f_765 = var1;
   }

   private void m_1163() {
      if (pizza.dlc.api.neuro.NeuroSystem.collecting() || this.rotationMode.is("Neuro") && (pizza.dlc.api.neuro.NeuroSystem.get()==null || !pizza.dlc.api.neuro.NeuroSystem.get().canAutoAttack(this.target))) { this.f_791=false; return; }
      if (!CombatRuntime.moduleEnabled("WindHop")) {
         if (!this.m_1075()) {
            this.f_791 = false;
         } else {
            ElytraTarget var1 = pizza.dlc.client.PizzaDLCClient.getInstance().getModuleManager().get(ElytraTarget.class);
            float var2 = mc.player.isFallFlying()
                  && var1.isEnabled()
                  && this.target.isFallFlying()
                  && CombatMath.blocksPerSecond(this.target) > var1.getPredictionBps().getValue()
                  && var1.getOvertake().isEnabled()
               ? var1.getAttackRange().getValue()
               : this.attackRange.getValue();
            boolean var3 = mc.player.getMainHandItem().is(Items.MACE);
            if (!mc.player.isUsingItem() || mc.player.isBlocking() || !this.attackOptions.get("Не бить когда ешь").isEnabled()) {
               this.f_791 = CriticalHitPredictor.m_3380(
                  this.target, this.attackOptions.get("Только криты").isEnabled(), this.attackOptions.get("Только в прыжке").isEnabled(), true, true, var2, this.m_1140(), this.attackOptions.get("Проверять кулдаун").isEnabled()
               );
               if (this.f_791) {
                  this.m_1088();
                  if (this.attackOptions.get("Отжимать щит").isEnabled() && mc.player.isBlocking()) {
                     mc.player.releaseUsingItem();
                  }

                  Vec3 var4 = mc.player.getDeltaMovement();
                  boolean neuroMiss = this.rotationMode.is("Neuro") && pizza.dlc.api.neuro.NeuroSystem.get()!=null && pizza.dlc.api.neuro.NeuroSystem.get().recordAutoAttack(this.target);
                  if (!neuroMiss) {
                     mc.gameMode.attack(mc.player, this.target);
                     pizza.dlc.client.modules.impl.combat.CombatSync.shareTarget(this.target);
                     pizza.dlc.client.modules.impl.visuals.TargetEsp.notifyHit();
                  }
                  mc.player.swing(InteractionHand.MAIN_HAND);
                  if ((this.rotationMode.is("HvH") || this.rotationMode.is("Smooth"))
                     && CombatRuntime.moduleEnabled("Strafe")
                     && !mc.player.isFallFlying()) {
                     this.m_1130(var4);
                  }

                  if (this.attackOptions.get("Ломать щит").isEnabled()) {
                     this.m_1168();
                  }

                  if (this.rotationMode.is("FuntimeSnap")) {
                     CombatRotations.plans().funtimeSnap().m_2033();
                  }
               }
            }
         }
      }
   }

   @Generated
   public CombatSliderSetting getRingSpeed() {
      return this.ringSpeed;
   }

   @Generated
   public ModeSetting getRingType() {
      return this.ringType;
   }

   private void m_1168() {
      int axe = CombatInventory.m_7079();
      LivingEntity victim=this.target;
      if (victim==null || victim.getUseItem().getItem()!=Items.SHIELD || axe<0) return;
      var expected=mc.player.getInventory().getItem(axe).copy();
      pizza.dlc.api.util.game.InventorySwapQueue.submit(this,"shield-break",
         pizza.dlc.api.util.game.SwapSystem.effectiveMode(pizza.dlc.api.util.game.InventorySwapQueue.configuredMode()),0,null,() -> {
            if (this.target!=victim || !victim.isAlive() || victim.getUseItem().getItem()!=Items.SHIELD
               || !net.minecraft.world.item.ItemStack.matches(mc.player.getInventory().getItem(axe),expected)
               || mc.player.distanceTo(victim)>this.attackRange.getValue()) return;
            int selected=mc.player.getInventory().getSelectedSlot();
            if (axe<9) mc.player.connection.send(new ServerboundSetCarriedItemPacket(axe));
            else mc.gameMode.handleContainerInput(0,axe,selected,ContainerInput.SWAP,mc.player);
            try { mc.gameMode.attack(mc.player,victim);mc.player.swing(InteractionHand.MAIN_HAND); }
            finally {
               if (axe<9) mc.player.connection.send(new ServerboundSetCarriedItemPacket(selected));
               else mc.gameMode.handleContainerInput(0,axe,selected,ContainerInput.SWAP,mc.player);
            }
         });
   }

   @Generated
   public BooleanSetting getSurfaceLighting() {
      return this.surfaceLighting;
   }

   @Generated
   public int m_1173() {
      return this.f_778;
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onFastFrame(FastAimEvent var1) {
      if (this.rotationMode.is("Neuro")) { this.m_1057(); return; }
      if (!this.rotationMode.is("FuntimeSnap")) {
         this.m_1057();
         if (this.f_765 != null
            && this.f_765.getUpdateRate() == CombatRotationPlan.UpdateRate.FAST
            && !this.rotationMode.is("BMW")
            && !this.rotationMode.is("Snap")) {
            this.m_1152();
         }
      }
   }

   @meteordevelopment.orbit.EventHandler(priority = 0)
   public void onRotation(CombatRotationEvent var1) {
      if (this.rotationMode.is("Neuro")) return;
      if (this.m_1027() && !this.f_791) {
         Vector2f var10000 = var1.getRotation();
         var10000.x = var10000.x + 20.0F;
      }
   }

   private static final class ClientLookState extends CombatRotationPlan {
      public final Vector2f f_718 = new Vector2f();

      public ClientLookState() {
         super(Vec3.ZERO);
      }

      public void m_1005(float var1, float var2) {
         this.f_718.set(var1, var2);
      }

      @Override
      public Vector2f rotate(Vector2f var1, Vector2f var2) {
         return new Vector2f(var2);
      }

      @Override
      public Vector2f targetAngles() {
         return new Vector2f(this.f_718);
      }
   }

   @Override public void onEnable() {
      super.onEnable();
      CombatRuntime.enabled(this);
      pizza.dlc.api.neuro.NeuroSystem.auraEnabled(this);
   }
   @Override public void onDisable() {
      try { resetState(); } finally { CombatRuntime.disabled(this); super.onDisable(); pizza.dlc.api.neuro.NeuroSystem.auraDisabled(); }
   }
}
