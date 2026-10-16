package pizza.dlc.api.rotation.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public final class NeuroEngine {
   private static final String MODEL_PATH = "/assets/pizzadlc/neuro/model.onnx";
   private static final String NORM_PATH = "/assets/pizzadlc/neuro/norm.json";
   private static final long BUDGET_NANOS = 2000000L;
   private static final int MAX_FAILURES = 3;
   private static final int MAX_SLOW_TICKS = 20;
   private static final NeuroEngine INSTANCE = new NeuroEngine();
   private final ReentrantLock lock = new ReentrantLock();
   private volatile OrtSession session;
   private volatile NeuroNorm norm;
   private volatile String failure;
   private volatile boolean loading;
   private OrtEnvironment env;
   private int failures;
   private int slowTicks;

   public static NeuroEngine get() {
      return INSTANCE;
   }

   private NeuroEngine() {
   }

   public void ensureLoading() {
      if (this.session != null || this.loading || this.failure != null) return;

      synchronized (this) {
         if (this.session != null || this.loading || this.failure != null) {
            return;
         }

         this.loading = true;
      }

      Thread thread = new Thread(this::load, "pizzadlc-neuro-load");
      thread.setDaemon(true);
      thread.start();
   }

   public boolean ready() {
      return this.session != null && this.norm != null;
   }

   public String failure() {
      return this.failure;
   }

   public NeuroNorm norm() {
      return this.norm;
   }

   public boolean run(float[] x, float[] hidden, float[] out) {
      OrtSession local = this.session;
      if (local == null) {
         return false;
      }

      this.lock.lock();

      try {
         if (this.session == null) {
            return false;
         }

         long started = System.nanoTime();
         float[][][] xIn = new float[][][]{{x}};
         float[][][] hIn = new float[][][]{{hidden}};
         OnnxTensor xt = OnnxTensor.createTensor(this.env, xIn);

         try {
            OnnxTensor ht = OnnxTensor.createTensor(this.env, hIn);

            try {
               Map<String, OnnxTensor> inputs = new HashMap<>(4);
               inputs.put("x", xt);
               inputs.put("h", ht);
               Result result = this.session.run(inputs);

               try {
                  float[][][] y = (float[][][])((OnnxValue)result.get("y").orElseThrow()).getValue();
                  float[][][] hOut = (float[][][])((OnnxValue)result.get("h_out").orElseThrow()).getValue();
                  out[0] = y[0][0][0];
                  out[1] = y[0][0][1];
                  System.arraycopy(hOut[0][0], 0, hidden, 0, hidden.length);
               } catch (Throwable ex) {
                  if (result != null) {
                     try {
                        result.close();
                     } catch (Throwable ex2) {
                        ex.addSuppressed(ex2);
                     }
                  }

                  throw ex;
               }

               if (result != null) {
                  result.close();
               }
            } catch (Throwable ex) {
               if (ht != null) {
                  try {
                     ht.close();
                  } catch (Throwable ex2) {
                     ex.addSuppressed(ex2);
                  }
               }

               throw ex;
            }

            if (ht != null) {
               ht.close();
            }
         } catch (Throwable ex) {
            if (xt != null) {
               try {
                  xt.close();
               } catch (Throwable ex2) {
                  ex.addSuppressed(ex2);
               }
            }

            throw ex;
         }

         if (xt != null) {
            xt.close();
         }

         this.failures = 0;
         this.slowTicks = System.nanoTime() - started > BUDGET_NANOS ? this.slowTicks + 1 : 0;
         if (this.slowTicks < MAX_SLOW_TICKS) {
            return true;
         }

         this.disable("инференс дольше 2 мс 20 тиков подряд");
         return false;
      } catch (Throwable ex) {
         if (++this.failures >= MAX_FAILURES) {
            this.disable("ошибка инференса: " + ex.getMessage());
         }

         return false;
      } finally {
         this.lock.unlock();
      }
   }

   public void shutdown() {
      this.lock.lock();

      try {
         this.closeSession();
      } finally {
         this.lock.unlock();
      }
   }

   private void load() {
      try {
         NeuroNorm parsed;
         try (InputStream in = NeuroEngine.class.getResourceAsStream(NORM_PATH)) {
            if (in == null) {
               throw new IllegalStateException("нет ресурса /assets/pizzadlc/neuro/norm.json");
            }

            parsed = NeuroNorm.load(in);
         }

         byte[] model = readAll(MODEL_PATH);
         OrtEnvironment environment = OrtEnvironment.getEnvironment();
         SessionOptions options = new SessionOptions();
         options.setIntraOpNumThreads(1);
         OrtSession created = environment.createSession(model, options);
         this.env = environment;
         this.norm = parsed;
         this.session = created;
      } catch (Throwable ex) {
         this.failure = String.valueOf(ex.getMessage());
      } finally {
         this.loading = false;
      }
   }

   private static byte[] readAll(String path) throws Exception {
      try (InputStream in = NeuroEngine.class.getResourceAsStream(path)) {
         if (in == null) {
            throw new IllegalStateException("нет ресурса " + path);
         }

         ByteArrayOutputStream out = new ByteArrayOutputStream(131072);
         byte[] chunk = new byte[8192];

         int read;
         while ((read = in.read(chunk)) > 0) {
            out.write(chunk, 0, read);
         }

         return out.toByteArray();
      }
   }

   private void disable(String reason) {
      this.failure = reason;
      this.closeSession();
   }

   private void closeSession() {
      OrtSession local = this.session;
      this.session = null;
      if (local != null) {
         try {
            local.close();
         } catch (Throwable ignored) {
         }
      }
   }
}
