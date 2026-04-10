package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * E2E / instrumented tests for the back button added to {@link BookingActivity}.
 *
 * Uses the {@link BookingActivity#EXTRA_INSTRUMENTATION_USE_INTENT_EVENT_ONLY} flag
 * so no Firestore connection is needed on CI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BookingBackButtonE2ETest {

    @Before
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    private static Intent bookingIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), BookingActivity.class);
        intent.putExtra(BookingActivity.EXTRA_INSTRUMENTATION_USE_INTENT_EVENT_ONLY, true);
        intent.putExtra("eventId", "e2e-back-1");
        intent.putExtra("eventTitle", "Jazz Night");
        intent.putExtra("eventLocation", "Montreal");
        intent.putExtra("eventDate", "May 1, 2026 at 8:00 PM");
        intent.putExtra("eventPrice", "75");
        intent.putExtra("eventStatus", EventStatus.AVAILABLE.toFirestoreValue());
        intent.putExtra("availableSeats", 40);
        return intent;
    }

    /** The back button must be visible on the event detail screen. */
    @Test
    public void bookingActivity_backButton_isVisible() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(bookingIntent())) {
            onView(withId(R.id.backButton)).check(matches(isDisplayed()));
        }
    }

    /** Clicking the back button must finish the activity. */
    @Test
    public void bookingActivity_backButton_finishesActivity() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(bookingIntent())) {
            onView(withId(R.id.backButton)).perform(click());
            // After finish(), the activity moves to DESTROYED state
            scenario.onActivity(activity ->
                    assertTrue("Activity should be finishing", activity.isFinishing()));
        }
    }

    /** Event details must still be shown alongside the back button. */
    @Test
    public void bookingActivity_withBackButton_eventDetailsStillVisible() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(bookingIntent())) {
            onView(withId(R.id.eventTitleText)).check(matches(isDisplayed()));
            onView(withId(R.id.confirmBookingBtn)).check(matches(isDisplayed()));
            onView(withId(R.id.backButton)).check(matches(isDisplayed()));
        }
    }
}
