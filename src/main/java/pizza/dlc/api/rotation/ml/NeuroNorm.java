package pizza.dlc.api.rotation.ml;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class NeuroNorm {
   public static final int[] SUPPORTED_VERSIONS = new int[]{1, 2};
   public static final int EXPECTED_IN_DIM = 12;
   private final int hidden;
   private final int inDim;
   private final float[] featMean;
   private final float[] featStd;
   private final float[] targetMean;
   private final float[] targetStd;
   private final float[] residualStd;
   private final float[] tau;

   private NeuroNorm(int hidden, int inDim, float[] featMean, float[] featStd, float[] targetMean, float[] targetStd, float[] residualStd, float[] tau) {
      this.hidden = hidden;
      this.inDim = inDim;
      this.featMean = featMean;
      this.featStd = featStd;
      this.targetMean = targetMean;
      this.targetStd = targetStd;
      this.residualStd = residualStd;
      this.tau = tau;
   }

   public static NeuroNorm parse(String json) {
      JsonObject o = JsonParser.parseString(json).getAsJsonObject();
      int version = required(o, "version").getAsInt();
      boolean known = false;

      for (int v : SUPPORTED_VERSIONS) {
         known |= v == version;
      }

      if (!known) {
         throw new IllegalStateException("norm.json: версия контракта " + version + ", поддерживаются " + Arrays.toString(SUPPORTED_VERSIONS));
      }

      int inDim = required(o, "in_dim").getAsInt();
      if (inDim != 12) {
         throw new IllegalStateException("norm.json: признаков " + inDim + ", ожидалось 12");
      }

      return new NeuroNorm(
         required(o, "hidden").getAsInt(),
         inDim,
         floats(o, "feat_mean", inDim),
         floats(o, "feat_std", inDim),
         floats(o, "target_mean", 2),
         floats(o, "target_std", 2),
         floats(o, "residual_std", 2),
         floats(o, "tau", 2)
      );
   }

   public static NeuroNorm load(InputStream in) throws IOException {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      byte[] chunk = new byte[8192];

      int read;
      while ((read = in.read(chunk)) > 0) {
         buf.write(chunk, 0, read);
      }

      return parse(buf.toString(StandardCharsets.UTF_8));
   }

   public void normalize(float[] raw) {
      for (int i = 0; i < this.inDim; i++) {
         float std = this.featStd[i];
         raw[i] = std > 1.0E-6F ? (raw[i] - this.featMean[i]) / std : 0.0F;
      }
   }

   public void denormalizeTarget(float[] y) {
      for (int i = 0; i < y.length && i < 2; i++) {
         y[i] = y[i] * this.targetStd[i] + this.targetMean[i];
      }
   }

   public int hidden() {
      return this.hidden;
   }

   public int inDim() {
      return this.inDim;
   }

   public float[] residualStd() {
      return (float[])this.residualStd.clone();
   }

   public float[] tau() {
      return (float[])this.tau.clone();
   }

   private static JsonElement required(JsonObject o, String key) {
      if (!o.has(key)) {
         throw new IllegalStateException("norm.json: нет поля " + key);
      }

      return o.get(key);
   }

   private static float[] floats(JsonObject o, String key, int expected) {
      JsonArray a = required(o, key).getAsJsonArray();
      if (a.size() != expected) {
         throw new IllegalStateException("norm.json: в " + key + " " + a.size() + " значений, ожидалось " + expected);
      }

      float[] out = new float[expected];

      for (int i = 0; i < expected; i++) {
         out[i] = a.get(i).getAsFloat();
      }

      return out;
   }
}
