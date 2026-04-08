package com.example.bookingapp;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Reservation {

    private static final String TAG = "Reservation";

    //Firestore
    private String eventId;
    private String category;
    private int quantity;
    private Date reservationDate;
    private String reservationId;
    private String status;
    private String totalPrice;
    private String userId;

    // info for email
    private String userEmail;
    private String eventTitle;
    private String eventLocation;
    private Date eventDate;
    private boolean emailSent;

    /**
     * Persisted in Firestore so the UI can show the correct label.
     * Values: null (not cancelled), "user_cancelled", "event_cancelled"
     */
    private String cancellationReason;

    // ── Observer pattern ──────────────────────────────────────────────────────
    // transient = Firestore won't try to serialize this list
    private transient List<NotificationListener> observers = new ArrayList<>();

    // ── Generated ticket (from sequence diagram) ──────────────────────────────
    // Created by confirmReservation() → Ticket.generateTicket()
    private transient Ticket generatedTicket;

    /** Required empty constructor for Firestore */
    public Reservation() {}

    public Reservation(String eventId, String category,
                       String userId, String userEmail,
                       String eventTitle, String eventLocation,
                       Date eventDate, int price) {
        this.eventId         = eventId;
        this.category        = category;
        this.quantity        = 1;
        this.reservationDate = new Date();
        this.reservationId   = "";
        this.status          = ReservationStatus.PENDING.toFirestoreValue();
        this.totalPrice      = String.valueOf(price);
        this.userId          = userId;
        this.userEmail       = userEmail;
        this.eventTitle      = eventTitle;
        this.eventLocation   = eventLocation;
        this.eventDate       = eventDate;
        this.emailSent       = false;
    }

    // Observer pattern

    public void attach(NotificationListener observer) {
        observers.add(observer);
    }
    public void detach(NotificationListener observer) {
        observers.remove(observer);
    }
    public void notifyObservers(String message) {
        for (NotificationListener observer : observers) {
            observer.update(message);
        }
    }


    public void confirmReservation() {
        // Step 1: Update status
        this.status = ReservationStatus.CONFIRMED.toFirestoreValue();

        // Step 2: Generate the ticket (sequence diagram: Reservation → Ticket)
        // This mirrors: Reservation -> Ticket: generateTicket()
        this.generatedTicket = Ticket.generateTicket(this);
        Log.d(TAG, "Ticket generated: " + generatedTicket.getTicketId()
                + " seat: " + generatedTicket.getSeatNumber());

        // Step 3: Notify observers — triggers emailNotification → sendEmail()
        // Message shown to customer and included in the email
        notifyObservers("Your booking for '" + eventTitle + "' is confirmed! "
                + "Ticket: " + generatedTicket.getTicketId()
                + ", Seat: " + generatedTicket.getSeatNumber());
    }


    public void cancelReservation() {
        this.status = ReservationStatus.CANCELLED.toFirestoreValue();
        notifyObservers("Your booking for '" + eventTitle + "' has been cancelled.");
    }


    public String getEventId()        { return eventId; }
    public String getCategory()       { return category; }
    public int getQuantity()          { return quantity; }
    public Date getReservationDate()  { return reservationDate; }
    public String getReservationId()  { return reservationId; }
    public String getStatus()         { return status; }
    public String getTotalPrice()     { return totalPrice; }
    public String getUserId()         { return userId; }
    public String getUserEmail()      { return userEmail; }
    public String getEventTitle()     { return eventTitle; }
    public String getEventLocation()  { return eventLocation; }
    public Date getEventDate()        { return eventDate; }
    public boolean isEmailSent()      { return emailSent; }
    public Ticket getGeneratedTicket(){ return generatedTicket; }
    public String getCancellationReason() { return cancellationReason; }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }
    public void setEmailSent(boolean emailSent) {
        this.emailSent = emailSent;
    }
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}