package com.example.bookingapp.models;

import com.google.firebase.firestore.Exclude; // Added this
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
        movie,
        concert,
        travel,
        sport
    }

    public enum EventStatus {
        available,
        cancelled,
        soldOut
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
        this.category = category.name(); // convert enum → String
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.status = status.name(); // convert enum → String
        this.price = price;
    }

    // eventId
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    // title
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // date
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    // location
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // category (returns enum)
    @Exclude
    public EventCategory getCategory() {
        if (category == null) return null;
        try {
            return EventCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // category (accepts enum)
    @Exclude // Added this
    public void setCategory(EventCategory category) {
        this.category = category.name();
    }

    // needed for Firebase deserialization
    public void setCategory(String category) {
        this.category = category;
    }

    // status (returns enum)
    @Exclude // Added this
    public EventStatus getStatus() {
        return EventStatus.valueOf(status);
    }

    // status (accepts enum)
    @Exclude // Added this
    public void setStatus(EventStatus status) {
        this.status = status.name();
    }

    // needed for Firebase deserialization
    public void setStatus(String status) {
        this.status = status;
    }

    // totalSeats
    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    // availableSeats
    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public int getPrice(){
        return price;
    }

    public void setPrice(int price){
        this.price = price;
    }
    public String getCategoryString() { return category; }
    public String getStatusString() { return status; }
}