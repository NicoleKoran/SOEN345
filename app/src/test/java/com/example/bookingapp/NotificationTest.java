package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.Date;

class NotificationTest {

    @Test
    void emptyConstructor_createsInstance() {
        Notification n = new Notification();
        assertNotNull(n);
    }

    @Test
    void constructor_setsNotificationIdAndMessage() {
        Notification n = new Notification("notif-1", "Your booking is confirmed!");
        assertEquals("notif-1", n.getNotificationId());
        assertEquals("Your booking is confirmed!", n.getMessage());
    }

    @Test
    void constructor_setSentDateToNow() {
        long before = System.currentTimeMillis();
        Notification n = new Notification("id", "msg");
        long after = System.currentTimeMillis();

        assertNotNull(n.getSentDate());
        assertTrue(n.getSentDate().getTime() >= before);
        assertTrue(n.getSentDate().getTime() <= after);
    }

    @Test
    void setMessage_updatesMessage() {
        Notification n = new Notification("id", "original");
        n.setMessage("updated");
        assertEquals("updated", n.getMessage());
    }

    @Test
    void setSentDate_updatesSentDate() {
        Notification n = new Notification("id", "msg");
        Date custom = new Date(0L);
        n.setSentDate(custom);
        assertEquals(custom, n.getSentDate());
    }

    @Test
    void send_doesNotThrow() {
        Notification n = new Notification("id", "msg");
        assertDoesNotThrow(n::send);
    }

    @Test
    void emptyConstructor_nullFieldsByDefault() {
        Notification n = new Notification();
        assertNull(n.getNotificationId());
        assertNull(n.getMessage());
        assertNull(n.getSentDate());
    }
}
