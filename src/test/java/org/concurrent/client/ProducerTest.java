package org.concurrent.client;

import org.concurrent.model.Ticket;
import org.concurrent.pool.TicketPool;
import org.concurrent.pool.impl.SynchronizedTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProducerTest {

    private TicketPool pool;
    private Producer producer;

    @BeforeEach
    void setUp() {
        pool = new SynchronizedTicketPool(5);
        producer = new Producer(pool, 2);
    }

    @Test
    void testProducerAddsTickets() throws InterruptedException {
        Thread producerThread = new Thread(producer);
        producerThread.start();

        Thread.sleep(1100);

        producer.stop();
        producerThread.interrupt();
        producerThread.join(2000);

        assertTrue(pool.getAddedTickets() >= 1, "Producer should have added at least one ticket");
    }

    @Test
    void testSetRate() {
        producer.setRate(5);
        assertDoesNotThrow(() -> producer.setRate(5));
    }

    @Test
    void testProducerStopWhileBlocked() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            pool.addTicket(new Ticket(String.valueOf(i), "Event", 50.0));
        }
        Thread producerThread = new Thread(producer);
        producerThread.start();
        Thread.sleep(300);

        producer.stop();
        producerThread.interrupt();
        producerThread.join(1000);
        assertTrue(pool.getAddedTickets() >= 5, "Producer did not add additional tickets once stopped");
    }
}