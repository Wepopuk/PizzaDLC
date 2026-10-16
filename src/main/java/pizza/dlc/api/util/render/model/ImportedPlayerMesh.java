package pizza.dlc.api.util.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import pizza.dlc.PizzaDLC;
import pizza.dlc.api.emote.EmoteEngine;
import pizza.dlc.api.emote.EmotePart;
import pizza.dlc.api.emote.EmotePoseCarrier;

/** Renderer for offline-imported skinned GLB models and rigid STL geometry. */
public final class ImportedPlayerMesh {
   private static final Map<String, SkinnedMeshData> CACHE = new ConcurrentHashMap<>();
   private static final Map<String, Identifier[]> TEXTURES = new ConcurrentHashMap<>();
   private static final ThreadLocal<float[]> SCRATCH = ThreadLocal.withInitial(() -> new float[0]);
   private static final EmotePart[] PARTS = {EmotePart.BODY, EmotePart.HEAD, EmotePart.RIGHT_ARM,
      EmotePart.LEFT_ARM, EmotePart.RIGHT_LEG, EmotePart.LEFT_LEG};
   private static final float[][] DEFAULT_POS = {{0, 0, 0}, {0, 0, 0}, {-5, 2, 0}, {5, 2, 0}, {-1.9F, 12, 0}, {1.9F, 12, 0}};

   private ImportedPlayerMesh() { }

   private static SkinnedMeshData load(String id) {
      try {
         return SkinnedMeshData.read(ImportedPlayerMesh.class.getResourceAsStream("/assets/pizzadlc/models/" + id + "/mesh.bin"));
      } catch (IOException exception) {
         PizzaDLC.LOGGER.warn("Failed to load imported player model {}", id, exception);
         return new SkinnedMeshData(new float[18], new float[6], java.util.List.of());
      }
   }

   public static boolean submit(String id, AvatarRenderState state, PoseStack pose,
      SubmitNodeCollector collector, int light, int overlay, float scale) {
      SkinnedMeshData data = CACHE.computeIfAbsent(id, ImportedPlayerMesh::load);
      if (data.batches().isEmpty()) return false;
      Identifier[] textures = TEXTURES.computeIfAbsent(id, key -> {
         int count = data.batches().stream().mapToInt(SkinnedMeshData.Batch::material).max().orElse(0) + 1;
         Identifier[] all = new Identifier[count];
         for (int i = 0; i < count; i++) all[i] = Identifier.fromNamespaceAndPath("pizzadlc", "models/" + key + "/material_" + i + ".png");
         return all;
      });
      boolean rigid = "hitler".equals(id);
      Matrix4f[] transforms = rigid ? null : transforms(data, state);
      pose.pushPose();
      // Scale around the soles, keeping the model grounded at any size.
      pose.translate(0, 1.5F, 0);
      pose.scale(scale, scale, scale);
      pose.translate(0, -1.5F, 0);
      for (SkinnedMeshData.Batch batch : data.batches()) {
         collector.submitCustomGeometry(pose, RenderTypes.entityCutout(textures[batch.material()]), (matrix, consumer) -> {
            float[] posed = batch.vertices();
            int stride = SkinnedMeshData.STRIDE;
            if (!rigid) {
               int length = batch.vertexCount() * 8;
               posed = SCRATCH.get();
               if (posed.length < length) { posed = new float[length]; SCRATCH.set(posed); }
               SkinnedMeshData.deform(batch, transforms, posed);
               stride = 8;
            }
            int[] indices = batch.indices();
            for (int triangle = 0; triangle < indices.length; triangle += 3) {
               vertex(matrix, consumer, posed, indices[triangle] * stride, light, overlay);
               vertex(matrix, consumer, posed, indices[triangle + 1] * stride, light, overlay);
               vertex(matrix, consumer, posed, indices[triangle + 2] * stride, light, overlay);
               vertex(matrix, consumer, posed, indices[triangle + 2] * stride, light, overlay);
            }
         });
      }
      if (!rigid) {
         submitItem(state.rightHandItemState, data, transforms[2], pose, collector, light, state.outlineColor, 0);
         submitItem(state.leftHandItemState, data, transforms[3], pose, collector, light, state.outlineColor, 1);
      }
      pose.popPose();
      return true;
   }

   private static Matrix4f[] transforms(SkinnedMeshData data, AvatarRenderState state) {
      float swing = (float)Math.cos(state.walkAnimationPos * .6662F) * 1.2F * Math.min(state.walkAnimationSpeed, 1);
      float[] pitch = {state.isCrouching ? .5F : 0, (float)Math.toRadians(state.xRot), -swing, swing, swing, -swing};
      if (state.attackTime > 0) {
         pitch[state.attackArm == HumanoidArm.RIGHT ? 2 : 3] -= (float)Math.sin(Math.sqrt(state.attackTime) * Math.PI) * 1.3F;
      }
      if (state.isUsingItem) {
         boolean right = (state.useItemHand == InteractionHand.MAIN_HAND) == (state.mainArm == HumanoidArm.RIGHT);
         pitch[right ? 2 : 3] = -.8F;
      }
      EmoteEngine.Pose emote = ((EmotePoseCarrier)state).pizzadlc$getEmotePose();
      if (emote == null) emote = ((EmotePoseCarrier)state).pizzadlc$getMovementPose();
      Matrix4f[] transforms = new Matrix4f[6];
      for (int part = 0; part < 6; part++) {
         float x = 0, y = 0, z = 0, rx = pitch[part];
         float ry = part == 1 ? (float)Math.toRadians(state.yRot) : 0, rz = 0;
         float[] values = emote == null ? null : emote.parts().get(PARTS[part]);
         if (values != null) {
            float weight = emote.weight();
            x = offset(values[0], DEFAULT_POS[part][0], weight);
            y = offset(values[1], DEFAULT_POS[part][1], weight);
            z = offset(values[2], DEFAULT_POS[part][2], weight);
            rx = blend(rx, values[3], weight); ry = blend(ry, values[4], weight); rz = blend(rz, values[5], weight);
         }
         int i = part * 3;
         float px = data.pivots()[i], py = data.pivots()[i + 1], pz = data.pivots()[i + 2];
         transforms[part] = new Matrix4f().translation(px + x, py + y, pz + z)
            .rotateZYX(rz, ry, rx).translate(-px, -py, -pz);
      }
      return transforms;
   }

   private static float blend(float vanilla, float emote, float weight) {
      return Float.isNaN(emote) ? vanilla : vanilla + (emote - vanilla) * weight;
   }

   private static float offset(float value, float vanilla, float weight) {
      return Float.isNaN(value) ? 0 : (value - vanilla) * weight / 16;
   }

   private static void submitItem(ItemStackRenderState item, SkinnedMeshData data, Matrix4f arm,
      PoseStack pose, SubmitNodeCollector collector, int light, int outline, int hand) {
      if (item.isEmpty()) return;
      pose.pushPose();
      pose.mulPose(arm);
      int i = hand * 3;
      pose.translate(data.hands()[i], data.hands()[i + 1], data.hands()[i + 2]);
      pose.mulPose(Axis.XP.rotationDegrees(-90));
      pose.mulPose(Axis.YP.rotationDegrees(180));
      item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, outline);
      pose.popPose();
   }

   private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float[] data, int i, int light, int overlay) {
      consumer.addVertex(pose, data[i], data[i + 1], data[i + 2]).setColor(-1).setUv(data[i + 3], data[i + 4])
         .setOverlay(overlay).setLight(light).setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
   }
}
