package org.concurrent.all.pool.impl;

import org.concurrent.all.model.Ticket;
import org.concurrent.all.pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SynchronizedTicketPool implements TicketPool {
    private final List<Ticket> tickets;
    private final int capacity;
    private final List<String> logs = new ArrayList<>();
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;

    public SynchronizedTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new ArrayList<>(capacity);
    }

    @Override
    public synchronized boolean addTicket(Ticket ticket) {
        while (tickets.size() == capacity) {
            logWait("FULL");
            try {
                wait();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logs.add(logTime() + " [" + Thread.currentThread().getName() + "] INTERRUPTED while waiting to add");
                return false;
            }
        }
        tickets.add(ticket);
        added++;
        totalAddedValue += ticket.getPrice();
        notifyAll();
        logAction("Added", ticket);
        return true;
    }

    @Override
    public synchronized Ticket purchaseTicket() {
        while (tickets.isEmpty()) {
            logWait("EMPTY");
            try {
                wait();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logs.add(logTime() + " [" + Thread.currentThread().getName() + "] INTERRUPTED while waiting to purchase");
                return null;
            }
        }
        Ticket t = tickets.remove(0);
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
        return tickets.size();
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
        return tickets.stream().mapToDouble(Ticket::getPrice).sum();
    }

    @Override
    public synchronized String getPoolInfo() {
        return String.format(
                "[Synchronized] Tickets left: %d/%d, Added: %d, Purchased: %d, Version: %d",
                tickets.size(), capacity, added, purchased, version
        );
    }

    @Override
    public synchronized String getLogs() {
        return String.join("\n", logs);
    }

    @Override
    public synchronized void logReaderMessage(String msg) {
        logs.add(logTime() + " [" + Thread.currentThread().getName() + "] " + msg);
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
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }
}
