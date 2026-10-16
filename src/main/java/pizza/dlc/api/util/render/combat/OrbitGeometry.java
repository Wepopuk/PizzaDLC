package pizza.dlc.api.util.render.combat;

public final class OrbitGeometry {

   public static void m_6442(float var0, float var1, double var2, float var4, float var5, int var6, OrbitGeometry.VertexWriter var7) {
      float var8 = var0 * 1.65F;
      float var9 = var1 - 0.15F;
      float var10 = Math.max(0.5F, 0.7F - 0.2F * var4);

      for (int var11 = 0; var11 < 360; var11 += 2) {
         double var12 = Math.toRadians(var11 + var2);
         float var14 = (float)(Math.cos(var12) * var8 * var10);
         float var15 = (float)(Math.sin(var12) * var8 * var10);

         for (int var16 = 0; var16 < var6; var16++) {
            float var17 = var9 / 1.7F + var9 / 2.0F * (float)Math.cos(Math.toRadians(var2 / 1.5 + var16 * 2));
            int var18 = Math.clamp((int)(255.0F * var4 * ((float)var16 / var6) * 0.08F), 0, 255);
            var7.m_6441(var14, var17, var15, var5, var18);
         }
      }

      int var19 = Math.clamp((int)(255.0F * var4 * 0.22F), 0, 255);

      for (int var20 = 0; var20 < 360; var20 += 2) {
         double var13 = Math.toRadians(var20 + var2);
         float var21 = (float)(Math.cos(var13) * var8 * var10);
         float var22 = (float)(Math.sin(var13) * var8 * var10);
         float var23 = var9 / 1.75F + var9 / 2.0F * (float)Math.cos(Math.toRadians(var2 / 1.5 + 30.0));
         var7.m_6441(var21, var23, var22, var5 * 1.08F, var19);
      }
   }

   private OrbitGeometry() {
   }

   @FunctionalInterface
   public interface VertexWriter {
      void m_6441(float var1, float var2, float var3, float var4, int var5);
   }
}
