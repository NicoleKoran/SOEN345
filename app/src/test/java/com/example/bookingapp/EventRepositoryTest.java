package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import com.google.android.gms.tasks.Tasks;
import androidx.test.core.app.ApplicationProvider;
import com.google.firebase.Timestamp;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class EventRepositoryTest {

    private EventRepository repository;
    private FirebaseFirestore firestore;
    private CollectionReference eventsCollection;
    private CollectionReference reservationsCollection;

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        firestore = mock(FirebaseFirestore.class);
        eventsCollection = mock(CollectionReference.class);
        reservationsCollection = mock(CollectionReference.class);
        when(firestore.collection("events")).thenReturn(eventsCollection);
        when(firestore.collection("reservations")).thenReturn(reservationsCollection);
        repository = new EventRepository(firestore);
    }

    @Test
    public void toFirestoreMap_includesAllEditableFields() throws Exception {
        Event event = new Event(
                "event-1",
                "Jazz Night",
                "Live music",
                "Montreal",
                new Date(1775452380000L),
                150,
                45,
                EventCategory.CONCERT,
                EventStatus.AVAILABLE
        );

        Map<?, ?> values = (Map<?, ?>) invokePrivate(repository, "toFirestoreMap", Event.class, event);

        assertEquals("event-1", values.get("eventId"));
        assertEquals("Jazz Night", values.get("title"));
        assertEquals("Live music", values.get("description"));
        assertEquals("Montreal", values.get("location"));
        assertEquals(150, values.get("totalSeats"));
        assertEquals(45, values.get("availableSeats"));
        assertEquals("concert", values.get("category"));
        assertEquals("available", values.get("status"));
        assertNotNull(values.get("date"));
    }

    @Test
    public void fromSnapshot_mapsFieldsAndAppliesFallbacks() throws Exception {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        Date date = new Date(1775452380000L);
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Movie Night");
        data.put("description", "Marvel marathon");
        data.put("location", "Toronto");
        data.put("date", new Timestamp(date));
        data.put("totalSeats", 120L);
        data.put("availableSeats", 80L);
        data.put("category", "movie");
        data.put("status", "soldOut");

        when(snapshot.getId()).thenReturn("doc-7");
        when(snapshot.getData()).thenReturn(data);

        Event event = (Event) invokePrivate(repository, "fromSnapshot", DocumentSnapshot.class, snapshot);

        assertEquals("doc-7", event.getEventId());
        assertEquals("Movie Night", event.getTitle());
        assertEquals("Marvel marathon", event.getDescription());
        assertEquals("Toronto", event.getLocation());
        assertEquals(date, event.getDate());
        assertEquals(120, event.getTotalSeats());
        assertEquals(80, event.getAvailableSeats());
        assertEquals(EventCategory.MOVIE, event.getCategory());
        assertEquals(EventStatus.SOLDOUT, event.getStatus());
    }

    @Test
    public void fromSnapshot_withNullDataThrowsHelpfulError() {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.getData()).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> invokePrivate(repository, "fromSnapshot", DocumentSnapshot.class, snapshot)
        );

        assertEquals("Event data is empty.", exception.getMessage());
    }

    @Test
    public void toReservationSummaries_buildsDisplayableSummaries() throws Exception {
        DocumentSnapshot first = mock(DocumentSnapshot.class);
        when(first.getId()).thenReturn("res-1");
        when(first.getData()).thenReturn(new HashMap<String, Object>() {{
            put("name", "Jane Doe");
            put("tickets", 2);
        }});

        DocumentSnapshot second = mock(DocumentSnapshot.class);
        when(second.getId()).thenReturn("res-2");
        when(second.getData()).thenReturn(null);

        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        when(snapshot.getDocuments()).thenReturn(List.of(first, second));

        @SuppressWarnings("unchecked")
        List<ReservationSummary> summaries = (List<ReservationSummary>) invokePrivate(
                repository,
                "toReservationSummaries",
                QuerySnapshot.class,
                snapshot
        );

        assertEquals(2, summaries.size());
        assertEquals("Reservation ID: res-1\nname: Jane Doe\ntickets: 2", summaries.get(0).toDisplayString());
        assertEquals("Reservation ID: res-2", summaries.get(1).toDisplayString());
    }

    @Test
    public void readInt_returnsNumberValueOrZero() throws Exception {
        assertEquals(42, invokePrivate(repository, "readInt", Object.class, 42L));
        assertEquals(0, invokePrivate(repository, "readInt", Object.class, "42"));
        assertEquals(0, invokePrivate(repository, "readInt", Object.class, null));
    }

    @Test
    public void readDate_supportsTimestampDateAndFallback() throws Exception {
        Date date = new Date(1775452380000L);

        assertEquals(date, invokePrivate(repository, "readDate", Object.class, new Timestamp(date)));
        assertEquals(date, invokePrivate(repository, "readDate", Object.class, date));
        assertNotNull(invokePrivate(repository, "readDate", Object.class, "invalid"));
    }

    @Test
    public void readString_returnsFallbackWhenValueMissing() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Jazz Night");

        assertEquals("Jazz Night", invokePrivate(repository, "readString", Map.class, String.class, String.class, data, "title", "fallback"));
        assertEquals("fallback", invokePrivate(repository, "readString", Map.class, String.class, String.class, data, "missing", "fallback"));
    }

    @Test
    public void addEvent_withExistingIdReturnsDuplicateError() {
        Event event = sampleEvent();
        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(eventsCollection.document("event-1")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(true);

        TestMessageCallback callback = new TestMessageCallback();
        repository.addEvent(event, callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("An event with this ID already exists.", callback.errorMessage);
    }

    @Test
    public void addEvent_withoutIdUsesGeneratedDocumentAndSetsId() {
        Event event = new Event("", "Jazz Night", "Live music", "Montreal", new Date(1775452380000L), 150, 45, EventCategory.CONCERT, EventStatus.AVAILABLE);
        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(eventsCollection.document()).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(false);
        when(documentReference.getId()).thenReturn("generated-id");
        when(documentReference.set(any())).thenReturn(Tasks.forResult(null));

        TestMessageCallback callback = new TestMessageCallback();
        repository.addEvent(event, callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("generated-id", event.getEventId());
        assertTrue(callback.successMessage.contains("generated-id"));
    }

    @Test
    public void getEvent_fromSnapshotThrows_callsOnError() {
        // Covers the catch(IllegalStateException) block in getEvent() (lines 64-65)
        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(eventsCollection.document("bad-event")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getData()).thenReturn(null); // null data causes fromSnapshot() to throw

        TestEventCallback callback = new TestEventCallback();
        repository.getEvent("bad-event", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Event data is empty.", callback.errorMessage);
    }

    @Test
    public void getEvent_whenMissingReturnsNotFound() {
        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(eventsCollection.document("missing")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(false);

        TestEventCallback callback = new TestEventCallback();
        repository.getEvent("missing", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Event not found.", callback.errorMessage);
    }

    @Test
    public void getEvent_whenPresentReturnsMappedEvent() {
        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(eventsCollection.document("event-1")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(true);
        when(snapshot.getId()).thenReturn("event-1");
        when(snapshot.getData()).thenReturn(snapshotData("Jazz Night", "concert", "available"));

        TestEventCallback callback = new TestEventCallback();
        repository.getEvent("event-1", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Jazz Night", callback.event.getTitle());
        assertEquals(EventCategory.CONCERT, callback.event.getCategory());
    }

    @Test
    public void getAllEvents_sortsByDateAndSkipsMalformedEntries() {
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot later = mock(DocumentSnapshot.class);
        DocumentSnapshot malformed = mock(DocumentSnapshot.class);
        DocumentSnapshot earlier = mock(DocumentSnapshot.class);
        when(eventsCollection.get()).thenReturn(Tasks.forResult(querySnapshot));
        when(querySnapshot.getDocuments()).thenReturn(List.of(later, malformed, earlier));

        when(later.getId()).thenReturn("later");
        when(later.getData()).thenReturn(snapshotData("Later", "concert", "available", new Date(2000L)));
        when(malformed.getData()).thenReturn(null);
        when(earlier.getId()).thenReturn("earlier");
        when(earlier.getData()).thenReturn(snapshotData("Earlier", "movie", "available", new Date(1000L)));

        TestEventsCallback callback = new TestEventsCallback();
        repository.getAllEvents(callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(2, callback.events.size());
        assertEquals("Earlier", callback.events.get(0).getTitle());
        assertEquals("Later", callback.events.get(1).getTitle());
    }

    @Test
    public void updateEvent_whenMissingReturnsNotFound() {
        Event event = sampleEvent();
        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(eventsCollection.document("event-1")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(false);

        TestMessageCallback callback = new TestMessageCallback();
        repository.updateEvent(event, callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Event not found.", callback.errorMessage);
    }

    @Test
    public void updateEvent_whenPresentSavesAndReturnsSuccess() {
        Event event = sampleEvent();
        DocumentReference documentReference = mock(DocumentReference.class);
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(eventsCollection.document("event-1")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(true);
        when(documentReference.set(any())).thenReturn(Tasks.forResult(null));

        TestMessageCallback callback = new TestMessageCallback();
        repository.updateEvent(event, callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Event updated successfully.", callback.successMessage);
    }

    @Test
    public void cancelEvent_successAndFailurePaths() {
        DocumentReference successRef = mock(DocumentReference.class);
        when(eventsCollection.document("event-1")).thenReturn(successRef);
        when(successRef.update("status", EventStatus.CANCELLED.toFirestoreValue())).thenReturn(Tasks.forResult(null));

        TestMessageCallback success = new TestMessageCallback();
        repository.cancelEvent("event-1", success);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Event cancelled successfully.", success.successMessage);

        DocumentReference failureRef = mock(DocumentReference.class);
        when(eventsCollection.document("event-2")).thenReturn(failureRef);
        when(failureRef.update("status", EventStatus.CANCELLED.toFirestoreValue()))
                .thenReturn(Tasks.forException(new RuntimeException("cancel failed")));

        TestMessageCallback failure = new TestMessageCallback();
        repository.cancelEvent("event-2", failure);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("cancel failed", failure.errorMessage);
    }

    @Test
    public void deleteEvent_successAndFailurePaths() {
        DocumentReference successRef = mock(DocumentReference.class);
        when(eventsCollection.document("event-1")).thenReturn(successRef);
        when(successRef.delete()).thenReturn(Tasks.forResult(null));

        TestMessageCallback success = new TestMessageCallback();
        repository.deleteEvent("event-1", success);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Event deleted successfully.", success.successMessage);

        DocumentReference failureRef = mock(DocumentReference.class);
        when(eventsCollection.document("event-2")).thenReturn(failureRef);
        when(failureRef.delete()).thenReturn(Tasks.forException(new RuntimeException("delete failed")));

        TestMessageCallback failure = new TestMessageCallback();
        repository.deleteEvent("event-2", failure);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("delete failed", failure.errorMessage);
    }

    @Test
    public void getReservationsForEvent_returnsTopLevelReservationsWhenPresent() {
        Query query = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot reservation = mock(DocumentSnapshot.class);
        when(reservationsCollection.whereEqualTo("eventId", "event-1")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forResult(querySnapshot));
        when(querySnapshot.isEmpty()).thenReturn(false);
        when(querySnapshot.getDocuments()).thenReturn(List.of(reservation));
        when(reservation.getId()).thenReturn("res-1");
        when(reservation.getData()).thenReturn(new HashMap<String, Object>() {{
            put("name", "Jane Doe");
        }});

        TestReservationsCallback callback = new TestReservationsCallback();
        repository.getReservationsForEvent("event-1", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, callback.reservations.size());
        assertTrue(callback.reservations.get(0).toDisplayString().contains("Jane Doe"));
    }

    @Test
    public void getReservationsForEvent_fallsBackToEventSubcollection() {
        Query query = mock(Query.class);
        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        CollectionReference subReservations = mock(CollectionReference.class);
        QuerySnapshot subSnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot reservation = mock(DocumentSnapshot.class);
        DocumentReference eventRef = mock(DocumentReference.class);

        when(reservationsCollection.whereEqualTo("eventId", "event-1")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forResult(emptySnapshot));
        when(emptySnapshot.isEmpty()).thenReturn(true);
        when(eventsCollection.document("event-1")).thenReturn(eventRef);
        when(eventRef.collection("reservations")).thenReturn(subReservations);
        when(subReservations.get()).thenReturn(Tasks.forResult(subSnapshot));
        when(subSnapshot.getDocuments()).thenReturn(List.of(reservation));
        when(reservation.getId()).thenReturn("res-2");
        when(reservation.getData()).thenReturn(new HashMap<String, Object>() {{
            put("tickets", 2);
        }});

        TestReservationsCallback callback = new TestReservationsCallback();
        repository.getReservationsForEvent("event-1", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, callback.reservations.size());
        assertTrue(callback.reservations.get(0).toDisplayString().contains("tickets: 2"));
    }

    @Test
    public void getReservationsForEvent_propagatesQueryFailure() {
        Query query = mock(Query.class);
        when(reservationsCollection.whereEqualTo("eventId", "event-1")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forException(new RuntimeException("lookup failed")));

        TestReservationsCallback callback = new TestReservationsCallback();
        repository.getReservationsForEvent("event-1", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("lookup failed", callback.errorMessage);
    }

    @Test
    public void addEvent_propagatesInitialLookupFailure() {
        Event event = sampleEvent();
        DocumentReference documentReference = mock(DocumentReference.class);
        when(eventsCollection.document("event-1")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forException(new RuntimeException("lookup failed")));

        TestMessageCallback callback = new TestMessageCallback();
        repository.addEvent(event, callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("lookup failed", callback.errorMessage);
    }

    @Test
    public void updateEvent_propagatesLookupFailure() {
        Event event = sampleEvent();
        DocumentReference documentReference = mock(DocumentReference.class);
        when(eventsCollection.document("event-1")).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(Tasks.forException(new RuntimeException("lookup failed")));

        TestMessageCallback callback = new TestMessageCallback();
        repository.updateEvent(event, callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("lookup failed", callback.errorMessage);
    }

    private Event sampleEvent() {
        return new Event(
                "event-1",
                "Jazz Night",
                "Live music",
                "Montreal",
                new Date(1775452380000L),
                150,
                45,
                EventCategory.CONCERT,
                EventStatus.AVAILABLE
        );
    }

    private Map<String, Object> snapshotData(String title, String category, String status) {
        return snapshotData(title, category, status, new Date(1775452380000L));
    }

    private Map<String, Object> snapshotData(String title, String category, String status, Date date) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", "Description");
        data.put("location", "Montreal");
        data.put("date", date);
        data.put("totalSeats", 100L);
        data.put("availableSeats", 50L);
        data.put("category", category);
        data.put("status", status);
        return data;
    }

    private static final class TestMessageCallback implements EventRepository.MessageCallback {
        String successMessage;
        String errorMessage;

        @Override
        public void onSuccess(String message) {
            successMessage = message;
        }

        @Override
        public void onError(String message) {
            errorMessage = message;
        }
    }

    private static final class TestEventCallback implements EventRepository.EventCallback {
        Event event;
        String errorMessage;

        @Override
        public void onSuccess(Event event) {
            this.event = event;
        }

        @Override
        public void onError(String message) {
            errorMessage = message;
        }
    }

    private static final class TestEventsCallback implements EventRepository.EventsCallback {
        List<Event> events;
        String errorMessage;

        @Override
        public void onSuccess(List<Event> events) {
            this.events = events;
        }

        @Override
        public void onError(String message) {
            errorMessage = message;
        }
    }

    // ── getReservationsForUser (US-10) ────────────────────────────────────────

    @Test
    public void getReservationsForUser_returnsReservationsSortedNewestFirst() {
        Query query = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot older = mock(DocumentSnapshot.class);
        DocumentSnapshot newer = mock(DocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("userId", "user-1")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forResult(querySnapshot));

        Reservation olderReservation = new Reservation(
                "evt-1", "movie", "user-1", "u@test.com",
                "Old Show", "MTL", new Date(1000L), 0);
        olderReservation.setReservationDate(new Date(1000L));
        Reservation newerReservation = new Reservation(
                "evt-2", "concert", "user-1", "u@test.com",
                "New Show", "MTL", new Date(9000L), 0);
        newerReservation.setReservationDate(new Date(9000L));

        when(older.getId()).thenReturn("res-old");
        when(older.toObject(Reservation.class)).thenReturn(olderReservation);
        when(newer.getId()).thenReturn("res-new");
        when(newer.toObject(Reservation.class)).thenReturn(newerReservation);
        when(querySnapshot.getDocuments()).thenReturn(List.of(older, newer));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getReservationsForUser("user-1", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(2, callback.reservations.size());
        // Newer reservation date comes first (sorted descending by reservationDate)
        assertEquals("res-new", callback.reservations.get(0).getReservationId());
        assertEquals("res-old", callback.reservations.get(1).getReservationId());
    }

    @Test
    public void getReservationsForUser_skipsMalformedDocuments() {
        Query query = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot good = mock(DocumentSnapshot.class);
        DocumentSnapshot bad = mock(DocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("userId", "user-2")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forResult(querySnapshot));

        Reservation goodReservation = new Reservation(
                "evt-1", "sport", "user-2", "b@test.com",
                "Game Night", "Toronto", new Date(), 0);
        when(good.getId()).thenReturn("res-good");
        when(good.toObject(Reservation.class)).thenReturn(goodReservation);
        when(bad.getId()).thenReturn("res-bad");
        when(bad.toObject(Reservation.class)).thenReturn(null); // null → skipped
        when(querySnapshot.getDocuments()).thenReturn(List.of(good, bad));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getReservationsForUser("user-2", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, callback.reservations.size());
        assertEquals("res-good", callback.reservations.get(0).getReservationId());
    }

    @Test
    public void getReservationsForUser_propagatesQueryFailure() {
        Query query = mock(Query.class);
        when(reservationsCollection.whereEqualTo("userId", "user-3")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forException(new RuntimeException("query error")));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getReservationsForUser("user-3", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("query error", callback.errorMessage);
    }

    // ── getConfirmedReservationsForEvent (US-14) ──────────────────────────────

    @Test
    public void getConfirmedReservationsForEvent_returnsOnlyConfirmedReservations() {
        Query q1 = mock(Query.class);
        Query q2 = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot confirmed = mock(DocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("eventId", "evt-1")).thenReturn(q1);
        when(q1.whereEqualTo("status", ReservationStatus.CONFIRMED.toFirestoreValue())).thenReturn(q2);
        when(q2.get()).thenReturn(Tasks.forResult(querySnapshot));

        Reservation confirmedReservation = new Reservation(
                "evt-1", "concert", "user-1", "u@test.com",
                "Jazz Night", "MTL", new Date(), 0);
        when(confirmed.getId()).thenReturn("res-confirmed");
        when(confirmed.toObject(Reservation.class)).thenReturn(confirmedReservation);
        when(querySnapshot.getDocuments()).thenReturn(List.of(confirmed));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getConfirmedReservationsForEvent("evt-1", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(1, callback.reservations.size());
        assertEquals("res-confirmed", callback.reservations.get(0).getReservationId());
    }

    @Test
    public void getConfirmedReservationsForEvent_propagatesQueryFailure() {
        Query q1 = mock(Query.class);
        Query q2 = mock(Query.class);
        when(reservationsCollection.whereEqualTo("eventId", "evt-fail")).thenReturn(q1);
        when(q1.whereEqualTo("status", ReservationStatus.CONFIRMED.toFirestoreValue())).thenReturn(q2);
        when(q2.get()).thenReturn(Tasks.forException(new RuntimeException("confirmed query failed")));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getConfirmedReservationsForEvent("evt-fail", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("confirmed query failed", callback.errorMessage);
    }

    @Test
    public void getConfirmedReservationsForEvent_nullDocument_isSkipped() {
        // toObject returns null → document is silently skipped
        Query q1 = mock(Query.class);
        Query q2 = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot nullDoc = mock(DocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("eventId", "evt-null")).thenReturn(q1);
        when(q1.whereEqualTo("status", ReservationStatus.CONFIRMED.toFirestoreValue())).thenReturn(q2);
        when(q2.get()).thenReturn(Tasks.forResult(querySnapshot));
        when(nullDoc.toObject(Reservation.class)).thenReturn(null);
        when(querySnapshot.getDocuments()).thenReturn(List.of(nullDoc));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getConfirmedReservationsForEvent("evt-null", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertNotNull(callback.reservations);
        assertEquals(0, callback.reservations.size()); // null doc skipped
    }

    @Test
    public void getConfirmedReservationsForEvent_toObjectThrows_documentSkipped() {
        // toObject throws a RuntimeException → caught, document skipped
        Query q1 = mock(Query.class);
        Query q2 = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot badDoc = mock(DocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("eventId", "evt-bad")).thenReturn(q1);
        when(q1.whereEqualTo("status", ReservationStatus.CONFIRMED.toFirestoreValue())).thenReturn(q2);
        when(q2.get()).thenReturn(Tasks.forResult(querySnapshot));
        when(badDoc.toObject(Reservation.class)).thenThrow(new RuntimeException("parse error"));
        when(querySnapshot.getDocuments()).thenReturn(List.of(badDoc));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getConfirmedReservationsForEvent("evt-bad", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertNotNull(callback.reservations);
        assertEquals(0, callback.reservations.size()); // malformed doc skipped
    }

    @Test
    public void getReservationsForUser_nullReservationDate_sortedToEnd() {
        // Tests the null-date comparator branches: a.getReservationDate()==null → return 1
        // and b.getReservationDate()==null → return -1
        Query query = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot withDate = mock(DocumentSnapshot.class);
        DocumentSnapshot noDate = mock(DocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("userId", "user-null-date")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forResult(querySnapshot));

        Reservation datedRes = new Reservation(
                "evt-1", "movie", "user-null-date", "u@test.com",
                "Show A", "MTL", new Date(5000L), 0);
        datedRes.setReservationDate(new Date(5000L));

        Reservation undatedRes = new Reservation(
                "evt-2", "concert", "user-null-date", "u@test.com",
                "Show B", "MTL", new Date(9000L), 0);
        undatedRes.setReservationDate(null); // no reservationDate

        when(withDate.getId()).thenReturn("res-dated");
        when(withDate.toObject(Reservation.class)).thenReturn(datedRes);
        when(noDate.getId()).thenReturn("res-undated");
        when(noDate.toObject(Reservation.class)).thenReturn(undatedRes);
        // Return undated first — sorting should push it to end
        when(querySnapshot.getDocuments()).thenReturn(List.of(noDate, withDate));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getReservationsForUser("user-null-date", callback);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(2, callback.reservations.size());
        // Item with a date should come first (descending); null date goes last
        assertEquals("res-dated", callback.reservations.get(0).getReservationId());
        assertEquals("res-undated", callback.reservations.get(1).getReservationId());
    }

    @Test
    public void getReservationsForUser_toObjectThrowsException_documentSkipped() {
        Query query = mock(Query.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot throwing = mock(DocumentSnapshot.class);
        DocumentSnapshot good = mock(DocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("userId", "user-throws")).thenReturn(query);
        when(query.get()).thenReturn(Tasks.forResult(querySnapshot));

        // First doc throws on toObject
        when(throwing.getId()).thenReturn("res-throws");
        when(throwing.toObject(Reservation.class)).thenThrow(new RuntimeException("deserialization failed"));

        Reservation goodRes = new Reservation(
                "evt-1", "sport", "user-throws", "g@test.com",
                "Good Show", "Toronto", new Date(), 0);
        when(good.getId()).thenReturn("res-good");
        when(good.toObject(Reservation.class)).thenReturn(goodRes);

        when(querySnapshot.getDocuments()).thenReturn(List.of(throwing, good));

        TestUserReservationsCallback callback = new TestUserReservationsCallback();
        repository.getReservationsForUser("user-throws", callback);
        shadowOf(Looper.getMainLooper()).idle();

        // Only the good document should be in the result
        assertEquals(1, callback.reservations.size());
        assertEquals("res-good", callback.reservations.get(0).getReservationId());
    }

    // ── UserReservationsCallback interface ────────────────────────────────────

    @Test
    public void userReservationsCallback_onSuccessAndOnError_deliverValues() {
        Reservation r = new Reservation(
                "e1", "movie", "u1", "a@b.com", "Show", "MTL", new Date(), 0);

        TestUserReservationsCallback successCb = new TestUserReservationsCallback();
        successCb.onSuccess(List.of(r));
        assertEquals(1, successCb.reservations.size());

        TestUserReservationsCallback errorCb = new TestUserReservationsCallback();
        errorCb.onError("something went wrong");
        assertEquals("something went wrong", errorCb.errorMessage);
    }

    private static final class TestReservationsCallback implements EventRepository.ReservationsCallback {
        List<ReservationSummary> reservations;
        String errorMessage;

        @Override
        public void onSuccess(List<ReservationSummary> reservations) {
            this.reservations = reservations;
        }

        @Override
        public void onError(String message) {
            errorMessage = message;
        }
    }

    private static final class TestUserReservationsCallback
            implements EventRepository.UserReservationsCallback {
        List<Reservation> reservations;
        String errorMessage;

        @Override
        public void onSuccess(List<Reservation> reservations) {
            this.reservations = reservations;
        }

        @Override
        public void onError(String message) {
            errorMessage = message;
        }
    }

    private Object invokePrivate(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        try {
            return method.invoke(target, argument);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Exception) {
                throw (Exception) exception.getCause();
            }
            throw exception;
        }
    }

    private Object invokePrivate(
            Object target,
            String methodName,
            Class<?> firstType,
            Class<?> secondType,
            Class<?> thirdType,
            Object firstArgument,
            Object secondArgument,
            Object thirdArgument
    ) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, firstType, secondType, thirdType);
        method.setAccessible(true);
        try {
            return method.invoke(target, firstArgument, secondArgument, thirdArgument);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Exception) {
                throw (Exception) exception.getCause();
            }
            throw exception;
        }
    }
}
