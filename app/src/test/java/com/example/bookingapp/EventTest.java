package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.Date;

class EventTest {

    @Test
    void constructor_setsAllAdminEventFields() {
        Date date = new Date();
        Event event = new Event(
                "event-123",
                "Admin Managed Event",
                "Description",
                "Montreal",
                date,
                250,
                100,
                EventCategory.SPORT,
                EventStatus.AVAILABLE
        );

        assertEquals("event-123", event.getEventId());
        assertEquals("Admin Managed Event", event.getTitle());
        assertEquals("Description", event.getDescription());
        assertEquals("Montreal", event.getLocation());
        assertEquals(date, event.getDate());
        assertEquals(250, event.getTotalSeats());
        assertEquals(100, event.getAvailableSeats());
        assertEquals(EventCategory.SPORT, event.getCategory());
        assertEquals(EventStatus.AVAILABLE, event.getStatus());
    }

    @Test
    void setEventId_updatesEventId() {
        Event event = new Event(
                "old-id",
                "Event",
                "Description",
                "Toronto",
                new Date(),
                10,
                5,
                EventCategory.MOVIE,
                EventStatus.AVAILABLE
        );

        event.setEventId("new-id");

        assertEquals("new-id", event.getEventId());
    }

    @Test
    void constructor_preservesNullAndZeroValues() {
        Event event = new Event(
                null,
                "",
                null,
                null,
                null,
                0,
                0,
                null,
                null
        );

        assertEquals(null, event.getEventId());
        assertEquals("", event.getTitle());
        assertEquals(null, event.getDescription());
        assertEquals(null, event.getLocation());
        assertEquals(null, event.getDate());
        assertEquals(0, event.getTotalSeats());
        assertEquals(0, event.getAvailableSeats());
        assertEquals(null, event.getCategory());
        assertEquals(null, event.getStatus());
    }

    @Test
    void setEventId_acceptsEmptyString() {
        Event event = new Event(
                "existing-id",
                "Event",
                "Description",
                "Toronto",
                new Date(),
                10,
                5,
                EventCategory.MOVIE,
                EventStatus.AVAILABLE
        );

        event.setEventId("");

        assertEquals("", event.getEventId());
    }

    @Test
    void defaultConstructorAndSettersPopulateListFlowFields() {
        Date date = new Date(1775452380000L);
        Event event = new Event();

        event.setEventId("event-1");
        event.setTitle("Jazz Night");
        event.setDate(date);
        event.setLocation("Montreal");
        event.setDescription("Live music");
        event.setCategory(EventCategory.CONCERT);
        event.setStatus(EventStatus.AVAILABLE);
        event.setTotalSeats(200);
        event.setAvailableSeats(40);
        event.setPrice(25);

        assertEquals("event-1", event.getEventId());
        assertEquals("Jazz Night", event.getTitle());
        assertEquals(date, event.getDate());
        assertEquals("Montreal", event.getLocation());
        assertEquals("Live music", event.getDescription());
        assertEquals(EventCategory.CONCERT, event.getCategory());
        assertEquals(EventStatus.AVAILABLE, event.getStatus());
        assertEquals(25, event.getPrice());
    }

    @Test
    void stringSettersAndEnumHelpersStayConsistent() {
        Event event = new Event();
        event.setCategory("travel");
        event.setStatus("cancelled");

        assertEquals(EventCategory.TRAVEL, event.getCategory());
        assertEquals(EventStatus.CANCELLED, event.getStatus());
        assertEquals(EventCategory.TRAVEL, event.getCategoryEnum());
        assertEquals(EventStatus.CANCELLED, event.getStatusEnum());

        event.setCategory(null);
        event.setStatus(null);

        assertNull(event.getCategory());
        assertNull(event.getStatus());

        event.setCategory("invalid");
        event.setStatus("unknown");

        assertNull(event.getCategory());
        assertNull(event.getStatus());
    }
}
