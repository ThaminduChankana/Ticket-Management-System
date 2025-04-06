package pool.impl;

import model.Ticket;
import pool.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SynchronizedTicketPool implements TicketPool {
    private final List<Ticket> tickets;
    private final int capacity;
    // Store logs in memory
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
    public synchronized boolean addTicket(Ticket ticket) throws InterruptedException {
        while (tickets.size() == capacity) {
            logWait("FULL");
            wait();
        }
        tickets.add(ticket);
        added++;
        totalAddedValue += ticket.getPrice();
        notifyAll();
        logAction("Added", ticket);
        return true;
    }

    @Override
    public synchronized Ticket purchaseTicket() throws InterruptedException {
        while (tickets.isEmpty()) {
            logWait("EMPTY");
            wait();
        }
        Ticket t = tickets.remove(0);
        purchased++;
        totalRevenue += t.getPrice();
        notifyAll();
        logAction("Consumed", t);
        return t;
    }

    @Override
    public synchronized void performExclusiveUpdate() throws InterruptedException {
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
        return String.format("[Synchronized] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                tickets.size(), capacity, added, purchased, version);
    }

    @Override
    public synchronized String getLogs() {
        return String.join("\n", logs);
    }

    @Override
    public synchronized void logReaderMessage(String msg) {
        String time = logTime();
        String entry = time + " [Reader] " + msg;
        logs.add(entry);
    }

    // ------------------ Logging Helpers (no direct prints) ------------------
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