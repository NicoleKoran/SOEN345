package com.example.bookingapp;

// Any class that wants to be notified about reservation events must implement this interface.

public interface NotificationListener {
    void update(String message);
}