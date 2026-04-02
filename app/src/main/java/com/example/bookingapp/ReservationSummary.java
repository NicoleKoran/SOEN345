package com.example.bookingapp;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReservationSummary {
    private final String reservationId;
    private final Map<String, Object> fields;

    public ReservationSummary(String reservationId, Map<String, Object> fields) {
        this.reservationId = reservationId;
        this.fields = fields;
    }

    public String toDisplayString() {
        List<String> keys = new ArrayList<>(fields.keySet());
        Collections.sort(keys);

        StringBuilder builder = new StringBuilder();
        builder.append("Reservation ID: ").append(reservationId);

        for (String key : keys) {
            if ("eventId".equals(key)) {
                continue;
            }
            builder.append("\n")
                    .append(key)
                    .append(": ")
                    .append(formatValue(fields.get(key)));
        }

        return builder.toString();
    }

    private String formatValue(Object value) {
        if (value instanceof Timestamp) {
            return formatDate(((Timestamp) value).toDate());
        }
        if (value instanceof Date) {
            return formatDate((Date) value);
        }
        return String.valueOf(value);
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(date);
    }
}
