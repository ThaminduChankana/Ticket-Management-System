package org.concurrent.reentrantlock.util;


import org.concurrent.reentrantlock.pool.TicketPool;

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
                String info = pool.getPoolInfo();
                pool.logReaderMessage("reads from " + info);

                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}