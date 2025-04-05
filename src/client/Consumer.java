package client;

import ticket.TicketPool;

public class Consumer implements Runnable {
    private final TicketPool pool;
    private final int rate;
    private volatile boolean running = true;

    public Consumer(TicketPool pool, int rate) {
        this.pool = pool;
        this.rate = rate;
    }

    public void stop() {
        running = false;
    }

    public void run() {
        try {
            while (running) {
                pool.purchaseTicket();
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
