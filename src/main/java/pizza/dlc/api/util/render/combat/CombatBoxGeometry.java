package pizza.dlc.api.util.render.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CombatBoxGeometry {

   public static Matrix4f m_6085(PoseStack var0) {
      return var0.last().pose();
   }

   public static void m_6086(Pose var0, VertexConsumer var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      if (Float.isFinite(var2) && Float.isFinite(var3) && Float.isFinite(var4) && Float.isFinite(var5) && Float.isFinite(var6) && Float.isFinite(var7)) {
         var1.addVertex(var0, var2, var3, var4).setColor(var8);
         var1.addVertex(var0, var5, var6, var7).setColor(var8);
      }
   }

   public static void m_6087(
      PoseStack var0, VertexConsumer var1, Vec3 var2, double var3, double var5, double var7, double var9, double var11, double var13, int var15
   ) {
      float var16 = (float)(var3 - var2.x);
      float var17 = (float)(var5 - var2.y);
      float var18 = (float)(var7 - var2.z);
      float var19 = (float)(var9 - var2.x);
      float var20 = (float)(var11 - var2.y);
      float var21 = (float)(var13 - var2.z);
      Pose var22 = var0.last();
      m_6090(var22, var1, var16, var17, var18, var19, var17, var18, var15);
      m_6090(var22, var1, var19, var17, var18, var19, var17, var21, var15);
      m_6090(var22, var1, var19, var17, var21, var16, var17, var21, var15);
      m_6090(var22, var1, var16, var17, var21, var16, var17, var18, var15);
      m_6090(var22, var1, var16, var20, var18, var19, var20, var18, var15);
      m_6090(var22, var1, var19, var20, var18, var19, var20, var21, var15);
      m_6090(var22, var1, var19, var20, var21, var16, var20, var21, var15);
      m_6090(var22, var1, var16, var20, var21, var16, var20, var18, var15);
      m_6090(var22, var1, var16, var17, var18, var16, var20, var18, var15);
      m_6090(var22, var1, var19, var17, var18, var19, var20, var18, var15);
      m_6090(var22, var1, var19, var17, var21, var19, var20, var21, var15);
      m_6090(var22, var1, var16, var17, var21, var16, var20, var21, var15);
   }

   private CombatBoxGeometry() {
   }

   public static void m_6088(
      PoseStack var0, VertexConsumer var1, Vec3 var2, double var3, double var5, double var7, double var9, double var11, double var13, int var15, boolean var16
   ) {
      float var17 = (float)(var3 - var2.x);
      float var18 = (float)(var5 - var2.y);
      float var19 = (float)(var7 - var2.z);
      float var20 = (float)(var9 - var2.x);
      float var21 = (float)(var11 - var2.y);
      float var22 = (float)(var13 - var2.z);
      Pose var23 = var0.last();
      if (var16) {
         m_6089(var23, var1, var17, var18, var19, var20, var18, var19, var20, var18, var22, var17, var18, var22, var15);
         m_6089(var23, var1, var17, var21, var19, var17, var21, var22, var20, var21, var22, var20, var21, var19, var15);
         m_6089(var23, var1, var17, var18, var19, var17, var21, var19, var20, var21, var19, var20, var18, var19, var15);
         m_6089(var23, var1, var17, var18, var22, var20, var18, var22, var20, var21, var22, var17, var21, var22, var15);
         m_6089(var23, var1, var17, var18, var19, var17, var18, var22, var17, var21, var22, var17, var21, var19, var15);
         m_6089(var23, var1, var20, var18, var19, var20, var21, var19, var20, var21, var22, var20, var18, var22, var15);
      } else {
         m_6086(var23, var1, var17, var18, var19, var20, var18, var19, var15);
         m_6086(var23, var1, var20, var18, var19, var20, var18, var22, var15);
         m_6086(var23, var1, var20, var18, var22, var17, var18, var22, var15);
         m_6086(var23, var1, var17, var18, var22, var17, var18, var19, var15);
         m_6086(var23, var1, var17, var21, var19, var20, var21, var19, var15);
         m_6086(var23, var1, var20, var21, var19, var20, var21, var22, var15);
         m_6086(var23, var1, var20, var21, var22, var17, var21, var22, var15);
         m_6086(var23, var1, var17, var21, var22, var17, var21, var19, var15);
         m_6086(var23, var1, var17, var18, var19, var17, var21, var19, var15);
         m_6086(var23, var1, var20, var18, var19, var20, var21, var19, var15);
         m_6086(var23, var1, var20, var18, var22, var20, var21, var22, var15);
         m_6086(var23, var1, var17, var18, var22, var17, var21, var22, var15);
      }
   }

   private static void m_6089(
      Pose var0,
      VertexConsumer var1,
      float var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      int var14
   ) {
      var1.addVertex(var0, var2, var3, var4).setColor(var14);
      var1.addVertex(var0, var5, var6, var7).setColor(var14);
      var1.addVertex(var0, var8, var9, var10).setColor(var14);
      var1.addVertex(var0, var11, var12, var13).setColor(var14);
   }

   public static void m_6090(Pose var0, VertexConsumer var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
      float var9 = var5 - var2;
      float var10 = var6 - var3;
      float var11 = var7 - var4;
      float var12 = var9 * var9 + var10 * var10 + var11 * var11;
      if (!(var12 <= 1.0E-6F)) {
         float var13 = 1.0F / (float)Math.sqrt(var12);
         var9 *= var13;
         var10 *= var13;
         var11 *= var13;
         var1.addVertex(var0, var2, var3, var4).setColor(var8).setNormal(var0, var9, var10, var11).setLineWidth(2.0F);
         var1.addVertex(var0, var5, var6, var7).setColor(var8).setNormal(var0, var9, var10, var11).setLineWidth(2.0F);
      }
   }
}
