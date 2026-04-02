package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EventCategoryTest {

    @Test
    void toFirestoreValue_returnsLowercaseCategory() {
        assertEquals("concert", EventCategory.CONCERT.toFirestoreValue());
    }

    @Test
    void fromValue_returnsDefaultMovieWhenNull() {
        assertEquals(EventCategory.MOVIE, EventCategory.fromValue(null));
    }

    @Test
    void fromValue_isCaseInsensitive() {
        assertEquals(EventCategory.TRAVEL, EventCategory.fromValue("TrAvEl"));
    }

    @Test
    void displayValues_returnsExpectedOrdering() {
        assertArrayEquals(
                new String[]{"movie", "concert", "travel", "sport"},
                EventCategory.displayValues()
        );
    }
}
