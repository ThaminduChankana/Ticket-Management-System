package org.concurrent.blockingqueue.pool;

import org.concurrent.blockingqueue.model.Ticket;
import org.concurrent.blockingqueue.pool.impl.BlockingQueueTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockingQueueTicketPoolTest {

    private TicketPool pool;

    @BeforeEach
    void setUp() {
        pool = new BlockingQueueTicketPool(2);
    }

    @Test
    void testAddTicketBlocksWhenFull() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Event", 100.0));
        pool.addTicket(new Ticket("2", "Event", 100.0));
        assertEquals(2, pool.getAvailableTickets());

        int initialAddedCount = pool.getAddedTickets();

        Thread adder = new Thread(() -> {
            try {
                pool.addTicket(new Ticket("3", "Event", 80.0));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        adder.start();
        Thread.sleep(300);
        assertEquals(2, pool.getAvailableTickets(), "Pool should still be full while adder thread is blocked");

        // Purchase one ticket to unblock the adder
        Ticket purchased = pool.purchaseTicket();
        adder.join(1000);

        assertNotNull(purchased, "Purchased ticket should not be null");
        assertEquals(initialAddedCount + 1, pool.getAddedTickets(),
                "The added ticket count should increase by one after unblocking");
    }

    @Test
    void testPurchaseTicketBlocksWhenEmpty() throws InterruptedException {
        Thread consumer = new Thread(() -> {
            try {
                pool.purchaseTicket();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();
        Thread.sleep(300);
        pool.addTicket(new Ticket("1", "Concert", 200.0));
        consumer.join(1000);
        assertEquals(1, pool.getAddedTickets());
        assertEquals(1, pool.getPurchasedTickets());
    }

    @Test
    void testSimpleAddAndPurchase() throws InterruptedException {
        Ticket ticket = new Ticket("T1", "RockFest", 99.99);
        pool.addTicket(ticket);
        assertEquals(1, pool.getAvailableTickets());
        Ticket purchased = pool.purchaseTicket();
        assertNotNull(purchased);
        assertEquals(ticket.toString(), purchased.toString());
    }

    @Test
    void testGetPoolInfoAndLogs() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Event", 100.0));
        String info = pool.getPoolInfo();
        assertTrue(info.contains("Tickets left"), "Pool info should mention tickets left");
        pool.purchaseTicket();
        String logs = pool.getLogs();
        assertFalse(logs.isEmpty(), "Logs should not be empty after operations");
    }
}