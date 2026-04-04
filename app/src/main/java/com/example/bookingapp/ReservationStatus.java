package com.example.bookingapp;

import java.util.Locale;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    /** converts str to lowercase stored in Firestore */
    public String toFirestoreValue() {
        return name().toLowerCase(Locale.US);
    }

    public static ReservationStatus fromValue(String value) {
        if (value == null) return PENDING;
        return ReservationStatus.valueOf(value.trim().toUpperCase(Locale.US));
    }
}
