package org.concurrent.reentrantlock.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketTest {

    @Test
    void testGetPriceAndToString() {
        Ticket ticket = new Ticket("ID123", "Super Ball", 150.50);
        assertEquals(150.50, ticket.getPrice(), 0.0001);
        String expected = String.format("Ticket{id='%s', event='%s', price=%.2f}",
                "ID123", "Super Ball", 150.50);
        assertEquals(expected, ticket.toString());
    }
}

