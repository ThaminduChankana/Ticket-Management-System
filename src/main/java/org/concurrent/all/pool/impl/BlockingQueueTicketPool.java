package org.concurrent.all.pool.impl;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueueTicketPool implements TicketPool {
    private final int capacity;
    private final BlockingQueue<Ticket> queue;
    private final List<String> logs = new ArrayList<>();
    private final Lock logLock = new ReentrantLock();

    // Stats tracking
    private AtomicInteger added = new AtomicInteger(0);
    private AtomicInteger purchased = new AtomicInteger(0);
    private AtomicInteger version = new AtomicInteger(0);
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;
    private final Lock statsLock = new ReentrantLock();

    public BlockingQueueTicketPool(int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    @Override
    public boolean addTicket(Ticket ticket) throws InterruptedException {
        // This will block if the queue is full
        boolean added = false;
        try {
            queue.put(ticket);
            added = true;
        } finally {
            if (added) {
                this.added.incrementAndGet();
                statsLock.lock();
                try {
                    totalAddedValue += ticket.getPrice();
                } finally {
                    statsLock.unlock();
                }
                logAction("Added", ticket);
            }
        }
        return true;
    }

    @Override
    public Ticket purchaseTicket() throws InterruptedException {
        // This will block if the queue is empty
        Ticket t = queue.take();
        purchased.incrementAndGet();
        statsLock.lock();
        try {
            totalRevenue += t.getPrice();
        } finally {
            statsLock.unlock();
        }
        logAction("Consumed", t);
        return t;
    }

    @Override
    public void performExclusiveUpdate() throws InterruptedException {
        int newVersion = version.incrementAndGet();
        logUpdate(newVersion);
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
        statsLock.lock();
        try {
            return totalRevenue;
        } finally {
            statsLock.unlock();
        }
    }

    @Override
    public double getTotalUnsoldValue() {
        double sum = 0.0;
        for (Ticket t : queue) {
            sum += t.getPrice();
        }
        return sum;
    }

    @Override
    public String getPoolInfo() {
        return String.format("[BlockingQueue] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                queue.size(), capacity, added.get(), purchased.get(), version.get());
    }

    @Override
    public String getLogs() {
        logLock.lock();
        try {
            return String.join("\n", logs);
        } finally {
            logLock.unlock();
        }
    }

    @Override
    public void logReaderMessage(String msg) {
        String time = logTime();
        String entry = time + " [" + Thread.currentThread().getName() + "] " + msg;
        logLock.lock();
        try {
            logs.add(entry);
        } finally {
            logLock.unlock();
        }
    }

    // ------------------ Logging Helpers (no direct prints) ------------------
    private void logWait(String state) {
        String msg = logTime() + " [" + Thread.currentThread().getName() + "] waiting (Pool " + state + ")";
        logLock.lock();
        try {
            logs.add(msg);
        } finally {
            logLock.unlock();
        }
    }

    private void logAction(String action, Ticket t) {
        String msg = logTime() + " [" + Thread.currentThread().getName() + "] " + action + " " + t;
        logLock.lock();
        try {
            logs.add(msg);
        } finally {
            logLock.unlock();
        }
    }

    private void logUpdate(int newVersion) {
        String msg = logTime() + " [" + Thread.currentThread().getName() + "] updated version to " + newVersion;
        logLock.lock();
        try {
            logs.add(msg);
        } finally {
            logLock.unlock();
        }
    }

    private String logTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}