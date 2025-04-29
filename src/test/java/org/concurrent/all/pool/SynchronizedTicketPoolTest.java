package org.concurrent.all.pool;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.impl.SynchronizedTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SynchronizedTicketPoolTest {

    private TicketPool pool;

    @BeforeEach
    void setUp() {
        pool = new SynchronizedTicketPool(5); // capacity = 5
    }

    @Test
    void testAddTicket() throws InterruptedException {
        Ticket t = new Ticket("1", "Concert", 100.0);
        boolean result = pool.addTicket(t);
        assertTrue(result, "Adding a ticket should return true");
        assertEquals(1, pool.getAddedTickets(), "Added ticket count should be 1");
        assertEquals(1, pool.getAvailableTickets(), "Pool should have 1 available ticket");
    }

    @Test
    void testPurchaseTicket() throws InterruptedException {
        Ticket t = new Ticket("1", "Concert", 100.0);
        pool.addTicket(t);
        Ticket purchased = pool.purchaseTicket();
        assertNotNull(purchased, "Should successfully purchase a ticket");
        assertEquals(1, pool.getPurchasedTickets(), "Purchased ticket count should be 1");
        assertEquals(0, pool.getAvailableTickets(), "Pool should be empty after purchase");
        assertEquals(t.getPrice(), pool.getTotalRevenue(), 0.0001,
                "Total revenue should match the purchased ticket’s price");
    }

    @Test
    void testAddBlocksWhenFull() throws InterruptedException {
        int capacity = 5;
        for (int i = 0; i < capacity; i++) {
            assertTrue(pool.addTicket(new Ticket(String.valueOf(i), "E", 10.0)));
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

        Ticket t = pool.purchaseTicket();
        assertNotNull(t);

        adder.join(500);
        assertFalse(adder.isAlive(), "addTicket should have unblocked");

        assertEquals(initialAdded + 1, pool.getAddedTickets(),
                "Added count must increase by one after unblocking");
        assertEquals(capacity, pool.getAvailableTickets(),
                "Pool should return to full capacity after unblock");
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
        assertTrue(pool.addTicket(t));

        consumer.join(500);
        assertFalse(consumer.isAlive(), "purchaseTicket should have unblocked");

        assertEquals(initialPurchased + 1, pool.getPurchasedTickets(),
                "Purchased count must increase by one after unblocking");
        assertEquals(0, pool.getAvailableTickets(),
                "Pool should be empty again after the blocked purchase");
    }

    @Test
    void testPerformExclusiveUpdate() throws InterruptedException {
        assertEquals(0, pool.getVersion(), "Initial version should be 0");
        pool.performExclusiveUpdate();
        assertEquals(1, pool.getVersion(), "After one update, version should be 1");
    }

    @Test
    void testGetTotalUnsoldValue() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Event", 50.0));
        pool.addTicket(new Ticket("2", "Event", 75.0));
        double expectedUnsold = 50.0 + 75.0;
        assertEquals(expectedUnsold, pool.getTotalUnsoldValue(), 0.001);
    }

    @Test
    void testGetPoolInfoAndLogs() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Event", 100.0));
        pool.purchaseTicket();
        pool.performExclusiveUpdate();
        String info = pool.getPoolInfo();
        assertTrue(info.contains("Added: 1"), "Pool info should mention 1 added ticket");
        assertTrue(info.contains("Purchased: 1"), "Pool info should mention 1 purchased ticket");
        String logs = pool.getLogs();
        assertFalse(logs.isEmpty(), "Logs should not be empty after operations");
    }

    @Test
    void testLogReaderMessage() throws InterruptedException {
        pool.logReaderMessage("Test Message");
        String logs = pool.getLogs();
        assertTrue(logs.contains("Test Message"), "Logs should contain the test message");
    }

    @Test
    void testBlockingBehaviorWhenFull() throws InterruptedException {
        int capacity = 5;
        for (int i = 1; i <= capacity; i++) {
            pool.addTicket(new Ticket(String.valueOf(i), "Event", 100.0));
        }
        assertEquals(capacity, pool.getAvailableTickets(), "Pool should be full initially");

        int initialAddedCount = pool.getAddedTickets();
        Thread adder = new Thread(() -> {
            try {
                pool.addTicket(new Ticket("extra", "Event", 80.0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        adder.start();

        Thread.sleep(50);
        assertTrue(adder.isAlive(), "Adder thread should be blocked waiting for available space");
        Ticket purchased = pool.purchaseTicket();
        assertNotNull(purchased, "We should have purchased one ticket to unblock the queue");

        adder.join(1000);

        assertEquals(initialAddedCount + 1, pool.getAddedTickets(),
                "After unblocking, the added ticket count should increase by one");
        assertEquals(capacity, pool.getAvailableTickets(), "Pool should have full capacity available after unblocking");
    }


}