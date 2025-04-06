package system.test;

import org.concurrent.client.Consumer;
import org.concurrent.client.Producer;
import org.concurrent.pool.TicketPool;
import org.concurrent.pool.impl.SynchronizedTicketPool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FullSystemNonFunctionalTest {
    @Test
    void testHighConcurrencyPerformance() throws InterruptedException {
        int capacity = 100;
        TicketPool pool = new SynchronizedTicketPool(capacity);
        int numProducers = 10;
        int numConsumers = 10;
        Producer[] producers = new Producer[numProducers];
        Consumer[] consumers = new Consumer[numConsumers];
        Thread[] producerThreads = new Thread[numProducers];
        Thread[] consumerThreads = new Thread[numConsumers];

        for (int i = 0; i < numProducers; i++) {
            producers[i] = new Producer(pool, 20);
            producerThreads[i] = new Thread(producers[i]);
        }
        for (int i = 0; i < numConsumers; i++) {
            consumers[i] = new Consumer(pool, 20);
            consumerThreads[i] = new Thread(consumers[i]);
        }

        long startTime = System.nanoTime();
        for (Thread t : producerThreads) {
            t.start();
        }
        for (Thread t : consumerThreads) {
            t.start();
        }

        Thread.sleep(10000);

        for (Producer p : producers) {
            p.stop();
        }
        for (Consumer c : consumers) {
            c.stop();
        }
        for (Thread t : producerThreads) {
            t.join();
        }
        for (Thread t : consumerThreads) {
            t.join();
        }
        long endTime = System.nanoTime();
        long durationNs = endTime - startTime;
        double durationSeconds = durationNs / 1_000_000_000.0;
        int added = pool.getAddedTickets();
        int purchased = pool.getPurchasedTickets();
        int totalOperations = added + purchased;
        double opsPerSecond = totalOperations / durationSeconds;
        System.out.println("Ops per second: " + opsPerSecond);
        assertTrue(opsPerSecond >= 300, "Throughput should be at least 300 ops/sec. Actual: " + opsPerSecond);
    }
}


