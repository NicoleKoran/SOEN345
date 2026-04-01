//package com.example.bookingapp;
//
//import com.example.bookingapp.models.Event;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//public class EventSeeder {
//    private static Date getRandomFutureDate() {
//        long now = System.currentTimeMillis();
//        // 30 days in milliseconds: 1000ms * 60s * 60m * 24h * 30d
//        long thirtyDaysInMs = 1000L * 60 * 60 * 24 * 30;
//
//        // Generate a random offset between 1 hour and 30 days
//        long randomOffset = (long) (Math.random() * thirtyDaysInMs);
//
//        return new Date(now + randomOffset);
//    }
//
//    public static void seedEvents() {
//
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        List<Event> events = new ArrayList<>();
//
//        events.add(new Event(
//                null,
//                "Avengers Movie Night",
//                getRandomFutureDate(),
//                "Montreal",
//                "Watch Avengers on the big screen",
//                Event.EventCategory.movie,
//                120,
//                120,
//                Event.EventStatus.available
//        ));
//
//        events.add(new Event(
//                null,
//                "Taylor Swift Concert",
//                getRandomFutureDate(),
//                "Toronto",
//                "Live concert experience",
//                Event.EventCategory.concert,
//                500,
//                500,
//                Event.EventStatus.available
//        ));
//
//        events.add(new Event(
//                null,
//                "Trip to New York",
//                getRandomFutureDate(),
//                "New York",
//                "Weekend group travel",
//                Event.EventCategory.travel,
//                40,
//                40,
//                Event.EventStatus.available
//        ));
//
//        events.add(new Event(
//                null,
//                "MNBA Game",
//                getRandomFutureDate(),
//                "Montreal",
//                "Basketball match",
//                Event.EventCategory.sport,
//                200,
//                200,
//                Event.EventStatus.available
//        ));
//
//        for (Event event : events) {
//
//            String id = db.collection("events").document().getId();
//
//            event.setEventId(id);
//
//            db.collection("events")
//                    .document(id)
//                    .set(event);
//        }
//    }
//}