package org.concurrent.all.pool;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.impl.ReentrantLockTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReentrantLockTicketPoolTest {

    private TicketPool pool;

    @BeforeEach
    void setUp() {
        pool = new ReentrantLockTicketPool(3);
    }

    @Test
    void testAddTicket() throws InterruptedException {
        Ticket t1 = new Ticket("1", "Festival", 120.0);
        pool.addTicket(t1);
        assertEquals(1, pool.getAvailableTickets());
        assertEquals(1, pool.getAddedTickets());
        assertEquals(120.0, pool.getTotalUnsoldValue(), 0.01);
    }

    @Test
    void testPurchaseTicket() throws InterruptedException {
        Ticket t1 = new Ticket("1", "Festival", 120.0);
        pool.addTicket(t1);
        Ticket purchased = pool.purchaseTicket();
        assertEquals(t1.toString(), purchased.toString(), "Purchased ticket should match the one added");
        assertEquals(0, pool.getAvailableTickets());
        assertEquals(1, pool.getPurchasedTickets());
        assertEquals(120.0, pool.getTotalRevenue(), 0.01);
    }

    @Test
    void testAddBlocksWhenFull() throws InterruptedException {
        int capacity = 3;
        for (int i = 0; i < capacity; i++) {
            pool.addTicket(new Ticket(String.valueOf(i), "E", 10.0));
        }
        assertEquals(capacity, pool.getAvailableTickets());

        int initialAdded = pool.getAddedTickets();

        Thread adder = new Thread(() -> {
            try {
                pool.addTicket(new Ticket("X", "E", 5.0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Adder");
        adder.start();

        Thread.sleep(100);
        assertTrue(adder.isAlive(), "addTicket should block when full");

        Ticket freed = pool.purchaseTicket();
        assertNotNull(freed);

        adder.join(500);
        assertFalse(adder.isAlive(), "addTicket should unblock after purchase");

        assertEquals(initialAdded + 1, pool.getAddedTickets(),
                "Added count should increment by one");
        assertEquals(capacity, pool.getAvailableTickets(),
                "Pool should return to full after unblock");
    }

    @Test
    void testPurchaseBlocksWhenEmpty() throws InterruptedException {
        assertEquals(0, pool.getAvailableTickets());
        int initialPurchased = pool.getPurchasedTickets();

        Thread consumer = new Thread(() -> {
            try {
                pool.purchaseTicket();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");
        consumer.start();

        Thread.sleep(100);
        assertTrue(consumer.isAlive(), "purchaseTicket should block when empty");

        Ticket t = new Ticket("Z", "E", 15.0);
        pool.addTicket(t);

        consumer.join(500);
        assertFalse(consumer.isAlive(), "purchaseTicket should unblock after add");

        assertEquals(initialPurchased + 1, pool.getPurchasedTickets(),
                "Purchased count should increment by one");
        assertEquals(0, pool.getAvailableTickets(),
                "Pool should be empty again after the purchase");
    }


    @Test
    void testPerformExclusiveUpdate() throws InterruptedException {
        assertEquals(0, pool.getVersion());
        pool.performExclusiveUpdate();
        assertEquals(1, pool.getVersion());
    }

    @Test
    void testGetTotalUnsoldValue() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Event", 50.0));
        pool.addTicket(new Ticket("2", "Event", 75.0));
        double expected = 50.0 + 75.0;
        assertEquals(expected, pool.getTotalUnsoldValue(), 0.001);
    }

    @Test
    void testGetPoolInfoAndLogs() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Event", 100.0));
        pool.purchaseTicket();
        pool.performExclusiveUpdate();
        String info = pool.getPoolInfo();
        assertTrue(info.contains("Added: 1"), "Pool info should mention 1 added ticket");
        String logs = pool.getLogs();
        assertFalse(logs.isEmpty(), "Logs should not be empty");
    }

    @Test
    void testLogReaderMessage() throws InterruptedException {
        pool.logReaderMessage("LockPool Test");
        String logs = pool.getLogs();
        assertTrue(logs.contains("LockPool Test"), "Logs should contain the provided message");
    }

    @Test
    void testConcurrentProducersAndConsumers() throws InterruptedException {
        int numThreads = 5;
        Thread[] threads = new Thread[numThreads * 2];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    try {
                        pool.addTicket(new Ticket("Prod", "Event", 50.0));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Producer-" + i);

            threads[numThreads + i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    try {
                        pool.purchaseTicket();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Consumer-" + i);
        }

        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        assertEquals(25, pool.getAddedTickets(), "Should have added 25 tickets");
        assertEquals(25, pool.getPurchasedTickets(), "Should have purchased 25 tickets");
        assertTrue(pool.getTotalRevenue() >= 0, "Total revenue should be non-negative");
    }
}