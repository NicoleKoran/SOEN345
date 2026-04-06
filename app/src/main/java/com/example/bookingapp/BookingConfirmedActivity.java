package com.example.bookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// Success screen shown after booking is confirmed.

public class BookingConfirmedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmed);

        // Unpack all extras sent from BookingActivity
        String reservationId = getIntent().getStringExtra("reservationId");
        String eventTitle    = getIntent().getStringExtra("eventTitle");
        String eventLocation = getIntent().getStringExtra("eventLocation");
        String eventDate     = getIntent().getStringExtra("eventDate");
        String price         = getIntent().getStringExtra("price");
        String ticketId      = getIntent().getStringExtra("ticketId");
        String seatNumber    = getIntent().getStringExtra("seatNumber");

        // Populate the screen
        ((TextView) findViewById(R.id.confirmedEventTitle))
                .setText(eventTitle);
        ((TextView) findViewById(R.id.confirmedLocation))
                .setText("📍 " + eventLocation);
        ((TextView) findViewById(R.id.confirmedDate))
                .setText("🗓 " + eventDate);
        ((TextView) findViewById(R.id.confirmedPrice))
                .setText("💳 $" + price);
        ((TextView) findViewById(R.id.confirmedReservationId))
                .setText("Reservation ID: " + reservationId);

        // NEW: Show ticket details (from Ticket.generateTicket())
        ((TextView) findViewById(R.id.confirmedTicketId))
                .setText("Ticket: " + ticketId);
        ((TextView) findViewById(R.id.confirmedSeatNumber))
                .setText("Seat: " + seatNumber);

        Button backBtn = findViewById(R.id.goHomeBtn);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}