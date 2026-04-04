package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReservationStatusTest {

    @Test
    void toFirestoreValue_pending_returnsPending() {
        assertEquals("pending", ReservationStatus.PENDING.toFirestoreValue());
    }

    @Test
    void toFirestoreValue_confirmed_returnsConfirmed() {
        assertEquals("confirmed", ReservationStatus.CONFIRMED.toFirestoreValue());
    }

    @Test
    void toFirestoreValue_cancelled_returnsCancelled() {
        assertEquals("cancelled", ReservationStatus.CANCELLED.toFirestoreValue());
    }

    @Test
    void fromValue_null_returnsPending() {
        assertEquals(ReservationStatus.PENDING, ReservationStatus.fromValue(null));
    }

    @Test
    void fromValue_lowercase_returnsMatchingEnum() {
        assertEquals(ReservationStatus.CONFIRMED, ReservationStatus.fromValue("confirmed"));
    }

    @Test
    void fromValue_uppercase_returnsMatchingEnum() {
        assertEquals(ReservationStatus.CANCELLED, ReservationStatus.fromValue("CANCELLED"));
    }

    @Test
    void fromValue_withLeadingTrailingWhitespace_returnsMatchingEnum() {
        assertEquals(ReservationStatus.PENDING, ReservationStatus.fromValue("  pending  "));
    }

    @Test
    void fromValue_mixedCase_returnsMatchingEnum() {
        assertEquals(ReservationStatus.CONFIRMED, ReservationStatus.fromValue("Confirmed"));
    }

    @Test
    void fromValue_invalidValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> ReservationStatus.fromValue("unknown"));
    }
}
