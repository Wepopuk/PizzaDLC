package pizza.dlc.api.rotation.ml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class TraceWriter implements AutoCloseable {
   private final ArrayBlockingQueue<String> queue;
   private final BufferedWriter writer;
   private final Thread thread;
   private final AtomicInteger dropped = new AtomicInteger();
   private final AtomicReference<IOException> failure = new AtomicReference<>();
   private volatile boolean running = true;

   public TraceWriter(File file, int capacity) throws IOException {
      this(file, capacity, true);
   }

   TraceWriter(File file, int capacity, boolean autoStart) throws IOException {
      File parent = file.getParentFile();
      if (parent != null) {
         parent.mkdirs();
      }

      this.queue = new ArrayBlockingQueue<>(capacity);
      this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8), 65536);
      this.thread = new Thread(this::drain, "pizzadlc-trace-writer");
      this.thread.setDaemon(true);
      if (autoStart) {
         this.thread.start();
      }
   }

   public void submit(String line) {
      if (this.queue.offer(line)) return;

      this.dropped.incrementAndGet();
   }

   public int dropped() {
      return this.dropped.get();
   }

   public IOException failure() {
      return this.failure.get();
   }

   private void drain() {
      try {
         while (this.running || !this.queue.isEmpty()) {
            String line = this.queue.poll(200L, TimeUnit.MILLISECONDS);
            if (line != null) {
               this.writer.write(line);
               this.writer.write(10);
               if (this.queue.isEmpty()) {
                  this.writer.flush();
               }
            }
         }
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
      } catch (IOException ex) {
         this.failure.compareAndSet(null, ex);
      }
   }

   @Override
   public void close() {
      this.running = false;

      try {
         this.thread.join(2000L);
      } catch (InterruptedException ignored) {
         Thread.currentThread().interrupt();
      } finally {
         try {
            this.writer.flush();
            this.writer.close();
         } catch (IOException ex) {
            this.failure.compareAndSet(null, ex);
         }
      }
   }
}
