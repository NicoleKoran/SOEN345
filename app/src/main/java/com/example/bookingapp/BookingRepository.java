package com.example.bookingapp;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class BookingRepository {

    private static final String TAG = "BookingRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public interface BookingCallback {
        void onSuccess(String reservationId, String eventTitle,
                       String eventLocation, String eventDate,
                       String price, String ticketId, String seatNumber);
        void onFailure(String errorMessage);
    }

    public BookingRepository() {
        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    /** Same-package tests inject mocks without touching production callers. */
    BookingRepository(FirebaseFirestore db, FirebaseAuth mAuth) {
        this.db = db;
        this.mAuth = mAuth;
    }

    // Entry point Customer.reserveTicket() calls it

    public void bookEvent(Event event, BookingCallback callback) {

        // ── Get the logged-in user ────────────────────────────────────────────
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure("You must be logged in to book.");
            return;
        }

        // Build the Customer object (UML: Customer)
        Customer customer = new Customer(
                firebaseUser.getUid(),
                firebaseUser.getDisplayName() != null
                        ? firebaseUser.getDisplayName() : "",
                firebaseUser.getEmail(),
                firebaseUser.getPhoneNumber() != null
                        ? firebaseUser.getPhoneNumber() : "",
                "" // password not stored client-side
        );

        //  Firestore references
        DocumentReference eventRef =
                db.collection("events").document(event.getEventId());
        DocumentReference reservationRef =
                db.collection("reservations").document();

        //email
        String formattedDate = "Date TBD";
        if (event.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            formattedDate = sdf.format(event.getDate());
        }
        final String finalFormattedDate = formattedDate;

        // All reads and writes happen atomically
        db.runTransaction(transaction -> {

            DocumentSnapshot eventSnap = transaction.get(eventRef);

            if (!eventSnap.exists()) {
                throw new FirebaseFirestoreException("Event not found.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String statusStr = eventSnap.getString("status");
            if (EventStatus.CANCELLED.toFirestoreValue().equals(statusStr)) {
                throw new FirebaseFirestoreException(
                        "This event has been cancelled.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            Long availableSeats = eventSnap.getLong("availableSeats");
            if (availableSeats == null || availableSeats <= 0) {
                throw new FirebaseFirestoreException(
                        "Sorry, this event is sold out.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            // Decrement seat count (Acceptance Criterion: seat count decremented)
            long newSeatCount = availableSeats - 1;
            transaction.update(eventRef, "availableSeats", newSeatCount);

            // If last seat taken, mark event as soldOut
            if (newSeatCount == 0) {
                transaction.update(eventRef, "status",
                        EventStatus.SOLDOUT.toFirestoreValue());
            }

            // Write reservation to Firestore (status = "pending" at this point)
            Reservation reservation = new Reservation(
                    event.getEventId(),
                    event.getCategory().toFirestoreValue(),
                    customer.getUserId(),
                    customer.getEmail(),
                    event.getTitle(),
                    event.getLocation(),
                    event.getDate(),
                    event.getAvailableSeats() // using as price placeholder
            );
            transaction.set(reservationRef, reservation);

            return null;

        }).addOnSuccessListener(unused -> {

            //Reservation in memory for the Observer pattern
            Reservation reservation = new Reservation(
                    event.getEventId(),
                    event.getCategory().toFirestoreValue(),
                    customer.getUserId(),
                    customer.getEmail(),
                    event.getTitle(),
                    event.getLocation(),
                    event.getDate(),
                    0
            );

            reservation.setReservationId(reservationRef.getId());

            // Attach email observer
            emailNotification emailObserver = new emailNotification(
                    reservationRef.getId(),
                    customer.getEmail(),
                    event.getTitle(),
                    event.getLocation(),
                    finalFormattedDate,
                    String.valueOf(event.getAvailableSeats()),
                    reservationRef.getId()
            );
            reservation.attach(emailObserver);
            reservation.confirmReservation();

            customer.receiveConfirmation(reservation);

            // Get the generated ticket info for confirmed booking xml
            Ticket ticket = reservation.getGeneratedTicket();
            String ticketId    = ticket != null ? ticket.getTicketId()   : "N/A";
            String seatNumber  = ticket != null ? ticket.getSeatNumber() : "N/A";

            // Firestore update
            reservationRef.update(
                    "status",    ReservationStatus.CONFIRMED.toFirestoreValue(),
                    "emailSent", true
            ).addOnFailureListener(e ->
                    Log.e(TAG, "Could not update reservation status: " + e.getMessage()));

            Log.d(TAG, "Booking complete. Reservation: " + reservationRef.getId()
                    + " Ticket: " + ticketId);

            // UI: success
            callback.onSuccess(
                    reservationRef.getId(),
                    event.getTitle(),
                    event.getLocation(),
                    finalFormattedDate,
                    String.valueOf(event.getAvailableSeats()),
                    ticketId,
                    seatNumber
            );

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed: " + e.getMessage());
            callback.onFailure(e.getMessage());
        });
    }
}
