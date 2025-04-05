package util;

import ticket.TicketPool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Reader implements Runnable {
    private final TicketPool pool;
    private final int rate;
    private volatile boolean running = true;

    public Reader(TicketPool pool, int rate) {
        this.pool = pool;
        this.rate = rate;
    }

    public void stop() {
        running = false;
    }

    private String logTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    public void run() {
        try {
            while (running) {
                String info = pool.getPoolInfo();
                System.out.println(logTime() + " " + "[" + Thread.currentThread().getName() + "]"
                        + " reads from " + info);
                Thread.sleep(1000 / rate);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}