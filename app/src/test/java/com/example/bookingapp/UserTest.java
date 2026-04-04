package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void emptyConstructor_createsInstance() {
        User u = new User();
        assertNotNull(u);
    }

    @Test
    void constructor_setsAllFields() {
        User u = new User("uid-1", "Alice", "alice@example.com", "+15141234567", "secret");
        assertEquals("uid-1", u.getUserId());
        assertEquals("Alice", u.getName());
        assertEquals("alice@example.com", u.getEmail());
        assertEquals("+15141234567", u.getPhoneNumber());
    }

    @Test
    void getUserId_returnsCorrectValue() {
        User u = new User("abc", "Bob", "bob@test.com", "123", "pw");
        assertEquals("abc", u.getUserId());
    }

    @Test
    void getName_returnsCorrectValue() {
        User u = new User("1", "Carol", "carol@test.com", "456", "pw");
        assertEquals("Carol", u.getName());
    }

    @Test
    void getEmail_returnsCorrectValue() {
        User u = new User("1", "Dan", "dan@test.com", "789", "pw");
        assertEquals("dan@test.com", u.getEmail());
    }

    @Test
    void getPhoneNumber_returnsCorrectValue() {
        User u = new User("1", "Eve", "eve@test.com", "+15559876543", "pw");
        assertEquals("+15559876543", u.getPhoneNumber());
    }

    @Test
    void cancelReservation_doesNotThrow() {
        User u = new User("1", "Frank", "frank@test.com", "000", "pw");
        assertDoesNotThrow(u::cancelReservation);
    }

    @Test
    void login_doesNotThrow() {
        User u = new User("1", "Grace", "grace@test.com", "000", "pw");
        assertDoesNotThrow(u::login);
    }

    @Test
    void searchEvents_doesNotThrow() {
        User u = new User("1", "Hank", "hank@test.com", "000", "pw");
        assertDoesNotThrow(u::searchEvents);
    }
}
