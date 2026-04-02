package com.example.bookingapp;

import java.util.Locale;

public enum EventStatus {
    AVAILABLE,
    CANCELLED,
    SOLDOUT;

    public String toFirestoreValue() {
        return this == SOLDOUT ? "soldOut" : name().toLowerCase(Locale.US);
    }

    public static EventStatus fromValue(String value) {
        if (value == null) {
            return AVAILABLE;
        }

        String normalizedValue = value.trim();
        if ("soldOut".equalsIgnoreCase(normalizedValue) || "sold_out".equalsIgnoreCase(normalizedValue)) {
            return SOLDOUT;
        }

        return EventStatus.valueOf(normalizedValue.toUpperCase(Locale.US));
    }

    public static String[] displayValues() {
        EventStatus[] statuses = values();
        String[] displayValues = new String[statuses.length];

        for (int index = 0; index < statuses.length; index++) {
            displayValues[index] = statuses[index].toFirestoreValue();
        }

        return displayValues;
    }
}
