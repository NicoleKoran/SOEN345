package com.example.bookingapp;
public class User {


    protected String userId;
    protected String name;
    protected String email;
    protected String phoneNumber;
    protected String password;

    /** Required empty constructor for Firestore deserialization */
    public User() {}

    public User(String userId, String name, String email,
                String phoneNumber, String password) {
        this.userId      = userId;
        this.name        = name;
        this.email       = email;
        this.phoneNumber = phoneNumber;
        this.password    = password;
    }

// functions defined in BookingRepository and other classes, defined here bc of the UML diagram
    public void cancelReservation() {
        // Delegated to BookingRepository
    }
    public void login() {
        // Delegated to FirebaseAuth — see LoginActivity
    }

    public void searchEvents() {
        // Delegated to MainActivity filter logic
    }


    // Getters
    public String getUserId()      { return userId; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPhoneNumber() { return phoneNumber; }
}