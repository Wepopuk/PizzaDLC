package pizza.dlc.api.rotation.angle;

import java.security.SecureRandom;
import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import pizza.dlc.api.rotation.Angle;
import pizza.dlc.api.rotation.AngleUtil;
import pizza.dlc.api.rotation.ml.AimFeatures;
import pizza.dlc.api.rotation.ml.Humanizer;
import pizza.dlc.api.rotation.ml.NeuroEngine;
import pizza.dlc.api.rotation.ml.NeuroNorm;

public class NeuralSmoothMode extends AngleSmoothMode {
   private static final double HOMING_ERROR_DEG = 2.0;
   private static final int HOMING_AFTER_TICKS = 5;
   public static volatile boolean lastTickGated = false;
   private final GrimSmoothMode fallback = new GrimSmoothMode();
   private final AimFeatures features = new AimFeatures();
   private final SecureRandom rng = new SecureRandom();
   private final float[] x = new float[12];
   private final float[] y = new float[2];
   private final float[] deltaDeg = new float[2];
   private final float homing;
   private Humanizer humanizer;
   private float[] hidden;
   private int lastTargetId = Integer.MIN_VALUE;
   private int ticksOnTarget;
   private int stuckTicks;
   private Vec3 prevAim;
   private double prevYawCounts;
   private double prevPitchCounts;

   public NeuralSmoothMode() {
      this(0.15F);
   }

   public NeuralSmoothMode(float homing) {
      super("Neural");
      this.homing = homing;
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      NeuroEngine engine = NeuroEngine.get();
      engine.ensureLoading();
      if (engine.ready() && vec3d != null && mc.player != null) {
         lastTickGated = false;
         NeuroNorm norm = engine.norm();
         if (this.humanizer == null || this.hidden == null || this.hidden.length != norm.hidden()) {
            this.humanizer = new Humanizer(norm.residualStd(), norm.tau(), this.rng);
            this.hidden = new float[norm.hidden()];
         }

         int targetId = entity != null ? entity.getId() : Integer.MIN_VALUE;
         if (targetId != this.lastTargetId || this.prevAim == null) {
            this.newTarget(targetId, vec3d);
         }

         Angle aimAngle = AngleUtil.calculateAngle(vec3d);
         Angle err = AngleUtil.calculateDelta(currentAngle, aimAngle);
         double gcd = Angle.computeGcd();
         double dist = mc.player.getEyePosition().distanceTo(vec3d);
         Vec3 velocity = mc.player.getDeltaMovement();
         this.features
            .build(
               this.x,
               err.getYaw(),
               err.getPitch(),
               this.prevYawCounts,
               this.prevPitchCounts,
               dist,
               this.ticksOnTarget,
               Math.hypot(velocity.x, velocity.z),
               mc.player.onGround(),
               mc.player.isSprinting(),
               mc.player.getAttackStrengthScale(0.5F),
               gcd
            );
         norm.normalize(this.x);
         if (!engine.run(this.x, this.hidden, this.y)) {
            return this.fallback.limitAngleChange(currentAngle, targetAngle, vec3d, entity);
         }

         norm.denormalizeTarget(this.y);
         this.humanizer.apply(this.y, this.deltaDeg, gcd);
         Angle freshErr = AngleUtil.calculateDelta(currentAngle, targetAngle);
         float[] rescued = this.rescue(this.deltaDeg, freshErr, gcd);
         this.ticksOnTarget++;
         this.prevAim = vec3d;
         this.prevYawCounts = rescued[0] / gcd;
         this.prevPitchCounts = rescued[1] / gcd;
         return new Angle(currentAngle.getYaw() + rescued[0], Mth.clamp(currentAngle.getPitch() + rescued[1], -90.0F, 90.0F));
      }

      lastTickGated = true;
      return this.fallback.limitAngleChange(currentAngle, targetAngle, vec3d, entity);
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }

   private void newTarget(int targetId, Vec3 aim) {
      this.lastTargetId = targetId;
      this.ticksOnTarget = 0;
      this.stuckTicks = 0;
      this.prevAim = aim;
      this.prevYawCounts = 0.0;
      this.prevPitchCounts = 0.0;
      this.features.reset();
      Arrays.fill(this.hidden, 0.0F);
      this.humanizer.reset();
   }

   private float[] rescue(float[] delta, Angle err, double gcd) {
      double magnitude = Math.hypot(err.getYaw(), err.getPitch());
      this.stuckTicks = magnitude > HOMING_ERROR_DEG ? this.stuckTicks + 1 : 0;
      return this.stuckTicks >= HOMING_AFTER_TICKS && !(this.homing <= 0.0F)
         ? new float[]{
         delta[0] + (float)(Math.round(err.getYaw() * this.homing / gcd) * gcd), delta[1] + (float)(Math.round(err.getPitch() * this.homing / gcd) * gcd)
      }
      : delta;
   }
}
