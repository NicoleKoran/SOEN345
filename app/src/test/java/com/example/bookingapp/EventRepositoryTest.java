package com.example.bookingapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EventRepositoryTest {

    private EventRepository repository;

    @BeforeEach
    void setUp() {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        repository = new EventRepository(firestore);
    }

    @Test
    void toFirestoreMap_includesAllEditableFields() throws Exception {
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
    void fromSnapshot_mapsFieldsAndAppliesFallbacks() throws Exception {
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
    void fromSnapshot_withNullDataThrowsHelpfulError() {
        DocumentSnapshot snapshot = mock(DocumentSnapshot.class);
        when(snapshot.getData()).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> invokePrivate(repository, "fromSnapshot", DocumentSnapshot.class, snapshot)
        );

        assertEquals("Event data is empty.", exception.getMessage());
    }

    @Test
    void toReservationSummaries_buildsDisplayableSummaries() throws Exception {
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
    void readInt_returnsNumberValueOrZero() throws Exception {
        assertEquals(42, invokePrivate(repository, "readInt", Object.class, 42L));
        assertEquals(0, invokePrivate(repository, "readInt", Object.class, "42"));
        assertEquals(0, invokePrivate(repository, "readInt", Object.class, null));
    }

    @Test
    void readDate_supportsTimestampDateAndFallback() throws Exception {
        Date date = new Date(1775452380000L);

        assertEquals(date, invokePrivate(repository, "readDate", Object.class, new Timestamp(date)));
        assertEquals(date, invokePrivate(repository, "readDate", Object.class, date));
        assertNotNull(invokePrivate(repository, "readDate", Object.class, "invalid"));
    }

    @Test
    void readString_returnsFallbackWhenValueMissing() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Jazz Night");

        assertEquals("Jazz Night", invokePrivate(repository, "readString", Map.class, String.class, String.class, data, "title", "fallback"));
        assertEquals("fallback", invokePrivate(repository, "readString", Map.class, String.class, String.class, data, "missing", "fallback"));
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
