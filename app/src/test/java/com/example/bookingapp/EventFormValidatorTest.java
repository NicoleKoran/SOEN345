package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
