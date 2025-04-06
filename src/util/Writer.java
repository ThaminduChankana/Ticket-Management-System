package util;

import ticket.TicketPool;

public class Writer implements Runnable {
    private final TicketPool pool;
    private volatile boolean running = true;
    private int rate;

    public Writer(TicketPool pool, int rate) {
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
            while (running) {
                pool.performExclusiveUpdate();
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}