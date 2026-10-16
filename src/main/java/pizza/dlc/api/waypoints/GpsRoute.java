package pizza.dlc.api.waypoints;

/** Straight-line guidance. Minecraft yaw 0 faces south, 90 faces west. */
public record GpsRoute(double x, Double y, double z, String name) {
   public GpsRoute {
      if (!Double.isFinite(x) || !Double.isFinite(z) || y != null && !Double.isFinite(y)) {
         throw new IllegalArgumentException("Non-finite GPS coordinates");
      }
   }

   public double horizontalDistance(double playerX, double playerZ) {
      return Math.hypot(x - playerX, z - playerZ);
   }

   public double distance(double playerX, double playerY, double playerZ) {
      double horizontal = horizontalDistance(playerX, playerZ);
      return y == null ? horizontal : Math.hypot(horizontal, y - playerY);
   }

   public boolean arrived(double playerX, double playerY, double playerZ) {
      return horizontalDistance(playerX, playerZ) <= 2.5 && (y == null || Math.abs(y - playerY) <= 3.0);
   }

   public double bearing(double playerX, double playerZ, double cameraYaw) {
      return wrapDegrees(Math.toDegrees(Math.atan2(-(x - playerX), z - playerZ)) - cameraYaw);
   }

   public static double wrapDegrees(double angle) {
      double wrapped = angle % 360;
      return wrapped >= 180 ? wrapped - 360 : wrapped < -180 ? wrapped + 360 : wrapped;
   }

   public static String voiceCue(double bearing) {
      if (Math.abs(bearing) >= 135) return "uturn";
      if (bearing > 30) return "left";
      if (bearing < -30) return "right";
      return "straight";
   }
}
