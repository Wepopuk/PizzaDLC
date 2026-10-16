package pizza.dlc.api.util.combat;

public class AimSensitivity implements pizza.dlc.api.interfaces.QuickImports {

   public static float m_1200(float var0) {
      return m_1201(var0);
   }

   public static float m_1201(float var0) {
      double var1 = m_1202();
      return var1 <= 0.0 ? var0 : (float)(Math.round(var0 / var1) * var1);
   }

   public static double m_1202() {
      double var0 = (Double)mc.options.sensitivity().get();
      double var2 = 0.502112676056338;
      return var2 * var2 * var2 * 1.2;
   }
}
