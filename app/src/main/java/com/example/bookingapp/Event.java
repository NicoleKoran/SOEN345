package com.example.bookingapp;

import com.google.firebase.firestore.Exclude;

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
    private int price;

    public Event() {
    }

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

    public Event(String eventId,
                 String title,
                 Date date,
                 String location,
                 String description,
                 EventCategory category,
                 int totalSeats,
                 int availableSeats,
                 EventStatus status,
                 int price) {
        this(eventId, title, description, location, date, totalSeats, availableSeats, category, status);
        this.price = price;
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public void setCategory(String category) {
        if (category == null) {
            this.category = null;
            return;
        }
        try {
            this.category = EventCategory.fromValue(category);
        } catch (IllegalArgumentException exception) {
            this.category = null;
        }
    }

    @Exclude
    public EventCategory getCategoryEnum() {
        return category;
    }

    @Exclude
    public void setCategoryEnum(EventCategory category) {
        this.category = category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        if (status == null) {
            this.status = null;
            return;
        }
        try {
            this.status = EventStatus.fromValue(status);
        } catch (IllegalArgumentException exception) {
            this.status = null;
        }
    }

    @Exclude
    public EventStatus getStatusEnum() {
        return status;
    }

    @Exclude
    public void setStatusEnum(EventStatus status) {
        this.status = status;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}
