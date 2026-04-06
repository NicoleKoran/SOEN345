package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.firebase.Timestamp;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class ReservationSummaryTest {

    @Test
    void toDisplayString_sortsKeysAndSkipsEventId() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("tickets", 2);
        fields.put("eventId", "event-1");
        fields.put("name", "Jane Doe");

        ReservationSummary summary = new ReservationSummary("res-1", fields);

        assertEquals(
                "Reservation ID: res-1\nname: Jane Doe\ntickets: 2",
                summary.toDisplayString()
        );
    }

    @Test
    void toDisplayString_formatsTimestampValues() {
        Date date = new Date(1775452380000L);
        Map<String, Object> fields = new HashMap<>();
        fields.put("createdAt", new Timestamp(date));

        ReservationSummary summary = new ReservationSummary("res-2", fields);

        String expected = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(date);
        assertTrue(summary.toDisplayString().contains(expected));
    }

    @Test
    void toDisplayString_formatsDateValues() {
        Date date = new Date(1775452380000L);
        Map<String, Object> fields = new HashMap<>();
        fields.put("reservedFor", date);

        ReservationSummary summary = new ReservationSummary("res-3", fields);

        String expected = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(date);
        assertTrue(summary.toDisplayString().contains(expected));
    }

    @Test
    void toDisplayString_handlesNullValues() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("notes", null);

        ReservationSummary summary = new ReservationSummary("res-4", fields);

        assertEquals("Reservation ID: res-4\nnotes: null", summary.toDisplayString());
    }
}
