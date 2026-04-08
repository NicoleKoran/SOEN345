package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

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
}
