package pizza.dlc.api.rotation.ml;

import java.util.random.RandomGenerator;

public final class Humanizer {
   private final float[] sigma;
   private final float[] alpha;
   private final RandomGenerator rng;
   private final double[] noise = new double[2];
   private int reactionLeft;

   public Humanizer(float[] residualStd, float[] tau, RandomGenerator rng) {
      this.sigma = (float[])residualStd.clone();
      this.alpha = new float[tau.length];

      for (int i = 0; i < tau.length; i++) {
         this.alpha[i] = tau[i] > 1.0E-6F ? (float)Math.exp(-1.0 / tau[i]) : 0.0F;
      }

      this.rng = rng;
      this.rearm();
   }

   public void reset() {
      this.noise[0] = 0.0;
      this.noise[1] = 0.0;
      this.rearm();
   }

   public boolean reacting() {
      return this.reactionLeft > 0;
   }

   public void nextNoise(double[] out) {
      for (int i = 0; i < 2; i++) {
         double a = this.alpha[i];
         this.noise[i] = a * this.noise[i] + Math.sqrt(1.0 - a * a) * this.sigma[i] * this.rng.nextGaussian();
         out[i] = this.noise[i];
      }
   }

   public void apply(float[] modelCounts, float[] outDegrees, double gcd) {
      double[] n = new double[2];
      this.nextNoise(n);
      boolean gated = this.reactionLeft > 0;
      if (gated) {
         this.reactionLeft--;
      }

      for (int i = 0; i < 2; i++) {
         double counts = (gated ? 0.0 : modelCounts[i]) + n[i];
         outDegrees[i] = (float)(Math.round(counts) * gcd);
      }
   }

   private void rearm() {
      double sampled = 4.0 + this.rng.nextGaussian() * 1.0;
      this.reactionLeft = (int)Math.max(2L, Math.min(8L, Math.round(sampled)));
   }
}
