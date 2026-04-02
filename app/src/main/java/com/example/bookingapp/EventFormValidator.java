package com.example.bookingapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class EventFormValidator {
    public static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";

    private EventFormValidator() {
    }

    public static Event buildEvent(String eventId,
                                   String title,
                                   String description,
                                   String location,
                                   String dateText,
                                   String totalSeatsText,
                                   String availableSeatsText,
                                   String categoryText,
                                   String statusText,
                                   boolean requireEventId) {
        String normalizedEventId = safeTrim(eventId);
        String normalizedTitle = safeTrim(title);
        String normalizedDescription = safeTrim(description);
        String normalizedLocation = safeTrim(location);
        String normalizedDateText = safeTrim(dateText);

        if (requireEventId && normalizedEventId.isEmpty()) {
            throw new IllegalArgumentException("Event ID is required for this action.");
        }
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("Title is required.");
        }
        if (normalizedDescription.isEmpty()) {
            throw new IllegalArgumentException("Description is required.");
        }
        if (normalizedLocation.isEmpty()) {
            throw new IllegalArgumentException("Location is required.");
        }
        if (normalizedDateText.isEmpty()) {
            throw new IllegalArgumentException("Date is required.");
        }

        Date parsedDate = parseDate(normalizedDateText);
        int totalSeats = parseSeatValue(totalSeatsText, "Total seats");
        int availableSeats = parseSeatValue(availableSeatsText, "Available seats");
        EventStatus selectedStatus = EventStatus.fromValue(statusText);
        availableSeats = normalizeAvailableSeats(selectedStatus, availableSeats);

        if (availableSeats > totalSeats) {
            throw new IllegalArgumentException("Available seats cannot exceed total seats.");
        }

        EventCategory category = EventCategory.fromValue(categoryText);
        EventStatus normalizedStatus = normalizeStatus(selectedStatus, availableSeats);

        return new Event(
                normalizedEventId,
                normalizedTitle,
                normalizedDescription,
                normalizedLocation,
                parsedDate,
                totalSeats,
                availableSeats,
                category,
                normalizedStatus
        );
    }

    public static String formatDate(Date date) {
        return createFormatter().format(date);
    }

    private static Date parseDate(String dateText) {
        try {
            return createFormatter().parse(dateText);
        } catch (ParseException exception) {
            throw new IllegalArgumentException("Date must use format " + DATE_PATTERN + ".");
        }
    }

    private static int parseSeatValue(String value, String fieldName) {
        String normalizedValue = safeTrim(value);
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        try {
            int seatValue = Integer.parseInt(normalizedValue);
            if (seatValue < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return seatValue;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    private static EventStatus normalizeStatus(EventStatus selectedStatus, int availableSeats) {
        if (selectedStatus == EventStatus.CANCELLED) {
            return EventStatus.CANCELLED;
        }

        if (availableSeats == 0) {
            return EventStatus.SOLDOUT;
        }

        return EventStatus.AVAILABLE;
    }

    private static int normalizeAvailableSeats(EventStatus selectedStatus, int availableSeats) {
        if (selectedStatus == EventStatus.CANCELLED || selectedStatus == EventStatus.SOLDOUT) {
            return 0;
        }
        return availableSeats;
    }

    private static SimpleDateFormat createFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        formatter.setLenient(false);
        return formatter;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
