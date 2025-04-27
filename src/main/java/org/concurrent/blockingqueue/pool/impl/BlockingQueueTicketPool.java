package org.concurrent.blockingqueue.pool.impl;

import org.concurrent.blockingqueue.model.Ticket;
import org.concurrent.blockingqueue.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockingQueueTicketPool implements TicketPool {
    private final int capacity;
    private final Queue<Ticket> queue;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Condition notEmpty = lock.writeLock().newCondition();
    private final Condition notFull = lock.writeLock().newCondition();
    private final List<String> logs = new ArrayList<>();
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;

    public BlockingQueueTicketPool(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
    }

    @Override
    public boolean addTicket(Ticket ticket) throws InterruptedException {
        lock.writeLock().lock();
        try {
            while (queue.size() == capacity) {
                logWait("FULL");
                notFull.await();
            }
            queue.offer(ticket);
            added++;
            totalAddedValue += ticket.getPrice();
            notEmpty.signalAll();
            logAction("Added", ticket);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Ticket purchaseTicket() throws InterruptedException {
        lock.writeLock().lock();
        try {
            while (queue.isEmpty()) {
                logWait("EMPTY");
                notEmpty.await();
            }
            Ticket t = queue.poll();
            purchased++;
            totalRevenue += t.getPrice();
            notFull.signalAll();
            logAction("Consumed", t);
            return t;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void performExclusiveUpdate() {
        lock.writeLock().lock();
        try {
            version++;
            logUpdate();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getAvailableTickets() {
        lock.readLock().lock();
        try {
            return queue.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ------------------ Thread-safe Getters ------------------
    @Override
    public int getAddedTickets() {
        lock.readLock().lock();
        try {
            return added;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getPurchasedTickets() {
        lock.readLock().lock();
        try {
            return purchased;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getVersion() {
        lock.readLock().lock();
        try {
            return version;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getTotalRevenue() {
        lock.readLock().lock();
        try {
            return totalRevenue;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public double getTotalUnsoldValue() {
        lock.readLock().lock();
        try {
            return queue.stream().mapToDouble(Ticket::getPrice).sum();
        } finally {
            lock.readLock().unlock();
        }
    }

    // ------------------ Logging Methods ------------------
    @Override
    public String getPoolInfo() {
        lock.readLock().lock();
        try {
            return String.format("[BlockingQueue] Tickets left: %d/%d, Added: %d, Purchased: %d, Version: %d",
                    queue.size(), capacity, added, purchased, version);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getLogs() {
        lock.readLock().lock();
        try {
            return String.join("\n", logs);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void logReaderMessage(String msg) {
        lock.writeLock().lock();
        try {
            String time = logTime();
            logs.add(time + " [" + Thread.currentThread().getName() + "] " + msg);
        } finally {
            lock.writeLock().unlock();
        }
    }

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