package pizza.dlc.client.modules.impl.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.events.impl.input.EventTravel;
import pizza.dlc.api.settings.impl.SliderSetting;
import pizza.dlc.client.modules.Module;
import pizza.dlc.client.modules.api.ModuleCategory;
import pizza.dlc.client.modules.api.ModuleInfo;

@ModuleInfo(
   name = "Fly",
   category = ModuleCategory.MOVEMENT,
   description = "Полёт"
)
public class Fly extends Module {
   private final SliderSetting speed = new SliderSetting("Скорость", this, 1.0F, 0.1F, 5.0F, 0.05F);

   @EventHandler
   public void onTravel(EventTravel e) {
      if (mc.player == null || e.getEntity() != mc.player) return;

      float s = this.speed.getValue();
      float yawRad = mc.player.getYRot() * (float) (Math.PI / 180.0);
      float sin = Mth.sin(yawRad);
      float cos = Mth.cos(yawRad);
      double ix = mc.player.xxa;
      double iz = mc.player.zza;
      double len = Math.sqrt(ix * ix + iz * iz);
      double vx = 0.0;
      double vz = 0.0;
      if (len > 1.0E-4) {
         ix = ix / len * s;
         iz = iz / len * s;
         vx = ix * cos - iz * sin;
         vz = iz * cos + ix * sin;
      }

      double vy = 0.0;
      Input keys = mc.player.input.keyPresses;
      if (keys.jump()) {
         vy += s;
      }

      if (keys.shift()) {
         vy -= s;
      }

      Vec3 vel = new Vec3(vx, vy, vz);
      mc.player.setDeltaMovement(vel);
      mc.player.move(MoverType.SELF, vel);
      mc.player.fallDistance = 0.0;
      e.setCancelled(true);
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setDeltaMovement(Vec3.ZERO);
      }

      super.onDisable();
   }
}
