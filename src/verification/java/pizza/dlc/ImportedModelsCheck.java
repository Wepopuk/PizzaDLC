package pizza.dlc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.joml.Matrix4f;
import pizza.dlc.api.util.render.model.SkinnedMeshData;

/** Checks actual bundled geometry, material images and weighted limb deformation. */
public final class ImportedModelsCheck {
   private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }

   public static void main(String[] args) throws Exception {
      for (String id : new String[]{"putin", "boykisser"}) {
         SkinnedMeshData mesh = SkinnedMeshData.read(ImportedModelsCheck.class.getResourceAsStream("/assets/pizzadlc/models/" + id + "/mesh.bin"));
         check(mesh.pivots()[6] < 0 && mesh.pivots()[9] > 0, id + " arm pivots swapped");
         float[] coverage = new float[6];
         float minimumY = Float.MAX_VALUE, maximumY = -Float.MAX_VALUE;
         float minimumX = Float.MAX_VALUE, maximumX = -Float.MAX_VALUE;
         int vertices = 0, triangles = 0, movedLegVertices = 0;
         Matrix4f[] identity = new Matrix4f[6], walking = new Matrix4f[6];
         for (int part = 0; part < 6; part++) { identity[part] = new Matrix4f(); walking[part] = new Matrix4f(); }
         walking[4].translate(mesh.pivots()[12], mesh.pivots()[13], mesh.pivots()[14]).rotateX(.7F)
            .translate(-mesh.pivots()[12], -mesh.pivots()[13], -mesh.pivots()[14]);
         for (SkinnedMeshData.Batch batch : mesh.batches()) {
            try (var stream = ImportedModelsCheck.class.getResourceAsStream("/assets/pizzadlc/models/" + id + "/material_" + batch.material() + ".png")) {
               check(stream != null && ImageIO.read(stream) != null, id + " missing material " + batch.material());
            }
            float[] data = batch.vertices();
            float[] rest = new float[batch.vertexCount() * 8], posed = new float[rest.length];
            SkinnedMeshData.deform(batch, identity, rest);
            SkinnedMeshData.deform(batch, walking, posed);
            for (int vertex = 0; vertex < batch.vertexCount(); vertex++) {
               int i = vertex * SkinnedMeshData.STRIDE, o = vertex * 8;
               for (int k = 0; k < 5; k++) check(Math.abs(rest[o + k] - data[i + k]) < .00001F, id + " identity pose changed geometry/UV");
               for (int k = 0; k < 8; k++) check(Float.isFinite(posed[o + k]), id + " invalid walking pose");
               float length = (float)Math.sqrt(posed[o + 5] * posed[o + 5] + posed[o + 6] * posed[o + 6] + posed[o + 7] * posed[o + 7]);
               check(Math.abs(length - 1) < .001F, id + " invalid posed normal");
               float movement = Math.abs(rest[o] - posed[o]) + Math.abs(rest[o + 1] - posed[o + 1]) + Math.abs(rest[o + 2] - posed[o + 2]);
               if (data[i + 12] > .9F && movement > .01F) movedLegVertices++;
               if (data[i + 12] == 0) check(movement < .00001F, id + " right-leg motion leaked to unrelated vertices");
               for (int part = 0; part < 6; part++) coverage[part] += data[i + 8 + part];
               minimumX = Math.min(minimumX, data[i]); maximumX = Math.max(maximumX, data[i]);
               minimumY = Math.min(minimumY, data[i + 1]); maximumY = Math.max(maximumY, data[i + 1]);
            }
            vertices += batch.vertexCount(); triangles += batch.indices().length / 3;
         }
         check(Math.abs(maximumY - 1.5F) < .0001F && Math.abs(maximumY - minimumY - 1.8F) < .0001F, id + " not grounded/normalized");
         check(minimumX < 0 && maximumX > 0 && Math.abs(minimumX + maximumX) < .05F, id + " off-centre geometry");
         for (int part = 0; part < 6; part++) check(coverage[part] > 5, id + " empty body part " + part);
         check(movedLegVertices > 10, id + " walking did not deform the leg");
         check(vertices == (id.equals("putin") ? 6290 : 41338), id + " lost source vertices");
         check(triangles == (id.equals("putin") ? 11003 : 59916), id + " lost source triangles");
         System.out.println("PASS: " + id + " geometry, materials, centred scale, all six bones, walking and normalized normals");
      }
      checkRigidModel();
      try {
         SkinnedMeshData.read(new ByteArrayInputStream(new byte[]{0, 0, 0, 0}));
         throw new AssertionError("Invalid mesh signature accepted");
      } catch (IOException expected) { }
   }

   private static void checkRigidModel() throws Exception {
      String folder = "/assets/pizzadlc/models/hitler/";
      SkinnedMeshData mesh = SkinnedMeshData.read(ImportedModelsCheck.class.getResourceAsStream(folder + "mesh.bin"));
      float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
      float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
      int triangles = 0;
      check(mesh.batches().size() == 1, "STL should use one material");
      for (SkinnedMeshData.Batch batch : mesh.batches()) {
         try (var stream = ImportedModelsCheck.class.getResourceAsStream(folder + "material_" + batch.material() + ".png")) {
            check(stream != null && ImageIO.read(stream) != null, "STL material missing/invalid");
         }
         float[] data = batch.vertices();
         for (int vertex = 0; vertex < batch.vertexCount(); vertex++) {
            int i = vertex * SkinnedMeshData.STRIDE;
            minX = Math.min(minX, data[i]); maxX = Math.max(maxX, data[i]);
            minY = Math.min(minY, data[i + 1]); maxY = Math.max(maxY, data[i + 1]);
            float length = (float)Math.sqrt(data[i + 5] * data[i + 5] + data[i + 6] * data[i + 6] + data[i + 7] * data[i + 7]);
            check(Math.abs(length - 1) < .001F, "STL normal is not normalized");
            check(data[i + 8] == 1, "STL vertex is not rigid");
            for (int part = 1; part < 6; part++) check(data[i + 8 + part] == 0, "STL has unexpected limb weights");
         }
         triangles += batch.indices().length / 3;
      }
      check(triangles > 10_000 && triangles <= 40_000, "STL simplification outside geometry budget");
      check(Math.abs(maxY - 1.5F) < .0001F && Math.abs(maxY - minY - 1.8F) < .0001F, "STL not grounded/normalized");
      check(minX < 0 && maxX > 0 && Math.abs(minX + maxX) < .0001F, "STL is off centre");
      System.out.println("PASS: hitler rigid geometry, material, centred scale, normals and " + triangles + " triangle budget");
   }
}
