package com.example.bookingapp.models;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

public class Event {
    private String eventId;
    private String title;
    private Date date;
    private String location;
    private String description;
    private String category;
    private String status;
    private int totalSeats;
    private int availableSeats;
    private int price;

    public enum EventCategory {
        movie, concert, travel, sport
    }

    public enum EventStatus {
        available, cancelled, soldOut
    }

    // default constructor
    public Event() {}

    // parameterized constructor
    public Event(String eventId, String title, Date date, String location,
                 String description, EventCategory category,
                 int totalSeats, int availableSeats, EventStatus status, int price) {
        this.eventId = eventId;
        this.title = title;
        this.date = date;
        this.location = location;
        this.description = description;
        this.category = category.name();
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.status = status.name();
        this.price = price;
    }

    // eventId
    public String getEventId() { return eventId; }

    // title
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    // date
    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    // location
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // description
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // category - plain String getter/setter for Firebase
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // category - enum getter/setter for Java code
    @Exclude
    public EventCategory getCategoryEnum() {
        if (category == null) return null;
        try {
            return EventCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Exclude
    public void setCategoryEnum(EventCategory category) {
        this.category = category.name();
    }

    // status - plain String getter/setter for Firebase
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // status - enum getter/setter for Java code
    @Exclude
    public EventStatus getStatusEnum() {
        if (status == null) return null;
        try {
            return EventStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Exclude
    public void setStatusEnum(EventStatus status) {
        this.status = status.name();
    }

    // totalSeats
    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    // availableSeats
    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    // price
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
}