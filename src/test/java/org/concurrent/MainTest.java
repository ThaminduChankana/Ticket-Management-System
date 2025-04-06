package org.concurrent;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    private String runMainWithInput(String input) throws InterruptedException {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            System.setOut(new PrintStream(outContent));
            Main.main(new String[0]);
        } finally {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        return outContent.toString();
    }

    @Test
    void testMinimalSession() throws InterruptedException {
        String input = "1\n10\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Final Status:"), "Output should contain final status");
        assertTrue(output.contains("Exiting program."), "Output should indicate program exit");
    }

    @Test
    void testAddProducer() throws InterruptedException {
        String input = "1\n10\naddProducer\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Added Producer-"), "Output should mention added producer");
        assertTrue(output.contains("Total Tickets Added:"), "Status should display total tickets added");
    }

    @Test
    void testAddConsumer() throws InterruptedException {
        String input = "1\n10\naddConsumer\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Added Consumer-"), "Output should mention added consumer");
        assertTrue(output.contains("Total Tickets Purchased:"), "Status should display total tickets purchased");
    }

    @Test
    void testAddWriterAndReaderWithLogs() throws InterruptedException {
        String input = "1\n10\naddWriter\naddReader\nlogs\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Added Writer-"), "Output should mention added writer");
        assertTrue(output.contains("Added Reader-"), "Output should mention added reader");
        assertTrue(output.contains("["), "Output should contain log messages");
    }

    @Test
    void testUnknownCommand() throws InterruptedException {
        String input = "1\n10\nfoobar\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Unknown command"), "Output should indicate unknown command");
    }

    @Test
    void testHelpCommand() throws InterruptedException {
        String input = "1\n10\nhelp\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Commands:"), "Output should list commands when help is requested");
    }

    @Test
    void testRemoveProducer() throws InterruptedException {
        String input = "1\n10\naddProducer\nremoveProducer 1\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Added Producer-"), "Output should mention added producer");
        assertTrue(output.contains("Removed producer #1"), "Output should mention removed producer");
    }

    @Test
    void testRemoveConsumer() throws InterruptedException {
        String input = "1\n10\naddConsumer\nremoveConsumer 1\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Added Consumer-"), "Output should mention added consumer");
        assertTrue(output.contains("Removed consumer #1"), "Output should mention removed consumer");
    }

    @Test
    void testRemoveWriter() throws InterruptedException {
        String input = "1\n10\naddWriter\nremoveWriter 1\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Added Writer-"), "Output should mention added writer");
        assertTrue(output.contains("Removed writer #1"), "Output should mention removed writer");
    }

    @Test
    void testRemoveReader() throws InterruptedException {
        String input = "1\n10\naddReader\nremoveReader 1\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("Added Reader-"), "Output should mention added reader");
        assertTrue(output.contains("Removed reader #1"), "Output should mention removed reader");
    }

    @Test
    void testSynchronizationMechanismReentrantLock() throws InterruptedException {
        String input = "2\n10\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("[CustomLock]"), "Output should indicate usage of ReentrantLock pool");
    }

    @Test
    void testSynchronizationMechanismBlockingQueue() throws InterruptedException {
        String input = "3\n10\nstatus\nexit\n";
        String output = runMainWithInput(input);
        assertTrue(output.contains("[BlockingQueue]"), "Output should indicate usage of BlockingQueue pool");
    }
}
