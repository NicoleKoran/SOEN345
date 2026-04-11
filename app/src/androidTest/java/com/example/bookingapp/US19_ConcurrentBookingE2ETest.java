package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * US-19 — Handle concurrent users without double-booking.
 *
 * Covers:
 *  - Confirm button is disabled immediately on tap (prevents double-tap)
 *  - Confirm button shows "Booking..." while request is in-flight
 *  - A second tap while in-flight is ignored (button not enabled)
 *  - Sold-out event disables the button before any interaction
 *  - Booking failure re-enables the button for retry
 *  - Repository is called exactly once even if button could be tapped twice
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class US19_ConcurrentBookingE2ETest {

    @Before
    public void setUp() {
        FirebaseAuth.getInstance().signOut();
        emailNotification.suppressEmailsForTesting = true;
    }

    @After
    public void tearDown() {
        emailNotification.suppressEmailsForTesting = false;
    }

    private static Intent availableIntent() {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), BookingActivity.class);
        i.putExtra(BookingActivity.EXTRA_INSTRUMENTATION_USE_INTENT_EVENT_ONLY, true);
        i.putExtra("eventId",        "us19-evt-1");
        i.putExtra("eventTitle",     "Jazz Night");
        i.putExtra("eventLocation",  "Montreal");
        i.putExtra("eventDate",      "May 1, 2026 at 8:00 PM");
        i.putExtra("eventPrice",     "75");
        i.putExtra("eventStatus",    EventStatus.AVAILABLE.toFirestoreValue());
        i.putExtra("availableSeats", 1);
        return i;
    }

    // ── Button disabled immediately on tap ────────────────────────────────────

    @Test
    public void confirmButton_disabledImmediatelyOnTap() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableIntent())) {

            // Repo never calls back — simulates slow network
            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            // intentionally never responds
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.confirmBookingBtn)).check(matches(not(isEnabled())));
        }
    }

    @Test
    public void confirmButton_showsBookingTextWhileInFlight() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            // never responds
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.confirmBookingBtn)).check(matches(withText("Booking...")));
        }
    }

    // ── Repository called exactly once (no double-booking) ────────────────────

    @Test
    public void confirmButton_repositoryCalledExactlyOnce() {
        final AtomicInteger callCount = new AtomicInteger(0);

        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callCount.incrementAndGet();
                            // never responds — keeps button disabled
                        }
                    });

            // Tap once (button gets disabled), then attempt a second tap
            onView(withId(R.id.confirmBookingBtn)).perform(click());
            // Second tap should be a no-op since button is disabled
            try {
                onView(withId(R.id.confirmBookingBtn)).perform(click());
            } catch (Exception ignored) {
                // Expected: Espresso may throw because the view is disabled
            }

            org.junit.Assert.assertEquals(
                    "bookEvent must be called exactly once", 1, callCount.get());
        }
    }

    // ── Sold out: button pre-disabled ─────────────────────────────────────────

    @Test
    public void soldOutEvent_confirmButtonDisabledBeforeAnyInteraction() {
        Intent i = availableIntent();
        i.putExtra("availableSeats", 0);
        i.putExtra("eventStatus",    EventStatus.SOLDOUT.toFirestoreValue());

        try (ActivityScenario<BookingActivity> ignored = ActivityScenario.launch(i)) {
            onView(withId(R.id.confirmBookingBtn)).check(matches(not(isEnabled())));
        }
    }

    // ── Failure path: button re-enabled for retry ─────────────────────────────

    @Test
    public void bookingFailure_reEnablesButtonForRetry() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callback.onFailure("No seats available.");
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.confirmBookingBtn)).check(matches(isEnabled()));
        }
    }

    @Test
    public void bookingFailure_showsErrorMessage() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callback.onFailure("ABORTED: seat taken by another user.");
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.statusText))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(org.hamcrest.Matchers.containsString("ABORTED"))));
        }
    }

    // ── Last seat scenario (1 seat remaining) ─────────────────────────────────

    @Test
    public void oneSeatRemaining_canStillBook() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callback.onSuccess(
                                    "res-last-seat", event.getTitle(), event.getLocation(),
                                    "May 1, 2026 at 8:00 PM", "75", "TK-LAST", "Z9");
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).check(matches(isEnabled()));
            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.confirmedReservationId))
                    .check(matches(withText(org.hamcrest.Matchers.containsString("res-last-seat"))));
        }
    }
}
