package client;

import ticket.Ticket;
import ticket.TicketPool;

public class Producer implements Runnable {
    private static final double TICKET_PRICE = 100.0;
    private final TicketPool pool;
    private final int rate;
    private volatile boolean running = true;

    public Producer(TicketPool pool, int rate) {
        this.pool = pool;
        this.rate = rate;
    }

    public void stop() {
        running = false;
    }

    public void run() {
        try {
            int count = 0;
            while (running) {
                pool.addTicket(new Ticket(
                        "[" + Thread.currentThread().getName() + "]" + "-" + (++count),
                        "Event",
                        TICKET_PRICE
                ));
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}