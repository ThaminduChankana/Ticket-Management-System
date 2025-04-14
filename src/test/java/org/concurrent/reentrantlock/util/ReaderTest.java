package org.concurrent.reentrantlock.util;

import org.concurrent.reentrantlock.model.Ticket;
import org.concurrent.reentrantlock.pool.TicketPool;
import org.concurrent.reentrantlock.pool.impl.ReentrantLockTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaderTest {

    private TicketPool pool;
    private Reader reader;

    @BeforeEach
    void setUp() {
        pool = new ReentrantLockTicketPool(3);
        try {
            pool.addTicket(new Ticket("1", "Event", 100.0));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reader = new Reader(pool, 2);
    }

    @Test
    void testLogReaderMessageFromReader() throws InterruptedException {
        Thread readerThread = new Thread(reader);
        readerThread.start();
        Thread.sleep(1100);
        reader.stop();
        readerThread.interrupt();
        readerThread.join(2000);
        String logs = pool.getLogs();
        long count = logs.lines().filter(line -> line.contains("reads from")).count();
        assertTrue(count >= 2, "Logs should contain at least 2 read messages");
    }

    @Test
    void testSetRate() {
        reader.setRate(3);
        assertDoesNotThrow(() -> reader.setRate(3));
    }
}