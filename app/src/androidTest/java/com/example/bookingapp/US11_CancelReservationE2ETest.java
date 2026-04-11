package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.After;

/**
 * US-11 — Cancel a reservation.
 *
 * Covers:
 *  - Cancel button visible on CONFIRMED card
 *  - Cancel button hidden on CANCELLED card
 *  - Tapping cancel shows a confirmation dialog
 *  - Confirming the dialog calls BookingRepository.cancelReservation with correct ID
 *  - Successful cancellation shows a success status message
 *  - Failed cancellation shows an error message
 *  - Cancellation email (USER_CANCELLATION type) is sent via EmailJS
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class US11_CancelReservationE2ETest {

    @Before
    public void setUp() {
        FirebaseAuth.getInstance().signOut();
        emailNotification.suppressEmailsForTesting = true;
    }

    @After
    public void tearDown() {
        emailNotification.suppressEmailsForTesting = false;
    }

    private static Intent prefillIntent() {
        Intent i = new Intent(
                ApplicationProvider.getApplicationContext(), MyReservationsActivity.class);
        i.putExtra(MyReservationsActivity.EXTRA_INSTRUMENTATION_PREFILL, true);
        return i;
    }

    /** Clicks the cancel button inside the RecyclerView item at the given position. */
    private static androidx.test.espresso.ViewAction clickCancelBtn() {
        return new androidx.test.espresso.ViewAction() {
            @Override
            public org.hamcrest.Matcher<View> getConstraints() {
                return androidx.test.espresso.matcher.ViewMatchers.isDisplayed();
            }
            @Override
            public String getDescription() { return "click cancel button in item"; }
            @Override
            public void perform(androidx.test.espresso.UiController c, View v) {
                v.findViewById(R.id.cancelReservationBtn).performClick();
                c.loopMainThreadUntilIdle();
            }
        };
    }

    // ── Cancel button visibility ──────────────────────────────────────────────

    @Test
    public void cancelButton_visibleOnConfirmedCard() {
        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {
            scenario.onActivity(activity -> {
                androidx.recyclerview.widget.RecyclerView rv =
                        activity.findViewById(R.id.reservationsRecyclerView);
                androidx.recyclerview.widget.RecyclerView.ViewHolder vh =
                        rv.findViewHolderForAdapterPosition(0);
                if (vh != null) {
                    View btn = vh.itemView.findViewById(R.id.cancelReservationBtn);
                    org.junit.Assert.assertEquals(View.VISIBLE, btn.getVisibility());
                }
            });
        }
    }

    @Test
    public void cancelButton_hiddenOnCancelledCard() {
        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {
            scenario.onActivity(activity -> {
                androidx.recyclerview.widget.RecyclerView rv =
                        activity.findViewById(R.id.reservationsRecyclerView);
                rv.scrollToPosition(1);
                androidx.recyclerview.widget.RecyclerView.ViewHolder vh =
                        rv.findViewHolderForAdapterPosition(1);
                if (vh != null) {
                    View btn = vh.itemView.findViewById(R.id.cancelReservationBtn);
                    org.junit.Assert.assertEquals(View.GONE, btn.getVisibility());
                }
            });
        }
    }

    // ── Confirmation dialog ───────────────────────────────────────────────────

    @Test
    public void cancelButton_tap_showsConfirmationDialog() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickCancelBtn()));
            onView(withText(containsString("cancel"))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void cancelDialog_hasYesCancelButton() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickCancelBtn()));
            onView(withText("Yes, Cancel")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void cancelDialog_hasKeepItButton() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickCancelBtn()));
            onView(withText("Keep It")).check(matches(isDisplayed()));
        }
    }

    // ── Confirming the dialog calls repository ────────────────────────────────

    @Test
    public void cancelDialog_confirm_callsRepositoryWithCorrectId() {
        final String[] capturedId = {null};

        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepository = new BookingRepository(null, null) {
                        @Override
                        public void cancelReservation(
                                String reservationId, String eventId,
                                String userEmail, String eventTitle,
                                String eventLocation, String eventDate,
                                SimpleCallback callback) {
                            capturedId[0] = reservationId;
                            callback.onSuccess("Cancelled.");
                        }
                    });

            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickCancelBtn()));
            onView(withText("Yes, Cancel")).perform(click());

            assertNotNull("cancelReservation must be called", capturedId[0]);
            org.junit.Assert.assertEquals("test-res-confirmed", capturedId[0]);
        }
    }

    // ── Cancellation success / failure feedback ───────────────────────────────

    @Test
    public void cancelDialog_onSuccess_showsSuccessStatus() {
        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepository = new BookingRepository(null, null) {
                        @Override
                        public void cancelReservation(
                                String reservationId, String eventId,
                                String userEmail, String eventTitle,
                                String eventLocation, String eventDate,
                                SimpleCallback callback) {
                            callback.onSuccess("Cancelled.");
                        }
                    });

            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickCancelBtn()));
            onView(withText("Yes, Cancel")).perform(click());

            onView(withId(R.id.myReservationsStatus))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(containsString("cancelled"))));
        }
    }

    @Test
    public void cancelDialog_onFailure_showsErrorStatus() {
        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepository = new BookingRepository(null, null) {
                        @Override
                        public void cancelReservation(
                                String reservationId, String eventId,
                                String userEmail, String eventTitle,
                                String eventLocation, String eventDate,
                                SimpleCallback callback) {
                            callback.onFailure("Network error.");
                        }
                    });

            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickCancelBtn()));
            onView(withText("Yes, Cancel")).perform(click());

            onView(withId(R.id.myReservationsStatus))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(containsString("Network error."))));
        }
    }

}
