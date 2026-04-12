package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * US-10 — View my reservation history.
 *
 * Covers:
 *  - "My Bookings" button visible on MainActivity for non-admin users
 *  - Clicking "My Bookings" launches MyReservationsActivity
 *  - Not-logged-in state shows an explanatory message
 *  - Pre-filled list shows both reservations
 *  - CONFIRMED badge is visible
 *  - CANCELLED BY YOU badge is visible
 *  - Empty placeholder is hidden when reservations are present
 *  - Back button closes the screen
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class US10_ViewReservationHistoryE2ETest {

    @Before
    public void setUp() {
        FirebaseAuth.getInstance().signOut();
        emailNotification.suppressEmailsForTesting = true;
        // Prevent MainActivity from redirecting to LoginActivity when no user is signed in,
        // so non-admin navigation tests can reach the main screen.
        MainActivity.skipRedirectForTesting = true;
    }

    @After
    public void tearDown() {
        emailNotification.suppressEmailsForTesting = false;
        MainActivity.skipRedirectForTesting = false;
    }

    private static Intent prefillIntent() {
        Intent i = new Intent(
                ApplicationProvider.getApplicationContext(), MyReservationsActivity.class);
        i.putExtra(MyReservationsActivity.EXTRA_INSTRUMENTATION_PREFILL, true);
        return i;
    }

    private static void setNonAdminPrefs() {
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean(LoginActivity.KEY_ADMIN_MODE, false)
                .apply();
    }

    // ── Navigation from MainActivity ──────────────────────────────────────────

    @Test
    public void mainActivity_nonAdmin_myBookingsButtonVisible() {
        setNonAdminPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.myReservationsBtn)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void mainActivity_myBookingsButton_launchesMyReservationsActivity() {
        setNonAdminPrefs();
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        Intents.init();
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.myReservationsBtn)).perform(click());
            intended(hasComponent(MyReservationsActivity.class.getName()));
        } finally {
            Intents.release();
        }
    }

    // ── US-10: Not logged in ──────────────────────────────────────────────────

    @Test
    public void myReservations_notLoggedIn_showsLoginMessage() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(MyReservationsActivity.class)) {
            onView(withId(R.id.myReservationsStatus))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(containsString("logged in"))));
        }
    }

    // ── US-10: Reservation list content ──────────────────────────────────────

    @Test
    public void myReservations_prefill_recyclerViewVisible() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.reservationsRecyclerView)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void myReservations_prefill_showsJazzNight() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withText("Jazz Night")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void myReservations_prefill_showsMovieNight() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withText("Movie Night")).check(matches(isDisplayed()));
        }
    }

    // ── US-10: Status badges ──────────────────────────────────────────────────

    @Test
    public void myReservations_confirmedCard_showsConfirmedBadge() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withText("CONFIRMED")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void myReservations_cancelledCard_showsCancelledByYouBadge() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withText("CANCELLED BY YOU")).check(matches(isDisplayed()));
        }
    }

    // ── US-10: Empty placeholder hidden when data present ─────────────────────

    @Test
    public void myReservations_withData_emptyPlaceholderHidden() {
        try (ActivityScenario<MyReservationsActivity> ignored =
                     ActivityScenario.launch(prefillIntent())) {
            onView(withId(R.id.emptyText)).check(matches(not(isDisplayed())));
        }
    }

    // ── US-10: Back button ────────────────────────────────────────────────────

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
}
