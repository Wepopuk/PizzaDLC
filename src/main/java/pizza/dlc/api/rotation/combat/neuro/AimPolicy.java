package pizza.dlc.api.rotation.combat.neuro;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.StringJoiner;

public final class AimPolicy {
   private final int f_10488;
   private final int f_10489;
   private final int f_10490;
   private final int f_10491;
   private final int f_10492;
   private final int f_10493;
   private final int f_10494;
   private final double f_10495;
   private final double f_10496;
   private final String[] f_10497;
   private final int[] f_10498;
   private final int[] f_10499;
   private final double[] f_10500;
   private final double[] f_10501;
   private final double[] f_10502;
   private final double[] f_10503;
   private final double[] f_10504;
   private final Map<String, float[]> f_10505 = new HashMap<>();
   private final Random f_10506;
   private final float[][] f_10507;
   private final double f_10508;
   private final double f_10509;
   private final ArrayDeque<AimPolicy.LayerWeights> f_10510 = new ArrayDeque<>();
   private String f_10511;
   private long f_10512;
   private double[] f_10513;
   private double[] f_10514;
   private int f_10515;
   private int f_10516;
   private static final long f_10517 = Long.MIN_VALUE;

   private static final long f_10547 = Long.MIN_VALUE;
   private static final long f_10548 = Long.MIN_VALUE;

   private static final long f_10550 = Long.MIN_VALUE;

   private static final long f_10552 = Long.MIN_VALUE;

   private static final long f_10561 = Long.MIN_VALUE;

   private void m_3591(float[] var1) {
      int var2 = 1 + 11 * this.f_10493;
      double[] var3 = new double[this.f_10492];

      for (int var4 = 0; var4 < this.f_10492; var4++) {
         var3[var4] = var1[var4 * var2];
      }

      this.f_10515 = this.m_3594(var3);
      this.f_10516 = 0;
      double[] var29 = null;
      double[] var5 = null;

      for (int var6 = 0; var6 < this.f_10493; var6++) {
         int var7 = this.f_10515 * var2 + 1 + 11 * var6;
         double[] var8 = new double[]{var1[var7], var1[var7 + 1]};
         double[] var9 = (double[])var8.clone();
         if (var6 > 0) {
            for (int var10 = 0; var10 < 2; var10++) {
               var9[var10] += 0.95 * Math.tanh(var1[var7 + 5 + var10]) * (var29[var10] - var5[var10]);
            }
         }

         double[] var30 = new double[4];

         for (int var11 = 0; var11 < 4; var11++) {
            var30[var11] = var1[var7 + 7 + var11];
         }

         int var31 = this.m_3594(var30);
         double var12 = 0.95 * Math.tanh(var1[var7 + 4]);
         double var14 = this.f_10506.nextGaussian();
         double var16 = var12 * var14 + Math.sqrt(1.0 - var12 * var12) * this.f_10506.nextGaussian();
         double[] var18 = new double[]{var14, var16};
         double[] var19 = new double[2];
         double[] var20 = new double[2];
         int var21 = 0;

         for (int var22 = 0; var22 < 2; var22++) {
            double var23 = Math.min(m_3602(var1[var7 + 2 + var22]) + this.f_10495, this.f_10496) * this.f_10509;
            var19[var22] = (var31 & 1 << var22) == 0 ? 0.0 : var9[var22] + var23 * var18[var22];
            double var25 = m_3606(this.f_10504[var22] / this.f_10503[var22]);
            double var27 = Math.max(-var25, Math.min(var25, var19[var22]));
            if (var27 != var19[var22]) {
               var21 |= 1 << var22;
            }

            var19[var22] = var27;
            var20[var22] = Math.sinh(var19[var22]) * this.f_10503[var22];
         }

         if (var21 != 0) {
            this.f_10516++;
         }

         if (var6 < this.f_10494) {
            this.f_10510.add(new AimPolicy.LayerWeights(var20, var21));
         }

         var29 = var19;
         var5 = var8;
      }
   }

   public AimPolicy(Path var1, long var2) throws IOException {
      this(var1, var2, 1.0, 1.0);
   }

   private static float[] m_3592(float[] var0, float[] var1, float[] var2) {
      float[] var3 = new float[var1.length];

      for (int var4 = 0; var4 < var3.length; var4++) {
         double var5 = var1[var4];
         int var7 = var4 * var2.length;

         for (int var8 = 0; var8 < var2.length; var8++) {
            var5 += (double)var0[var7 + var8] * var2[var8];
         }

         var3[var4] = (float)var5;
      }

      return var3;
   }

