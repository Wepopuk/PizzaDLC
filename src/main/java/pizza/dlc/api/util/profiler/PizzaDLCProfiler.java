package pizza.dlc.api.util.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class PizzaDLCProfiler {
   private static volatile boolean enabled;
   private static final Map<String, Sample> SAMPLES = new ConcurrentHashMap<>();
   private static long frameStart;
   private static double frameMsEma;
   private static final double EMA = 0.1;

   private PizzaDLCProfiler() {
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static void setEnabled(boolean value) {
      if (enabled == value) return;

      enabled = value;
      SAMPLES.clear();
      frameMsEma = 0.0;
   }

   public static long begin() {
      return enabled ? System.nanoTime() : 0L;
   }

   public static void end(String name, long start) {
      if (!enabled || start == 0L) return;

      Sample sample = SAMPLES.computeIfAbsent(name, k -> new Sample());
      sample.accum = sample.accum + (System.nanoTime() - start);
   }

   public static void endFrame() {
      if (!enabled) return;

      long now = System.nanoTime();
      if (frameStart != 0L) {
         double ms = (now - frameStart) / 1000000.0;
         if (ms < 1000.0) {
            frameMsEma = frameMsEma + (ms - frameMsEma) * EMA;
         }
      }

      frameStart = now;

      for (Sample s : SAMPLES.values()) {
         s.roll();
      }
   }

   public static double frameMs() {
      return frameMsEma;
   }

   public static List<Row> top(int limit) {
      List<Row> rows = new ArrayList<>(SAMPLES.size());

      for (Entry<String, Sample> e : SAMPLES.entrySet()) {
         double ms = e.getValue().emaMs;
         if (ms > 5.0E-4) {
            rows.add(new Row(e.getKey(), ms));
         }
      }

      rows.sort((a, b) -> Double.compare(b.ms(), a.ms()));
      return rows.size() > limit ? rows.subList(0, limit) : rows;
   }

   public record Row(String name, double ms) {
   }

   private static final class Sample {
      long accum;
      double emaMs;

      void roll() {
         double ms = this.accum / 1000000.0;
         this.emaMs = this.emaMs + (ms - this.emaMs) * EMA;
         this.accum = 0L;
      }
   }
}
