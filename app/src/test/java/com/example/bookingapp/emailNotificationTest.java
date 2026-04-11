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

    // ── NotificationType tests ─────────────────────────────────────────────────

    @Test
    void backwardCompatibleConstructor_defaultsToBookingConfirmation() {
        emailNotification n = buildNotification();
        assertEquals(emailNotification.NotificationType.BOOKING_CONFIRMATION,
                n.getNotificationType());
    }

    @Test
    void fullConstructor_userCancellation_setsType() {
        emailNotification n = new emailNotification(
                "n-1", "user@test.com", "Jazz Night", "Montreal",
                "Apr 12, 2026", "50", "res-1",
                emailNotification.NotificationType.USER_CANCELLATION);
        assertEquals(emailNotification.NotificationType.USER_CANCELLATION,
                n.getNotificationType());
    }

    @Test
    void fullConstructor_eventCancellation_setsType() {
        emailNotification n = new emailNotification(
                "n-2", "user@test.com", "Jazz Night", "Montreal",
                "Apr 12, 2026", "50", "res-2",
                emailNotification.NotificationType.EVENT_CANCELLATION);
        assertEquals(emailNotification.NotificationType.EVENT_CANCELLATION,
                n.getNotificationType());
    }

    @Test
    void update_userCancellation_doesNotThrow() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            emailNotification n = new emailNotification(
                    "n-3", "user@test.com", "Jazz Night", "Montreal",
                    "Apr 12, 2026", "", "res-3",
                    emailNotification.NotificationType.USER_CANCELLATION);
            assertDoesNotThrow(() -> n.update("Your reservation has been cancelled."));
            assertEquals("Your reservation has been cancelled.", n.getMessage());
            assertNotNull(n.getSentDate());
        }
    }

    @Test
    void update_eventCancellation_doesNotThrow() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            emailNotification n = new emailNotification(
                    "n-4", "user@test.com", "Jazz Night", "Montreal",
                    "Apr 12, 2026", "", "res-4",
                    emailNotification.NotificationType.EVENT_CANCELLATION);
            assertDoesNotThrow(() -> n.update("Event has been cancelled by organizer."));
            assertEquals("Event has been cancelled by organizer.", n.getMessage());
        }
    }

    @Test
    void notificationTypeEnum_hasThreeValues() {
        assertEquals(3, emailNotification.NotificationType.values().length);
    }

    @Test
    void sendEmail_userCancellationType_doesNotThrow() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            emailNotification n = new emailNotification(
                    "n-5", "user@test.com", "Concert", "Montreal",
                    "Apr 12, 2026", "", "res-5",
                    emailNotification.NotificationType.USER_CANCELLATION);
            assertDoesNotThrow(() -> n.sendEmail("cancelled"));
        }
    }

    @Test
    void sendEmail_eventCancellationType_doesNotThrow() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            emailNotification n = new emailNotification(
                    "n-6", "user@test.com", "Concert", "Montreal",
                    "Apr 12, 2026", "", "res-6",
                    emailNotification.NotificationType.EVENT_CANCELLATION);
            assertDoesNotThrow(() -> n.sendEmail("event cancelled"));
        }
    }

    @Test
    void sendEmail_whenSuppressed_returnsImmediatelyWithoutThrow() {
        try (MockedStatic<Log> logMock = mockStatic(Log.class)) {
            emailNotification.suppressEmailsForTesting = true;
            try {
                emailNotification n = buildNotification();
                assertDoesNotThrow(() -> n.sendEmail("should be suppressed"));
                // Log.d must be called with the suppression message
                logMock.verify(() -> Log.d(
                        eq("emailNotification"),
                        org.mockito.ArgumentMatchers.contains("suppressed")));
            } finally {
                emailNotification.suppressEmailsForTesting = false;
            }
        }
    }
}
