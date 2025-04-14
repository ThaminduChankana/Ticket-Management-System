package org.concurrent.synchronize;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynchronizedMainTest {
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
            SynchronizedMain.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        String output = out.toString();
        assertTrue(output.contains("Exiting program."));
    }

    @Test
    public void testCapacityThenExit() throws Exception {
        String input = "100\nexit\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        InputStream oldIn = System.in;
        try {
            System.setIn(in);
            System.setOut(new PrintStream(out));
            SynchronizedMain.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        String output = out.toString();
        assertTrue(output.contains("Enter pool capacity:"));
        assertTrue(output.contains("Final Status:"));
    }

    @Test
    public void testAddProducerThenExit() throws Exception {
        String input = "100\naddProducer 1\nexit\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        InputStream oldIn = System.in;
        try {
            System.setIn(in);
            System.setOut(new PrintStream(out));
            SynchronizedMain.main(new String[0]);
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        String output = out.toString();
        assertTrue(output.contains("Added Producer-1 at rate 1"));
        assertTrue(output.contains("Stopping all threads..."));
        assertTrue(output.contains("Final Status:"));
    }
}
