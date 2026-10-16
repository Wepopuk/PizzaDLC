package pizza.dlc.api.util.math;

import java.util.concurrent.ThreadLocalRandom;

public final class MathUtility {
   private MathUtility() {
   }

   public static float random(float min, float max) {
      return min == max ? min : (float)ThreadLocalRandom.current().nextDouble(min, max);
   }
}
