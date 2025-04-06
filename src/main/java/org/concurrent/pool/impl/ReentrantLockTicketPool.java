package org.concurrent.pool.impl;


import org.concurrent.model.Ticket;
import org.concurrent.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTicketPool implements TicketPool {
    private final List<Ticket> tickets;
    private final int capacity;

    // Custom read-write lock implementation
    private final Lock lock = new ReentrantLock();
    private final Condition readCondition = lock.newCondition();
    private final Condition writeCondition = lock.newCondition();
    // State tracking
    private final List<String> logs = new ArrayList<>();
    private int readers = 0;
    private int writers = 0;
    private int writeRequests = 0;
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;

    public ReentrantLockTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new ArrayList<>(capacity);
    }

    // Custom read-write lock implementation
    private void acquireReadLock() throws InterruptedException {
        lock.lock();
        try {
            while (writers > 0 || writeRequests > 0) {
                readCondition.await();
            }
            readers++;
        } finally {
            lock.unlock();
        }
    }

    private void releaseReadLock() {
        lock.lock();
        try {
            readers--;
            if (readers == 0) {
                writeCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    private void acquireWriteLock() throws InterruptedException {
        lock.lock();
        try {
            writeRequests++;
            while (readers > 0 || writers > 0) {
                writeCondition.await();
            }
            writeRequests--;
            writers++;
        } finally {
            lock.unlock();
        }
    }

    private void releaseWriteLock() {
        lock.lock();
        try {
            writers--;
            writeCondition.signal();
            readCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean addTicket(Ticket ticket) throws InterruptedException {
        acquireWriteLock();
        try {
            while (tickets.size() == capacity) {
                logWait("FULL");
                // Release lock temporarily to allow other operations
                releaseWriteLock();
                Thread.sleep(100);
                acquireWriteLock();
            }
            tickets.add(ticket);
            added++;
            totalAddedValue += ticket.getPrice();
            logAction("Added", ticket);
            return true;
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public Ticket purchaseTicket() throws InterruptedException {
        acquireWriteLock();
        try {
            while (tickets.isEmpty()) {
                logWait("EMPTY");
                // Release lock temporarily to allow other operations
                releaseWriteLock();
                Thread.sleep(100);
                acquireWriteLock();
            }
            Ticket t = tickets.remove(0);
            purchased++;
            totalRevenue += t.getPrice();
            logAction("Consumed", t);
            return t;
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public void performExclusiveUpdate() throws InterruptedException {
        acquireWriteLock();
        try {
            version++;
            logUpdate();
        } finally {
            releaseWriteLock();
        }
    }

    @Override
    public int getAvailableTickets() {
        try {
            acquireReadLock();
            return tickets.size();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public int getAddedTickets() {
        try {
            acquireReadLock();
            return added;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public int getPurchasedTickets() {
        try {
            acquireReadLock();
            return purchased;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public int getVersion() {
        try {
            acquireReadLock();
            return version;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public double getTotalRevenue() {
        try {
            acquireReadLock();
            return totalRevenue;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0.0;
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public double getTotalUnsoldValue() {
        try {
            acquireReadLock();
            return tickets.stream().mapToDouble(Ticket::getPrice).sum();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0.0;
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public String getPoolInfo() {
        try {
            acquireReadLock();
            return String.format("[CustomLock] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                    tickets.size(), capacity, added, purchased, version);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "[Interrupted]";
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public String getLogs() {
        try {
            acquireReadLock();
            return String.join("\n", logs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } finally {
            releaseReadLock();
        }
    }

    @Override
    public void logReaderMessage(String msg) {
        try {
            acquireWriteLock();
            String time = logTime();
            String entry = time + " [" + Thread.currentThread().getName() + "] " + msg;
            logs.add(entry);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            releaseWriteLock();
        }
    }

    // Logging helpers
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