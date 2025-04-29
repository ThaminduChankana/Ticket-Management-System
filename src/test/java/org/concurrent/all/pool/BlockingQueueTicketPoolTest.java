package org.concurrent.all.pool;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.impl.BlockingQueueTicketPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class BlockingQueueTicketPoolTest {

    private TicketPool pool;

    @BeforeEach
    void setUp() {
        pool = new BlockingQueueTicketPool(2);
    }

    @Test
    void testSimpleAddAndPurchase() throws InterruptedException {
        Ticket t1 = new Ticket("T1", "EventA", 50.0);
        assertTrue(pool.addTicket(t1), "addTicket should return true when space is available");

        assertEquals(1, pool.getAvailableTickets(), "Pool size should be 1 after one add");
        Ticket purchased = pool.purchaseTicket();
        assertNotNull(purchased, "purchaseTicket should return a Ticket when available");
        assertEquals(t1.toString(), purchased.toString(), "Purchased ticket should match the one added");

        assertEquals(0, pool.getAvailableTickets(), "Pool should be empty after purchase");
    }

    @Test
    void testBlockingAndUnblockingBehavior() throws Exception {
        pool.addTicket(new Ticket("1", "A", 10.0));
        pool.addTicket(new Ticket("2", "B", 20.0));
        assertEquals(2, pool.getAvailableTickets(), "Pool should be full");
        int initialAddedCount = pool.getAddedTickets();
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<Boolean> blockedAdd =
                exec.submit(() -> pool.addTicket(new Ticket("3", "C", 30.0)));

        Thread.sleep(200);
        assertFalse(blockedAdd.isDone(), "addTicket should block when the pool is full");

        Ticket freed = pool.purchaseTicket();
        assertNotNull(freed, "purchaseTicket should unblock and return a ticket");

        assertTrue(blockedAdd.get(1, TimeUnit.SECONDS),
                "addTicket should return true once space frees up");

        exec.shutdownNow();

        assertEquals(initialAddedCount + 1,
                pool.getAddedTickets(),
                "Added count should increase by one after unblocking");
    }


    @Test
    void testBlockingPurchaseUnblocksOnAdd() throws Exception {
        assertEquals(0, pool.getAvailableTickets(), "Pool should start empty");

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<Ticket> blockedPurchase =
                exec.submit(() -> pool.purchaseTicket());

        Thread.sleep(200);
        assertFalse(blockedPurchase.isDone(), "purchaseTicket should block when the pool is empty");

        Ticket t = new Ticket("X", "Z", 99.0);
        assertTrue(pool.addTicket(t), "addTicket should succeed");

        Ticket consumed = blockedPurchase.get(1, TimeUnit.SECONDS);
        assertNotNull(consumed, "purchaseTicket should return the added ticket");
        assertEquals(t.toString(), consumed.toString());

        exec.shutdownNow();
        assertEquals(1, pool.getPurchasedTickets(), "Purchased count should be 1 after one successful purchase");
    }

    @Test
    void testMetricsAndValues() throws InterruptedException {
        Ticket t1 = new Ticket("A", "Alpha", 10.0);
        Ticket t2 = new Ticket("B", "Beta", 20.0);

        pool.addTicket(t1);
        pool.addTicket(t2);
        pool.purchaseTicket();

        assertEquals(2, pool.getAddedTickets(), "getAddedTickets() should count two adds");
        assertEquals(1, pool.getPurchasedTickets(), "getPurchasedTickets() should count one purchase");
        assertEquals(1, pool.getAvailableTickets(), "One ticket should remain");
        assertEquals(10.0, pool.getTotalRevenue(), 0.0001, "Total revenue should equal price of consumed ticket");
        assertEquals(20.0, pool.getTotalUnsoldValue(), 0.0001,
                "Total unsold value should equal price of remaining ticket");
    }

    @Test
    void testPerformExclusiveUpdateAndLogging() throws InterruptedException {
        assertEquals(0, pool.getVersion(), "Initial version must be 0");

        pool.performExclusiveUpdate();
        pool.performExclusiveUpdate();

        assertEquals(2, pool.getVersion(), "Version should increment on each performExclusiveUpdate()");

        String logs = pool.getLogs();
        assertTrue(logs.contains("updated version to 1"), "Logs must record first update");
        assertTrue(logs.contains("updated version to 2"), "Logs must record second update");
    }

    @Test
    void testLogReaderMessage() {
        String msg = "Custom read test";
        pool.logReaderMessage(msg);
        String logs = pool.getLogs();
        assertTrue(logs.contains(msg),
                "getLogs() must include messages logged via logReaderMessage()");
    }

    @Test
    void testGetPoolInfoFormat() throws InterruptedException {
        pool.addTicket(new Ticket("1", "Evt", 5.0));
        pool.addTicket(new Ticket("2", "Evt", 5.0));
        String info = pool.getPoolInfo();

        assertTrue(info.startsWith("[BlockingQueue]"),
                "getPoolInfo() must begin with pool type label");
        assertTrue(info.contains("Tickets left : 2/2"),
                "getPoolInfo() must report correct size/capacity");
        assertTrue(info.contains("Added: 2"),
                "getPoolInfo() must report total added count");
        assertTrue(info.contains("Purchased: 0"),
                "getPoolInfo() must report total purchased count");
        assertTrue(info.contains("Version: 0"),
                "getPoolInfo() must report correct version");
    }
}
