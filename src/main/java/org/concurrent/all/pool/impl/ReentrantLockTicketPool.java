package org.concurrent.all.pool.impl;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantLockTicketPool implements TicketPool {
    private final List<Ticket> tickets;
    private final int capacity;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Condition notFull = writeLock.newCondition();
    private final Condition notEmpty = writeLock.newCondition();

    private final List<String> logs = new ArrayList<>();
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAdded = 0.0;

    public ReentrantLockTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new ArrayList<>(capacity);
    }

    @Override
    public boolean addTicket(Ticket ticket) {
        writeLock.lock();
        try {
            while (tickets.size() >= capacity) {
                logWait("FULL");
                try {
                    notFull.await();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logs.add(logTime() + " [" + Thread.currentThread().getName() + "] INTERRUPTED while waiting to add");
                    return false;
                }
            }
            tickets.add(ticket);
            added++;
            totalAdded += ticket.getPrice();
            logAction("Added", ticket);
            notEmpty.signal();
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Ticket purchaseTicket() {
        writeLock.lock();
        try {
            while (tickets.isEmpty()) {
                logWait("EMPTY");
                try {
                    notEmpty.await();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logs.add(logTime() + " [" + Thread.currentThread().getName() + "] INTERRUPTED while waiting to purchase");
                    return null;
                }
            }
            Ticket t = tickets.remove(0);
            purchased++;
            totalRevenue += t.getPrice();
            logAction("Purchased", t);
            notFull.signal();
            return t;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void performExclusiveUpdate() {
        writeLock.lock();
        try {
            version++;
            logUpdate();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int getAvailableTickets() {
        readLock.lock();
        try {
            return tickets.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getAddedTickets() {
        readLock.lock();
        try {
            return added;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getPurchasedTickets() {
        readLock.lock();
        try {
            return purchased;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int getVersion() {
        readLock.lock();
        try {
            return version;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getTotalRevenue() {
        readLock.lock();
        try {
            return totalRevenue;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public double getTotalUnsoldValue() {
        readLock.lock();
        try {
            return tickets.stream().mapToDouble(Ticket::getPrice).sum();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getPoolInfo() {
        readLock.lock();
        try {
            return String.format(
                    "[ReentrantLock] Tickets left: %d/%d, Added: %d, Purchased: %d, Version: %d",
                    tickets.size(), capacity, added, purchased, version
            );
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String getLogs() {
        readLock.lock();
        try {
            return String.join("\n", logs);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void logReaderMessage(String msg) {
        writeLock.lock();
        try {
            logs.add(logTime() + " [" + Thread.currentThread().getName() + "] " + msg);
        } finally {
            writeLock.unlock();
        }
    }

    // ─── Logging helpers ────────────────────────────────────────────────────────────

    private void logWait(String state) {
        logs.add(logTime() + " [" + Thread.currentThread().getName() + "] WAIT - Pool " + state);
    }

    private void logAction(String action, Ticket t) {
        logs.add(logTime() + " [" + Thread.currentThread().getName() + "] " + action + " " + t);
    }

    private void logUpdate() {
        logs.add(logTime() + " [" + Thread.currentThread().getName() + "] updated version to " + version);
    }

    private String logTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