   private static String m_3593(DataInputStream var0) throws IOException {
      int var1 = m_3601(var0);
      byte[] var2 = var0.readNBytes(var1);
      if (var2.length != var1) {
         throw new EOFException();
      } else {
         return new String(var2, StandardCharsets.UTF_8);
      }
   }

   private int m_3594(double[] var1) {
      double var2 = Arrays.stream(var1).max().orElseThrow();
      double var4 = 0.0;
      double[] var6 = new double[var1.length];

      for (int var7 = 0; var7 < var6.length; var7++) {
         var6[var7] = Math.exp((var1[var7] - var2) / this.f_10508);
         var4 += var6[var7];
      }

      double var10 = this.f_10506.nextDouble() * var4;

      for (int var9 = 0; var9 < var6.length; var9++) {
         var10 -= var6[var9];
         if (var10 < 0.0) {
            return var9;
         }
      }

      return var6.length - 1;
   }

   public static void main(String[] var0) throws Exception {
      if (var0.length != 2) {
         throw new IllegalArgumentException("Usage: HumanAimPolicy policy.bin raw_features.csv");
      }

      AimPolicy var1 = new AimPolicy(Path.of(var0[0]), 42L);

      String var3;
      try (BufferedReader var2 = Files.newBufferedReader(Path.of(var0[1]))) {
         while ((var3 = var2.readLine()) != null) {
            if (!var3.isBlank()) {
               String[] var4 = var3.split(",");
               float[] var5 = new float[var4.length];

               for (int var6 = 0; var6 < var4.length; var6++) {
                  var5[var6] = Float.parseFloat(var4[var6]);
               }

               float[] var14 = var1.m_3607(var5);
               StringJoiner var7 = new StringJoiner(",");

               for (float var11 : var14) {
                  var7.add(Float.toString(var11));
               }

               System.out.println(var7);
            }
         }
      }
   }

   private float[] m_3595(String var1, float[] var2) {
      float[] var3 = this.f_10505.get(var1 + "");
      float[] var4 = this.f_10505.get(var1 + "");
      return m_3592(var3, var4, var2);
   }

   private static double m_3596(double var0) {
      return 1.0 / (1.0 + Math.exp(-Math.max(-60.0, Math.min(60.0, var0))));
   }

   public void m_3597() {
      for (float[] var4 : this.f_10507) {
         Arrays.fill(var4, 0.0F);
      }

      this.f_10510.clear();
      this.f_10511 = null;
      this.f_10512 = f_10561;
      this.f_10513 = null;
      this.f_10514 = null;
      this.f_10515 = -1;
      this.f_10516 = 0;
   }

   public void m_3598(long var1, double var3, double var5) {
      if (this.f_10513 == null || var1 != this.f_10512) {
         throw new IllegalStateException("Output must belong to the current action");
      }

      if (Double.isFinite(var3) && Double.isFinite(var5)) {
         this.f_10514 = new double[]{var3, var5};
      } else {
         throw new IllegalArgumentException("Nonfinite output delta");
      }
   }

   public String[] m_3599() {
      return (String[])this.f_10497.clone();
   }

   private static double[] m_3600(DataInputStream var0, int var1) throws IOException {
      double[] var2 = new double[var1];

      for (int var3 = 0; var3 < var1; var3++) {
         var2[var3] = var0.readDouble();
         if (!Double.isFinite(var2[var3])) {
            throw new IOException("Nonfinite metadata");
         }
      }

      return var2;
   }

   private static int m_3601(DataInputStream var0) throws IOException {
      int var1 = var0.readInt();
      if (var1 >= 1 && var1 <= 10000) {
         return var1;
      } else {
         throw new IOException("Invalid dimension");
      }
   }

   private static double m_3602(double var0) {
      return Math.max(var0, 0.0) + Math.log1p(Math.exp(-Math.abs(var0)));
   }

