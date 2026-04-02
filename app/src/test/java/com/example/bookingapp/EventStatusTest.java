package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EventStatusTest {

    @Test
    void toFirestoreValue_returnsSoldOutCamelCase() {
        assertEquals("soldOut", EventStatus.SOLDOUT.toFirestoreValue());
    }

    @Test
    void fromValue_returnsAvailableWhenNull() {
        assertEquals(EventStatus.AVAILABLE, EventStatus.fromValue(null));
    }

    @Test
    void fromValue_acceptsSoldOutAlias() {
        assertEquals(EventStatus.SOLDOUT, EventStatus.fromValue("sold_out"));
    }

    @Test
    void displayValues_returnsExpectedOrdering() {
        assertArrayEquals(
                new String[]{"available", "cancelled", "soldOut"},
                EventStatus.displayValues()
        );
    }
}
