package org.concurrent.all.pool.impl;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

public class BlockingQueueTicketPool implements TicketPool {

    private final LinkedBlockingQueue<Ticket> queue;
    private final int capacity;

    private final AtomicInteger added        = new AtomicInteger();
    private final AtomicInteger purchased    = new AtomicInteger();
    private final AtomicInteger version      = new AtomicInteger();
    private final DoubleAdder   totalRevenue = new DoubleAdder();
    private final DoubleAdder   totalAdded   = new DoubleAdder();

    private final List<String> logs = new ArrayList<>();

    public BlockingQueueTicketPool(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    @Override
    public boolean addTicket(Ticket ticket) throws InterruptedException {
        queue.put(ticket);
        added.incrementAndGet();
        totalAdded.add(ticket.getPrice());
        logAction("Added", ticket);
        return true;
    }

    @Override
    public Ticket purchaseTicket() throws InterruptedException {
        Ticket t = queue.take();
        purchased.incrementAndGet();
        totalRevenue.add(t.getPrice());
        logAction("Purchased", t);
        return t;
    }

    @Override
    public void performExclusiveUpdate() {
        version.incrementAndGet();
        logUpdate();
    }

    @Override
    public int getAvailableTickets() {
        return queue.size();
    }

    @Override
    public int getAddedTickets() {
        return added.get();
    }

    @Override
    public int getPurchasedTickets() {
        return purchased.get();
    }

    @Override
    public int getVersion() {
        return version.get();
    }

    @Override
    public double getTotalRevenue() {
        return totalRevenue.sum();
    }

    @Override
    public double getTotalUnsoldValue() {
        return queue.stream().mapToDouble(Ticket::getPrice).sum();
    }

    @Override
    public String getPoolInfo() {

        return String.format("[BlockingQueue] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                queue.size(), capacity,
                added.get(), purchased.get(), version.get()
        );
    }

    @Override
    public String getLogs() {
        return String.join("\n", logs);
    }

    @Override
    public void logReaderMessage(String msg) {
        logs.add(logTime() + " [" + Thread.currentThread().getName() + "] " + msg);
    }

    private void logAction(String action, Ticket t) {
        logs.add(logTime() + " [" + Thread.currentThread().getName() + "] " + action + " " + t);
    }

    private void logUpdate() {
        logs.add(logTime() + " [" + Thread.currentThread().getName() + "] updated version to " + version.get());
    }

    private String logTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
