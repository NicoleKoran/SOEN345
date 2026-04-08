package com.example.bookingapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventRepository {
    private final FirebaseFirestore firestore;
    private final CollectionReference eventsCollection;
    private final CollectionReference reservationsCollection;

    public EventRepository() {
        this(FirebaseFirestore.getInstance());
    }

    EventRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
        this.eventsCollection = firestore.collection("events");
        this.reservationsCollection = firestore.collection("reservations");
    }

    public void addEvent(Event event, MessageCallback callback) {
        DocumentReference documentReference = event.getEventId().isEmpty()
                ? eventsCollection.document()
                : eventsCollection.document(event.getEventId());

        documentReference.get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onError("An event with this ID already exists.");
                        return;
                    }

                    event.setEventId(documentReference.getId());
                    documentReference.set(toFirestoreMap(event))
                            .addOnSuccessListener(unused -> callback.onSuccess("Event added with ID " + documentReference.getId() + "."))
                            .addOnFailureListener(error -> callback.onError(error.getMessage()));
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void getEvent(String eventId, EventCallback callback) {
        eventsCollection.document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("Event not found.");
                        return;
                    }

                    try {
                        callback.onSuccess(fromSnapshot(snapshot));
                    } catch (IllegalStateException exception) {
                        callback.onError(exception.getMessage());
                    }
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void getAllEvents(EventsCallback callback) {
        eventsCollection.get()
                .addOnSuccessListener(snapshot -> {
                    List<Event> events = new ArrayList<>();

                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            events.add(fromSnapshot(document));
                        } catch (IllegalStateException ignored) {
                            // Skip malformed events instead of failing the whole admin view.
                        }
                    }

                    Collections.sort(events, Comparator.comparing(Event::getDate));
                    callback.onSuccess(events);
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void updateEvent(Event event, MessageCallback callback) {
        DocumentReference documentReference = eventsCollection.document(event.getEventId());

        documentReference.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        callback.onError("Event not found.");
                        return;
                    }

                    documentReference.set(toFirestoreMap(event))
                            .addOnSuccessListener(unused -> callback.onSuccess("Event updated successfully."))
                            .addOnFailureListener(error -> callback.onError(error.getMessage()));
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void cancelEvent(String eventId, MessageCallback callback) {
        eventsCollection.document(eventId)
                .update("status", EventStatus.CANCELLED.toFirestoreValue())
                .addOnSuccessListener(unused -> callback.onSuccess("Event cancelled successfully."))
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void deleteEvent(String eventId, MessageCallback callback) {
        eventsCollection.document(eventId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess("Event deleted successfully."))
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    public void getReservationsForEvent(String eventId, ReservationsCallback callback) {
        reservationsCollection.whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        callback.onSuccess(toReservationSummaries(snapshot));
                        return;
                    }

                    loadEventSubcollectionReservations(eventId, callback);
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    /** US-10: Fetches all reservations for a specific user, ordered by reservation date. */
    public void getReservationsForUser(String userId, UserReservationsCallback callback) {
        reservationsCollection.whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            Reservation r = document.toObject(Reservation.class);
                            if (r != null) {
                                r.setReservationId(document.getId());
                                reservations.add(r);
                            }
                        } catch (Exception ignored) {
                            // Skip malformed documents
                        }
                    }
                    Collections.sort(reservations, (a, b) -> {
                        if (a.getReservationDate() == null) return 1;
                        if (b.getReservationDate() == null) return -1;
                        return b.getReservationDate().compareTo(a.getReservationDate());
                    });
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    /** US-14: Fetches all confirmed reservations for an event (used before cancelling). */
    public void getConfirmedReservationsForEvent(String eventId, UserReservationsCallback callback) {
        reservationsCollection
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", ReservationStatus.CONFIRMED.toFirestoreValue())
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        try {
                            Reservation r = document.toObject(Reservation.class);
                            if (r != null) {
                                r.setReservationId(document.getId());
                                reservations.add(r);
                            }
                        } catch (Exception ignored) {
                            // Skip malformed documents
                        }
                    }
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    private void loadEventSubcollectionReservations(String eventId, ReservationsCallback callback) {
        firestore.collection("events")
                .document(eventId)
                .collection("reservations")
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(toReservationSummaries(snapshot)))
                .addOnFailureListener(error -> callback.onError(error.getMessage()));
    }

    private List<ReservationSummary> toReservationSummaries(QuerySnapshot snapshot) {
        List<ReservationSummary> reservations = new ArrayList<>();

        for (DocumentSnapshot document : snapshot.getDocuments()) {
            Map<String, Object> fields = document.getData();
            reservations.add(new ReservationSummary(document.getId(), fields == null ? new HashMap<>() : fields));
        }

        return reservations;
    }

    private Map<String, Object> toFirestoreMap(Event event) {
        Map<String, Object> values = new HashMap<>();
        values.put("eventId", event.getEventId());
        values.put("title", event.getTitle());
        values.put("description", event.getDescription());
        values.put("location", event.getLocation());
        values.put("date", event.getDate());
        values.put("totalSeats", event.getTotalSeats());
        values.put("availableSeats", event.getAvailableSeats());
        values.put("category", event.getCategory().toFirestoreValue());
        values.put("status", event.getStatus().toFirestoreValue());
        return values;
    }

    private Event fromSnapshot(DocumentSnapshot snapshot) {
        Map<String, Object> data = snapshot.getData();
        if (data == null) {
            throw new IllegalStateException("Event data is empty.");
        }

        return new Event(
                readString(data, "eventId", snapshot.getId()),
                readString(data, "title", ""),
                readString(data, "description", ""),
                readString(data, "location", ""),
                readDate(data.get("date")),
                readInt(data.get("totalSeats")),
                readInt(data.get("availableSeats")),
                EventCategory.fromValue(readString(data, "category", EventCategory.MOVIE.toFirestoreValue())),
                EventStatus.fromValue(readString(data, "status", EventStatus.AVAILABLE.toFirestoreValue()))
        );
    }

    private String readString(Map<String, Object> data, String key, String fallbackValue) {
        Object value = data.get(key);
        return value == null ? fallbackValue : String.valueOf(value);
    }

    private int readInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private Date readDate(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return new Date();
    }

    public interface MessageCallback {
        void onSuccess(String message);

        void onError(String message);
    }

    public interface EventCallback {
        void onSuccess(Event event);

        void onError(String message);
    }

    public interface ReservationsCallback {
        void onSuccess(List<ReservationSummary> reservations);

        void onError(String message);
    }

    public interface EventsCallback {
        void onSuccess(List<Event> events);

        void onError(String message);
    }

    public interface UserReservationsCallback {
        void onSuccess(List<Reservation> reservations);
        void onError(String message);
    }
}
