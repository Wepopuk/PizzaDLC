package pizza.dlc.client.modules.impl.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.input.EventTravel;
import pizza.dlc.api.settings.impl.BooleanSetting;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Assist",
   category = ModuleCategory.OTHER,
   description = "Помощник под сервер (RW: Dragon Fly)"
)
public class Assist extends Module {
   private final BooleanSetting dragonFly = new BooleanSetting("Dragon Fly", this);
   private final SliderSetting flySpeed = new SliderSetting("Скорость", this, 1.2F, 0.5F, 10.0F, 0.1F);
   private final SliderSetting verticalSpeed = new SliderSetting("Верт. скорость", this, 1.2F, 0.5F, 10.0F, 0.1F);

   public Assist() {
      this.dragonFly.setEnabled(true);
   }

   @EventHandler
   public void onTravel(EventTravel e) {
      if (mc.player == null || mc.level == null || e.getEntity() != mc.player) return;

      if (this.dragonFly.isEnabled()) {
         if (mc.player.getAbilities().flying) {
            float fly = this.flySpeed.getValue();
            float reducedFly = fly * 0.8334F;
            float vert = this.verticalSpeed.getValue();
            float reducedVert = vert * 0.55F;
            boolean movingH = Math.abs(mc.player.zza) > 0.001F || Math.abs(mc.player.xxa) > 0.001F;
            double horizontalSpeed = fly;
            Input keys = mc.player.input.keyPresses;
            double motionY;
            if (keys.jump() && !keys.shift()) {
               motionY = movingH ? reducedVert : vert;
               if (movingH) {
                  horizontalSpeed = reducedFly;
               }
            } else if (keys.shift()) {
               motionY = -(movingH ? reducedVert : vert);
               if (movingH) {
                  horizontalSpeed = reducedFly;
               }
            } else {
               motionY = 0.0;
            }

            Vec3 h = this.horizontalMotion(horizontalSpeed);
            Vec3 motion = new Vec3(h.x, motionY, h.z);
            mc.player.setDeltaMovement(motion);
            mc.player.move(MoverType.SELF, motion);
            mc.player.fallDistance = 0.0;
            e.setCancelled(true);
         }
      }
   }

   private Vec3 horizontalMotion(double speed) {
      double forward = mc.player.zza;
      double strafe = mc.player.xxa;
      if (forward == 0.0 && strafe == 0.0) {
         return Vec3.ZERO;
      }

      float yaw = mc.player.getYRot();
      if (forward != 0.0) {
         if (strafe > 0.0) {
            yaw += forward > 0.0 ? -45.0F : 45.0F;
         } else if (strafe < 0.0) {
            yaw += forward > 0.0 ? 45.0F : -45.0F;
         }

         strafe = 0.0;
         forward = forward > 0.0 ? 1.0 : -1.0;
      }

      double radians = Math.toRadians(yaw + 90.0F);
      double sin = Mth.sin((float)radians);
      double cos = Mth.cos((float)radians);
      double motionX = forward * speed * cos + strafe * speed * sin;
      double motionZ = forward * speed * sin - strafe * speed * cos;
      return new Vec3(motionX, 0.0, motionZ);
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setDeltaMovement(Vec3.ZERO);
      }

      super.onDisable();
   }
}
