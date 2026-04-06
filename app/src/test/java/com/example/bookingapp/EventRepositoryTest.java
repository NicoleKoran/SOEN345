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
