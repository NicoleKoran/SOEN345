package com.example.bookingapp;

import java.util.Date;

public class Event {
    private String eventId;
    private String title;
    private String description;
    private String location;
    private Date date;
    private int totalSeats;
    private int availableSeats;
    private EventCategory category;
    private EventStatus status;

    public Event(String eventId,
                 String title,
                 String description,
                 String location,
                 Date date,
                 int totalSeats,
                 int availableSeats,
                 EventCategory category,
                 EventStatus status) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.date = date;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.category = category;
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Date getDate() {
        return date;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public EventCategory getCategory() {
        return category;
    }

    public EventStatus getStatus() {
        return status;
    }
}
