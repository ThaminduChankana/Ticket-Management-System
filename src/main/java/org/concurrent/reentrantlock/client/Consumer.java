package org.concurrent.reentrantlock.client;


import org.concurrent.reentrantlock.pool.TicketPool;

public class Consumer implements Runnable {
    private final TicketPool pool;
    private volatile boolean running = true;
    private int rate;

    public Consumer(TicketPool pool, int rate) {
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
                pool.purchaseTicket();
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
