package org.concurrent.blockingqueue.pool;


import org.concurrent.blockingqueue.model.Ticket;

public interface TicketPool {
    boolean addTicket(Ticket ticket) throws InterruptedException;

    Ticket purchaseTicket() throws InterruptedException;

    void performExclusiveUpdate() throws InterruptedException;

    int getAvailableTickets();

    int getAddedTickets();

    int getPurchasedTickets();

    int getVersion();

    double getTotalRevenue();

    double getTotalUnsoldValue();

    String getPoolInfo();

    String getLogs();

    void logReaderMessage(String msg);
}