package util;

import ticket.TicketPool;

public class Writer implements Runnable {
    private final TicketPool pool;
    private final int rate;
    private volatile boolean running = true;

    public Writer(TicketPool pool, int rate) {
        this.pool = pool;
        this.rate = rate;
    }

    public void stop() {
        running = false;
    }

    public void run() {
        try {
            while (running) {
                pool.performExclusiveUpdate();
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
