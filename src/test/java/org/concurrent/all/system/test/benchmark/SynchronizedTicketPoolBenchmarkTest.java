package org.concurrent.all.system.test.benchmark;
import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;
import org.concurrent.all.pool.impl.SynchronizedTicketPool;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SynchronizedTicketPoolBenchmarkTest {
    private static final int CAPACITY = 1000;
    private static final int OPS_PER_THREAD = 10_000;
    private static final int[] THREAD_COUNTS = {2, 4, 16, 64};

    @Test
    public void benchmarkSynchronizedTicketPool() throws InterruptedException {
        System.out.println("=== SynchronizedTicketPool Benchmark ===");
        for (int tc : THREAD_COUNTS) {
            double time = measure(new SynchronizedTicketPool(CAPACITY), tc, OPS_PER_THREAD);
            System.out.printf("Threads: %d, Time: %.3f s%n", tc, time);
        }
    }

    private double measure(TicketPool pool, int threadCount, int opsPerThread) throws InterruptedException {
        int producers = threadCount / 2;
        int consumers = threadCount - producers;
        ExecutorService prodExec = Executors.newFixedThreadPool(producers);
        ExecutorService consExec = Executors.newFixedThreadPool(consumers);
        CountDownLatch prodLatch = new CountDownLatch(producers);
        CountDownLatch consLatch = new CountDownLatch(consumers);

        for (int i = 0; i < producers; i++) {
            prodExec.submit(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    try {
                        pool.addTicket(new Ticket(UUID.randomUUID().toString(), "Event", Math.random() * 100));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                prodLatch.countDown();
            });
        }
        for (int i = 0; i < consumers; i++) {
            consExec.submit(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    try {
                        pool.purchaseTicket();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                consLatch.countDown();
            });
        }

        long start = System.currentTimeMillis();
        prodLatch.await();
        consLatch.await();
        prodExec.shutdown();
        consExec.shutdown();

        return (System.currentTimeMillis() - start) / 1000.0;
    }
}