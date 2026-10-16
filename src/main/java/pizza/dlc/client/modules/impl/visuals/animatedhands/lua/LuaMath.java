package pizza.dlc.client.modules.impl.visuals.animatedhands.lua;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class LuaMath {
   public double PI = (float)Math.PI;

   public void scale(PoseStack pose, double x, double y, double z) {
      pose.scale((float)x, (float)y, (float)z);
   }

   public void push(PoseStack pose) {
      pose.pushPose();
   }

   public void pop(PoseStack pose) {
      pose.popPose();
   }

   public void moveX(PoseStack pose, double v) {
      pose.translate(v, 0.0, 0.0);
   }

   public void moveY(PoseStack pose, double v) {
      pose.translate(0.0, v, 0.0);
   }

   public void moveZ(PoseStack pose, double v) {
      pose.translate(0.0, 0.0, v);
   }

   public void translate(PoseStack pose, double x, double y, double z) {
      pose.translate(x, y, z);
   }

   public void rotateX(PoseStack pose, double degrees) {
      pose.mulPose(Axis.XP.rotationDegrees((float)degrees));
   }

   public void rotateY(PoseStack pose, double degrees) {
      pose.mulPose(Axis.YP.rotationDegrees((float)degrees));
   }

   public void rotateZ(PoseStack pose, double degrees) {
      pose.mulPose(Axis.ZP.rotationDegrees((float)degrees));
   }

   public void rotateX(PoseStack pose, double degrees, double px, double py, double pz) {
      pose.rotateAround(Axis.XP.rotationDegrees((float)degrees), (float)px, (float)py, (float)pz);
   }

   public void rotateY(PoseStack pose, double degrees, double px, double py, double pz) {
      pose.rotateAround(Axis.YP.rotationDegrees((float)degrees), (float)px, (float)py, (float)pz);
   }

   public void rotateZ(PoseStack pose, double degrees, double px, double py, double pz) {
      pose.rotateAround(Axis.ZP.rotationDegrees((float)degrees), (float)px, (float)py, (float)pz);
   }

   public double sin(double v) {
      return Math.sin(v);
   }

   public double cos(double v) {
      return Math.cos(v);
   }

   public double clamp(double v, double min, double max) {
      return min <= max ? Math.clamp(v, min, max) : Math.clamp(v, max, min);
   }

   public double floor(double v) {
      return Math.floor(v);
   }

   public double abs(double v) {
      return Math.abs(v);
   }

   public double lerp(double delta, double from, double to) {
      return Mth.lerp(delta, from, to);
   }

   public double pow(double v, double e) {
      return Math.pow(v, e);
   }

   public double ceil(double v) {
      return Math.ceil(v);
   }

   public double round(double v) {
      return Math.round(v);
   }

   public void shear(PoseStack pose, double x, double y, double z) {
      float sx = (float)x;
      float sy = (float)y;
      float sz = (float)z;
      Matrix4f shear = new Matrix4f(1.0F, sx, sx, 0.0F, sy, 1.0F, sy, 0.0F, sz, sz, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
      pose.last().pose().mul(shear);
   }
}
