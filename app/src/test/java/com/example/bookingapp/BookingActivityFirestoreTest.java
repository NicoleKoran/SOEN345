package com.example.bookingapp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class BookingActivityFirestoreTest {

    @Before
    public void initFirebase() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
    }

    private Intent baseIntent(String eventId) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), BookingActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("eventTitle", "Listed Title");
        intent.putExtra("eventLocation", "Listed Loc");
        intent.putExtra("eventDate", "May 1");
        intent.putExtra("eventPrice", "99");
        intent.putExtra("availableSeats", 5);
        return intent;
    }

    @SuppressWarnings("unchecked")
    private void stubFirestoreGet(boolean exists, Map<String, Object> dataOrNull) {
        FirebaseFirestore fs = mock(FirebaseFirestore.class);
        CollectionReference col = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        Task<DocumentSnapshot> getTask = mock(Task.class);

        when(fs.collection("events")).thenReturn(col);
        when(col.document(anyString())).thenReturn(docRef);
        when(docRef.get()).thenReturn(getTask);

        when(getTask.addOnSuccessListener(any(OnSuccessListener.class))).thenAnswer(inv -> {
            OnSuccessListener<DocumentSnapshot> l = inv.getArgument(0);
            DocumentSnapshot snap = mock(DocumentSnapshot.class);
            when(snap.exists()).thenReturn(exists);
            when(snap.getId()).thenReturn("evt-x");
            when(snap.getData()).thenReturn(dataOrNull);
            l.onSuccess(snap);
            return getTask;
        });
        when(getTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(getTask);

        try (var fsStatic = mockStatic(FirebaseFirestore.class)) {
            fsStatic.when(FirebaseFirestore::getInstance).thenReturn(fs);
            lastActivity[0] = Robolectric.buildActivity(BookingActivity.class, baseIntent("evt-x"))
                    .setup().get();
            ShadowLooper.idleMainLooper();
        }
    }

    private final BookingActivity[] lastActivity = new BookingActivity[1];

    @Test
    public void onCreate_snapshotNotExists_finishesActivity() {
        stubFirestoreGet(false, null);
        assertTrue(lastActivity[0].isFinishing());
    }

    @Test
    public void onCreate_eventCancelled_disablesConfirmWithLabel() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "T");
        data.put("description", "");
        data.put("location", "L");
        data.put("date", new Timestamp(new Date()));
        data.put("totalSeats", 10L);
        data.put("availableSeats", 5L);
        data.put("category", "movie");
        data.put("status", EventStatus.CANCELLED.toFirestoreValue());
        stubFirestoreGet(true, data);

        Button btn = lastActivity[0].findViewById(R.id.confirmBookingBtn);
        assertFalse(btn.isEnabled());
        assertEquals("Event Cancelled", btn.getText().toString());
    }

    @Test
    public void onCreate_soldOut_disablesConfirmWithLabel() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "T");
        data.put("description", "");
        data.put("location", "L");
        data.put("date", new Timestamp(new Date()));
        data.put("totalSeats", 10L);
        data.put("availableSeats", 0L);
        data.put("category", "movie");
        data.put("status", EventStatus.AVAILABLE.toFirestoreValue());
        stubFirestoreGet(true, data);

        Button btn = lastActivity[0].findViewById(R.id.confirmBookingBtn);
        assertFalse(btn.isEnabled());
        assertEquals("Sold Out", btn.getText().toString());
    }

    @Test
    public void onCreate_statusSoldOutWithSeatsLeft_stillShowsSoldOut() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "T");
        data.put("description", "");
        data.put("location", "L");
        data.put("date", new Timestamp(new Date()));
        data.put("totalSeats", 10L);
        data.put("availableSeats", 5L);
        data.put("category", "movie");
        data.put("status", EventStatus.SOLDOUT.toFirestoreValue());
        stubFirestoreGet(true, data);

        Button btn = lastActivity[0].findViewById(R.id.confirmBookingBtn);
        assertFalse(btn.isEnabled());
        assertEquals("Sold Out", btn.getText().toString());
    }

    @Test
    public void onCreate_availableEvent_enablesConfirmWithPrice() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "T");
        data.put("description", "");
        data.put("location", "L");
        data.put("date", new Timestamp(new Date()));
        data.put("totalSeats", 10L);
        data.put("availableSeats", 3L);
        data.put("category", "movie");
        data.put("status", EventStatus.AVAILABLE.toFirestoreValue());
        stubFirestoreGet(true, data);

        Button btn = lastActivity[0].findViewById(R.id.confirmBookingBtn);
        assertTrue(btn.isEnabled());
        assertTrue(btn.getText().toString().contains("Confirm Booking"));
        assertTrue(btn.getText().toString().contains("99"));
    }

    @Test
    public void onCreate_firestoreGetFailure_finishesActivity() {
        FirebaseFirestore fs = mock(FirebaseFirestore.class);
        CollectionReference col = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        Task<DocumentSnapshot> getTask = mock(Task.class);
        when(fs.collection("events")).thenReturn(col);
        when(col.document(anyString())).thenReturn(docRef);
        when(docRef.get()).thenReturn(getTask);
        when(getTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(getTask);
        when(getTask.addOnFailureListener(any(OnFailureListener.class))).thenAnswer(inv -> {
            inv.getArgument(0, OnFailureListener.class).onFailure(new Exception("boom"));
            return getTask;
        });

        try (var fsStatic = mockStatic(FirebaseFirestore.class)) {
            fsStatic.when(FirebaseFirestore::getInstance).thenReturn(fs);
            BookingActivity activity =
                    Robolectric.buildActivity(BookingActivity.class, baseIntent("e1")).setup().get();
            ShadowLooper.idleMainLooper();
            assertTrue(activity.isFinishing());
        }
    }

    @Test
    public void toEvent_nullData_returnsNull() throws Exception {
        BookingActivity activity =
                Robolectric.buildActivity(BookingActivity.class).setup().get();
        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.getData()).thenReturn(null);

        Method m = BookingActivity.class.getDeclaredMethod("toEvent", DocumentSnapshot.class);
        m.setAccessible(true);
        assertNull(m.invoke(activity, snap));
    }

    @Test
    public void readDate_timestampAndDateAndFallback() throws Exception {
        BookingActivity activity =
                Robolectric.buildActivity(BookingActivity.class).setup().get();
        Method readDate = BookingActivity.class.getDeclaredMethod("readDate", Object.class);
        readDate.setAccessible(true);

        Date d = new Date(1234567890000L);
        assertEquals(d, readDate.invoke(activity, new Timestamp(d)));
        assertEquals(d, readDate.invoke(activity, d));
        Object fallback = readDate.invoke(activity, "not-a-date");
        assertTrue(fallback instanceof Date);
    }

    @Test
    public void readInt_numberOrZero() throws Exception {
        BookingActivity activity =
                Robolectric.buildActivity(BookingActivity.class).setup().get();
        Method readInt = BookingActivity.class.getDeclaredMethod("readInt", Object.class);
        readInt.setAccessible(true);
        assertEquals(7, readInt.invoke(activity, 7L));
        assertEquals(0, readInt.invoke(activity, "x"));
    }
}
