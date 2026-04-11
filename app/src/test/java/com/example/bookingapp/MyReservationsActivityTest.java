package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Unit tests for MyReservationsActivity (US-10, US-11).
 */
@RunWith(RobolectricTestRunner.class)
public class MyReservationsActivityTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    }

    // ── Activity lifecycle ─────────────────────────────────────────────────────

    @Test
    public void onCreate_activityCreatesSuccessfully() {
        // No auth mock needed — FirebaseApp is initialized in setUp(); activity creates fine
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).create().get();
        assertNotNull(activity);
    }

    @Test
    public void backButton_finishesActivity() {
        // No auth mock needed — the back button is wired up independently of auth state
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();
        activity.findViewById(R.id.backButton).performClick();
        assertTrue(activity.isFinishing());
    }

    @Test
    public void noUser_showsStatusMessage() {
        // Real Firebase Auth returns null for getCurrentUser() in test environment
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        TextView statusText = activity.findViewById(R.id.myReservationsStatus);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertTrue(statusText.getText().toString().toLowerCase().contains("logged in"));
    }

    // ── ReservationsAdapter badge logic ───────────────────────────────────────

    @Test
    public void adapter_confirmedReservation_showsConfirmedBadge() {
        Reservation r = buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null);
        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(r),
                        reservation -> { /* no-op */ }
                );

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("CONFIRMED",
                holder.itemView.findViewById(R.id.reservationStatus)
                        instanceof TextView
                        ? ((TextView) holder.itemView.findViewById(R.id.reservationStatus)).getText().toString()
                        : "");
        // Cancel button should be visible for confirmed reservations
        assertEquals(View.VISIBLE,
                holder.itemView.findViewById(R.id.cancelReservationBtn).getVisibility());
    }

    @Test
    public void adapter_userCancelledReservation_showsCancelledByYouBadge() {
        Reservation r = buildReservation(ReservationStatus.CANCELLED.toFirestoreValue(), "user_cancelled");
        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(r),
                        reservation -> { }
                );

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView badge = holder.itemView.findViewById(R.id.reservationStatus);
        assertEquals("CANCELLED BY YOU", badge.getText().toString());
        // Cancel button should be hidden for already-cancelled reservations
        assertEquals(View.GONE,
                holder.itemView.findViewById(R.id.cancelReservationBtn).getVisibility());
    }

    @Test
    public void adapter_eventCancelledReservation_showsEventCancelledBadge() {
        Reservation r = buildReservation(ReservationStatus.CANCELLED.toFirestoreValue(), "event_cancelled");
        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(r),
                        reservation -> { }
                );

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView badge = holder.itemView.findViewById(R.id.reservationStatus);
        assertEquals("EVENT CANCELLED", badge.getText().toString());
        assertEquals(View.GONE,
                holder.itemView.findViewById(R.id.cancelReservationBtn).getVisibility());
    }

    @Test
    public void adapter_pendingReservation_showsPendingBadge() {
        Reservation r = buildReservation(ReservationStatus.PENDING.toFirestoreValue(), null);
        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(r),
                        reservation -> { }
                );

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView badge = holder.itemView.findViewById(R.id.reservationStatus);
        assertEquals("PENDING", badge.getText().toString());
    }

    @Test
    public void adapter_nullEventDate_showsDateTbd() {
        Reservation r = buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null);
        r.setEventTitle("Concert");
        // eventDate is null by default in the empty constructor path

        Reservation noDate = new Reservation(
                "evt-1", "concert", "uid-1", "u@test.com",
                "Concert", "Montreal", null, 0);
        noDate.setReservationId("res-1");

        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(noDate),
                        reservation -> { }
                );

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView date = holder.itemView.findViewById(R.id.reservationDate);
        assertTrue(date.getText().toString().contains("Date TBD"));
    }

    @Test
    public void adapter_getItemCount_matchesListSize() {
        java.util.List<Reservation> list = java.util.Arrays.asList(
                buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null),
                buildReservation(ReservationStatus.CANCELLED.toFirestoreValue(), "user_cancelled")
        );
        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(list, r -> { });
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void adapter_setReservations_updatesCount() {
        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.emptyList(), r -> { });
        assertEquals(0, adapter.getItemCount());

        adapter.setReservations(java.util.Arrays.asList(
                buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null),
                buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null)
        ));
        assertEquals(2, adapter.getItemCount());
    }

    // ── Adapter edge cases ─────────────────────────────────────────────────────

    @Test
    public void adapter_nullEventTitle_showsUnknownEvent() {
        Reservation r = new Reservation();
        r.setReservationId("res-1");
        r.setStatus(ReservationStatus.PENDING.toFirestoreValue());
        // eventTitle is null from empty constructor

        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(r), reservation -> {});

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView title = holder.itemView.findViewById(R.id.reservationEventTitle);
        assertEquals("Unknown Event", title.getText().toString());
    }

    @Test
    public void adapter_nullEventLocation_showsEmptyString() {
        Reservation r = new Reservation();
        r.setReservationId("res-1");
        r.setStatus(ReservationStatus.PENDING.toFirestoreValue());
        // eventLocation is null from empty constructor

        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(r), reservation -> {});

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        TextView location = holder.itemView.findViewById(R.id.reservationLocation);
        assertEquals("", location.getText().toString());
    }

    @Test
    public void adapter_cancelButton_click_invokesListener() {
        Reservation r = buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null);
        final boolean[] listenerCalled = {false};

        MyReservationsActivity.ReservationsAdapter adapter =
                new MyReservationsActivity.ReservationsAdapter(
                        java.util.Collections.singletonList(r),
                        reservation -> listenerCalled[0] = true);

        android.widget.FrameLayout parent = new android.widget.FrameLayout(
                ApplicationProvider.getApplicationContext());
        MyReservationsActivity.ReservationsAdapter.ViewHolder holder =
                adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        holder.itemView.findViewById(R.id.cancelReservationBtn).performClick();
        assertTrue(listenerCalled[0]);
    }

    // ── onCancelClicked (private) ──────────────────────────────────────────────

    @Test
    public void onCancelClicked_alreadyCancelledReservation_showsStatusMessage() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        Reservation r = buildReservation(ReservationStatus.CANCELLED.toFirestoreValue(), "user_cancelled");
        invokePrivate(activity, "onCancelClicked", Reservation.class, r);

        TextView statusText = activity.findViewById(R.id.myReservationsStatus);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertTrue(statusText.getText().toString().toLowerCase().contains("already cancelled"));
    }

    @Test
    public void onCancelClicked_confirmedReservation_showsConfirmationDialog() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        Reservation r = buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null);
        invokePrivate(activity, "onCancelClicked", Reservation.class, r);

        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
    }

    // ── doCancel (private) ─────────────────────────────────────────────────────

    @Test
    public void doCancel_onSuccess_callsCancelReservationAndShowsStatus() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        BookingRepository mockRepo = mock(BookingRepository.class);
        doAnswer(inv -> {
            BookingRepository.SimpleCallback cb = inv.getArgument(6);
            cb.onSuccess("Reservation cancelled successfully.");
            return null;
        }).when(mockRepo).cancelReservation(any(), any(), any(), any(), any(), any(),
                any(BookingRepository.SimpleCallback.class));

        setField(activity, "bookingRepository", mockRepo);

        Reservation r = buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null);
        invokePrivate(activity, "doCancel", Reservation.class, r);

        // Verify cancelReservation was called with the correct reservation id
        org.mockito.Mockito.verify(mockRepo).cancelReservation(
                org.mockito.ArgumentMatchers.eq("res-test"),
                any(), any(), any(), any(), any(),
                any(BookingRepository.SimpleCallback.class));
        // statusText is visible — may show "logged in" because loadReservations
        // runs after and finds no user in the test environment, but that's fine.
        TextView statusText = activity.findViewById(R.id.myReservationsStatus);
        assertEquals(View.VISIBLE, statusText.getVisibility());
    }

    @Test
    public void doCancel_onFailure_showsErrorStatus() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        BookingRepository mockRepo = mock(BookingRepository.class);
        doAnswer(inv -> {
            BookingRepository.SimpleCallback cb = inv.getArgument(6);
            cb.onFailure("Network error");
            return null;
        }).when(mockRepo).cancelReservation(any(), any(), any(), any(), any(), any(),
                any(BookingRepository.SimpleCallback.class));

        setField(activity, "bookingRepository", mockRepo);

        Reservation r = buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null);
        invokePrivate(activity, "doCancel", Reservation.class, r);

        TextView statusText = activity.findViewById(R.id.myReservationsStatus);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertTrue(statusText.getText().toString().contains("Could not cancel"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reservation buildReservation(String status, String cancellationReason) {
        Reservation r = new Reservation(
                "evt-1", "concert", "uid-1", "user@test.com",
                "Jazz Night", "Montreal", new Date(), 0);
        r.setReservationId("res-test");
        r.setStatus(status);
        r.setCancellationReason(cancellationReason);
        return r;
    }

    // ── onCancelClicked dialog positive-button click ──────────────────────────

    @Test
    public void onCancelClicked_positiveButtonClick_triggersDoCancel() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        // Inject a no-op mock BookingRepository so doCancel doesn't hit Firestore
        BookingRepository mockRepo = mock(BookingRepository.class);
        setField(activity, "bookingRepository", mockRepo);

        Reservation confirmed = buildReservation(
                ReservationStatus.CONFIRMED.toFirestoreValue(), null);

        // Show the cancel-confirmation dialog
        invokePrivate(activity, "onCancelClicked", Reservation.class, confirmed);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(dialog);

        // Clicking "Yes, Cancel" fires the positive-button lambda → doCancel(reservation)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();

        // doCancel must have forwarded the reservation ID to cancelReservation
        org.mockito.Mockito.verify(mockRepo).cancelReservation(
                eq("res-test"), any(), any(), any(), any(), any(),
                any(BookingRepository.SimpleCallback.class));
    }

    // ── Instrumentation fast-path (loadInstrumentationReservations) ──────────

    @Test
    public void loadReservations_withInstrumentationFlag_showsTwoReservations() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                MyReservationsActivity.class);
        intent.putExtra(MyReservationsActivity.EXTRA_INSTRUMENTATION_PREFILL, true);
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class, intent).setup().get();

        // The instrumentation path should bypass Firebase and pre-populate the list
        RecyclerView recyclerView = activity.findViewById(R.id.reservationsRecyclerView);
        TextView emptyText = activity.findViewById(R.id.emptyText);
        assertEquals(View.VISIBLE, recyclerView.getVisibility());
        assertEquals(View.GONE, emptyText.getVisibility());
        assertEquals(2, recyclerView.getAdapter().getItemCount());
    }

    // ── loadReservations with a logged-in user (getReservationsForUser paths) ─

    @Test
    public void loadReservations_loggedInUser_emptyList_showsEmptyPlaceholder() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        EventRepository mockRepo = mock(EventRepository.class);
        doAnswer(inv -> {
            EventRepository.UserReservationsCallback cb = inv.getArgument(1);
            cb.onSuccess(Collections.emptyList());
            return null;
        }).when(mockRepo).getReservationsForUser(any(), any());
        setField(activity, "eventRepository", mockRepo);

        try (MockedStatic<FirebaseAuth> authMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseUser mockUser = mock(FirebaseUser.class);
            authMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            when(mockAuth.getCurrentUser()).thenReturn(mockUser);
            when(mockUser.getUid()).thenReturn("uid-test");

            invokePrivateNoArg(activity, "loadReservations");
        }

        RecyclerView recyclerView = activity.findViewById(R.id.reservationsRecyclerView);
        TextView emptyText = activity.findViewById(R.id.emptyText);
        assertEquals(View.GONE, recyclerView.getVisibility());
        assertEquals(View.VISIBLE, emptyText.getVisibility());
    }

    @Test
    public void loadReservations_loggedInUser_withReservations_showsRecyclerView() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        Reservation res = buildReservation(ReservationStatus.CONFIRMED.toFirestoreValue(), null);
        EventRepository mockRepo = mock(EventRepository.class);
        doAnswer(inv -> {
            EventRepository.UserReservationsCallback cb = inv.getArgument(1);
            cb.onSuccess(Arrays.asList(res));
            return null;
        }).when(mockRepo).getReservationsForUser(any(), any());
        setField(activity, "eventRepository", mockRepo);

        try (MockedStatic<FirebaseAuth> authMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseUser mockUser = mock(FirebaseUser.class);
            authMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            when(mockAuth.getCurrentUser()).thenReturn(mockUser);
            when(mockUser.getUid()).thenReturn("uid-test");

            invokePrivateNoArg(activity, "loadReservations");
        }

        RecyclerView recyclerView = activity.findViewById(R.id.reservationsRecyclerView);
        assertEquals(View.VISIBLE, recyclerView.getVisibility());
        assertEquals(1, recyclerView.getAdapter().getItemCount());
    }

    @Test
    public void loadReservations_loggedInUser_repoError_showsErrorStatus() throws Exception {
        MyReservationsActivity activity =
                Robolectric.buildActivity(MyReservationsActivity.class).setup().get();

        EventRepository mockRepo = mock(EventRepository.class);
        doAnswer(inv -> {
            EventRepository.UserReservationsCallback cb = inv.getArgument(1);
            cb.onError("network failure");
            return null;
        }).when(mockRepo).getReservationsForUser(any(), any());
        setField(activity, "eventRepository", mockRepo);

        try (MockedStatic<FirebaseAuth> authMock = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseUser mockUser = mock(FirebaseUser.class);
            authMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            when(mockAuth.getCurrentUser()).thenReturn(mockUser);
            when(mockUser.getUid()).thenReturn("uid-test");

            invokePrivateNoArg(activity, "loadReservations");
        }

        TextView statusText = activity.findViewById(R.id.myReservationsStatus);
        assertTrue(statusText.getText().toString().contains("network failure"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void invokePrivateNoArg(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }

    private void invokePrivate(Object target, String methodName, Class<?> paramType, Object arg) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, paramType);
        method.setAccessible(true);
        method.invoke(target, arg);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
