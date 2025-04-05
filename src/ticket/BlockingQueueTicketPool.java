package ticket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueueTicketPool implements TicketPool {
    private final int capacity;
    private final Queue<Ticket> queue;
    private int added = 0;
    private int purchased = 0;
    private int version = 0;
    private double totalRevenue = 0.0;
    private double totalAddedValue = 0.0;

    public BlockingQueueTicketPool(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
    }

    public synchronized boolean addTicket(Ticket ticket) throws InterruptedException {
        while (queue.size() == capacity) {
            logWait("FULL");
            wait();
        }
        queue.add(ticket);
        added++;
        totalAddedValue += ticket.getPrice();
        notifyAll();
        logAction("Added", ticket);
        return true;
    }

    public synchronized Ticket purchaseTicket() throws InterruptedException {
        while (queue.isEmpty()) {
            logWait("EMPTY");
            wait();
        }
        Ticket t = queue.poll();
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
        return queue.size();
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
        return queue.stream().mapToDouble(Ticket::getPrice).sum();
    }

    public synchronized String getPoolInfo() {
        return String.format("[BlockingQueue] Tickets left : %d/%d, Added: %d, Purchased: %d, Version: %d",
                queue.size(), capacity, added, purchased, version);
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
