package com.example.bookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private Button confirmBtn;
    private TextView statusText, eventTitleText, eventLocationText,
            eventDateText, eventPriceText, seatsText;

    // The Event object reconstructed from Intent extras
    private Event currentEvent;
    public BookingRepository bookingRepo; //changed to public for tests

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // ── Wire up UI elements ───────────────────────────────────────────────
        bookingRepo       = new BookingRepository();
        confirmBtn        = findViewById(R.id.confirmBookingBtn);
        statusText        = findViewById(R.id.statusText);
        eventTitleText    = findViewById(R.id.eventTitleText);
        eventLocationText = findViewById(R.id.eventLocationText);
        eventDateText     = findViewById(R.id.eventDateText);
        eventPriceText    = findViewById(R.id.eventPriceText);
        seatsText         = findViewById(R.id.seatsText);

        // ── Receive the event from EventAdapter via Intent ────────────────────
        String eventId       = getIntent().getStringExtra("eventId");
        String eventTitle    = getIntent().getStringExtra("eventTitle");
        String eventLocation = getIntent().getStringExtra("eventLocation");
        String eventDate     = getIntent().getStringExtra("eventDate");
        String eventPrice    = getIntent().getStringExtra("eventPrice");
        String eventStatus   = getIntent().getStringExtra("eventStatus");
        int availableSeats   = getIntent().getIntExtra("availableSeats", 0);

        if (eventId == null) {
            Toast.makeText(this, "Event data missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ── Populate the UI ───────────────────────────────────────────────────
        eventTitleText.setText(eventTitle);
        eventLocationText.setText("📍 " + eventLocation);
        eventDateText.setText("🗓 " + eventDate);
        eventPriceText.setText("$" + eventPrice);
        seatsText.setText(availableSeats + " seats remaining");

        // ── Load the full Event object from Firestore for the transaction ──────
        // We need the full Event so BookingRepository can run the transaction
        confirmBtn.setEnabled(false);
        confirmBtn.setText("Loading...");

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Use EventRepository's deserialization logic
                    // by reading fields manually to stay consistent with the project
                    currentEvent = toEvent(snapshot);

                    // ── Set button state based on EventStatus enum ────────────
                    if (currentEvent.getStatus() == EventStatus.CANCELLED) {
                        confirmBtn.setText("Event Cancelled");
                        // button stays disabled

                    } else if (currentEvent.getAvailableSeats() <= 0
                            || currentEvent.getStatus() == EventStatus.SOLDOUT) {
                        confirmBtn.setText("Sold Out");
                        // button stays disabled

                    } else {
                        confirmBtn.setText("Confirm Booking – $" + eventPrice);
                        confirmBtn.setEnabled(true);
                        confirmBtn.setOnClickListener(v -> confirmBooking());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Converts a Firestore DocumentSnapshot into an Event object.
     * Mirrors the logic in EventRepository.fromSnapshot() to stay consistent.
     */
    private Event toEvent(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        java.util.Map<String, Object> data = snapshot.getData();
        if (data == null) return null;

        return new Event(
                snapshot.getId(),
                data.get("title") == null ? "" : String.valueOf(data.get("title")),
                data.get("description") == null ? "" : String.valueOf(data.get("description")),
                data.get("location") == null ? "" : String.valueOf(data.get("location")),
                readDate(data.get("date")),
                readInt(data.get("totalSeats")),
                readInt(data.get("availableSeats")),
                EventCategory.fromValue(data.get("category") == null
                        ? "movie" : String.valueOf(data.get("category"))),
                EventStatus.fromValue(data.get("status") == null
                        ? "available" : String.valueOf(data.get("status")))
        );
    }

    private java.util.Date readDate(Object value) {
        if (value instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) value).toDate();
        }
        if (value instanceof java.util.Date) return (java.util.Date) value;
        return new java.util.Date();
    }

    private int readInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    /**
     * Called when user taps "Confirm Booking".
     * Disables the button immediately to prevent double-booking,
     * then delegates to BookingRepository.
     */
    private void confirmBooking() {
        // Disable immediately — prevents double-tap
        confirmBtn.setEnabled(false);
        confirmBtn.setText("Booking...");
        statusText.setVisibility(View.GONE);

        bookingRepo.bookEvent(currentEvent, new BookingRepository.BookingCallback() {

            @Override
            public void onSuccess(String reservationId, String eventTitle,
                                  String eventLocation, String eventDate,
                                  String price, String ticketId, String seatNumber) {

                // Pack everything for the confirmation screen
                Intent intent = new Intent(
                        BookingActivity.this, BookingConfirmedActivity.class);
                intent.putExtra("reservationId", reservationId);
                intent.putExtra("eventTitle",    eventTitle);
                intent.putExtra("eventLocation", eventLocation);
                intent.putExtra("eventDate",     eventDate);
                intent.putExtra("price",         price);
                intent.putExtra("ticketId",      ticketId);    // NEW
                intent.putExtra("seatNumber",    seatNumber);  // NEW
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                // Let them try again
                confirmBtn.setEnabled(true);
                confirmBtn.setText("Confirm Booking");
                statusText.setText("Booking failed: " + errorMessage);
                statusText.setVisibility(View.VISIBLE);
            }
        });
    }
}