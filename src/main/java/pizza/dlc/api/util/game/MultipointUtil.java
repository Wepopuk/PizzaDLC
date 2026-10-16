package pizza.dlc.api.util.game;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MultipointUtil {
   private static final double DEFAULT_STEP = 0.1D;

   private MultipointUtil() {
   }

   public static Vec3 getNearestPoint(Entity entity, double expand) {
      return getNearestPoint(entity, expand, DEFAULT_STEP);
   }

   public static Vec3 getNearestPoint(Entity entity, double expand, double step) {
      LocalPlayer player = Minecraft.getInstance().player;
      AABB box = entity.getBoundingBox();
      if (player == null) {
         return box.getCenter();
      }
      Vec3 eye = player.getEyePosition();
      double x = nearestNode(box.minX + expand, box.maxX - expand, step, eye.x);
      double y = nearestNode(box.minY + expand, box.maxY - expand, step, eye.y);
      double z = nearestNode(box.minZ + expand, box.maxZ - expand, step, eye.z);
      if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
         return box.getCenter();
      }
      return new Vec3(x, y, z);
   }

   private static double nearestNode(double min, double max, double step, double target) {
      if (max < min || step <= 0.0D) {
         return Double.NaN;
      }
      long lastIndex = (long) Math.floor((max - min) / step + 1.0E-9D);
      long index = Math.round((target - min) / step - 1.0E-9D);
      index = Math.max(0L, Math.min(lastIndex, index));
      return min + index * step;
   }
}
