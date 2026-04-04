package com.example.bookingapp;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer who can browse events and make reservations
 * In Android, the "current customer" is always the Firebase logged-in user.
 *
 */
public class Customer extends User {

    // The customer's list of reservations — loaded from Firestore when needed
    private List<Reservation> reservations;

    /** Required empty constructor */
    public Customer() {
        this.reservations = new ArrayList<>();
    }

    public Customer(String userId, String name, String email,
                    String phoneNumber, String password) {
        super(userId, name, email, phoneNumber, password);
        this.reservations = new ArrayList<>();
    }
// register() is handled by RegisterEmailActivity, defining it due to UML diagram
    public void register() {
        // Delegated to RegisterEmailActivity / RegisterPhoneActivity
    }
    public void viewReservations() {
        // Delegated to a future ReservationsActivity / Firestore query
    }


    /**
     * Initiates a ticket reservation for an event.
     * In Android, this is triggered when the user taps "Confirm Booking
     * in BookingActivity, which calls BookingRepository.bookEvent()
     * repo is the BookingRepository that handles Firestore + email
     *  callback is the result passed back to the UI
     */
    public void reserveTicket(Event event,
                              BookingRepository repo,
                              BookingRepository.BookingCallback callback) {
        // Delegates to BookingRepository which runs the Firestore transaction
        // and triggers the Observer pattern for email confirmation
        repo.bookEvent(event, callback);
    }

    public void receiveConfirmation(Reservation reservation) {
        // The emailNotification observer handles the actual email delivery.
        // This method represents the conceptual "receive" step in the UML.
        reservations.add(reservation);
    }

    // Getters / Setters
    public List<Reservation> getReservations() {
        return reservations;
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
    }
}