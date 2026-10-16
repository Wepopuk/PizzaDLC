package pizza.dlc.api.util.combat;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import lombok.Generated;

public class AimNoise {
   private final Set<Float> f_28016;
   private final float f_28017;
   private final float f_28018;
   private final float f_28019;
   private float f_28020 = 1.0F;
   private final int f_28021;

   public AimNoise(float var1, float var2, float var3) {
      this.f_28016 = new HashSet<>();
      this.f_28017 = var3;
      this.f_28018 = var1;
      this.f_28019 = var2;
      this.f_28021 = (int)((var2 - var1) / var3);
   }

   @Generated
   public float m_7966() {
      return this.f_28020;
   }

   public void m_7967() {
      Random var1 = new Random();
      if (this.f_28016.size() >= this.f_28021) {
         this.f_28016.clear();
      }

      float var2;
      do {
         var2 = this.f_28018 + var1.nextFloat() * (this.f_28019 - this.f_28018);
      } while (!this.m_7968(var2));

      for (float var3 = var2 - this.f_28017; var3 <= var2 + this.f_28017; var3 += 0.01F) {
         this.f_28016.add(var3);
      }

      this.f_28020 = var2;
   }

   private boolean m_7968(float var1) {
      return this.f_28016.stream().noneMatch(var2 -> Math.abs(var2 - var1) < this.f_28017);
   }
}
