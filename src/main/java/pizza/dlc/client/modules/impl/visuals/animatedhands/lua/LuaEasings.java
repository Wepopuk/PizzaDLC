package pizza.dlc.client.modules.impl.visuals.animatedhands.lua;

public final class LuaEasings {
   private static final double C1 = 1.70158F;
   private static final double C2 = C1 * 1.525F;
   private static final double C3 = C1 + 1.0;
   private static final double C4 = 2.0 * Math.PI / 3.0;
   private static final double C5 = 2.0 * Math.PI / 4.5;

   public double easeInSine(double x) {
      return 1.0 - Math.cos(x * Math.PI / 2.0);
   }

   public double easeOutSine(double x) {
      return Math.sin(x * Math.PI / 2.0);
   }

   public double easeInOutSine(double x) {
      return -(Math.cos(Math.PI * x) - 1.0) / 2.0;
   }

   public double easeInQuad(double x) {
      return x * x;
   }

   public double easeOutQuad(double x) {
      return 1.0 - (1.0 - x) * (1.0 - x);
   }

   public double easeInOutQuad(double x) {
      return x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0;
   }

   public double easeInCubic(double x) {
      return x * x * x;
   }

   public double easeOutCubic(double x) {
      return 1.0 - Math.pow(1.0 - x, 3.0);
   }

   public double easeInOutCubic(double x) {
      return x < 0.5 ? 4.0 * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 3.0) / 2.0;
   }

   public double easeInQuart(double x) {
      return x * x * x * x;
   }

   public double easeOutQuart(double x) {
      return 1.0 - Math.pow(1.0 - x, 4.0);
   }

   public double easeInOutQuart(double x) {
      return x < 0.5 ? 8.0 * x * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 4.0) / 2.0;
   }

   public double easeInQuint(double x) {
      return x * x * x * x * x;
   }

   public double easeOutQuint(double x) {
      return 1.0 - Math.pow(1.0 - x, 5.0);
   }

   public double easeInOutQuint(double x) {
      return x < 0.5 ? 16.0 * x * x * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 5.0) / 2.0;
   }

   public double easeInExpo(double x) {
      return x == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * x - 10.0);
   }

   public double easeOutExpo(double x) {
      return x == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * x);
   }

   public double easeInOutExpo(double x) {
      if (x == 0.0) return 0.0;
      if (x == 1.0) return 1.0;
      return x < 0.5 ? Math.pow(2.0, 20.0 * x - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * x + 10.0)) / 2.0;
   }

   public double easeInCirc(double x) {
      return 1.0 - Math.sqrt(1.0 - Math.pow(x, 2.0));
   }

   public double easeOutCirc(double x) {
      return Math.sqrt(1.0 - Math.pow(x - 1.0, 2.0));
   }

   public double easeInOutCirc(double x) {
      return x < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * x, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * x + 2.0, 2.0)) + 1.0) / 2.0;
   }

   public double easeInBack(double x) {
      return C3 * x * x * x - C1 * x * x;
   }

   public double easeOutBack(double x) {
      return 1.0 + C3 * Math.pow(x - 1.0, 3.0) + C1 * Math.pow(x - 1.0, 2.0);
   }

   public double easeInOutBack(double x) {
      return x < 0.5
         ? Math.pow(2.0 * x, 2.0) * ((C2 + 1.0) * 2.0 * x - C2) / 2.0
         : (Math.pow(2.0 * x - 2.0, 2.0) * ((C2 + 1.0) * (x * 2.0 - 2.0) + C2) + 2.0) / 2.0;
   }

   public double easeInElastic(double x) {
      if (x == 0.0) return 0.0;
      if (x == 1.0) return 1.0;
      return -Math.pow(2.0, 10.0 * x - 10.0) * Math.sin((x * 10.0 - 10.75) * C4);
   }

   public double easeOutElastic(double x) {
      if (x == 0.0) return 0.0;
      if (x == 1.0) return 1.0;
      return Math.pow(2.0, -10.0 * x) * Math.sin((x * 10.0 - 0.75) * C4) + 1.0;
   }

   public double easeInOutElastic(double x) {
      if (x == 0.0) return 0.0;
      if (x == 1.0) return 1.0;
      return x < 0.5
         ? -(Math.pow(2.0, 20.0 * x - 10.0) * Math.sin((20.0 * x - 11.125) * C5)) / 2.0
         : Math.pow(2.0, -20.0 * x + 10.0) * Math.sin((20.0 * x - 11.125) * C5) / 2.0 + 1.0;
   }

   public double easeOutBounce(double x) {
      double n1 = 7.5625;
      double d1 = 2.75;
      if (x < 1.0 / d1) return n1 * x * x;
      if (x < 2.0 / d1) {
         x -= 1.5 / d1;
         return n1 * x * x + 0.75;
      }
      if (x < 2.5 / d1) {
         x -= 2.25 / d1;
         return n1 * x * x + 0.9375;
      }
      x -= 2.625 / d1;
      return n1 * x * x + 0.984375;
   }

   public double easeInBounce(double x) {
      return 1.0 - this.easeOutBounce(1.0 - x);
   }

   public double easeInOutBounce(double x) {
      return x < 0.5 ? (1.0 - this.easeOutBounce(1.0 - 2.0 * x)) / 2.0 : (1.0 + this.easeOutBounce(2.0 * x - 1.0)) / 2.0;
   }

   public double cubicEase(double x) {
      return x * x * (3.0 - 2.0 * x);
   }
}
