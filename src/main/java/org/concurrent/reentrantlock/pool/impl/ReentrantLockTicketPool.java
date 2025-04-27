package org.concurrent.reentrantlock.pool.impl;



import org.concurrent.reentrantlock.model.Ticket;
import org.concurrent.reentrantlock.pool.TicketPool;

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
    private double totalAddedValue = 0.0;

    public ReentrantLockTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new ArrayList<>(capacity);
    }

    @Override
    public boolean addTicket(Ticket ticket) throws InterruptedException {
        writeLock.lock();
        try {
            while (tickets.size() >= capacity) {
                logWait("FULL");
                notFull.await();
            }
            tickets.add(ticket);
            added++;
            totalAddedValue += ticket.getPrice();
            logAction("Added", ticket);
            notEmpty.signal();
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Ticket purchaseTicket() throws InterruptedException {
        writeLock.lock();
        try {
            while (tickets.isEmpty()) {
                logWait("EMPTY");
                notEmpty.await();
            }
            Ticket t = tickets.remove(0);
            purchased++;
            totalRevenue += t.getPrice();
            logAction("Consumed", t);
            notFull.signal();
            return t;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void performExclusiveUpdate() throws InterruptedException {
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
            return String.format("[ReentrantLock] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                    tickets.size(), capacity, added, purchased, version);
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
            String time = logTime();
            String entry = time + " [" + Thread.currentThread().getName() + "] " + msg;
            logs.add(entry);
        } finally {
            writeLock.unlock();
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
