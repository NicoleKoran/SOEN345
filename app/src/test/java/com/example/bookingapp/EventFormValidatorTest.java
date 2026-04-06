package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EventFormValidatorTest {

    @Test
    void buildEvent_setsSoldOutWhenAvailableSeatsAreZero() {
        Event event = EventFormValidator.buildEvent(
                "event-1",
                "Taylor Swift Concert",
                "Live concert experience",
                "Toronto",
                "2026-04-06 08:13",
                "500",
                "0",
                "concert",
                "available",
                true
        );

        assertEquals(EventStatus.SOLDOUT, event.getStatus());
    }

    @Test
    void buildEvent_rejectsAvailableSeatsGreaterThanTotalSeats() {
        assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "Taylor Swift Concert",
                        "Live concert experience",
                        "Toronto",
                        "2026-04-06 08:13",
                        "100",
                        "200",
                        "concert",
                        "available",
                        true
                )
        );
    }

    @Test
    void buildEvent_requiresEventIdWhenRequested() {
        assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "",
                        "Taylor Swift Concert",
                        "Live concert experience",
                        "Toronto",
                        "2026-04-06 08:13",
                        "500",
                        "500",
                        "concert",
                        "available",
                        true
                )
        );
    }

    @Test
    void buildEvent_forcesAvailableSeatsToZeroWhenCancelled() {
        Event event = EventFormValidator.buildEvent(
                "event-1",
                "Taylor Swift Concert",
                "Live concert experience",
                "Toronto",
                "2026-04-06 08:13",
                "500",
                "250",
                "concert",
                "cancelled",
                true
        );

        assertEquals(0, event.getAvailableSeats());
        assertEquals(EventStatus.CANCELLED, event.getStatus());
    }

    @Test
    void buildEvent_forcesAvailableSeatsToZeroWhenSoldOutSelected() {
        Event event = EventFormValidator.buildEvent(
                "event-1",
                "Taylor Swift Concert",
                "Live concert experience",
                "Toronto",
                "2026-04-06 08:13",
                "500",
                "250",
                "concert",
                "soldOut",
                true
        );

        assertEquals(0, event.getAvailableSeats());
        assertEquals(EventStatus.SOLDOUT, event.getStatus());
    }

    @Test
    void formatDate_returnsValidatorPattern() {
        Event event = EventFormValidator.buildEvent(
                "event-1",
                "Taylor Swift Concert",
                "Live concert experience",
                "Toronto",
                "2026-04-06 08:13",
                "500",
                "100",
                "concert",
                "available",
                true
        );

        assertEquals("2026-04-06 08:13", EventFormValidator.formatDate(event.getDate()));
    }

    @Test
    void buildEvent_trimsInputsAndAllowsBlankIdWhenNotRequired() {
        Event event = EventFormValidator.buildEvent(
                "  ",
                "  Taylor Swift Concert  ",
                "  Live concert experience  ",
                "  Toronto  ",
                " 2026-04-06 08:13 ",
                " 500 ",
                " 100 ",
                " concert ",
                " available ",
                false
        );

        assertEquals("", event.getEventId());
        assertEquals("Taylor Swift Concert", event.getTitle());
        assertEquals("Live concert experience", event.getDescription());
        assertEquals("Toronto", event.getLocation());
        assertEquals(EventCategory.CONCERT, event.getCategory());
        assertEquals(EventStatus.AVAILABLE, event.getStatus());
        assertEquals(500, event.getTotalSeats());
        assertEquals(100, event.getAvailableSeats());
    }

    @Test
    void buildEvent_requiresTitle() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "   ",
                        "Live concert experience",
                        "Toronto",
                        "2026-04-06 08:13",
                        "500",
                        "100",
                        "concert",
                        "available",
                        true
                )
        );

        assertEquals("Title is required.", exception.getMessage());
    }

    @Test
    void buildEvent_requiresDescription() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "Taylor Swift Concert",
                        " ",
                        "Toronto",
                        "2026-04-06 08:13",
                        "500",
                        "100",
                        "concert",
                        "available",
                        true
                )
        );

        assertEquals("Description is required.", exception.getMessage());
    }

    @Test
    void buildEvent_requiresLocation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "Taylor Swift Concert",
                        "Live concert experience",
                        "",
                        "2026-04-06 08:13",
                        "500",
                        "100",
                        "concert",
                        "available",
                        true
                )
        );

        assertEquals("Location is required.", exception.getMessage());
    }

    @Test
    void buildEvent_requiresDate() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "Taylor Swift Concert",
                        "Live concert experience",
                        "Toronto",
                        " ",
                        "500",
                        "100",
                        "concert",
                        "available",
                        true
                )
        );

        assertEquals("Date is required.", exception.getMessage());
    }

    @Test
    void buildEvent_rejectsInvalidDateFormat() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "Taylor Swift Concert",
                        "Live concert experience",
                        "Toronto",
                        "04/06/2026 08:13",
                        "500",
                        "100",
                        "concert",
                        "available",
                        true
                )
        );

        assertTrue(exception.getMessage().contains(EventFormValidator.DATE_PATTERN));
    }

    @Test
    void buildEvent_rejectsNegativeSeatCounts() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "Taylor Swift Concert",
                        "Live concert experience",
                        "Toronto",
                        "2026-04-06 08:13",
                        "-1",
                        "100",
                        "concert",
                        "available",
                        true
                )
        );

        assertEquals("Total seats cannot be negative.", exception.getMessage());
    }

    @Test
    void buildEvent_rejectsNonNumericAvailableSeats() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                EventFormValidator.buildEvent(
                        "event-1",
                        "Taylor Swift Concert",
                        "Live concert experience",
                        "Toronto",
                        "2026-04-06 08:13",
                        "500",
                        "ten",
                        "concert",
                        "available",
                        true
                )
        );

        assertEquals("Available seats must be a whole number.", exception.getMessage());
    }
}
