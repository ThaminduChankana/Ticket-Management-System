package org.concurrent.reentrantlock;

import org.concurrent.reentrantlock.pool.TicketPool;
import org.concurrent.reentrantlock.client.Consumer;
import org.concurrent.reentrantlock.client.Producer;
import org.concurrent.reentrantlock.pool.impl.ReentrantLockTicketPool;
import org.concurrent.reentrantlock.util.Reader;
import org.concurrent.reentrantlock.util.Writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ReentrantLockMain {
    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);

        System.out.println("Using ReentrantLock TicketPool.");
        System.out.print("Enter pool capacity: ");
        int capacity = sc.nextInt();

        TicketPool pool = new ReentrantLockTicketPool(capacity);

        int produceRate = 2;
        int consumeRate = 2;
        int writeRate = 1;
        int readRate = 1;

        List<Producer> producers = new ArrayList<>();
        List<Thread> producerThreads = new ArrayList<>();

        List<Consumer> consumers = new ArrayList<>();
        List<Thread> consumerThreads = new ArrayList<>();

        List<Writer> writers = new ArrayList<>();
        List<Thread> writerThreads = new ArrayList<>();

        List<Reader> readers = new ArrayList<>();
        List<Thread> readerThreads = new ArrayList<>();

        boolean running = true;
        int producerCount = 0;
        int consumerCount = 0;
        int writerCount = 0;
        int readerCount = 0;
        sc.nextLine();

        printHelp();
        while (running) {
            System.out.print("> ");
            String line = sc.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "help":
                    printHelp();
                    break;

                case "status":
                    System.out.println(pool.getPoolInfo());
                    System.out.println("Total Tickets Added: " + pool.getAddedTickets());
                    System.out.println("Total Tickets Purchased: " + pool.getPurchasedTickets());
                    System.out.println("Total tickets left in the pool: " + pool.getAvailableTickets());
                    System.out.printf("Total Sold Value: $%.2f%n", pool.getTotalRevenue());
                    System.out.printf("Total Unsold Value: $%.2f%n", pool.getTotalUnsoldValue());
                    break;

                case "addproducer":
                    int pRate = produceRate;
                    if (parts.length >= 2) {
                        pRate = Integer.parseInt(parts[1]);
                    } else {
                        System.out.println("Using default rate " + produceRate + " for producer.");
                    }
                    Producer p = new Producer(pool, pRate);
                    producers.add(p);
                    Thread pThread = new Thread(p, "Producer-" + (++producerCount));
                    producerThreads.add(pThread);
                    pThread.start();
                    System.out.println("Added Producer-" + producerCount + " at rate " + pRate);
                    break;

                case "removeproducer":
                    if (parts.length < 2) {
                        System.out.println("Usage: removeProducer <index>");
                        break;
                    }
                    int producerIndex = Integer.parseInt(parts[1]) - 1;
                    if (producerIndex < 0 || producerIndex >= producers.size()) {
                        System.out.println("Invalid producer index.");
                        break;
                    }
                    Producer removedProducer = producers.get(producerIndex);
                    removedProducer.stop();
                    Thread removedProducerThread = producerThreads.get(producerIndex);
                    removedProducerThread.join(200);
                    producers.remove(producerIndex);
                    producerThreads.remove(producerIndex);
                    System.out.println("Removed producer #" + (producerIndex + 1));
                    break;

                case "addconsumer":
                    int cRate = consumeRate;
                    if (parts.length >= 2) {
                        cRate = Integer.parseInt(parts[1]);
                    } else {
                        System.out.println("Using default rate " + consumeRate + " for consumer.");
                    }
                    Consumer c = new Consumer(pool, cRate);
                    consumers.add(c);
                    Thread cThread = new Thread(c, "Consumer-" + (++consumerCount));
                    consumerThreads.add(cThread);
                    cThread.start();
                    System.out.println("Added Consumer-" + consumerCount + " at rate " + cRate);
                    break;

                case "removeconsumer":
                    if (parts.length < 2) {
                        System.out.println("Usage: removeConsumer <index>");
                        break;
                    }
                    int consumerIndex = Integer.parseInt(parts[1]) - 1;
                    if (consumerIndex < 0 || consumerIndex >= consumers.size()) {
                        System.out.println("Invalid consumer index.");
                        break;
                    }
                    Consumer removedConsumer = consumers.get(consumerIndex);
                    removedConsumer.stop();
                    Thread removedConsumerThread = consumerThreads.get(consumerIndex);
                    removedConsumerThread.join(200);
                    consumers.remove(consumerIndex);
                    consumerThreads.remove(consumerIndex);
                    System.out.println("Removed consumer #" + (consumerIndex + 1));
                    break;

                case "addwriter":
                    int wRate = writeRate;
                    if (parts.length >= 2) {
                        wRate = Integer.parseInt(parts[1]);
                    } else {
                        System.out.println("Using default rate " + writeRate + " for writer.");
                    }
                    Writer w = new Writer(pool, wRate);
                    writers.add(w);
                    Thread wThread = new Thread(w, "Writer-" + (++writerCount));
                    writerThreads.add(wThread);
                    wThread.start();
                    System.out.println("Added Writer-" + writerCount + " at rate " + wRate);
                    break;

                case "removewriter":
                    if (parts.length < 2) {
                        System.out.println("Usage: removeWriter <index>");
                        break;
                    }
                    int writerIndex = Integer.parseInt(parts[1]) - 1;
                    if (writerIndex < 0 || writerIndex >= writers.size()) {
                        System.out.println("Invalid writer index.");
                        break;
                    }
                    Writer removedWriter = writers.get(writerIndex);
                    removedWriter.stop();
                    Thread removedWriterThread = writerThreads.get(writerIndex);
                    removedWriterThread.join(200);
                    writers.remove(writerIndex);
                    writerThreads.remove(writerIndex);
                    System.out.println("Removed writer #" + (writerIndex + 1));
                    break;

                case "addreader":
                    int rRate = readRate;
                    if (parts.length >= 2) {
                        rRate = Integer.parseInt(parts[1]);
                    } else {
                        System.out.println("Using default rate " + readRate + " for reader.");
                    }
                    Reader r = new Reader(pool, rRate);
                    readers.add(r);
                    Thread rThread = new Thread(r, "Reader-" + (++readerCount));
                    readerThreads.add(rThread);
                    rThread.start();
                    System.out.println("Added Reader-" + readerCount + " at rate " + rRate);
                    break;

                case "removereader":
                    if (parts.length < 2) {
                        System.out.println("Usage: removeReader <index>");
                        break;
                    }
                    int readerIndex = Integer.parseInt(parts[1]) - 1;
                    if (readerIndex < 0 || readerIndex >= readers.size()) {
                        System.out.println("Invalid reader index.");
                        break;
                    }
                    Reader removedReader = readers.get(readerIndex);
                    removedReader.stop();
                    Thread removedReaderThread = readerThreads.get(readerIndex);
                    removedReaderThread.join(200);
                    readers.remove(readerIndex);
                    readerThreads.remove(readerIndex);
                    System.out.println("Removed reader #" + (readerIndex + 1));
                    break;

                case "logs":
                    long endTime = System.currentTimeMillis() + 10_000;
                    int displayedCount = 0;
                    while (System.currentTimeMillis() < endTime) {
                        String allLogs = pool.getLogs();
                        if (!allLogs.isEmpty()) {
                            String[] lines = allLogs.split("\n");
                            // Print only the new lines since last time
                            for (int i = displayedCount; i < lines.length; i++) {
                                System.out.println(lines[i]);
                            }
                            displayedCount = lines.length;
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                    break;

                case "exit":
                    running = false;
                    break;

                default:
                    System.out.println("Unknown command. Type 'help' for a list of commands.");
                    break;
            }
        }

        System.out.println("Stopping all threads...");

        for (Producer prod : producers) {
            prod.stop();
        }
        for (Thread t : producerThreads) {
            t.join(200);
        }

        for (Consumer cons : consumers) {
            cons.stop();
        }
        for (Thread t : consumerThreads) {
            t.join(200);
        }

        for (Writer wr : writers) {
            wr.stop();
        }
        for (Thread t : writerThreads) {
            t.join(200);
        }

        for (Reader rd : readers) {
            rd.stop();
        }
        for (Thread t : readerThreads) {
            t.join(200);
        }

        System.out.println("\nFinal Status:");
        System.out.println(pool.getPoolInfo());
        System.out.println("Total Tickets Added: " + pool.getAddedTickets());
        System.out.println("Total Tickets Purchased: " + pool.getPurchasedTickets());
        System.out.println("Total tickets left in the pool: " + pool.getAvailableTickets());
        System.out.println("Current Version: " + pool.getVersion());
        System.out.printf("Total Sold Value: $%.2f%n", pool.getTotalRevenue());
        System.out.printf("Total Unsold Value: $%.2f%n", pool.getTotalUnsoldValue());

        System.out.println("Exiting program.");
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  help                 - Show this help message");
        System.out.println("  status               - Display the ticket pool’s real-time state");
        System.out.println("  addProducer [rate]    - Add producer (default rate: 2)");
        System.out.println("  removeProducer <idx>  - Remove producer at 1-based index");
        System.out.println("  addConsumer [rate]    - Add consumer (default rate: 2)");
        System.out.println("  removeConsumer <idx>  - Remove consumer at index");
        System.out.println("  addWriter [rate]      - Add writer (default rate: 1)");
        System.out.println("  removeWriter <idx>    - Remove writer at index");
        System.out.println("  addReader [rate]      - Add reader (default rate: 1)");
        System.out.println("  removeReader <idx>    - Remove reader at index");
        System.out.println("  logs                 - Tail logs for 10 seconds");
        System.out.println("  exit                 - Exit program");
    }
}
