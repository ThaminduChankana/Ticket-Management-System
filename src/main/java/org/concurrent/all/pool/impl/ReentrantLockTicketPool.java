package org.concurrent.all.pool.impl;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantLockTicketPool implements TicketPool {
    private final List<Ticket> tickets;
    private final int capacity;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
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
        while (true) {
            writeLock.lock();
            try {
                if (tickets.size() < capacity) {
                    tickets.add(ticket);
                    added++;
                    totalAddedValue += ticket.getPrice();
                    logAction("Added", ticket);
                    return true;
                }
                logWait("FULL");
            } finally {
                writeLock.unlock();
            }

            Thread.sleep(100);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    @Override
    public Ticket purchaseTicket() throws InterruptedException {
        while (true) {
            writeLock.lock();
            try {
                if (!tickets.isEmpty()) {
                    Ticket t = tickets.remove(0);
                    purchased++;
                    totalRevenue += t.getPrice();
                    logAction("Consumed", t);
                    return t;
                }
                logWait("EMPTY");
            } finally {
                writeLock.unlock();
            }

            Thread.sleep(100);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
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
