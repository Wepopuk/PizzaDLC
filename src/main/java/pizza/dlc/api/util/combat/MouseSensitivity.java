package pizza.dlc.api.util.combat;

public final class MouseSensitivity implements pizza.dlc.api.interfaces.QuickImports {

   public static float m_9128(float var0) {
      return m_9133(var0);
   }

   public static float m_9129(long var0, double var2) {
      return (float)(var0 * var2) * 0.15F;
   }

   public static float m_9130(float var0) {
      return m_9134(var0);
   }

   public static double m_9131(double var0, boolean var2) {
      if (Double.isFinite(var0) && !(var0 < 0.0) && !(var0 > 1.0)) {
         double var3 = var0 * 0.6F + 0.2F;
         return var3 * var3 * var3 * (var2 ? 1.0 : 8.0);
      } else {
         throw new IllegalArgumentException("Sensitivity must be within 0..1");
      }
   }

   public static float m_9132(float var0, double var1) {
      if (Float.isFinite(var0) && Double.isFinite(var1) && !(var1 <= 0.0)) {
         double var3 = var1 * 0.15F;
         return m_9129(Math.round(var0 / var3), var1);
      } else {
         throw new IllegalArgumentException("Invalid mouse delta or gain");
      }
   }

   public static float m_9133(float var0) {
      return m_9132(var0, m_9135());
   }

   public static float m_9134(float var0) {
      return (float)Math.round(var0 / (m_9135() * 0.15F));
   }

   public static double m_9135() {
      boolean var0 = mc.player != null && !mc.options.smoothCamera && mc.options.getCameraType().isFirstPerson() && mc.player.isScoping();
      return m_9131((Double)mc.options.sensitivity().get(), var0);
   }

   public static float m_9136() {
      return m_9137();
   }

   public static float m_9137() {
      return (float)m_9135();
   }

   private MouseSensitivity() {
   }

   public static float m_9138() {
      return m_9139();
   }

   public static float m_9139() {
      return m_9129(1L, m_9135());
   }
}
