package pizza.dlc.client.modules.impl.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import pizza.dlc.api.rotation.RotationController;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.ModeSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "ViewModel",
   description = "Кастом-анимации замаха + смещение/масштаб руки",
   category = ModuleCategory.VISUALS
)
public class ViewModel extends Module {
   public static ViewModel INSTANCE;
   private static final String M_DEFAULT = "Дефолт";
   private static final String M_SWIPE = "Свайп";
   private static final String M_SPIN = "Спин";
   private static final String M_PUSH = "Пуш";
   private static final String M_STAB = "Стэб";
   private static final String M_DOWN = "Даун";
   private static final String M_WHIP = "Хлыст";
   private static final String M_EXHIBITION = "Эксибишн";
   public final ModeSetting animationMode = new ModeSetting("Анимация", this, M_DEFAULT, M_SWIPE, M_SPIN, M_PUSH, M_STAB, M_DOWN, M_WHIP, M_EXHIBITION);
   public final SliderSetting swingPower = new SliderSetting("Длит. свинга", this, 5.0F, 1.0F, 10.0F, 0.05F);
   public final SliderSetting tiltStrength = new SliderSetting("Сила наклона", this, 1.0F, 0.0F, 2.0F, 0.05F);
   public final BooleanSetting onlyAura = new BooleanSetting("Только с аурой", this);
   public final BooleanSetting viewModelEnabled = new BooleanSetting("Вьюмодель", this);
   public final SliderSetting rightX = new SliderSetting("Правая X", this, 0.0F, -1.0F, 1.0F, 0.1F);
   public final SliderSetting rightY = new SliderSetting("Правая Y", this, 0.0F, -1.0F, 1.0F, 0.1F);
   public final SliderSetting rightZ = new SliderSetting("Правая Z", this, 0.0F, -1.0F, 1.0F, 0.1F);
   public final SliderSetting rightScale = new SliderSetting("Правая масштаб", this, 1.0F, 0.5F, 1.5F, 0.05F);
   public final SliderSetting leftX = new SliderSetting("Левая X", this, 0.0F, -1.0F, 1.0F, 0.1F);
   public final SliderSetting leftY = new SliderSetting("Левая Y", this, 0.0F, -1.0F, 1.0F, 0.1F);
   public final SliderSetting leftZ = new SliderSetting("Левая Z", this, 0.0F, -1.0F, 1.0F, 0.1F);
   public final SliderSetting leftScale = new SliderSetting("Левая масштаб", this, 1.0F, 0.5F, 1.5F, 0.05F);

   public ViewModel() {
      INSTANCE = this;

      for (SliderSetting s : new SliderSetting[]{this.rightX, this.rightY, this.rightZ, this.rightScale, this.leftX, this.leftY, this.leftZ, this.leftScale}) {
         s.setVisible(() -> this.viewModelEnabled.isEnabled());
      }
   }

   public boolean swingActive() {
      if (!this.isEnabled() || this.animationMode.is(M_DEFAULT)) {
         return false;
      }

      if (!this.onlyAura.isEnabled()) {
         return true;
      }

      RotationController rc = RotationController.INSTANCE;
      return rc != null && rc.isActive();
   }

   public int getSwingDuration() {
      return Math.max(1, (int)this.swingPower.getValue());
   }

   private float ts(float degrees) {
      return degrees * this.tiltStrength.getValue();
   }

   public void applyHandScale(PoseStack pose, HumanoidArm arm) {
      if (!this.viewModelEnabled.isEnabled()) return;

      float s = arm == HumanoidArm.RIGHT ? this.rightScale.getValue() : this.leftScale.getValue();
      pose.scale(s, s, s);
   }

   public void applyHandPosition(PoseStack pose, HumanoidArm arm) {
      if (!this.viewModelEnabled.isEnabled()) return;

      if (arm == HumanoidArm.RIGHT) {
         pose.translate(this.rightX.getValue(), this.rightY.getValue(), this.rightZ.getValue());
      } else {
         pose.translate(-this.leftX.getValue(), this.leftY.getValue(), this.leftZ.getValue());
      }
   }

