package org.concurrent.blockingqueue.pool.impl;

import org.concurrent.blockingqueue.model.Ticket;
import org.concurrent.blockingqueue.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTicketPool implements TicketPool {
    private final int capacity;
    private final BlockingQueue<Ticket> queue;
    private final List<String> logs = new ArrayList<>();
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;

    public BlockingQueueTicketPool(int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public synchronized boolean addTicket(Ticket ticket) throws InterruptedException {
        while (!queue.offer(ticket)) {
            logWait("FULL");
            wait();
        }
        added++;
        totalAddedValue += ticket.getPrice();
        notifyAll();
        logAction("Added", ticket);
        return true;
    }

    @Override
    public synchronized Ticket purchaseTicket() throws InterruptedException {
        Ticket t;
        while ((t = queue.poll()) == null) {
            logWait("EMPTY");
            wait();
        }
        purchased++;
        totalRevenue += t.getPrice();
        notifyAll();
        logAction("Consumed", t);
        return t;
    }

    @Override
    public synchronized void performExclusiveUpdate() {
        version++;
        logUpdate();
    }

    @Override
    public synchronized int getAvailableTickets() {
        return queue.size();
    }

    @Override
    public synchronized int getAddedTickets() {
        return added;
    }

    @Override
    public synchronized int getPurchasedTickets() {
        return purchased;
    }

    @Override
    public synchronized int getVersion() {
        return version;
    }

    @Override
    public synchronized double getTotalRevenue() {
        return totalRevenue;
    }

    @Override
    public synchronized double getTotalUnsoldValue() {
        return queue.stream().mapToDouble(Ticket::getPrice).sum();
    }

    @Override
    public synchronized String getPoolInfo() {
        return String.format("[BlockingQueue] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                queue.size(), capacity, added, purchased, version);
    }

    @Override
    public synchronized String getLogs() {
        return String.join("\n", logs);
    }

    @Override
    public synchronized void logReaderMessage(String msg) {
        String time = logTime();
        String entry = time + " [" + Thread.currentThread().getName() + "] " + msg;
        logs.add(entry);
    }

    // ------------------ Logging Helpers ------------------
    private void logWait(String state) {
        String msg = logTime() + " [" + Thread.currentThread().getName() + "] waiting (Pool " + state + ")";
        logs.add(msg);
    }

    private void logAction(String action, Ticket t) {
        String msg = logTime() + " [" + Thread.currentThread().getName() + "] " + action + " " + t;
        logs.add(msg);
    }

    private void logUpdate() {
        String msg = logTime() + " [" + Thread.currentThread().getName() + "] updated version to " + version;
        logs.add(msg);
    }

    private String logTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}