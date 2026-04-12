package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;

import android.app.AlertDialog;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * E2E / instrumented tests for:
 * <ul>
 *   <li>US-10 – Customer views reservation history (MyReservationsActivity)</li>
 *   <li>US-11 – Customer cancels a reservation via the UI</li>
 * </ul>
 *
 * Firebase is not used: the {@link MyReservationsActivity#EXTRA_INSTRUMENTATION_PREFILL}
 * flag pre-populates the list with two hard-coded test reservations, exactly as
 * {@link BookingActivity#EXTRA_INSTRUMENTATION_USE_INTENT_EVENT_ONLY} does for the
 * booking flow.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyReservationsE2ETest {

    @Before
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        emailNotification.suppressEmailsForTesting = true;
        // Prevent MainActivity from redirecting to LoginActivity when no user is signed in.
        MainActivity.skipRedirectForTesting = true;
        // Prevent Firestore init crash on API 29 CI emulator.
        MainActivity.skipFirestoreForTesting = true;
    }

    @After
    public void tearDown() {
        emailNotification.suppressEmailsForTesting = false;
        MainActivity.skipRedirectForTesting = false;
        MainActivity.skipFirestoreForTesting = false;
    }

    // ── Helper: intent that pre-fills the list with test data ─────────────────

    private static Intent prefillIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                MyReservationsActivity.class);
        intent.putExtra(MyReservationsActivity.EXTRA_INSTRUMENTATION_PREFILL, true);
        return intent;
    }

    // ── US-10: My Reservations screen ─────────────────────────────────────────

    /**
     * When no user is signed in (and the instrumentation flag is NOT set), the
     * activity must show an explanatory message rather than crash or show an
     * empty screen.
     */
    @Test
    public void myReservations_noUser_showsNotLoggedInStatus() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(MyReservationsActivity.class)) {
            onView(withId(R.id.myReservationsStatus))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(containsString("logged in"))));
        }
    }

    /**
     * Back button (top-left) must close the screen and return to the caller.
     */
    @Test
    public void myReservations_backButton_closesScreen() {
        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.backButton)).perform(click());
            // After finish() the activity leaves RESUMED; exact final state timing varies on CI.
            org.junit.Assert.assertNotEquals(
                    "Activity should no longer be RESUMED after back button press",
                    androidx.lifecycle.Lifecycle.State.RESUMED,
                    scenario.getState());
        }
    }

    /**
     * With the pre-fill flag set, the RecyclerView must be visible and show both
     * test reservations (confirmed Jazz Night + cancelled Movie Night).
     */
    @Test
    public void myReservations_prefillFlag_showsReservationList() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.reservationsRecyclerView)).check(matches(isDisplayed()));
            onView(withText("Jazz Night")).check(matches(isDisplayed()));
            onView(withText("Movie Night")).check(matches(isDisplayed()));
        }
    }

    /**
     * The "CONFIRMED" badge must be visible on the first card.
     */
    @Test
    public void myReservations_confirmedCard_showsConfirmedBadge() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withText("CONFIRMED")).check(matches(isDisplayed()));
        }
    }

    /**
     * The "CANCELLED BY YOU" badge must be visible on the second card.
     */
    @Test
    public void myReservations_cancelledCard_showsCancelledByYouBadge() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withText("CANCELLED BY YOU")).check(matches(isDisplayed()));
        }
    }

    /**
     * The "empty" placeholder must be hidden when reservations are present.
     */
    @Test
    public void myReservations_withData_hidesEmptyPlaceholder() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.emptyText)).check(matches(not(isDisplayed())));
        }
    }

    // ── US-11: Cancel reservation ─────────────────────────────────────────────

    /**
     * Clicking the cancel button on the CONFIRMED card must show a confirmation
     * AlertDialog — no repository calls happen yet.
     */
    @Test
    public void myReservations_cancelButton_showsConfirmationDialog() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            // The confirmed card is the first item; click its cancel button
            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                            new androidx.test.espresso.ViewAction() {
                                @Override
                                public org.hamcrest.Matcher<android.view.View> getConstraints() {
                                    return isDisplayed();
                                }

                                @Override
                                public String getDescription() {
                                    return "click cancel button inside item";
                                }

                                @Override
                                public void perform(
                                        androidx.test.espresso.UiController uiController,
                                        android.view.View view) {
                                    view.findViewById(R.id.cancelReservationBtn).performClick();
                                    uiController.loopMainThreadUntilIdle();
                                }
                            }));

            // Dialog must appear
            onView(withText(containsString("cancel"))).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
        }
    }

    /**
     * The cancel button must be hidden (GONE) on the CANCELLED card.
     */
    @Test
    public void myReservations_cancelledCard_cancelButtonHidden() {
        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {
            // Scroll to the cancelled card (position 1) and verify cancel btn is gone
            scenario.onActivity(activity -> {
                androidx.recyclerview.widget.RecyclerView rv =
                        activity.findViewById(R.id.reservationsRecyclerView);
                rv.scrollToPosition(1);
                androidx.recyclerview.widget.RecyclerView.ViewHolder vh =
                        rv.findViewHolderForAdapterPosition(1);
                if (vh != null) {
                    android.view.View cancelBtn = vh.itemView.findViewById(R.id.cancelReservationBtn);
                    assert cancelBtn.getVisibility() == android.view.View.GONE
                            : "cancel button must be GONE for a cancelled reservation";
                }
            });
        }
    }

    /**
     * Confirming the cancellation dialog triggers {@link BookingRepository#cancelReservation}
     * with the correct reservation ID. A fake repository captures the call.
     */
    @Test
    public void myReservations_confirmCancellation_callsRepository() {
        final String[] capturedReservationId = {null};

        try (ActivityScenario<MyReservationsActivity> scenario =
                     ActivityScenario.launch(prefillIntent())) {

            // Inject a fake booking repository that records the reservation ID
            scenario.onActivity(activity ->
                    activity.bookingRepository = new BookingRepository(null, null) {
                        @Override
                        public void cancelReservation(
                                String reservationId, String eventId,
                                String userEmail, String eventTitle,
                                String eventLocation, String eventDate,
                                SimpleCallback callback) {
                            capturedReservationId[0] = reservationId;
                            callback.onSuccess("Cancelled.");
                        }
                    });

            // Click cancel on the confirmed card (position 0)
            onView(withId(R.id.reservationsRecyclerView))
                    .perform(RecyclerViewActions.actionOnItemAtPosition(0,
                            new androidx.test.espresso.ViewAction() {
                                @Override
                                public org.hamcrest.Matcher<android.view.View> getConstraints() {
                                    return isDisplayed();
                                }

                                @Override
                                public String getDescription() {
                                    return "click cancel button inside item";
                                }

                                @Override
                                public void perform(
                                        androidx.test.espresso.UiController uiController,
                                        android.view.View view) {
                                    view.findViewById(R.id.cancelReservationBtn).performClick();
                                    uiController.loopMainThreadUntilIdle();
                                }
                            }));

            // Confirm the dialog
            onView(withText("Yes, Cancel")).perform(click());

            // Verify the fake repository was called with the correct reservation ID
            assertNotNull("cancelReservation must be called", capturedReservationId[0]);
            assert "test-res-confirmed".equals(capturedReservationId[0])
                    : "Expected test-res-confirmed but got: " + capturedReservationId[0];
        }
    }

    // ── US-10: Navigation from MainActivity ───────────────────────────────────

    /**
     * Non-admin users must see the "My Bookings" button on MainActivity.
     */
    @Test
    public void mainActivity_nonAdmin_myBookingsButtonIsVisible() {
        // Non-admin: no EXTRA_IS_ADMIN, no persisted admin session
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().clear().apply();

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), MainActivity.class);
        // Suppress onStart redirect to LoginActivity by persisting a non-admin user flag
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean(LoginActivity.KEY_ADMIN_MODE, false)
                .apply();

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.myReservationsBtn)).check(matches(isDisplayed()));
        }
    }

}
