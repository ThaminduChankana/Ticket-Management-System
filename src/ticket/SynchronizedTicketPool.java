package ticket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SynchronizedTicketPool implements TicketPool {
    private final List<Ticket> tickets;
    private final int capacity;
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;

    public SynchronizedTicketPool(int capacity) {
        this.capacity = capacity;
        this.tickets = new ArrayList<>(capacity);
    }

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

    public synchronized void performExclusiveUpdate() {
        version++;
        logUpdate();
    }

    public synchronized int getAvailableTickets() {
        return tickets.size();
    }

    public synchronized int getAddedTickets() {
        return added;
    }

    public synchronized int getPurchasedTickets() {
        return purchased;
    }

    public synchronized int getVersion() {
        return version;
    }

    public synchronized double getTotalRevenue() {
        return totalRevenue;
    }

    public synchronized double getTotalUnsoldValue() {
        return tickets.stream().mapToDouble(Ticket::getPrice).sum();
    }

    public synchronized String getPoolInfo() {
        return String.format("[Synchronized] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                tickets.size(), capacity, added, purchased, version);
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

    public double getTotalAddedValue() {
        return totalAddedValue;
    }

    public void setTotalAddedValue(double totalAddedValue) {
        this.totalAddedValue = totalAddedValue;
    }
}