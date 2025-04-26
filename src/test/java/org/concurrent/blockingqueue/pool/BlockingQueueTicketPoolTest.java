package org.concurrent.blockingqueue.pool;

import org.concurrent.blockingqueue.model.Ticket;
import org.concurrent.blockingqueue.pool.impl.BlockingQueueTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BlockingQueueTicketPoolTest {

    private TicketPool pool;

    @BeforeEach
    void setUp() {
        // pool capacity = 2
        pool = new BlockingQueueTicketPool(2);
    }

    @Test
    void testAddTicketBlocksWhenFull() throws InterruptedException {
        // Fill the queue
        pool.addTicket(new Ticket("1", "Event", 100.0));
        pool.addTicket(new Ticket("2", "Event", 100.0));
        assertEquals(2, pool.getAvailableTickets(), "Pool should be full (2/2)");

        int initialAddedCount = pool.getAddedTickets();  // should be 2

        // This thread will block because the pool is full
        Thread adder = new Thread(() -> {
            try {
                pool.addTicket(new Ticket("3", "Event", 80.0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Adder-Thread");
        adder.start();

        // Allow a brief moment for the adder thread to reach the await()
        Thread.sleep(300);
        assertEquals(2, pool.getAvailableTickets(),
                "Pool should still be full while adder thread is blocked");

        // Purchase one ticket to unblock the adder
        Ticket purchased = pool.purchaseTicket();
        assertNotNull(purchased, "Purchased ticket should not be null");

        // Wait for adder to finish
        adder.join(1_000);

        // added count should now be initial + 1 (i.e. 3)
        assertEquals(initialAddedCount + 1, pool.getAddedTickets(),
                "The added-ticket count should increase by one after unblocking");
    }

    @Test
    void testPurchaseTicketBlocksWhenEmpty() throws InterruptedException {
        Thread consumer = new Thread(() -> {
            try {
                pool.purchaseTicket();  // will block until a ticket is added
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer-Thread");
        consumer.start();

        // Give the consumer a moment to block on an empty pool
        Thread.sleep(300);

        // Now add a ticket to release the consumer
        pool.addTicket(new Ticket("1", "Concert", 200.0));

        consumer.join(1_000);

        assertEquals(1, pool.getAddedTickets(), "Exactly one ticket should have been added");
        assertEquals(1, pool.getPurchasedTickets(), "Exactly one ticket should have been purchased");
    }

    @Test
    void testSimpleAddAndPurchase() throws InterruptedException {
        Ticket ticket = new Ticket("T1", "RockFest", 99.99);

        pool.addTicket(ticket);
        assertEquals(1, pool.getAvailableTickets(), "One ticket should now be available");

        Ticket purchased = pool.purchaseTicket();
        assertNotNull(purchased, "Purchased ticket should not be null");
        assertEquals(ticket.toString(), purchased.toString(),
                "Purchased ticket should be the same as the one added");
    }

    @Test
    void testGetPoolInfoAndLogs() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Event", 100.0));

        String info = pool.getPoolInfo();
        assertTrue(info.contains("Tickets left"),
                "Pool info should include the 'Tickets left' summary");

        pool.purchaseTicket();

        String logs = pool.getLogs();
        assertFalse(logs.isEmpty(),
                "Logs should contain entries after add/purchase operations");
    }
}
