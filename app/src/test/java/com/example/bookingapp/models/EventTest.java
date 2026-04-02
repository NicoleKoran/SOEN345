package com.example.bookingapp.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.Date;

class EventTest {

    @Test
    void defaultConstructorAndSettersPopulateAllFields() {
        Date date = new Date(1775452380000L);
        Event event = new Event();

        event.setEventId("event-1");
        event.setTitle("Jazz Night");
        event.setDate(date);
        event.setLocation("Montreal");
        event.setDescription("Live music");
        event.setCategory("concert");
        event.setStatus("available");
        event.setTotalSeats(200);
        event.setAvailableSeats(40);
        event.setPrice(25);

        assertEquals("event-1", event.getEventId());
        assertEquals("Jazz Night", event.getTitle());
        assertEquals(date, event.getDate());
        assertEquals("Montreal", event.getLocation());
        assertEquals("Live music", event.getDescription());
        assertEquals("concert", event.getCategory());
        assertEquals("available", event.getStatus());
        assertEquals(200, event.getTotalSeats());
        assertEquals(40, event.getAvailableSeats());
        assertEquals(25, event.getPrice());
    }

    @Test
    void parameterizedConstructorMapsEnumFieldsToStrings() {
        Date date = new Date(1775452380000L);
        Event event = new Event(
                "event-2",
                "Movie Night",
                date,
                "Toronto",
                "Marvel marathon",
                Event.EventCategory.movie,
                120,
                15,
                Event.EventStatus.soldOut,
                30
        );

        assertEquals("movie", event.getCategory());
        assertEquals("soldOut", event.getStatus());
        assertEquals(30, event.getPrice());
    }

    @Test
    void enumHelpers_returnEnumsWhenStoredStringsAreValid() {
        Event event = new Event();
        event.setCategory("travel");
        event.setStatus("cancelled");

        assertEquals(Event.EventCategory.travel, event.getCategoryEnum());
        assertEquals(Event.EventStatus.cancelled, event.getStatusEnum());
    }

    @Test
    void enumHelpers_returnNullWhenStoredStringsAreInvalidOrMissing() {
        Event event = new Event();
        event.setCategory("invalid");
        event.setStatus("unknown");

        assertNull(event.getCategoryEnum());
        assertNull(event.getStatusEnum());

        event.setCategory(null);
        event.setStatus(null);

        assertNull(event.getCategoryEnum());
        assertNull(event.getStatusEnum());
    }

    @Test
    void enumSetters_storeEnumNamesAsStrings() {
        Event event = new Event();

        event.setCategoryEnum(Event.EventCategory.sport);
        event.setStatusEnum(Event.EventStatus.available);

        assertEquals("sport", event.getCategory());
        assertEquals("available", event.getStatus());
    }
}
