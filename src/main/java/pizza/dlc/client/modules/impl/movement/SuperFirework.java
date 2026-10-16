package pizza.dlc.client.modules.impl.movement;

import net.minecraft.util.Mth;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "SuperFirework",
   description = "Множитель ускорения фейерверка на элитре (по углам)",
   category = ModuleCategory.MOVEMENT
)
public class SuperFirework extends Module {
   private final SliderSetting[] yaw = new SliderSetting[9];
   private final SliderSetting[] pitch = new SliderSetting[9];

   public SuperFirework() {
      for (int i = 0; i < 9; i++) {
         int lo = i * 5;
         this.yaw[i] = new SliderSetting("Yaw " + lo + "–" + (lo + 5) + "°", this, 1.5F, 0.0F, 3.0F, 0.01F);
      }

      for (int i = 0; i < 9; i++) {
         int lo = i * 5;
         this.pitch[i] = new SliderSetting("Pitch " + lo + "–" + (lo + 5) + "°", this, 1.5F, 0.0F, 3.0F, 0.01F);
      }
   }

   public float getBoostPower(float playerYaw, float playerPitch) {
      return (this.getYawBoost(playerYaw) + this.getPitchBoost(playerPitch)) * 0.5F;
   }

   private float getYawBoost(float yawDegrees) {
      double yaw = Math.abs(Mth.wrapDegrees(yawDegrees)) % 90.0;
      double deltaToDiagonal = Math.abs(yaw - 45.0);
      return getBucket(deltaToDiagonal, this.yaw);
   }

   private float getPitchBoost(float pitchDegrees) {
      double absPitch = Math.min(45.0, Math.abs((double)pitchDegrees));
      return getBucket(absPitch, this.pitch);
   }

   private static float getBucket(double angle, SliderSetting[] buckets) {
      int index = Mth.clamp((int)Math.floor(angle / 5.0), 0, 8);
      return buckets[index].getValue();
   }
}
