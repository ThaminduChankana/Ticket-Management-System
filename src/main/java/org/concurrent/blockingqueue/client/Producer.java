package org.concurrent.blockingqueue.client;


import org.concurrent.blockingqueue.model.Ticket;
import org.concurrent.blockingqueue.pool.TicketPool;

public class Producer implements Runnable {
    private static final double TICKET_PRICE = 100.0;
    private final TicketPool pool;
    private volatile boolean running = true;
    private int rate;

    public Producer(TicketPool pool, int rate) {
        this.pool = pool;
        this.rate = rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        try {
            int count = 0;
            while (running) {
                pool.addTicket(new Ticket(
                        "[" + Thread.currentThread().getName() + "]-" + (++count),
                        "Tomorrowland",
                        TICKET_PRICE
                ));
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
