package pizza.dlc.api.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pizza.dlc.api.events.impl.render.EventRender3D;

public final class Render3DUtil {
   private static final float LINE_WIDTH = 1.5F;
   private static int circleSegments = -1;
   private static float[] circleCos = new float[0];
   private static float[] circleSin = new float[0];

   private Render3DUtil() {
   }

   public static void drawLine(EventRender3D e, double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
      Vec3 cam = e.getCamera().pos;
      e.getCollector().submitCustomGeometry(e.getPose(), RenderTypes.LINES, (pose, vc) -> line(pose, vc, cam, x1, y1, z1, x2, y2, z2, argb));
   }

   public static void drawLines(EventRender3D e, LineBatch batch) {
      Vec3 cam = e.getCamera().pos;
      e.getCollector().submitCustomGeometry(e.getPose(), RenderTypes.LINES, (pose, vc) -> batch.emit(new LineWriter(pose, vc, cam)));
   }

   public static void drawBox(EventRender3D e, AABB box, int argb) {
      drawBox(e, box, argb, false);
   }

   public static void drawBox(EventRender3D e, AABB box, int argb, boolean throughWalls) {
      RenderType type = throughWalls ? EspRenderTypes.linesThroughWalls() : RenderTypes.LINES;
      Vec3 cam = e.getCamera().pos;
      e.getCollector().submitCustomGeometry(e.getPose(), type, (pose, vc) -> {
         double x0 = box.minX;
         double y0 = box.minY;
         double z0 = box.minZ;
         double x1 = box.maxX;
         double y1 = box.maxY;
         double z1 = box.maxZ;
         line(pose, vc, cam, x0, y0, z0, x1, y0, z0, argb);
         line(pose, vc, cam, x0, y0, z1, x1, y0, z1, argb);
         line(pose, vc, cam, x0, y1, z0, x1, y1, z0, argb);
         line(pose, vc, cam, x0, y1, z1, x1, y1, z1, argb);
         line(pose, vc, cam, x0, y0, z0, x0, y1, z0, argb);
         line(pose, vc, cam, x1, y0, z0, x1, y1, z0, argb);
         line(pose, vc, cam, x0, y0, z1, x0, y1, z1, argb);
         line(pose, vc, cam, x1, y0, z1, x1, y1, z1, argb);
         line(pose, vc, cam, x0, y0, z0, x0, y0, z1, argb);
         line(pose, vc, cam, x1, y0, z0, x1, y0, z1, argb);
         line(pose, vc, cam, x0, y1, z0, x0, y1, z1, argb);
         line(pose, vc, cam, x1, y1, z0, x1, y1, z1, argb);
      });
   }

   private static void line(Pose pose, VertexConsumer vc, Vec3 cam, double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
      float rx1 = (float)(x1 - cam.x);
      float ry1 = (float)(y1 - cam.y);
      float rz1 = (float)(z1 - cam.z);
      float rx2 = (float)(x2 - cam.x);
      float ry2 = (float)(y2 - cam.y);
      float rz2 = (float)(z2 - cam.z);
      float nx = rx2 - rx1;
      float ny = ry2 - ry1;
      float nz = rz2 - rz1;
      float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (!(len < 1.0E-5F)) {
         nx /= len;
         ny /= len;
         nz /= len;
         vc.addVertex(pose, rx1, ry1, rz1).setColor(argb).setNormal(pose, nx, ny, nz).setLineWidth(LINE_WIDTH);
         vc.addVertex(pose, rx2, ry2, rz2).setColor(argb).setNormal(pose, nx, ny, nz).setLineWidth(LINE_WIDTH);
      }
   }

   public static void drawFilledBox(EventRender3D e, AABB box, int argb) {
      drawFilledBox(e, box, argb, false);
   }

