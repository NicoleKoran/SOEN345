package com.example.bookingapp;

import java.util.Date;

public class Notification {

    // ── UML fields ────────────────────────────────────────────────────────────
    private String notificationId;
    private String message;
    private Date sentDate;

    /** Required empty constructor */
    public Notification() {}

    public Notification(String notificationId, String message) {
        this.notificationId = notificationId;
        this.message        = message;
        this.sentDate       = new Date(); // set to now when created
    }

    public void send() {
        // Overridden by emailNotification --> NotificationListener.update()
    }

    // getters
    public String getNotificationId() { return notificationId; }
    public String getMessage()        { return message; }
    public Date getSentDate()         { return sentDate; }

    //  Setters
    public void setMessage(String message)   { this.message = message; }
    public void setSentDate(Date sentDate)   { this.sentDate = sentDate; }
}
