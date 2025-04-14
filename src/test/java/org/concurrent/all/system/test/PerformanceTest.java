package org.concurrent.all.system.test;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;
import org.concurrent.all.pool.impl.BlockingQueueTicketPool;
import org.concurrent.all.pool.impl.ReentrantLockTicketPool;
import org.concurrent.all.pool.impl.SynchronizedTicketPool;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

class PerformanceTest {
    private static final int WARMUP_SECONDS = 5;
    private static final int TEST_SECONDS = 10;
    private static final int[] THREAD_COUNTS = {1, 4, 16, 64};
    private static final int POOL_CAPACITY = 1000;
    private static final double WRITE_RATIO = 0.5;
    private static final int OPERATION_TIMEOUT_MS = 100;

    @Test
    void runAllPerformanceTests() throws Exception {
        System.out.println("=== Ticket Pool Performance Comparison ===");
        System.out.printf("Pool Capacity: %d | Test Duration: %ds%n%n",
                POOL_CAPACITY, TEST_SECONDS);

        testImplementation("Synchronized", SynchronizedTicketPool::new);
        testImplementation("ReentrantLock", ReentrantLockTicketPool::new);
        testImplementation("BlockingQueue", BlockingQueueTicketPool::new);
    }

    private void testImplementation(String name, PoolCreator creator) throws Exception {
        System.out.println("Testing " + name + " implementation:");

        for (int threadCount : THREAD_COUNTS) {
            // Warmup
            runTest(creator, threadCount, true);

            // Actual test
            TestResult result = runTest(creator, threadCount, false);

            System.out.printf("  %2d Threads | Throughput: %,.0f ops/sec | Latency: %.3f ms/op%n",
                    threadCount, result.throughput(), result.avgLatencyMs());
        }
        System.out.println();
    }

    private TestResult runTest(PoolCreator creator, int threadCount, boolean warmup)
            throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        TicketPool pool = creator.create(POOL_CAPACITY);
        AtomicLong totalOperations = new AtomicLong();
        List<Long> operationTimes = new CopyOnWriteArrayList<>();

        // Test duration control
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);

        // Create tasks
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                while (running.get()) {
                    boolean isWrite = ThreadLocalRandom.current().nextDouble() < WRITE_RATIO;
                    long startTime = System.nanoTime();

                    try {
                        if (isWrite) {
                            pool.addTicket(new Ticket("T", "Event", 100));
                        } else {
                            pool.purchaseTicket();
                        }
                        long duration = System.nanoTime() - startTime;
                        totalOperations.incrementAndGet();
                        operationTimes.add(duration);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }));
        }

        // Warmup phase
        if (warmup) {
            startLatch.countDown();
            LockSupport.parkNanos(Duration.ofSeconds(WARMUP_SECONDS).toNanos());
            running.set(false);
            executor.shutdownNow();
            return null;
        }

        // Test phase
        operationTimes.clear();
        totalOperations.set(0);
        running.set(true);

        Instant testStart = Instant.now();
        startLatch.countDown();

        // Run for exactly TEST_SECONDS
        while (Duration.between(testStart, Instant.now()).getSeconds() < TEST_SECONDS) {
            LockSupport.parkNanos(100_000); // 0.1ms sleep
        }
        running.set(false);

        // Collect results
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        // Calculate metrics
        long totalOps = totalOperations.get();
        double avgLatency = operationTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0) / 1_000_000.0;

        return new TestResult(
                totalOps / (double) TEST_SECONDS,
                avgLatency
        );
    }

    private record TestResult(double throughput, double avgLatencyMs) {}

    @FunctionalInterface
    private interface PoolCreator {
        TicketPool create(int capacity) throws Exception;
    }
}