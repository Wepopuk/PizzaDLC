package pizza.dlc.api.util.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class ProjectionUtil {
   private static final Matrix4f MVP = new Matrix4f();
   private static double camX;
   private static double camY;
   private static double camZ;
   private static int winW;
   private static int winH;
   private static int guiScale = 1;
   private static boolean valid;
   private static final Vector4f SCRATCH = new Vector4f();

   private ProjectionUtil() {
   }

   public static void update(CameraRenderState cam, Window win) {
      Matrix4f projection = new Matrix4f(cam.projectionMatrix);
      applyViewBobbing(cam, projection);
      projection.mul(cam.viewRotationMatrix);
      MVP.set(projection);
      camX = cam.pos.x;
      camY = cam.pos.y;
      camZ = cam.pos.z;
      winW = win.getWidth();
      winH = win.getHeight();
      guiScale = Math.max(1, win.getGuiScale());
      valid = true;
   }

   private static void applyViewBobbing(CameraRenderState cam, Matrix4f projection) {
      if (!(Boolean)Minecraft.getInstance().options.bobView().get() || !cam.entityRenderState.isPlayer) return;

      if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
         float walk = cam.entityRenderState.backwardsInterpolatedWalkDistance;
         float bob = cam.entityRenderState.bob;
         if (!(Math.abs(bob) <= 1.0E-5F)) {
            PoseStack bobStack = new PoseStack();
            bobStack.translate(Mth.sin(walk * (float) Math.PI) * bob * 0.5F, -Math.abs(Mth.cos(walk * (float) Math.PI) * bob), 0.0F);
            bobStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(walk * (float) Math.PI) * bob * 3.0F));
            bobStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(walk * (float) Math.PI - 0.2F) * bob) * 5.0F));
            projection.mul(bobStack.last().pose());
         }
      }
   }

   @Nullable
   public static Vector2f worldToScreen(Vec3 world) {
      Vector2f out = new Vector2f();
      return worldToScreen(world.x, world.y, world.z, out) ? out : null;
   }

   public static boolean worldToScreen(double wx, double wy, double wz, Vector2f dest) {
      if (!valid) {
         return false;
      }

      SCRATCH.set((float)(wx - camX), (float)(wy - camY), (float)(wz - camZ), 1.0F);
      MVP.transform(SCRATCH);
      if (SCRATCH.w <= 1.0E-5F) {
         return false;
      }

      float ndcX = SCRATCH.x / SCRATCH.w;
      float ndcY = SCRATCH.y / SCRATCH.w;
      float sx = (ndcX * 0.5F + 0.5F) * winW;
      float sy = (1.0F - (ndcY * 0.5F + 0.5F)) * winH;
      dest.set(sx / guiScale, sy / guiScale);
      return true;
   }
}