   public static void drawFilledBox(EventRender3D e, AABB box, int argb, boolean throughWalls) {
      RenderType type = throughWalls ? EspRenderTypes.filledThroughWalls() : RenderTypes.debugFilledBox();
      Vec3 cam = e.getCamera().pos;
      e.getCollector().submitCustomGeometry(e.getPose(), type, (pose, vc) -> {
         float x0 = (float)(box.minX - cam.x);
         float y0 = (float)(box.minY - cam.y);
         float z0 = (float)(box.minZ - cam.z);
         float x1 = (float)(box.maxX - cam.x);
         float y1 = (float)(box.maxY - cam.y);
         float z1 = (float)(box.maxZ - cam.z);
         quad(pose, vc, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
         quad(pose, vc, argb, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
         quad(pose, vc, argb, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
         quad(pose, vc, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
         quad(pose, vc, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
         quad(pose, vc, argb, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
      });
   }

   public static void drawCylinder(
      EventRender3D e, double cx, double cz, double baseY, double topY, double radius, int colorBottom, int colorTop, int segments, boolean throughWalls
   ) {
      RenderType type = throughWalls ? EspRenderTypes.filledThroughWalls() : RenderTypes.debugFilledBox();
      Vec3 cam = e.getCamera().pos;
      ensureCircleTable(segments);
      float[] tcos = circleCos;
      float[] tsin = circleSin;
      e.getCollector().order(1000).submitCustomGeometry(e.getPose(), type, (pose, vc) -> {
         float by = (float)(baseY - cam.y);
         float ty = (float)(topY - cam.y);

         for (int i = 0; i < segments; i++) {
            int j = i + 1 == segments ? 0 : i + 1;
            float x0 = (float)(cx + tcos[i] * radius - cam.x);
            float z0 = (float)(cz + tsin[i] * radius - cam.z);
            float x1 = (float)(cx + tcos[j] * radius - cam.x);
            float z1 = (float)(cz + tsin[j] * radius - cam.z);
            vc.addVertex(pose, x0, by, z0).setColor(colorBottom);
            vc.addVertex(pose, x1, by, z1).setColor(colorBottom);
            vc.addVertex(pose, x1, ty, z1).setColor(colorTop);
            vc.addVertex(pose, x0, ty, z0).setColor(colorTop);
         }
      });
   }

   private static void ensureCircleTable(int segments) {
      if (circleSegments == segments) return;

      float[] cos = new float[segments];
      float[] sin = new float[segments];

      for (int i = 0; i < segments; i++) {
         double a = (double)i / segments * Math.PI * 2.0;
         cos[i] = (float)Math.cos(a);
         sin[i] = (float)Math.sin(a);
      }

      circleCos = cos;
      circleSin = sin;
      circleSegments = segments;
   }

   private static void quad(
      Pose pose,
      VertexConsumer vc,
      int argb,
      float ax,
      float ay,
      float az,
      float bx,
      float by,
      float bz,
      float cx,
      float cy,
      float cz,
      float dx,
      float dy,
      float dz
   ) {
      vc.addVertex(pose, ax, ay, az).setColor(argb);
      vc.addVertex(pose, bx, by, bz).setColor(argb);
      vc.addVertex(pose, cx, cy, cz).setColor(argb);
      vc.addVertex(pose, dx, dy, dz).setColor(argb);
   }

   public static void drawTexturedBillboard(EventRender3D e, Vec3 worldCenter, float size, float rotationDeg, RenderType type, int c1, int c2, int c3, int c4) {
      if (type == null) return;

      CameraRenderState cam = e.getCamera();
      Vec3 camPos = cam.pos;
      Quaternionf orient = cam.orientation;
      float cx = (float)(worldCenter.x - camPos.x);
      float cy = (float)(worldCenter.y - camPos.y);
      float cz = (float)(worldCenter.z - camPos.z);
      Vector3f right = orient.transform(1.0F, 0.0F, 0.0F, new Vector3f());
      Vector3f up = orient.transform(0.0F, 1.0F, 0.0F, new Vector3f());
      float rad = (float)Math.toRadians(rotationDeg);
      float cos = (float)Math.cos(rad);
      float sin = (float)Math.sin(rad);
      Vector3f r = new Vector3f(right).mul(cos).add(new Vector3f(up).mul(sin)).mul(size);
      Vector3f u = new Vector3f(up).mul(cos).sub(new Vector3f(right).mul(sin)).mul(size);
      e.getCollector().order(1000).submitCustomGeometry(e.getPose(), type, (pose, vc) -> {
         billVert(pose, vc, cx - r.x - u.x, cy - r.y - u.y, cz - r.z - u.z, 0.0F, 1.0F, c1);
         billVert(pose, vc, cx + r.x - u.x, cy + r.y - u.y, cz + r.z - u.z, 1.0F, 1.0F, c2);
         billVert(pose, vc, cx + r.x + u.x, cy + r.y + u.y, cz + r.z + u.z, 1.0F, 0.0F, c3);
         billVert(pose, vc, cx - r.x + u.x, cy - r.y + u.y, cz - r.z + u.z, 0.0F, 0.0F, c4);
      });
   }

   public static void drawTexturedBillboards(EventRender3D e, RenderType type, BillboardBatch batch) {
      if (type == null) return;

      CameraRenderState cam = e.getCamera();
      Vec3 camPos = cam.pos;
      Quaternionf orient = cam.orientation;
      Vector3f right = orient.transform(1.0F, 0.0F, 0.0F, new Vector3f());
      Vector3f up = orient.transform(0.0F, 1.0F, 0.0F, new Vector3f());
      e.getCollector()
         .order(1000)
         .submitCustomGeometry(e.getPose(), type, (pose, vc) -> batch.emit(new BillboardWriter(pose, vc, camPos, right, up)));
   }

   public static void drawTexturedGround(EventRender3D e, Vec3 worldCenter, float radius, float rotationDeg, RenderType type, int c1, int c2, int c3, int c4) {
      drawTexturedGrounds(e, type, w -> w.quad(worldCenter, radius, rotationDeg, c1, c2, c3, c4));
   }

   public static void drawTexturedGrounds(EventRender3D e, RenderType type, GroundBatch batch) {
      if (type == null) return;

      Vec3 camPos = e.getCamera().pos;
      e.getCollector().order(1000).submitCustomGeometry(e.getPose(), type, (pose, vc) -> batch.emit(new GroundWriter(pose, vc, camPos)));
   }

   private static void billVert(Pose pose, VertexConsumer vc, float x, float y, float z, float u, float v, int argb) {
      vc.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setLight(15728880);
   }

   @FunctionalInterface
   public interface BillboardBatch {
      void emit(BillboardWriter writer);
   }

   public static final class BillboardWriter {
      private final Pose pose;
      private final VertexConsumer vc;
      private final double camX;
      private final double camY;
      private final double camZ;
      private final float rx;
      private final float ry;
      private final float rz;
      private final float ux;
      private final float uy;
      private final float uz;

      private BillboardWriter(Pose pose, VertexConsumer vc, Vec3 cam, Vector3f right, Vector3f up) {
         this.pose = pose;
         this.vc = vc;
         this.camX = cam.x;
         this.camY = cam.y;
         this.camZ = cam.z;
         this.rx = right.x;
         this.ry = right.y;
         this.rz = right.z;
         this.ux = up.x;
         this.uy = up.y;
         this.uz = up.z;
      }

      public void quad(double worldX, double worldY, double worldZ, float size, float rotationDeg, int argb) {
         float cx = (float)(worldX - this.camX);
         float cy = (float)(worldY - this.camY);
         float cz = (float)(worldZ - this.camZ);
         float rad = rotationDeg * (float) (Math.PI / 180.0);
         float cos = Mth.cos(rad) * size;
         float sin = Mth.sin(rad) * size;
         float arx = this.rx * cos + this.ux * sin;
         float ary = this.ry * cos + this.uy * sin;
         float arz = this.rz * cos + this.uz * sin;
         float aux = this.ux * cos - this.rx * sin;
         float auy = this.uy * cos - this.ry * sin;
         float auz = this.uz * cos - this.rz * sin;
         Render3DUtil.billVert(this.pose, this.vc, cx - arx - aux, cy - ary - auy, cz - arz - auz, 0.0F, 1.0F, argb);
         Render3DUtil.billVert(this.pose, this.vc, cx + arx - aux, cy + ary - auy, cz + arz - auz, 1.0F, 1.0F, argb);
         Render3DUtil.billVert(this.pose, this.vc, cx + arx + aux, cy + ary + auy, cz + arz + auz, 1.0F, 0.0F, argb);
         Render3DUtil.billVert(this.pose, this.vc, cx - arx + aux, cy - ary + auy, cz - arz + auz, 0.0F, 0.0F, argb);
      }
   }

   @FunctionalInterface
   public interface GroundBatch {
      void emit(GroundWriter writer);
   }

   public static final class GroundWriter {
      private final Pose pose;
      private final VertexConsumer vc;
      private final double camX;
      private final double camY;
      private final double camZ;

      private GroundWriter(Pose pose, VertexConsumer vc, Vec3 cam) {
         this.pose = pose;
         this.vc = vc;
         this.camX = cam.x;
         this.camY = cam.y;
         this.camZ = cam.z;
      }

      public void quad(Vec3 worldCenter, float radius, float rotationDeg, int c1, int c2, int c3, int c4) {
         float cx = (float)(worldCenter.x - this.camX);
         float cy = (float)(worldCenter.y - this.camY);
         float cz = (float)(worldCenter.z - this.camZ);
         float rad = rotationDeg * (float) (Math.PI / 180.0);
         float cos = Mth.cos(rad) * radius;
         float sin = Mth.sin(rad) * radius;
         float fx = -sin;
         Render3DUtil.billVert(this.pose, this.vc, cx - cos - fx, cy, cz - sin - cos, 0.0F, 1.0F, c1);
         Render3DUtil.billVert(this.pose, this.vc, cx + cos - fx, cy, cz + sin - cos, 1.0F, 1.0F, c2);
         Render3DUtil.billVert(this.pose, this.vc, cx + cos + fx, cy, cz + sin + cos, 1.0F, 0.0F, c3);
         Render3DUtil.billVert(this.pose, this.vc, cx - cos + fx, cy, cz - sin + cos, 0.0F, 0.0F, c4);
      }
   }

   @FunctionalInterface
   public interface LineBatch {
      void emit(LineWriter writer);
   }

   public static final class LineWriter {
      private final Pose pose;
      private final VertexConsumer vc;
      private final Vec3 cam;

      private LineWriter(Pose pose, VertexConsumer vc, Vec3 cam) {
         this.pose = pose;
         this.vc = vc;
         this.cam = cam;
      }

      public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
         Render3DUtil.line(this.pose, this.vc, this.cam, x1, y1, z1, x2, y2, z2, argb);
      }
   }
}