   public void renderSwordAnimation(PoseStack pose, float swingProgress, float equipProgress, HumanoidArm arm) {
      if (this.animationMode.is(M_DEFAULT)) {
         pose.translate(0.56F, -0.52F, -0.72F);
         float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
         pose.mulPose(Axis.XP.rotationDegrees(this.ts(g * -60.0F)));
         pose.mulPose(Axis.YP.rotationDegrees(this.ts(g * -30.0F)));
      } else if (this.animationMode.is(M_SWIPE)) {
         if (swingProgress > 0.0F) {
            float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
            pose.translate(0.56F, equipProgress * -0.2F - 0.5F, -0.7F);
            pose.mulPose(Axis.YP.rotationDegrees(45.0F));
            pose.mulPose(Axis.XP.rotationDegrees(this.ts(g * -85.0F)));
            pose.translate(-0.1F, 0.28F, 0.2F);
            pose.mulPose(Axis.XP.rotationDegrees(-85.0F));
         } else {
            float n = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
            float m = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
            float f1 = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
            pose.translate(n, m, f1);
            this.applyEquipOffset(pose, arm, equipProgress);
            this.applySwingOffset(pose, arm, swingProgress);
         }
      } else if (this.animationMode.is(M_SPIN)) {
         float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
         this.applyEquipOffset(pose, arm, 0.0F);
         pose.mulPose(Axis.XP.rotationDegrees(50.0F));
         pose.mulPose(Axis.YP.rotationDegrees(-60.0F));
         pose.mulPose(Axis.ZP.rotationDegrees(110.0F + this.ts(20.0F * g)));
      } else if (this.animationMode.is(M_PUSH)) {
         float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
         this.applyEquipOffset(pose, arm, 0.0F);
         pose.mulPose(Axis.XP.rotationDegrees(50.0F));
         pose.mulPose(Axis.YP.rotationDegrees(-60.0F + this.ts(30.0F * g)));
         pose.mulPose(Axis.ZP.rotationDegrees(110.0F));
      } else if (this.animationMode.is(M_STAB)) {
         float g = Mth.sin(swingProgress * (float) Math.PI);
         this.applyEquipOffset(pose, arm, 0.0F);
         pose.translate(0.1F, -0.2F, -0.3F);
         pose.mulPose(Axis.XP.rotationDegrees(-36.0F + this.ts(-30.0F * g)));
         pose.mulPose(Axis.YP.rotationDegrees(this.ts(25.0F * g)));
         pose.mulPose(Axis.ZP.rotationDegrees(12.0F));
      } else if (this.animationMode.is(M_DOWN)) {
         float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
         this.applyEquipOffset(pose, arm, 0.0F);
         pose.translate(0.0F, -0.2F, -0.4F);
         pose.mulPose(Axis.XP.rotationDegrees(-3.0F + this.ts(-120.0F * g)));
      } else if (this.animationMode.is(M_WHIP)) {
         float p3 = swingProgress * swingProgress * swingProgress;
         float g = Mth.sin(p3 * (float) Math.PI);
         float h = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
         this.applyEquipOffset(pose, arm, 0.0F);
         pose.translate(0.0F, -0.15F * h, -0.5F * g);
         pose.mulPose(Axis.XP.rotationDegrees(this.ts(-70.0F * g)));
         pose.mulPose(Axis.ZP.rotationDegrees(this.ts(-12.0F * h)));
      } else if (this.animationMode.is(M_EXHIBITION)) {
         int i = arm == HumanoidArm.RIGHT ? 1 : -1;
         float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
         float h = Mth.sin(swingProgress * (float) Math.PI);
         this.applyEquipOffset(pose, arm, 0.0F);
         pose.translate(0.0F, 0.2F * (1.0F - g) - 0.15F, -0.15F);
         pose.mulPose(Axis.XP.rotationDegrees(this.ts(-90.0F * g)));
         pose.mulPose(Axis.YP.rotationDegrees(this.ts(i * 20.0F * h)));
         pose.mulPose(Axis.ZP.rotationDegrees(this.ts(i * 15.0F * h)));
      }
   }

   private void applyEquipOffset(PoseStack pose, HumanoidArm arm, float equipProgress) {
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      pose.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(PoseStack pose, HumanoidArm arm, float swingProgress) {
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
      pose.mulPose(Axis.YP.rotationDegrees(i * (45.0F + this.ts(f * -20.0F))));
      float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      pose.mulPose(Axis.ZP.rotationDegrees(this.ts(i * g * -20.0F)));
      pose.mulPose(Axis.XP.rotationDegrees(this.ts(g * -80.0F)));
      pose.mulPose(Axis.YP.rotationDegrees(i * -45.0F));
   }
}