   public AimPolicy(Path var1, long var2, double var4, double var6) throws IOException {
      this.f_10512 = f_10517;
      this.f_10515 = -1;
      if (var4 > 0.0 && var6 >= 0.0 && Double.isFinite(var4) && Double.isFinite(var6)) {
         this.f_10508 = var4;
         this.f_10509 = var6;
         this.f_10506 = new Random(var2);

         try (DataInputStream var8 = new DataInputStream(new BufferedInputStream(Files.newInputStream(var1)))) {
            if (!Arrays.equals(var8.readNBytes(4), new byte[]{72, 65, 77, 49}) || var8.readInt() != 1) {
               throw new IOException("Unsupported model format");
            }

            this.f_10488 = m_3601(var8);
            this.f_10489 = m_3601(var8);
            this.f_10490 = m_3601(var8);
            this.f_10491 = m_3601(var8);
            this.f_10492 = m_3601(var8);
            this.f_10493 = m_3601(var8);
            this.f_10494 = m_3601(var8);
            if (this.f_10491 > 16 || this.f_10492 > 64 || this.f_10493 > 120 || this.f_10494 > this.f_10493) {
               throw new IOException("Invalid architecture");
            }

            this.f_10495 = var8.readDouble();
            this.f_10496 = var8.readDouble();
            if (!(this.f_10495 > 0.0) || !(this.f_10496 >= this.f_10495)) {
               throw new IOException("Invalid scale bounds");
            }

            this.f_10497 = new String[m_3601(var8)];

            for (int var9 = 0; var9 < this.f_10497.length; var9++) {
               this.f_10497[var9] = m_3593(var8);
            }

            this.f_10498 = new int[this.f_10488];
            this.f_10499 = new int[this.f_10488];
            this.f_10500 = new double[this.f_10488];

            for (int var20 = 0; var20 < this.f_10488; var20++) {
               this.f_10498[var20] = var8.readInt();
               this.f_10499[var20] = var8.readInt();
               this.f_10500[var20] = var8.readDouble();
               if (this.f_10498[var20] < 0
                  || this.f_10498[var20] >= this.f_10497.length
                  || this.f_10499[var20] < 0
                  || this.f_10499[var20] > 4
                  || !(this.f_10500[var20] > 0.0)) {
                  throw new IOException("Invalid feature transform");
               }
            }

            this.f_10501 = m_3600(var8, this.f_10488);
            this.f_10502 = m_3600(var8, this.f_10488);
            this.f_10503 = m_3600(var8, 2);
            this.f_10504 = m_3600(var8, 2);

            for (double var12 : this.f_10502) {
               if (!(var12 > 0.0)) {
                  throw new IOException("Invalid normalization");
               }
            }

            for (int var22 = 0; var22 < 2; var22++) {
               if (!(this.f_10503[var22] > 0.0) || !(this.f_10504[var22] > 0.0)) {
                  throw new IOException("Invalid action units");
               }
            }

            int var23 = m_3601(var8);

            for (int var24 = 0; var24 < var23; var24++) {
               String var25 = m_3593(var8);
               int var26 = m_3601(var8);
               long var13 = 1L;
               if (var26 > 4) {
                  throw new IOException("Invalid tensor rank");
               }

               for (int var15 = 0; var15 < var26; var15++) {
                  var13 *= m_3601(var8);
                  if (var13 > 20000000L) {
                     throw new IOException("Tensor too large");
                  }
               }

               float[] var27 = new float[(int)var13];

               for (int var16 = 0; var16 < var27.length; var16++) {
                  var27[var16] = var8.readFloat();
                  if (!Float.isFinite(var27[var16])) {
                     throw new IOException("Nonfinite weight");
                  }
               }

               if (this.f_10505.put(var25, var27) != null) {
                  throw new IOException("Duplicate tensor");
               }
            }

            if (var8.read() != -1) {
               throw new IOException("Trailing model data");
            }
         }

         this.m_3603("encoder.weight", this.f_10489 * this.f_10488);
         this.m_3603("encoder.bias", this.f_10489);

         for (int var19 = 0; var19 < this.f_10491; var19++) {
            this.m_3603(var19 + "", 3 * this.f_10490 * (var19 == 0 ? this.f_10489 : this.f_10490));
            this.m_3603(var19 + "", 3 * this.f_10490 * this.f_10490);
            this.m_3603(var19 + "", 3 * this.f_10490);
            this.m_3603(var19 + "", 3 * this.f_10490);
         }

         this.m_3603("head.weight", this.f_10492 * (1 + 11 * this.f_10493) * this.f_10490);
         this.m_3603("head.bias", this.f_10492 * (1 + 11 * this.f_10493));
         this.f_10507 = new float[this.f_10491][this.f_10490];
      } else {
         throw new IllegalArgumentException("Invalid sampling settings");
      }
   }

   private void m_3603(String var1, int var2) throws IOException {
      if (!this.f_10505.containsKey(var1) || this.f_10505.get(var1).length != var2) {
         throw new IOException(var1 + "");
      }
   }

   public float[] m_3604(Map<String, ? extends Number> var1) {
      float[] var2 = new float[this.f_10497.length];

      for (int var3 = 0; var3 < var2.length; var3++) {
         Number var4 = (Number)var1.get(this.f_10497[var3]);
         if (var4 == null) {
            throw new IllegalArgumentException(this.f_10497[var3] + "");
         }

         var2[var3] = var4.floatValue();
         if (!Float.isFinite(var2[var3])) {
            throw new IllegalArgumentException(this.f_10497[var3] + "");
         }
      }

      return var2;
   }

