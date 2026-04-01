package com.example.bookingapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;
public class Reservation {
    private String eventId;             // capital E — matches your Firestore field
    private String category;
    private int quantity;
    private Timestamp reservationDate;
    private String reservationId;
    private String status;              // uses ReservationStatus enum values
    private Double totalPrice;
    private String userId;

    private String userEmail;
    private String eventTitle;
    private String eventLocation;
    private Timestamp eventDate;
    private boolean emailSent;

    // ── Observer pattern: list of observers ───────────────────────────────────
    // @Exclude tells Firestore to ignore this field (can't serialize interfaces)
    @Exclude
    private List<NotificationListener> observers = new ArrayList<>();

    // Firestore requires empty constructor
    public Reservation() {}

    /**
     * Constructor used when creating a new reservation at booking time.
     * Status starts as "pending" and moves to "confirmed" via confirmReservation().
     */
    public Reservation(String eventId, String category,
                       String userId, String userEmail,
                       String eventTitle, String eventLocation,
                       Timestamp eventDate, Double price) {

        this.eventId         = eventId;
        this.category        = category;
        this.quantity        = 1;
        this.reservationDate = Timestamp.now();
        this.reservationId   = "";
        this.status          = ReservationStatus.pending.name(); // starts as pending
        this.totalPrice      = price;
        this.userId          = userId;
        this.userEmail       = userEmail;
        this.eventTitle      = eventTitle;
        this.eventLocation   = eventLocation;
        this.eventDate       = eventDate;
        this.emailSent       = false;
    }

    // ── Observer pattern methods ───────────────────────────────────────────────

    /**
     * Registers an observer to be notified on reservation events.
     * UML: + attach(observer: NotificationListener): void
     */
    public void attach(NotificationListener observer) {
        observers.add(observer);
    }

    /**
     * Removes an observer.
     * UML: + detach(observer: NotificationListener): void
     */
    public void detach(NotificationListener observer) {
        observers.remove(observer);
    }

    /**
     * Tells all observers something happened.
     * UML: + notifyObservers(message: String): void
     */
    public void notifyObservers(String message) {
        for (NotificationListener observer : observers) {
            observer.update(message); // → triggers emailNotification.update()
        }
    }

    // ── UML business methods ──────────────────────────────────────────────────

    /**
     * Confirms the reservation and notifies all observers.
     * This triggers the email via the Observer pattern.
     * UML: + confirmReservation(): void
     */
    public void confirmReservation() {
        this.status = ReservationStatus.confirmed.name();
        // This one line triggers the entire email chain:
        // notifyObservers → emailNotification.update() → sendEmail()
        notifyObservers("Your booking for '" + eventTitle + "' is confirmed!");
    }

    /**
     * Cancels the reservation and notifies all observers.
     * UML: + cancelReservation(): void
     */
    public void cancelReservation() {
        this.status = ReservationStatus.cancelled.name();
        notifyObservers("Your booking for '" + eventTitle + "' has been cancelled.");
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getEventId()            { return eventId; }
    public String getCategory()           { return category; }
    public int getQuantity()              { return quantity; }
    public Timestamp getReservationDate() { return reservationDate; }
    public String getReservationId()      { return reservationId; }
    public String getStatus()             { return status; }
    public Double getTotalPrice()         { return totalPrice; }
    public String getUserId()             { return userId; }
    public String getUserEmail()          { return userEmail; }
    public String getEventTitle()         { return eventTitle; }
    public String getEventLocation()      { return eventLocation; }
    public Timestamp getEventDate()       { return eventDate; }
    public boolean isEmailSent()          { return emailSent; }





}
