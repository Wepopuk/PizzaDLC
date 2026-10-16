package pizza.dlc.api.util.render.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pizza.dlc.PizzaDLC;

public final class VerityMesh {
   private static final float CENTER_Y = 0.75f;
   private static final float UNIT = 2.2f / 16.0f;

   private static float[] triangles;
   private static boolean parsed;

   private VerityMesh() {
   }

   public static void submit(AvatarRenderState state, PoseStack pose, SubmitNodeCollector collector,
      int light, int overlay, Identifier texture, float scale) {
      float[] data = geometry();
      if (data.length == 0) {
         return;
      }
      pose.pushPose();
      pose.translate(0.0f, CENTER_Y, 0.0f);
      pose.scale(scale, scale, scale);
      collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, consumer) -> {
         for (int triangle = 0; triangle < data.length; triangle += 24) {
            vertex(matrix, consumer, data, triangle, light, overlay);
            vertex(matrix, consumer, data, triangle + 8, light, overlay);
            vertex(matrix, consumer, data, triangle + 16, light, overlay);
            vertex(matrix, consumer, data, triangle + 16, light, overlay);
         }
      });
      pose.popPose();
   }

   private static synchronized float[] geometry() {
      if (!parsed) {
         parsed = true;
         triangles = parse();
      }
      return triangles;
   }

   private static float[] parse() {
      try (InputStream stream = VerityMesh.class.getResourceAsStream("/assets/pizzadlc/models/verity/verity.bbmodel")) {
         if (stream == null) {
            return new float[0];
         }
         JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
         float uvWidth = 64.0f;
         float uvHeight = 64.0f;
         if (root.has("resolution")) {
            JsonObject resolution = root.getAsJsonObject("resolution");
            uvWidth = resolution.get("width").getAsFloat();
            uvHeight = resolution.get("height").getAsFloat();
         }
         List<Float> out = new ArrayList<>(1 << 16);
         for (JsonElement entry : root.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            if (!element.has("vertices") || !element.has("faces")) {
               continue;
            }
            float[] origin = triple(element, "origin");
            float[] rotation = triple(element, "rotation");
            JsonObject vertices = element.getAsJsonObject("vertices");
            for (Map.Entry<String, JsonElement> face : element.getAsJsonObject("faces").entrySet()) {
               JsonObject data = face.getValue().getAsJsonObject();
               JsonArray order = data.getAsJsonArray("vertices");
               JsonObject uv = data.getAsJsonObject("uv");
               int corners = order.size();
               if (corners < 3) {
                  continue;
               }
               for (int corner = 2; corner < corners; corner++) {
                  appendTriangle(out, vertices, uv, origin, rotation, uvWidth, uvHeight,
                     order.get(0).getAsString(), order.get(corner - 1).getAsString(), order.get(corner).getAsString());
               }
            }
         }
         float[] packed = new float[out.size()];
         for (int i = 0; i < packed.length; i++) {
            packed[i] = out.get(i).floatValue();
         }
         return packed;
      } catch (Exception exception) {
         PizzaDLC.LOGGER.warn("Failed to read Verity model", exception);
         return new float[0];
      }
   }

   private static void appendTriangle(List<Float> out, JsonObject vertices, JsonObject uv, float[] origin,
      float[] rotation, float uvWidth, float uvHeight,
      String first, String second, String third) {
      float[][] points = {
         point(vertices, origin, rotation, first),
         point(vertices, origin, rotation, second),
         point(vertices, origin, rotation, third)
      };
      if (points[0] == null || points[1] == null || points[2] == null) {
         return;
      }
      float[] normal = normal(points[0], points[1], points[2]);
      String[] ids = {first, second, third};
      for (int corner = 0; corner < 3; corner++) {
         float[] position = points[corner];
         float[] texture = uvOf(uv, ids[corner], uvWidth, uvHeight);
         out.add(Float.valueOf(position[0]));
         out.add(Float.valueOf(position[1]));
         out.add(Float.valueOf(position[2]));
         out.add(Float.valueOf(texture[0]));
         out.add(Float.valueOf(texture[1]));
         out.add(Float.valueOf(normal[0]));
         out.add(Float.valueOf(normal[1]));
         out.add(Float.valueOf(normal[2]));
      }
   }

   private static float[] point(JsonObject vertices, float[] origin, float[] rotation, String id) {
      if (!vertices.has(id)) {
         return null;
      }
      JsonArray raw = vertices.getAsJsonArray(id);
      float x = raw.get(0).getAsFloat() - origin[0];
      float y = raw.get(1).getAsFloat() - origin[1];
      float z = raw.get(2).getAsFloat() - origin[2];

      float[] rotated = rotate(x, y, z, rotation);
      x = rotated[0] + origin[0];
      y = rotated[1] + origin[1];
      z = rotated[2] + origin[2];

      return new float[]{x * UNIT, -y * UNIT, z * UNIT};
   }

   private static float[] rotate(float x, float y, float z, float[] rotation) {
      float rx = (float) Math.toRadians(rotation[0]);
      float ry = (float) Math.toRadians(rotation[1]);
      float rz = (float) Math.toRadians(rotation[2]);
      float cos = (float) Math.cos(rx);
      float sin = (float) Math.sin(rx);
      float y1 = y * cos - z * sin;
      float z1 = y * sin + z * cos;
      cos = (float) Math.cos(ry);
      sin = (float) Math.sin(ry);
      float x2 = x * cos + z1 * sin;
      float z2 = -x * sin + z1 * cos;
      cos = (float) Math.cos(rz);
      sin = (float) Math.sin(rz);
      float x3 = x2 * cos - y1 * sin;
      float y3 = x2 * sin + y1 * cos;
      return new float[]{x3, y3, z2};
   }

   private static float[] uvOf(JsonObject uv, String id, float uvWidth, float uvHeight) {
      if (uv == null || !uv.has(id)) {
         return new float[]{0.0f, 0.0f};
      }
      JsonArray raw = uv.getAsJsonArray(id);
      return new float[]{raw.get(0).getAsFloat() / uvWidth, raw.get(1).getAsFloat() / uvHeight};
   }

   private static float[] normal(float[] a, float[] b, float[] c) {
      float ux = b[0] - a[0];
      float uy = b[1] - a[1];
      float uz = b[2] - a[2];
      float vx = c[0] - a[0];
      float vy = c[1] - a[1];
      float vz = c[2] - a[2];
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (length < 1.0e-6f) {
         return new float[]{0.0f, -1.0f, 0.0f};
      }
      return new float[]{nx / length, ny / length, nz / length};
   }

   private static float[] triple(JsonObject element, String key) {
      if (!element.has(key)) {
         return new float[]{0.0f, 0.0f, 0.0f};
      }
      JsonArray raw = element.getAsJsonArray(key);
      return new float[]{raw.get(0).getAsFloat(), raw.get(1).getAsFloat(), raw.get(2).getAsFloat()};
   }

   private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float[] data, int i, int light, int overlay) {
      consumer.addVertex(pose, data[i], data[i + 1], data[i + 2])
         .setColor(-1).setUv(data[i + 3], data[i + 4])
         .setOverlay(overlay).setLight(light)
         .setNormal(pose, data[i + 5], data[i + 6], data[i + 7]);
   }
}
