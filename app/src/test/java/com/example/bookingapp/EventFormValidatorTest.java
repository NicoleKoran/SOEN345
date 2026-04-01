package com.example.bookingapp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EventFormValidatorTest {

    @Test
    public void buildEvent_setsSoldOutWhenAvailableSeatsAreZero() {
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

    @Test(expected = IllegalArgumentException.class)
    public void buildEvent_rejectsAvailableSeatsGreaterThanTotalSeats() {
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
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildEvent_requiresEventIdWhenRequested() {
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
        );
    }
}
