package org.concurrent.blockingqueue;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class BlockingQueueMainTest {
    @Test
    public void testEmptyInputThenExit() throws Exception {
        String input = "100\nexit\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        InputStream oldIn = System.in;
        try {
            System.setIn(in);
            System.setOut(new PrintStream(out));
            BlockingQueueMain.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        String output = out.toString();
        assertTrue(output.contains("Exiting program."));
    }

    @Test
    public void testCapacityThenExit() throws Exception {
        String input = "300\nexit\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        InputStream oldIn = System.in;
        try {
            System.setIn(in);
            System.setOut(new PrintStream(out));
            BlockingQueueMain.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        String output = out.toString();
        assertTrue(output.contains("Enter pool capacity:"));
        assertTrue(output.contains("Final Status:"));
    }

    @Test
    public void testAddWriterThenExit() throws Exception {
        String input = "300\naddWriter 1\nexit\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        InputStream oldIn = System.in;
        try {
            System.setIn(in);
            System.setOut(new PrintStream(out));
            BlockingQueueMain.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        String output = out.toString();
        assertTrue(output.contains("Added Writer-1 at rate 1"));
        assertTrue(output.contains("Stopping all threads..."));
        assertTrue(output.contains("Final Status:"));
    }
}

