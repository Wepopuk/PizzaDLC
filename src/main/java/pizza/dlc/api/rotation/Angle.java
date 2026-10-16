package pizza.dlc.api.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.interfaces.QuickImports;
import pizza.dlc.api.util.math.MathUtility;

public class Angle implements QuickImports {
   public static Angle DEFAULT = new Angle(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public Angle adjustSensitivity() {
      double gcd = computeGcd();
      Angle previousAngle = RotationController.INSTANCE.getServerAngle();
      float adjustedYaw = this.adjustAxis(this.yaw, previousAngle.yaw, gcd);
      float adjustedPitch = this.adjustAxis(this.pitch, previousAngle.pitch, gcd);
      return new Angle(adjustedYaw, Mth.clamp(adjustedPitch, -90.0F, 90.0F));
   }

   public Angle random(float f) {
      return new Angle(this.yaw + MathUtility.random(-f, f), this.pitch + MathUtility.random(-f, f));
   }

   private float adjustAxis(float axisValue, float previousValue, double gcd) {
      float delta = axisValue - previousValue;
      return previousValue + (float)Math.round(delta / gcd) * (float)gcd;
   }

   public static double computeGcd() {
      return Math.pow((Double)mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
   }

   public final Vec3 toVector() {
      float f = this.pitch * (float) (Math.PI / 180.0);
      float g = -this.yaw * (float) (Math.PI / 180.0);
      float h = Mth.cos(g);
      float i = Mth.sin(g);
      float j = Mth.cos(f);
      float k = Mth.sin(f);
      return new Vec3(i * j, -k, h * j);
   }

   public Angle addYaw(float yaw) {
      return new Angle(this.yaw + yaw, this.pitch);
   }

   public Angle addPitch(float pitch) {
      this.pitch = Mth.clamp(this.pitch + pitch, -90.0F, 90.0F);
      return this;
   }

   public Angle copy() {
      return new Angle(this.yaw, this.pitch);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Override
   public String toString() {
      return "Angle(yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ")";
   }

   public Angle(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }
}
