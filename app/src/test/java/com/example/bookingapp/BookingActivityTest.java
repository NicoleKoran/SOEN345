package com.example.bookingapp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class BookingActivityTest {

    @Before
    public void initFirebase() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
    }


     //Activity should finish if eventId is missing

    @Test
    public void onCreate_missingEventId_finishesActivity() {
        BookingActivity activity =
                Robolectric.buildActivity(BookingActivity.class).setup().get();

        assertTrue(activity.isFinishing());
    }


     //UI should populate correctly from intent

    @Test
    public void onCreate_validIntent_populatesUI() {
        withIdleFirestoreGet(() -> {
            Intent intent = new Intent(ApplicationProvider.getApplicationContext(), BookingActivity.class);
            intent.putExtra("eventId", "123");
            intent.putExtra("eventTitle", "Concert");
            intent.putExtra("eventLocation", "Montreal");
            intent.putExtra("eventDate", "May 1");
            intent.putExtra("eventPrice", "50");
            intent.putExtra("availableSeats", 10);

            BookingActivity activity =
                    Robolectric.buildActivity(BookingActivity.class, intent).setup().get();

            TextView title = activity.findViewById(R.id.eventTitleText);
            TextView location = activity.findViewById(R.id.eventLocationText);

            assertEquals("Concert", title.getText().toString());
            assertTrue(location.getText().toString().contains("Montreal"));
        });
    }


     //Confirm button should start disabled (before Firestore loads)

    @Test
    public void confirmButton_initiallyDisabled() {
        withIdleFirestoreGet(() -> {
            Intent intent = new Intent(ApplicationProvider.getApplicationContext(), BookingActivity.class);
            intent.putExtra("eventId", "123");

            BookingActivity activity =
                    Robolectric.buildActivity(BookingActivity.class, intent).setup().get();

            Button btn = activity.findViewById(R.id.confirmBookingBtn);
            assertFalse(btn.isEnabled());
        });
    }


     //Successful booking should navigate to BookingConfirmedActivity

    @Test
    public void confirmBooking_success_navigatesToConfirmation() throws Exception {

        BookingActivity activity =
                Robolectric.buildActivity(BookingActivity.class).setup().get();

        BookingRepository mockRepo = mock(BookingRepository.class);
        setField(activity, "bookingRepo", mockRepo);

        Button btn = activity.findViewById(R.id.confirmBookingBtn);
        btn.setEnabled(true); // simulate enabled state

        doAnswer(invocation -> {
            BookingRepository.BookingCallback callback = invocation.getArgument(1);

            callback.onSuccess(
                    "res123",
                    "Concert",
                    "Montreal",
                    "May 1",
                    "50",
                    "TKT-123",
                    "A1"
            );
            return null;
        }).when(mockRepo).bookEvent(any(), any());

        btn.performClick();
        ShadowLooper.idleMainLooper();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(
                BookingConfirmedActivity.class.getName(),
                next.getComponent().getClassName()
        );
    }


     //Failed booking should show error and re-enable button

    @Test
    public void confirmBooking_failure_showsError() throws Exception {

        BookingActivity activity =
                Robolectric.buildActivity(BookingActivity.class).setup().get();

        BookingRepository mockRepo = mock(BookingRepository.class);
        setField(activity, "bookingRepo", mockRepo);

        Button btn = activity.findViewById(R.id.confirmBookingBtn);
        TextView status = activity.findViewById(R.id.statusText);

        btn.setEnabled(true);

        doAnswer(invocation -> {
            BookingRepository.BookingCallback callback = invocation.getArgument(1);
            callback.onFailure("Network error");
            return null;
        }).when(mockRepo).bookEvent(any(), any());

        btn.performClick();
        ShadowLooper.idleMainLooper();

        assertTrue(status.getText().toString().contains("Booking failed"));
        assertEquals(android.view.View.VISIBLE, status.getVisibility());
        assertTrue(btn.isEnabled());
    }


     //Confirm booking disables button immediately (prevents double click)

    @Test
    public void confirmBooking_disablesButtonImmediately() throws Exception {

        BookingActivity activity =
                Robolectric.buildActivity(BookingActivity.class).setup().get();

        BookingRepository mockRepo = mock(BookingRepository.class);
        setField(activity, "bookingRepo", mockRepo);

        Button btn = activity.findViewById(R.id.confirmBookingBtn);
        btn.setEnabled(true);

        doNothing().when(mockRepo).bookEvent(any(), any());

        btn.performClick();
        ShadowLooper.idleMainLooper();

        assertFalse(btn.isEnabled());
        assertEquals("Booking...", btn.getText().toString());
    }


    /**
     * Avoids a real Firestore {@code get()} completing later during another test and posting
     * {@link android.widget.Toast} on a destroyed activity (Robolectric NPE in {@code ShadowToast}).
     */
    @SuppressWarnings("unchecked")
    private void withIdleFirestoreGet(Runnable body) {
        FirebaseFirestore fs = mock(FirebaseFirestore.class);
        CollectionReference col = mock(CollectionReference.class);
        DocumentReference docRef = mock(DocumentReference.class);
        Task<DocumentSnapshot> getTask = mock(Task.class);
        when(fs.collection("events")).thenReturn(col);
        when(col.document(anyString())).thenReturn(docRef);
        when(docRef.get()).thenReturn(getTask);
        when(getTask.addOnSuccessListener(any())).thenReturn(getTask);
        when(getTask.addOnFailureListener(any())).thenReturn(getTask);

        try (var fsStatic = mockStatic(FirebaseFirestore.class)) {
            fsStatic.when(FirebaseFirestore::getInstance).thenReturn(fs);
            body.run();
        }
    }

     //Helper: inject private field

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}