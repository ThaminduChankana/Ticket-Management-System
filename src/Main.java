import client.Consumer;
import client.Producer;
import ticket.BlockingQueueTicketPool;
import ticket.ReentrantLockTicketPool;
import ticket.SynchronizedTicketPool;
import ticket.TicketPool;
import util.Reader;
import util.Writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);

        System.out.println("Select synchronization mechanism:");
        System.out.println("1. Synchronized");
        System.out.println("2. ReentrantLock");
        System.out.println("3. BlockingQueue");
        int choice = sc.nextInt();

        System.out.print("Enter pool capacity: ");
        int capacity = sc.nextInt();

        System.out.print("Number of producers: ");
        int producers = sc.nextInt();

        System.out.print("Number of consumers: ");
        int consumers = sc.nextInt();

        System.out.print("Number of writers: ");
        int writers = sc.nextInt();

        System.out.print("Number of readers: ");
        int readers = sc.nextInt();

        TicketPool pool = createPool(choice, capacity);

        List<Producer> producerList = new ArrayList<>();
        List<Consumer> consumerList = new ArrayList<>();
        List<Writer> writerList = new ArrayList<>();
        List<Reader> readerList = new ArrayList<>();

        // Default rates
        int produceRate = 2;
        int consumeRate = 2;
        int writeRate = 1;
        int readRate = 1;

        // Start threads
        for (int i = 0; i < producers; i++) {
            Producer p = new Producer(pool, produceRate);
            producerList.add(p);
            new Thread(p, "Producer-" + (i + 1)).start();
        }

        for (int i = 0; i < consumers; i++) {
            Consumer c = new Consumer(pool, consumeRate);
            consumerList.add(c);
            new Thread(c, "Consumer-" + (i + 1)).start();
        }

        for (int i = 0; i < writers; i++) {
            Writer w = new Writer(pool, writeRate);
            writerList.add(w);
            new Thread(w, "Writer-" + (i + 1)).start();
        }

        for (int i = 0; i < readers; i++) {
            Reader r = new Reader(pool, readRate);
            readerList.add(r);
            new Thread(r, "Reader-" + (i + 1)).start();
        }

        // Run for 10 seconds
        Thread.sleep(10000);

        // Stop all
        producerList.forEach(Producer::stop);
        consumerList.forEach(Consumer::stop);
        writerList.forEach(Writer::stop);
        readerList.forEach(Reader::stop);

        // Final report
        System.out.println("\nFinal Status:");
        System.out.println(pool.getPoolInfo());
        System.out.println("Total Tickets Added: " + pool.getAddedTickets());
        System.out.println("Total Tickets Purchased: " + pool.getPurchasedTickets());
        System.out.println("Total tickets left in the pool: " + pool.getAvailableTickets());
        System.out.println("Current Version: " + pool.getVersion());
        System.out.printf("Total Sold Value: $%.2f%n", pool.getTotalRevenue());
        System.out.printf("Total Unsold Value: $%.2f%n", pool.getTotalUnsoldValue());
    }

    private static TicketPool createPool(int choice, int capacity) {
        switch (choice) {
            case 1:
                return new SynchronizedTicketPool(capacity);
            case 2:
                return new ReentrantLockTicketPool(capacity);
            case 3:
                return new BlockingQueueTicketPool(capacity);
            default:
                throw new IllegalArgumentException("Invalid choice");
        }
    }
}