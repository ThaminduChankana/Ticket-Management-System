package util;

import ticket.TicketPool;

public class Reader implements Runnable {
    private final TicketPool pool;
    private volatile boolean running = true;
    private int rate;

    public Reader(TicketPool pool, int rate) {
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
                // Instead of printing here, we log to the pool:
                String info = pool.getPoolInfo();
                pool.logReaderMessage("reads from " + info);

                // Sleep
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}