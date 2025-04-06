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

        // 1. Choose the synchronization mechanism
        System.out.println("Select synchronization mechanism:");
        System.out.println("1. Synchronized");
        System.out.println("2. ReentrantLock");
        System.out.println("3. BlockingQueue");
        int choice = sc.nextInt();

        // 2. Configure initial system parameters
        System.out.print("Enter pool capacity: ");
        int capacity = sc.nextInt();

        TicketPool pool = createPool(choice, capacity);

        // Default rates
        int produceRate = 2;
        int consumeRate = 2;
        int writeRate = 1;
        int readRate = 1;

        // 3. Start with zero producers/consumers/writers/readers
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
        sc.nextLine(); // consume leftover newline

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
                    System.out.printf("Total Sold Value: $%.2f%n", pool.getTotalRevenue());
                    System.out.printf("Total Unsold Value: $%.2f%n", pool.getTotalUnsoldValue());
                    break;

                case "addproducer":
                    if (parts.length < 2) {
                        System.out.println("Usage: addProducer <rate>");
                        break;
                    }
                    int pRate = Integer.parseInt(parts[1]);
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
                    if (parts.length < 2) {
                        System.out.println("Usage: addConsumer <rate>");
                        break;
                    }
                    int cRate = Integer.parseInt(parts[1]);
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
                    if (parts.length < 2) {
                        System.out.println("Usage: addWriter <rate>");
                        break;
                    }
                    int wRate = Integer.parseInt(parts[1]);
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
                    if (parts.length < 2) {
                        System.out.println("Usage: addReader <rate>");
                        break;
                    }
                    int rRate = Integer.parseInt(parts[1]);
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
                    // Show logs for 10 seconds, printing only new lines as they appear
                    long endTime = System.currentTimeMillis() + 10_000; // 10 seconds
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

        // Stopping all threads
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

        // Final status
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

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  help                 - Show this help message");
        System.out.println("  status               - Display the ticket pool’s real-time state");
        System.out.println("  addProducer <rate>   - Dynamically add a producer with given rate");
        System.out.println("  removeProducer <idx> - Remove the producer at index <idx> (1-based)");
        System.out.println("  addConsumer <rate>   - Dynamically add a consumer with given rate");
        System.out.println("  removeConsumer <idx> - Remove the consumer at index <idx>");
        System.out.println("  addWriter <rate>     - Dynamically add a writer with given rate");
        System.out.println("  removeWriter <idx>   - Remove the writer at index <idx>");
        System.out.println("  addReader <rate>     - Dynamically add a reader with given rate");
        System.out.println("  removeReader <idx>   - Remove the reader at index <idx>");
        System.out.println("  logs                 - Tail all logs (new lines) for 10 seconds");
        System.out.println("  exit                 - Stop everything and exit");
    }
}
