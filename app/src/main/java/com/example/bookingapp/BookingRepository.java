package com.example.bookingapp;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.List;
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

    public interface SimpleCallback {
        void onSuccess(String message);
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

    /**
     * US-11: Cancels a reservation atomically — sets status to CANCELLED and
     * restores the seat to the event. Also sends a cancellation email via the
     * Observer pattern.
     */
    public void cancelReservation(String reservationId, String eventId,
                                  String userEmail, String eventTitle,
                                  String eventLocation, String eventDate,
                                  SimpleCallback callback) {

        DocumentReference reservationRef =
                db.collection("reservations").document(reservationId);
        DocumentReference eventRef =
                db.collection("events").document(eventId);

        db.runTransaction(transaction -> {
            DocumentSnapshot eventSnap = transaction.get(eventRef);
            if (!eventSnap.exists()) {
                throw new FirebaseFirestoreException("Event not found.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            DocumentSnapshot reservationSnap = transaction.get(reservationRef);
            if (!reservationSnap.exists()) {
                throw new FirebaseFirestoreException("Reservation not found.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            String currentStatus = reservationSnap.getString("status");
            if (ReservationStatus.CANCELLED.toFirestoreValue().equals(currentStatus)) {
                throw new FirebaseFirestoreException(
                        "Reservation is already cancelled.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            // Mark reservation cancelled with reason so the UI can distinguish it
            transaction.update(reservationRef,
                    "status",             ReservationStatus.CANCELLED.toFirestoreValue(),
                    "cancellationReason", "user_cancelled");

            // Restore the seat on the event (if event is not cancelled itself)
            String eventStatus = eventSnap.getString("status");
            if (!EventStatus.CANCELLED.toFirestoreValue().equals(eventStatus)) {
                Long available = eventSnap.getLong("availableSeats");
                long newAvailable = (available != null ? available : 0) + 1;
                transaction.update(eventRef, "availableSeats", newAvailable);
                // If it was sold out, mark available again
                if (EventStatus.SOLDOUT.toFirestoreValue().equals(eventStatus)) {
                    transaction.update(eventRef, "status",
                            EventStatus.AVAILABLE.toFirestoreValue());
                }
            }

            return null;
        }).addOnSuccessListener(unused -> {
            // Send cancellation email notification via Observer pattern
            Reservation reservation = new Reservation();
            reservation.setReservationId(reservationId);
            reservation.setEventTitle(eventTitle);
            reservation.setUserEmail(userEmail);

            emailNotification emailObserver = new emailNotification(
                    reservationId,
                    userEmail,
                    eventTitle,
                    eventLocation,
                    eventDate,
                    "",
                    reservationId,
                    emailNotification.NotificationType.USER_CANCELLATION
            );
            reservation.attach(emailObserver);
            reservation.cancelReservation();

            Log.d(TAG, "Reservation cancelled: " + reservationId);
            callback.onSuccess("Reservation cancelled successfully.");

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Cancel reservation failed: " + e.getMessage());
            callback.onFailure(e.getMessage());
        });
    }

    /**
     * US-14: Cancels an event and emails all customers who have confirmed
     * reservations, notifying them that the event was cancelled.
     */
    public void cancelEventWithNotifications(String eventId, String eventTitle,
                                             String eventLocation, String eventDate,
                                             SimpleCallback callback) {

        EventRepository eventRepository = new EventRepository(db);

        eventRepository.cancelEvent(eventId, new EventRepository.MessageCallback() {
            @Override
            public void onSuccess(String message) {
                // Cancel all confirmed reservations and notify their holders
                eventRepository.getConfirmedReservationsForEvent(eventId,
                        new EventRepository.UserReservationsCallback() {
                            @Override
                            public void onSuccess(List<Reservation> reservations) {
                                if (reservations.isEmpty()) {
                                    callback.onSuccess("Event cancelled. No customers to notify.");
                                    return;
                                }

                                // Batch-update every reservation status in Firestore so
                                // the "My Bookings" screen reflects the cancellation immediately.
                                WriteBatch batch = db.batch();
                                for (Reservation reservation : reservations) {
                                    batch.update(
                                            db.collection("reservations")
                                              .document(reservation.getReservationId()),
                                            "status",             ReservationStatus.CANCELLED.toFirestoreValue(),
                                            "cancellationReason", "event_cancelled"
                                    );
                                }

                                batch.commit().addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.e(TAG, "Batch reservation cancel failed: "
                                                + (task.getException() != null
                                                   ? task.getException().getMessage() : "unknown"));
                                    }
                                });

                                // Send EVENT_CANCELLATION email to each affected customer
                                for (Reservation reservation : reservations) {
                                    if (reservation.getUserEmail() == null
                                            || reservation.getUserEmail().isEmpty()) {
                                        continue;
                                    }
                                    emailNotification emailObserver = new emailNotification(
                                            reservation.getReservationId(),
                                            reservation.getUserEmail(),
                                            eventTitle,
                                            eventLocation,
                                            eventDate,
                                            "",
                                            reservation.getReservationId(),
                                            emailNotification.NotificationType.EVENT_CANCELLATION
                                    );
                                    reservation.attach(emailObserver);
                                    reservation.cancelReservation();
                                }

                                Log.d(TAG, "Event cancelled and " + reservations.size()
                                        + " customers notified.");
                                callback.onSuccess("Event cancelled. "
                                        + reservations.size() + " customer(s) notified.");
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Could not load reservations for notification: " + error);
                                callback.onSuccess("Event cancelled (notification error: " + error + ").");
                            }
                        });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }
}
