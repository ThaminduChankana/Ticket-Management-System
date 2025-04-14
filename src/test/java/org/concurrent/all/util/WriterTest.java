package org.concurrent.all.util;

import org.concurrent.all.pool.TicketPool;
import org.concurrent.all.pool.impl.SynchronizedTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriterTest {

    private TicketPool pool;
    private Writer writer;

    @BeforeEach
    void setUp() {
        pool = new SynchronizedTicketPool(3);
        writer = new Writer(pool, 1);
    }

    @Test
    void testPerformExclusiveUpdateViaWriter() throws InterruptedException {
        Thread writerThread = new Thread(writer);
        writerThread.start();
        Thread.sleep(2200);
        writer.stop();
        writerThread.interrupt();
        writerThread.join(2000);
        assertTrue(pool.getVersion() >= 2, "Pool version should have increased at least twice");
    }

    @Test
    void testSetRate() {
        writer.setRate(3);
        assertDoesNotThrow(() -> writer.setRate(3));
    }
}