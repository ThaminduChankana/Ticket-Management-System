package system.test;

import org.concurrent.client.Consumer;
import org.concurrent.client.Producer;
import org.concurrent.pool.TicketPool;
import org.concurrent.pool.impl.SynchronizedTicketPool;
import org.concurrent.util.Reader;
import org.concurrent.util.Writer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullSystemFunctionalTest {
    @Test
    void testFullSystemFunctionality() throws InterruptedException {
        int capacity = 50;
        TicketPool pool = new SynchronizedTicketPool(capacity);
        Producer producer1 = new Producer(pool, 5);
        Producer producer2 = new Producer(pool, 5);
        Consumer consumer1 = new Consumer(pool, 5);
        Consumer consumer2 = new Consumer(pool, 5);
        Writer writer = new Writer(pool, 2);
        Reader reader = new Reader(pool, 2);
        Thread prodThread1 = new Thread(producer1);
        Thread prodThread2 = new Thread(producer2);
        Thread consThread1 = new Thread(consumer1);
        Thread consThread2 = new Thread(consumer2);
        Thread writerThread = new Thread(writer);
        Thread readerThread = new Thread(reader);
        prodThread1.start();
        prodThread2.start();
        consThread1.start();
        consThread2.start();
        writerThread.start();
        readerThread.start();
        Thread.sleep(5000);
        producer1.stop();
        producer2.stop();
        consumer1.stop();
        consumer2.stop();
        writer.stop();
        reader.stop();
        prodThread1.join();
        prodThread2.join();
        consThread1.join();
        consThread2.join();
        writerThread.join();
        readerThread.join();
        int added = pool.getAddedTickets();
        int purchased = pool.getPurchasedTickets();
        int available = pool.getAvailableTickets();
        assertEquals(added, purchased + available);
        assertTrue(available <= capacity);
        assertTrue(pool.getTotalRevenue() >= 0);
    }
}

