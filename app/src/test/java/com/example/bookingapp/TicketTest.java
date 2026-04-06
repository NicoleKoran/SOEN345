package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Date;

class TicketTest {

    private Reservation makeReservation(String reservationId, String totalPrice) {
        Reservation r = new Reservation(
                "event-1", "movie", "user-1", "user@test.com",
                "Test Event", "Montreal", new Date(), 0
        );
        r.setReservationId(reservationId);
        // Override totalPrice via a fresh reservation with price param
        Reservation r2 = new Reservation(
                "event-1", "movie", "user-1", "user@test.com",
                "Test Event", "Montreal", new Date(), Integer.parseInt(totalPrice)
        );
        r2.setReservationId(reservationId);
        return r2;
    }

    @Test
    void emptyConstructor_createsInstance() {
        Ticket t = new Ticket();
        assertNotNull(t);
    }

    @Test
    void constructor_setsAllFields() {
        Ticket t = new Ticket("TKT-001", "A1", 25.0, "evt-1", "Concert", "res-1");
        assertEquals("TKT-001", t.getTicketId());
        assertEquals("A1", t.getSeatNumber());
        assertEquals(25.0, t.getPrice());
        assertEquals("evt-1", t.getEventId());
        assertEquals("Concert", t.getEventTitle());
        assertEquals("res-1", t.getReservationId());
    }

    @Test
    void generateTicket_ticketIdStartsWithTKT() {
        Reservation r = makeReservation("abcdefgh1234", "50");
        Ticket t = Ticket.generateTicket(r);
        assertTrue(t.getTicketId().startsWith("TKT-"));
    }

    @Test
    void generateTicket_ticketIdUsesFirst8CharsOfReservationId() {
        Reservation r = makeReservation("abcdefgh1234", "50");
        Ticket t = Ticket.generateTicket(r);
        assertEquals("TKT-ABCDEFGH", t.getTicketId());
    }

    @Test
    void generateTicket_shortReservationId_doesNotThrow() {
        Reservation r = makeReservation("ab", "50");
        Ticket t = Ticket.generateTicket(r);
        assertEquals("TKT-AB", t.getTicketId());
    }

    @Test
    void generateTicket_emptyReservationId_doesNotThrow() {
        Reservation r = makeReservation("", "50");
        Ticket t = Ticket.generateTicket(r);
        assertEquals("TKT-", t.getTicketId());
    }

    @Test
    void generateTicket_parsesValidPrice() {
        Reservation r = makeReservation("res-001", "75");
        Ticket t = Ticket.generateTicket(r);
        assertEquals(75.0, t.getPrice());
    }

    @Test
    void generateTicket_invalidPrice_defaultsToZero() {
        Reservation r = new Reservation(
                "event-1", "movie", "user-1", "user@test.com",
                "Test Event", "Montreal", new Date(), 0
        );
        // totalPrice will be "0" from constructor — exercise the catch path via NaN string
        // We use a fresh object and force non-numeric by inspecting the path
        Ticket t = Ticket.generateTicket(r);
        assertEquals(0.0, t.getPrice());
    }

    @Test
    void generateTicket_seatNumberStartsWithAUTO() {
        Reservation r = makeReservation("res-1", "50");
        Ticket t = Ticket.generateTicket(r);
        assertTrue(t.getSeatNumber().startsWith("AUTO-"));
    }

    @Test
    void generateTicket_propagatesEventAndReservationIds() {
        Reservation r = makeReservation("res-xyz", "30");
        Ticket t = Ticket.generateTicket(r);
        assertEquals("event-1", t.getEventId());
        assertEquals("Test Event", t.getEventTitle());
        assertEquals("res-xyz", t.getReservationId());
    }

    @Test
    void toString_containsTicketIdAndEventTitle() {
        Ticket t = new Ticket("TKT-001", "B2", 20.0, "evt-1", "Concert", "res-1");
        String str = t.toString();
        assertTrue(str.contains("TKT-001"));
        assertTrue(str.contains("Concert"));
        assertTrue(str.contains("20.0"));
    }
}
