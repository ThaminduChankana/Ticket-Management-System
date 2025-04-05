package ticket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantLockTicketPool implements TicketPool {
    private final List<Ticket> tickets;
    private final int capacity;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;

    public ReentrantLockTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new ArrayList<>(capacity);
    }

    public boolean addTicket(Ticket ticket) throws InterruptedException {
        writeLock.lock();
        try {
            while (tickets.size() == capacity) {
                logWait("FULL");
                writeLock.unlock();
                Thread.sleep(100);
                writeLock.lock();
            }
            tickets.add(ticket);
            added++;
            totalAddedValue += ticket.getPrice();
            logAction("Added", ticket);
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    public Ticket purchaseTicket() throws InterruptedException {
        writeLock.lock();
        try {
            while (tickets.isEmpty()) {
                logWait("EMPTY");
                writeLock.unlock();
                Thread.sleep(100);
                writeLock.lock();
            }
            Ticket t = tickets.remove(0);
            purchased++;
            totalRevenue += t.getPrice();
            logAction("Consumed", t);
            return t;
        } finally {
            writeLock.unlock();
        }
    }

    public void performExclusiveUpdate() {
        writeLock.lock();
        try {
            version++;
            logUpdate();
        } finally {
            writeLock.unlock();
        }
    }

    public int getAvailableTickets() {
        readLock.lock();
        try {
            return tickets.size();
        } finally {
            readLock.unlock();
        }
    }

    public int getAddedTickets() {
        readLock.lock();
        try {
            return added;
        } finally {
            readLock.unlock();
        }
    }

    public int getPurchasedTickets() {
        readLock.lock();
        try {
            return purchased;
        } finally {
            readLock.unlock();
        }
    }

    public int getVersion() {
        readLock.lock();
        try {
            return version;
        } finally {
            readLock.unlock();
        }
    }

    public double getTotalRevenue() {
        readLock.lock();
        try {
            return totalRevenue;
        } finally {
            readLock.unlock();
        }
    }

    public double getTotalUnsoldValue() {
        readLock.lock();
        try {
            return tickets.stream().mapToDouble(Ticket::getPrice).sum();
        } finally {
            readLock.unlock();
        }
    }

    public String getPoolInfo() {
        readLock.lock();
        try {
            return String.format("[ReentrantLock] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                    tickets.size(), capacity, added, purchased, version);
        } finally {
            readLock.unlock();
        }
    }

    private void logWait(String state) {
        System.out.println(logTime() + " " + "[" + Thread.currentThread().getName() + "]"
                + " waiting (Pool " + state + ")");
    }

    private void logAction(String action, Ticket t) {
        System.out.println(logTime() + " " + "[" + Thread.currentThread().getName() + "]"
                + " " + action + " " + t);
    }

    private void logUpdate() {
        System.out.println(logTime() + " " + "[" + Thread.currentThread().getName() + "]"
                + " updated version to " + version);
    }

    private String logTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}