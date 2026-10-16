package zako.opt.particle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public final class Workers {
    static final int COUNT = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 2));
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(COUNT, r -> {
        Thread t = new Thread(r, "zakoopt-worker-" + THREAD_ID.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private Workers() {
    }

    static Throwable run(int blocks, IntConsumer task) {
        Job job = new Job(blocks, task);
        for (int h = Math.min(COUNT, blocks - 1); h > 0; --h) {
            POOL.execute(job::run);
        }
        job.run();
        while (job.completed.get() < blocks) {
            Thread.onSpinWait();
        }
        return job.error.get();
    }

    private static final class Job {
        final int blocks;
        final IntConsumer task;
        final AtomicInteger next = new AtomicInteger();
        final AtomicInteger completed = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference();

        Job(int blocks, IntConsumer task) {
            this.blocks = blocks;
            this.task = task;
        }

        void run() {
            int b;
            while ((b = this.next.getAndIncrement()) < this.blocks) {
                try {
                    if (this.error.get() != null) continue;
                    this.task.accept(b);
                }
                catch (Throwable t) {
                    this.error.compareAndSet(null, t);
                }
                finally {
                    this.completed.incrementAndGet();
                }
            }
        }
    }
}

