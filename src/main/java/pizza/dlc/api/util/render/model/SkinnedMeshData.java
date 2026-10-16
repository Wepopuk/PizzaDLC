package pizza.dlc.api.util.render.model;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

/** Indexed positions, UVs, normals and six normalized limb weights per vertex. */
public record SkinnedMeshData(float[] pivots, float[] hands, List<Batch> batches) {
   public static final int STRIDE = 14;
   public record Batch(int material, float[] vertices, int[] indices) {
      public int vertexCount() { return vertices.length / STRIDE; }
   }

   public static SkinnedMeshData read(InputStream stream) throws IOException {
      if (stream == null) throw new IOException("Missing skinned mesh resource");
      try (DataInputStream in = new DataInputStream(stream)) {
         if (in.readInt() != 0x43534D31) throw new IOException("Invalid mesh signature");
         float[] pivots = readFloats(in, 18);
         float[] hands = readFloats(in, 6);
         int count = in.readInt();
         if (count < 1 || count > 64) throw new IOException("Invalid batch count");
         List<Batch> batches = new ArrayList<>(count);
         int totalVertices = 0;
         for (int b = 0; b < count; b++) {
            int material = in.readInt();
            int vertices = in.readInt();
            int indices = in.readInt();
            totalVertices += vertices;
            if (material < 0 || material > 63 || vertices < 1 || vertices > 200_000
               || totalVertices > 400_000 || indices < 3 || indices > 1_000_000 || indices % 3 != 0) {
               throw new IOException("Invalid mesh counts");
            }
            float[] data = readFloats(in, vertices * STRIDE);
            for (int vertex = 0; vertex < vertices; vertex++) {
               float sum = 0;
               for (int part = 0; part < 6; part++) {
                  float weight = data[vertex * STRIDE + 8 + part];
                  if (weight < 0 || weight > 1.0001F) throw new IOException("Invalid skin weight");
                  sum += weight;
               }
               if (Math.abs(sum - 1) > .001F) throw new IOException("Unnormalized skin weights");
            }
            int[] order = new int[indices];
            for (int i = 0; i < indices; i++) {
               order[i] = in.readInt();
               if (order[i] < 0 || order[i] >= vertices) throw new IOException("Invalid mesh index");
            }
            batches.add(new Batch(material, data, order));
         }
         if (in.read() != -1) throw new IOException("Unexpected mesh trailing data");
         return new SkinnedMeshData(pivots, hands, List.copyOf(batches));
      }
   }

   private static float[] readFloats(DataInputStream in, int count) throws IOException {
      float[] values = new float[count];
      for (int i = 0; i < count; i++) {
         values[i] = in.readFloat();
         if (!Float.isFinite(values[i])) throw new IOException("Nonfinite mesh coordinate");
      }
      return values;
   }

   /** Called inside the deferred draw, with a reusable per-thread scratch array. */
   public static void deform(Batch batch, Matrix4f[] transforms, float[] out) {
      float[] data = batch.vertices();
      for (int vertex = 0; vertex < batch.vertexCount(); vertex++) {
         int i = vertex * STRIDE;
         float x = data[i], y = data[i + 1], z = data[i + 2];
         float nx = data[i + 5], ny = data[i + 6], nz = data[i + 7];
         float px = 0, py = 0, pz = 0, tx = 0, ty = 0, tz = 0;
         for (int part = 0; part < 6; part++) {
            float w = data[i + 8 + part];
            if (w == 0) continue;
            Matrix4f m = transforms[part];
            px += w * (m.m00() * x + m.m10() * y + m.m20() * z + m.m30());
            py += w * (m.m01() * x + m.m11() * y + m.m21() * z + m.m31());
            pz += w * (m.m02() * x + m.m12() * y + m.m22() * z + m.m32());
            tx += w * (m.m00() * nx + m.m10() * ny + m.m20() * nz);
            ty += w * (m.m01() * nx + m.m11() * ny + m.m21() * nz);
            tz += w * (m.m02() * nx + m.m12() * ny + m.m22() * nz);
         }
         float length = (float)Math.sqrt(tx * tx + ty * ty + tz * tz);
         if (length < 1.0E-6F) { tx = nx; ty = ny; tz = nz; length = 1; }
         int o = vertex * 8;
         out[o] = px; out[o + 1] = py; out[o + 2] = pz;
         out[o + 3] = data[i + 3]; out[o + 4] = data[i + 4];
         out[o + 5] = tx / length; out[o + 6] = ty / length; out[o + 7] = tz / length;
      }
   }
}
