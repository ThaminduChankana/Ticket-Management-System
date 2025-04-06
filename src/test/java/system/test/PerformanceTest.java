package system.test;

import org.concurrent.model.Ticket;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import org.concurrent.pool.TicketPool;
import org.concurrent.pool.impl.SynchronizedTicketPool;
import org.concurrent.pool.impl.ReentrantLockTicketPool;
import org.concurrent.pool.impl.BlockingQueueTicketPool;

class PerformanceTest {
    private static final int WARMUP_RUNS = 3;
    private static final int TEST_RUNS = 5;
    private static final int[] THREAD_COUNTS = {1, 4, 16, 64};
    private static final int POOL_CAPACITY = 1000;
    private static final int TEST_DURATION_MS = 3000;

    @Test
    void runAllPerformanceTests() throws Exception {
        System.out.println("=== Ticket Pool Performance Comparison ===");
        System.out.printf("Pool Capacity: %d | Test Duration: %dms%n%n", POOL_CAPACITY, TEST_DURATION_MS);
        testImplementation("Synchronized", SynchronizedTicketPool::new);
        testImplementation("ReentrantLock", ReentrantLockTicketPool::new);
        testImplementation("BlockingQueue", BlockingQueueTicketPool::new);
    }

    private void testImplementation(String name, PoolCreator creator) throws InterruptedException {
        System.out.println("Testing " + name + " implementation:");
        for (int threads : THREAD_COUNTS) {
            for (int i = 0; i < WARMUP_RUNS; i++) {
                runTest(creator.create(POOL_CAPACITY), threads, true);
            }
            long totalOps = 0;
            for (int i = 0; i < TEST_RUNS; i++) {
                totalOps += runTest(creator.create(POOL_CAPACITY), threads, false);
            }
            double avgOps = totalOps / (double) TEST_RUNS;
            double opsPerSec = avgOps / (TEST_DURATION_MS / 1000.0);
            double latencyMs = TEST_DURATION_MS / avgOps;
            System.out.printf("  %2d Threads | Throughput: %,.0f ops/sec | Latency: %.3f ms/op%n",
                    threads, opsPerSec, latencyMs);
        }
        System.out.println();
    }

    private long runTest(TicketPool pool, int threadCount, boolean warmup) throws InterruptedException {
        AtomicInteger operations = new AtomicInteger();
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                while (System.currentTimeMillis() - startTime < TEST_DURATION_MS) {
                    try {
                        if (Math.random() > 0.5) {
                            pool.addTicket(new Ticket("T", "Event", 100));
                        } else {
                            pool.purchaseTicket();
                        }
                        operations.incrementAndGet();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            threads[i].start();
        }
        Thread.sleep(TEST_DURATION_MS);
        for (Thread t : threads) {
            t.interrupt();
        }
        for (Thread t : threads) {
            t.join(1000);
        }
        if (!warmup) {
            assertConsistency(pool);
        }
        return operations.get();
    }

    private void assertConsistency(TicketPool pool) {
        int totalTickets = pool.getAddedTickets();
        int purchased = pool.getPurchasedTickets();
        int available = pool.getAvailableTickets();
        if (totalTickets != purchased + available) {
            System.err.printf("Consistency check failed! Added: %d, Purchased: %d, Available: %d%n",
                    totalTickets, purchased, available);
        }
    }

    @FunctionalInterface
    private interface PoolCreator {
        TicketPool create(int capacity);
    }
}