   public AimPolicy.PolicyResult m_3605(Map<String, ? extends Number> var1, String var2, long var3) {
      Objects.requireNonNull(var2, "episodeKey");
      if (!var2.equals(this.f_10511)) {
         this.m_3597();
         this.f_10511 = var2;
      }

      if (this.f_10512 != f_10547 && var3 <= this.f_10512) {
         return null;
      }

      if (this.f_10512 != f_10548 && var3 - this.f_10512 > 50000000L) {
         this.m_3597();
         this.f_10511 = var2;
      }

      if (this.f_10512 != f_10550 && var3 - this.f_10512 < 8000000L) {
         return null;
      }

      float[] var5 = this.m_3604(var1);
      if (this.f_10512 != f_10552) {
         var5[0] = (float)((var3 - this.f_10512) / 1000000.0);
      }

      if (!(var5[0] < 8.0F) && !(var5[0] > 33.34)) {
         if (this.f_10513 != null) {
            double var6 = var5[0] / 1000.0;

            for (int var8 = 0; var8 < 2; var8++) {
               double var9 = this.f_10514 == null ? this.f_10513[var8] * var6 : this.f_10514[var8];
               double var11 = var5[12 + var8] * var6;
               if (Math.abs(var11 - var9) > Math.max(2.0 * var6, 0.15 * Math.abs(var9))) {
                  this.f_10510.clear();
               }
            }
         }

         this.f_10514 = null;
         float[] var13 = this.m_3607(var5);
         if (this.f_10510.isEmpty()) {
            this.m_3591(var13);
         }

         AimPolicy.LayerWeights var7 = this.f_10510.removeFirst();
         this.f_10513 = var7.velocity();
         this.f_10512 = var3;
         return new AimPolicy.PolicyResult(this.f_10513[0], this.f_10513[1], this.f_10515, this.f_10516, var3, var7.clippedAxes());
      } else {
         this.m_3597();
         this.f_10511 = var2;
         this.f_10512 = var3;
         return null;
      }
   }

   private static double m_3606(double var0) {
      return Math.copySign(Math.log(Math.abs(var0) + Math.hypot(var0, 1.0)), var0);
   }

   public float[] m_3607(float[] var1) {
      if (var1.length != this.f_10497.length) {
         throw new IllegalArgumentException("Wrong feature count");
      }

      for (float var5 : var1) {
         if (!Float.isFinite(var5)) {
            throw new IllegalArgumentException("Nonfinite input");
         }
      }

      float[] var14 = new float[this.f_10488];

      for (int var16 = 0; var16 < this.f_10488; var16++) {
         double var20 = var1[this.f_10498[var16]];
         double var6 = this.f_10500[var16];

         var20 = switch (this.f_10499[var16]) {
            case 0 -> var20 / var6;
            case 1 -> m_3606(var20 / var6);
            case 2 -> Math.sin(var20 * var6);
            case 3 -> Math.cos(var20 * var6);
            case 4 -> Math.log1p(Math.max(var20, 0.0) / var6);
            default -> throw new IllegalStateException();
         };
         var14[var16] = (float)(((float)var20 - this.f_10501[var16]) / this.f_10502[var16]);
      }

      var14 = this.m_3595("encoder", var14);

      for (int var17 = 0; var17 < var14.length; var17++) {
         var14[var17] = (float)(var14[var17] * m_3596(var14[var17]));
      }

      for (int var18 = 0; var18 < this.f_10491; var18++) {
         float[] var22 = m_3592(this.f_10505.get(var18 + ""), this.f_10505.get(var18 + ""), var14);
         float[] var24 = m_3592(this.f_10505.get(var18 + ""), this.f_10505.get(var18 + ""), this.f_10507[var18]);
         float[] var26 = new float[this.f_10490];

         for (int var7 = 0; var7 < this.f_10490; var7++) {
            double var8 = m_3596(var22[var7] + var24[var7]);
            double var10 = m_3596(var22[this.f_10490 + var7] + var24[this.f_10490 + var7]);
            double var12 = Math.tanh(var22[2 * this.f_10490 + var7] + var8 * var24[2 * this.f_10490 + var7]);
            var26[var7] = (float)((1.0 - var10) * var12 + var10 * this.f_10507[var18][var7]);
         }

         this.f_10507[var18] = var26;
         var14 = var26;
      }

      float[] var19 = this.m_3595("head", var14);

      for (float var28 : var19) {
         if (!Float.isFinite(var28)) {
            this.m_3597();
            throw new IllegalStateException("Nonfinite network output");
         }
      }

      return var19;
   }

   private record LayerWeights(double[] velocity, int clippedAxes) {
   }

   public record PolicyResult(double yawVelocity, double pitchVelocity, int component, int clippedSteps, long timestampNs, int clippedAxes) {
   }
}
