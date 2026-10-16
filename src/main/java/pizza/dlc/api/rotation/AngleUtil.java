package pizza.dlc.api.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.interfaces.QuickImports;

public final class AngleUtil implements QuickImports {
   public static Angle fromVec2(Vec2 vector2f) {
      return new Angle(vector2f.y, vector2f.x);
   }

   public static Angle fromVec3d(Vec3 vector) {
      return new Angle(
         Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0)),
         Mth.wrapDegrees((float)Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   public static Angle calculateDelta(Angle start, Angle end) {
      float deltaYaw = Mth.wrapDegrees(end.getYaw() - start.getYaw());
      float deltaPitch = Mth.wrapDegrees(end.getPitch() - start.getPitch());
      return new Angle(deltaYaw, deltaPitch);
   }

   public static Angle calculateAngle(Vec3 to) {
      return fromVec3d(to.subtract(mc.player.getEyePosition()));
   }

   public static Angle pitch(float pitch) {
      return new Angle(mc.player.getYRot(), pitch);
   }

   public static Angle cameraAngle() {
      return new Angle(mc.player.getYRot(), mc.player.getXRot());
   }

   private AngleUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
