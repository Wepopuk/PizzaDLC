package pizza.dlc.api.rotation.ml;

public final class AimFeatures {
   public static final int DIM = 12;
   private static final double DIST_SCALE = 6.0;
   private static final double TICKS_CAP = 40.0;
   private static final double ATTACK_READY_THRESHOLD = 0.9;
   private boolean hasPrev;
   private double prevErrYawDeg;
   private double prevErrPitchDeg;

   public void reset() {
      this.hasPrev = false;
   }

   public void build(
      float[] out,
      double errYawDeg,
      double errPitchDeg,
      double ownYawCounts,
      double ownPitchCounts,
      double dist,
      int tickOnTarget,
      double ownSpeed,
      boolean onGround,
      boolean sprint,
      double cooldown,
      double gcd
   ) {
      double errYaw = errYawDeg / gcd;
      double errPitch = errPitchDeg / gcd;
      double dErrYaw = this.hasPrev ? wrapDeg(errYawDeg - this.prevErrYawDeg) / gcd : 0.0;
      double dErrPitch = this.hasPrev ? (errPitchDeg - this.prevErrPitchDeg) / gcd : 0.0;
      double tAngVelYaw = this.hasPrev ? wrapDeg((dErrYaw + ownYawCounts) * gcd) / gcd : 0.0;
      double tAngVelPitch = this.hasPrev ? dErrPitch + ownPitchCounts : 0.0;
      out[0] = (float)errYaw;
      out[1] = (float)errPitch;
      out[2] = (float)dErrYaw;
      out[3] = (float)dErrPitch;
      out[4] = (float)(dist / DIST_SCALE);
      out[5] = (float)tAngVelYaw;
      out[6] = (float)tAngVelPitch;
      out[7] = (float)(Math.min((double)tickOnTarget, TICKS_CAP) / TICKS_CAP);
      out[8] = (float)ownSpeed;
      out[9] = onGround ? 1.0F : 0.0F;
      out[10] = sprint ? 1.0F : 0.0F;
      out[11] = cooldown > ATTACK_READY_THRESHOLD ? 1.0F : 0.0F;
      this.prevErrYawDeg = errYawDeg;
      this.prevErrPitchDeg = errPitchDeg;
      this.hasPrev = true;
   }

   static double wrapDeg(double d) {
      return ((d + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
   }
}
