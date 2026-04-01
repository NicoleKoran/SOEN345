package com.example.bookingapp;

/**
 * Observer interface from the UML diagram.
 *
 * This is the core of the Observer Design Pattern:
 *   - Any class that wants to be notified about reservation events
 *     must implement this interface.
 *   - When something happens ( booking confirmed), the Reservation
 *     class calls update() on every registered observer
 *
 * emailNotification implements this interface,
 * so it gets called automatically when a reservation is confirmed
 *
 * UML: <<interface>> NotificationListener
 *   + update(message: String): void
 */
public interface NotificationListener {
    /**
     * Called by the subject (Reservation)
     * a human readable description of what happened,
     *                e.g. "Booking confirmed for Tame Impala Concert"
     */
    void update(String message);
}
