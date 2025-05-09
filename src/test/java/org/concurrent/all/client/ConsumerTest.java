package org.concurrent.all.client;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;
import org.concurrent.all.pool.impl.SynchronizedTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerTest {

    private TicketPool pool;
    private Consumer consumer;

    @BeforeEach
    void setUp() {
        pool = new SynchronizedTicketPool(5);
        consumer = new Consumer(pool, 2);
    }

    @Test
    void testConsumerPurchasesTickets() throws InterruptedException {
        pool.addTicket(new Ticket("1", "ConcertA", 100.0));
        pool.addTicket(new Ticket("2", "ConcertB", 100.0));

        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        Thread.sleep(1100);

        consumer.stop();
        consumerThread.interrupt();
        consumerThread.join(2000);

        assertTrue(pool.getPurchasedTickets() >= 2, "Consumer should have purchased at least two tickets");
        assertEquals(0, pool.getAvailableTickets(), "Pool should be empty after consumer runs");
    }

    @Test
    void testSetRate() {
        consumer.setRate(5);
        assertDoesNotThrow(() -> consumer.setRate(5));
    }

    @Test
    void testConsumerStopWhileBlocked() throws InterruptedException {
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();
        Thread.sleep(300);
        consumer.stop();
        consumerThread.interrupt();
        consumerThread.join(1000);
        assertEquals(0, pool.getPurchasedTickets(), "Consumer should not have purchased any tickets");
    }
}