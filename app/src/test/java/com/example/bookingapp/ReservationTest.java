package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import android.util.Log;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Date;

class ReservationTest {

    private Reservation buildReservation() {
        return new Reservation(
                "event-1", "movie", "user-1", "user@test.com",
                "Test Concert", "Montreal", new Date(), 50
        );
    }

    @Test
    void emptyConstructor_createsInstance() {
        assertNotNull(new Reservation());
    }

    @Test
    void constructor_setsAllFields() {
        Date date = new Date();
        Reservation r = new Reservation(
                "evt-1", "sport", "uid-1", "a@b.com",
                "Big Game", "Toronto", date, 30
        );

        assertEquals("evt-1", r.getEventId());
        assertEquals("sport", r.getCategory());
        assertEquals("uid-1", r.getUserId());
        assertEquals("a@b.com", r.getUserEmail());
        assertEquals("Big Game", r.getEventTitle());
        assertEquals("Toronto", r.getEventLocation());
        assertEquals(date, r.getEventDate());
        assertEquals("30", r.getTotalPrice());
        assertEquals(1, r.getQuantity());
        assertFalse(r.isEmailSent());
        assertEquals(ReservationStatus.PENDING.toFirestoreValue(), r.getStatus());
        assertEquals("", r.getReservationId());
        assertNotNull(r.getReservationDate());
    }

    @Test
    void setReservationId_updatesId() {
        Reservation r = buildReservation();
        r.setReservationId("res-abc");
        assertEquals("res-abc", r.getReservationId());
    }

    @Test
    void setEmailSent_updatesFlag() {
        Reservation r = buildReservation();
        r.setEmailSent(true);
        assertTrue(r.isEmailSent());
    }

    @Test
    void attach_addsObserver_notifyObservers_invokesIt() {
        Reservation r = buildReservation();
        NotificationListener observer = mock(NotificationListener.class);

        r.attach(observer);
        r.notifyObservers("hello");

        verify(observer).update("hello");
    }

    @Test
    void detach_removesObserver_notifyObservers_doesNotInvokeIt() {
        Reservation r = buildReservation();
        NotificationListener observer = mock(NotificationListener.class);

        r.attach(observer);
        r.detach(observer);
        r.notifyObservers("hello");

        verify(observer, never()).update(any());
    }

    @Test
    void notifyObservers_multipleObservers_allReceiveMessage() {
        Reservation r = buildReservation();
        NotificationListener obs1 = mock(NotificationListener.class);
        NotificationListener obs2 = mock(NotificationListener.class);

        r.attach(obs1);
        r.attach(obs2);
        r.notifyObservers("broadcast");

        verify(obs1).update("broadcast");
        verify(obs2).update("broadcast");
    }

    @Test
    void confirmReservation_setsStatusToConfirmed() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            Reservation r = buildReservation();
            r.setReservationId("res-001");

            r.confirmReservation();

            assertEquals(ReservationStatus.CONFIRMED.toFirestoreValue(), r.getStatus());
        }
    }

    @Test
    void confirmReservation_generatesTicket() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            Reservation r = buildReservation();
            r.setReservationId("abcdefgh");

            r.confirmReservation();

            assertNotNull(r.getGeneratedTicket());
            assertTrue(r.getGeneratedTicket().getTicketId().startsWith("TKT-"));
        }
    }

    @Test
    void confirmReservation_notifiesObserversWithTicketInfo() {
        try (MockedStatic<Log> ignored = mockStatic(Log.class)) {
            Reservation r = buildReservation();
            r.setReservationId("res-123");

            NotificationListener observer = mock(NotificationListener.class);
            r.attach(observer);
            r.confirmReservation();

            verify(observer).update(argThat(msg ->
                    msg.contains("Test Concert") && msg.contains("TKT-")
            ));
        }
    }

    @Test
    void cancelReservation_setsStatusToCancelled() {
        Reservation r = buildReservation();
        r.cancelReservation();
        assertEquals(ReservationStatus.CANCELLED.toFirestoreValue(), r.getStatus());
    }

    @Test
    void cancelReservation_notifiesObservers() {
        Reservation r = buildReservation();
        NotificationListener observer = mock(NotificationListener.class);
        r.attach(observer);

        r.cancelReservation();

        verify(observer).update(argThat(msg -> msg.contains("Test Concert") && msg.contains("cancelled")));
    }

    @Test
    void getGeneratedTicket_beforeConfirm_returnsNull() {
        Reservation r = buildReservation();
        assertNull(r.getGeneratedTicket());
    }

    // ── cancellationReason (US-11 / US-14) ────────────────────────────────────

    @Test
    void cancellationReason_initiallyNull() {
        Reservation r = buildReservation();
        assertNull(r.getCancellationReason());
    }

    @Test
    void setCancellationReason_userCancelled_persistsValue() {
        Reservation r = buildReservation();
        r.setCancellationReason("user_cancelled");
        assertEquals("user_cancelled", r.getCancellationReason());
    }

    @Test
    void setCancellationReason_eventCancelled_persistsValue() {
        Reservation r = buildReservation();
        r.setCancellationReason("event_cancelled");
        assertEquals("event_cancelled", r.getCancellationReason());
    }

    @Test
    void setEventTitle_updatesField() {
        Reservation r = new Reservation();
        r.setEventTitle("Jazz Night");
        assertEquals("Jazz Night", r.getEventTitle());
    }

    @Test
    void setUserEmail_updatesField() {
        Reservation r = new Reservation();
        r.setUserEmail("customer@test.com");
        assertEquals("customer@test.com", r.getUserEmail());
    }
}
