package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import android.util.Log;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class emailNotificationTest {

    private emailNotification buildNotification() {
        return new emailNotification(
                "notif-001",
                "user@example.com",
                "Rock Concert",
                "Montreal",
                "May 10, 2026 at 8:00 PM",
                "75",
                "res-abc123"
        );
    }

    @Test
    void constructor_setsNotificationId() {
        emailNotification n = buildNotification();
        assertEquals("notif-001", n.getNotificationId());
    }

    @Test
    void getNotificationId_returnsCorrectValue() {
        emailNotification n = buildNotification();
        assertEquals("notif-001", n.getNotificationId());
    }

    @Test
    void getMessage_initiallyNull() {
        emailNotification n = buildNotification();
        assertNull(n.getMessage());
    }

    @Test
    void getSentDate_initiallyNull() {
        emailNotification n = buildNotification();
        assertNull(n.getSentDate());
    }

    @Test
    void update_setsMessage() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            emailNotification n = buildNotification();
            n.update("Booking confirmed!");
            assertEquals("Booking confirmed!", n.getMessage());
        }
    }

    @Test
    void update_setsSentDateToNow() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            long before = System.currentTimeMillis();
            emailNotification n = buildNotification();
            n.update("Booking confirmed!");
            long after = System.currentTimeMillis();

            assertNotNull(n.getSentDate());
            assertTrue(n.getSentDate().getTime() >= before);
            assertTrue(n.getSentDate().getTime() <= after);
        }
    }

    @Test
    void update_withDifferentMessages_setsLatestMessage() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            emailNotification n = buildNotification();
            n.update("First message");
            n.update("Second message");
            assertEquals("Second message", n.getMessage());
        }
    }

    @Test
    void sendEmail_doesNotThrow() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            emailNotification n = buildNotification();
            assertDoesNotThrow(() -> n.sendEmail("Test booking message"));
        }
    }
}
