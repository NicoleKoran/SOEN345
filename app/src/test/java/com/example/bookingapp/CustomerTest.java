package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Date;

class CustomerTest {

    @Test
    void emptyConstructor_createsInstanceWithEmptyReservations() {
        Customer c = new Customer();
        assertNotNull(c);
        assertNotNull(c.getReservations());
        assertTrue(c.getReservations().isEmpty());
    }

    @Test
    void constructor_setsAllFieldsViaUser() {
        Customer c = new Customer("uid-1", "Alice", "alice@example.com", "+15141234567", "pw");
        assertEquals("uid-1", c.getUserId());
        assertEquals("Alice", c.getName());
        assertEquals("alice@example.com", c.getEmail());
        assertEquals("+15141234567", c.getPhoneNumber());
        assertTrue(c.getReservations().isEmpty());
    }

    @Test
    void receiveConfirmation_addsReservationToList() {
        Customer c = new Customer("uid-1", "Alice", "alice@test.com", "123", "pw");
        Reservation r = new Reservation(
                "evt-1", "movie", "uid-1", "alice@test.com",
                "Concert", "Montreal", new Date(), 50
        );

        c.receiveConfirmation(r);

        assertEquals(1, c.getReservations().size());
        assertSame(r, c.getReservations().get(0));
    }

    @Test
    void addReservation_addsToList() {
        Customer c = new Customer("uid-1", "Alice", "alice@test.com", "123", "pw");
        Reservation r = new Reservation(
                "evt-2", "sport", "uid-1", "alice@test.com",
                "Marathon", "Quebec", new Date(), 0
        );

        c.addReservation(r);

        assertEquals(1, c.getReservations().size());
        assertSame(r, c.getReservations().get(0));
    }

    @Test
    void receiveConfirmation_multipleReservations_allAdded() {
        Customer c = new Customer("uid-1", "Alice", "alice@test.com", "123", "pw");
        Reservation r1 = new Reservation("e1", "movie", "u1", "a@b.com", "Show", "MTL", new Date(), 10);
        Reservation r2 = new Reservation("e2", "sport", "u1", "a@b.com", "Game", "MTL", new Date(), 20);

        c.receiveConfirmation(r1);
        c.receiveConfirmation(r2);

        assertEquals(2, c.getReservations().size());
    }

    @Test
    void register_doesNotThrow() {
        Customer c = new Customer();
        assertDoesNotThrow(c::register);
    }

    @Test
    void viewReservations_doesNotThrow() {
        Customer c = new Customer();
        assertDoesNotThrow(c::viewReservations);
    }
}
