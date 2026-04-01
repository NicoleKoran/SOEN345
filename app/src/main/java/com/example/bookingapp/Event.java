package com.example.bookingapp;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

/** Srab if you already have this file feel free to remove this one **/

public class Event {
    private String eventId;
    private String title;
    private String location;
    private String description;
    private Timestamp date;           // Firebase Timestamp maps to UML Date
    private long totalSeats;
    private long availableSeats;

    // Stored as String in Firestore, converted to enum in code
    private String category;          // "concert", "movie", etc.
    private String status;            // "available", "cancelled", "soldOut"

    // ── Observer pattern fields ───────────────────────────────────────────────
    // Transient means Firestore won't try to serialize this list
    private transient List<NotificationListener> observers = new ArrayList<>();

    // Firestore requires an empty constructor to deserialize documents
    public Event() {}

    // ── Observer pattern methods (from UML right side) ────────────────────────

    /**
     * Registers a new observer.
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
     * Notifies all registered observers with a message.
     * UML: + notifyObservers(message: String): void
     */
    public void notifyObservers(String message) {
        for (NotificationListener observer : observers) {
            observer.update(message); // calls emailNotification.update()
        }
    }

    // ── UML methods ───────────────────────────────────────────────────────────

    /**
     * Returns true if the event has seats and is not cancelled.
     * UML: + checkAvailability(): boolean (implied by availableSeats field)
     */
    public boolean checkAvailability() {
        return availableSeats > 0 && !EventStatus.cancelled.name().equals(status);
    }

    /**
     * Marks the event as cancelled.
     * UML: + cancelEvent(): void
     */
    public void cancelEvent() {
        this.status = EventStatus.cancelled.name();
        notifyObservers("Event '" + title + "' has been cancelled.");
    }

    // ── Getters (Firestore needs these to read the object) ────────────────────
    public String getEventId()        { return eventId; }
    public String getTitle()          { return title; }
    public String getDescription()    { return description; }
    public String getLocation()       { return location; }
    public String getCategory()       { return category; }
    public String getStatus()         { return status; }
    public Timestamp getDate()        { return date; }
    public long getPrice()            { return 0; } // override per event if needed
    public long getAvailableSeats()   { return availableSeats; }
    public long getTotalSeats()       { return totalSeats; }
}
