package pizza.dlc.api.rotation.ml;

public record TraceSample(
   long session,
   int segment,
   int tick,
   String mode,
   double sens,
   double gcd,
   float yaw,
   float pitch,
   float srvYaw,
   float srvPitch,
   double cx,
   double cy,
   float errYaw,
   float errPitch,
   double dist,
   double tdx,
   double tdy,
   double tdz,
   double tvx,
   double tvy,
   double tvz,
   boolean onGround,
   boolean sprint,
   double speed,
   boolean swing,
   float cooldown,
   boolean hurt,
   boolean gate,
   int tid
) {
   public String toJson() {
      StringBuilder sb = new StringBuilder(320);
      sb.append('{');
      lng(sb, "s", this.session);
      sb.append(',');
      lng(sb, "e", this.segment);
      sb.append(',');
      lng(sb, "t", this.tick);
      sb.append(',');
      sb.append("\"mode\":\"").append(this.mode).append("\",");
      num(sb, "sens", this.sens, 4);
      sb.append(',');
      num(sb, "gcd", this.gcd, 6);
      sb.append(',');
      num(sb, "yaw", this.yaw, 3);
      sb.append(',');
      num(sb, "pitch", this.pitch, 3);
      sb.append(',');
      num(sb, "srvYaw", this.srvYaw, 3);
      sb.append(',');
      num(sb, "srvPitch", this.srvPitch, 3);
      sb.append(',');
      num(sb, "cx", this.cx, 4);
      sb.append(',');
      num(sb, "cy", this.cy, 4);
      sb.append(',');
      num(sb, "errYaw", this.errYaw, 3);
      sb.append(',');
      num(sb, "errPitch", this.errPitch, 3);
      sb.append(',');
      num(sb, "dist", this.dist, 3);
      sb.append(',');
      num(sb, "tdx", this.tdx, 3);
      sb.append(',');
      num(sb, "tdy", this.tdy, 3);
      sb.append(',');
      num(sb, "tdz", this.tdz, 3);
      sb.append(',');
      num(sb, "tvx", this.tvx, 4);
      sb.append(',');
      num(sb, "tvy", this.tvy, 4);
      sb.append(',');
      num(sb, "tvz", this.tvz, 4);
      sb.append(',');
      bool(sb, "onGround", this.onGround);
      sb.append(',');
      bool(sb, "sprint", this.sprint);
      sb.append(',');
      num(sb, "speed", this.speed, 4);
      sb.append(',');
      bool(sb, "swing", this.swing);
      sb.append(',');
      num(sb, "cooldown", this.cooldown, 3);
      sb.append(',');
      bool(sb, "hurt", this.hurt);
      sb.append(',');
      bool(sb, "gate", this.gate);
      sb.append(',');
      lng(sb, "tid", this.tid);
      sb.append('}');
      return sb.toString();
   }

   private static void num(StringBuilder sb, String key, double v, int digits) {
      sb.append('"').append(key).append("\":");
      double f = Math.pow(10.0, digits);
      double r = Math.round(v * f) / f;
      if (r == Math.rint(r) && Math.abs(r) < 1.0E15) {
         sb.append((long)r);
      } else {
         sb.append(r);
      }
   }

   private static void lng(StringBuilder sb, String key, long v) {
      sb.append('"').append(key).append("\":").append(v);
   }

   private static void bool(StringBuilder sb, String key, boolean v) {
      sb.append('"').append(key).append("\":").append(v);
   }
}
