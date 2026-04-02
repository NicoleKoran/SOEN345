package com.example.bookingapp;

import java.util.Locale;

public enum EventCategory {
    MOVIE,
    CONCERT,
    TRAVEL,
    SPORT;

    public String toFirestoreValue() {
        return name().toLowerCase(Locale.US);
    }

    public static EventCategory fromValue(String value) {
        if (value == null) {
            return MOVIE;
        }

        return EventCategory.valueOf(value.trim().toUpperCase(Locale.US));
    }

    public static String[] displayValues() {
        EventCategory[] categories = values();
        String[] displayValues = new String[categories.length];

        for (int index = 0; index < categories.length; index++) {
            displayValues[index] = categories[index].toFirestoreValue();
        }

        return displayValues;
    }
}
