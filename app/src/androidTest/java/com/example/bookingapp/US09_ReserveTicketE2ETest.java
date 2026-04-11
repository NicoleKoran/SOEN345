package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * US-09 — Reserve a ticket for an event.
 *
 * Covers:
 *  - Event details displayed correctly on the booking screen
 *  - Confirm button enabled for available events
 *  - Confirm button disabled / labelled "Sold Out" for sold-out events
 *  - Confirm button disabled / labelled "Event Cancelled" for cancelled events
 *  - Successful booking opens the confirmation screen
 *  - Booking failure shows an error and re-enables the button (US-19 re-enable)
 *  - Confirmation email HTTP request is sent with correct fields (EmailJS path)
 *  - Button is disabled immediately on tap to prevent double-booking (US-19)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class US09_ReserveTicketE2ETest {

    private MockWebServer emailServer;

    @Before
    public void setUp() throws Exception {
        FirebaseAuth.getInstance().signOut();
        emailNotification.suppressEmailsForTesting = true;
        emailServer = new MockWebServer();
        emailServer.start();
    }

    @After
    public void tearDown() throws Exception {
        emailNotification.suppressEmailsForTesting = false;
        emailNotification.testEmailEndpointUrl = null;
        if (emailServer != null) emailServer.shutdown();
    }

    // ── Intent helpers ────────────────────────────────────────────────────────

    private static Intent availableEventIntent() {
        Intent i = new Intent(ApplicationProvider.getApplicationContext(), BookingActivity.class);
        i.putExtra(BookingActivity.EXTRA_INSTRUMENTATION_USE_INTENT_EVENT_ONLY, true);
        i.putExtra("eventId",       "us09-evt-available");
        i.putExtra("eventTitle",    "Jazz Night");
        i.putExtra("eventLocation", "Montreal");
        i.putExtra("eventDate",     "May 1, 2026 at 8:00 PM");
        i.putExtra("eventPrice",    "75");
        i.putExtra("eventStatus",   EventStatus.AVAILABLE.toFirestoreValue());
        i.putExtra("availableSeats", 40);
        return i;
    }

    private static Intent soldOutEventIntent() {
        Intent i = availableEventIntent();
        i.putExtra("eventId",        "us09-evt-soldout");
        i.putExtra("availableSeats", 0);
        i.putExtra("eventStatus",    EventStatus.SOLDOUT.toFirestoreValue());
        return i;
    }

    private static Intent cancelledEventIntent() {
        Intent i = availableEventIntent();
        i.putExtra("eventId",      "us09-evt-cancelled");
        i.putExtra("eventStatus",  EventStatus.CANCELLED.toFirestoreValue());
        return i;
    }

    // ── US-09: Event details are shown ────────────────────────────────────────

    @Test
    public void bookingScreen_showsEventTitle() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(availableEventIntent())) {
            onView(withId(R.id.eventTitleText))
                    .check(matches(withText("Jazz Night")));
        }
    }

    @Test
    public void bookingScreen_showsEventLocation() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(availableEventIntent())) {
            onView(withId(R.id.eventLocationText))
                    .check(matches(withText(containsString("Montreal"))));
        }
    }

    @Test
    public void bookingScreen_showsSeatsRemaining() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(availableEventIntent())) {
            onView(withId(R.id.seatsText))
                    .check(matches(withText(containsString("40"))));
        }
    }

    @Test
    public void bookingScreen_showsPrice() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(availableEventIntent())) {
            onView(withId(R.id.eventPriceText))
                    .check(matches(withText(containsString("75"))));
        }
    }

    // ── US-09: Confirm button states ──────────────────────────────────────────

    @Test
    public void bookingScreen_availableEvent_confirmButtonEnabled() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(availableEventIntent())) {
            onView(withId(R.id.confirmBookingBtn)).check(matches(isEnabled()));
        }
    }

    @Test
    public void bookingScreen_soldOutEvent_confirmButtonDisabled() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(soldOutEventIntent())) {
            onView(withId(R.id.confirmBookingBtn)).check(matches(not(isEnabled())));
            onView(withId(R.id.confirmBookingBtn)).check(matches(withText("Sold Out")));
        }
    }

    @Test
    public void bookingScreen_cancelledEvent_confirmButtonDisabled() {
        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(cancelledEventIntent())) {
            onView(withId(R.id.confirmBookingBtn)).check(matches(not(isEnabled())));
            onView(withId(R.id.confirmBookingBtn))
                    .check(matches(withText(containsString("Cancelled"))));
        }
    }

    // ── US-09: Successful booking opens confirmation screen ───────────────────

    @Test
    public void confirmBooking_success_opensConfirmationScreen() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableEventIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callback.onSuccess(
                                    "res-us09-1", event.getTitle(), event.getLocation(),
                                    "May 1, 2026 at 8:00 PM", "75", "TK-001", "A1");
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());

            // Confirmation screen must appear with the correct reservation ID
            onView(withId(R.id.confirmedReservationId))
                    .check(matches(withText(containsString("res-us09-1"))));
        }
    }

    @Test
    public void confirmBooking_success_showsEventTitleOnConfirmationScreen() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableEventIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callback.onSuccess(
                                    "res-us09-2", event.getTitle(), event.getLocation(),
                                    "May 1, 2026 at 8:00 PM", "75", "TK-002", "B2");
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.confirmedEventTitle))
                    .check(matches(withText("Jazz Night")));
        }
    }

    // ── US-09: Booking failure re-enables button ──────────────────────────────

    @Test
    public void confirmBooking_failure_showsErrorMessage() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableEventIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callback.onFailure("No seats left.");
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.statusText))
                    .check(matches(isDisplayed()))
                    .check(matches(withText(containsString("No seats left."))));
        }
    }

    @Test
    public void confirmBooking_failure_reEnablesConfirmButton() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableEventIntent())) {

            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            callback.onFailure("Transaction failed.");
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.confirmBookingBtn)).check(matches(isEnabled()));
        }
    }

    // ── US-09: Confirmation email is sent via EmailJS ─────────────────────────

    @Test
    public void confirmBooking_sendsEmailWithCorrectFields() throws Exception {
        emailNotification.testEmailEndpointUrl = emailServer.url("/send").toString();
        emailServer.enqueue(new MockResponse().setResponseCode(200));

        emailNotification notifier = new emailNotification(
                "notif-us09", "customer@example.com",
                "Jazz Night", "Montreal", "May 1, 2026 at 8:00 PM",
                "75", "res-us09-email",
                emailNotification.NotificationType.BOOKING_CONFIRMATION);
        notifier.sendEmail("confirmed");

        RecordedRequest req = emailServer.takeRequest(15, TimeUnit.SECONDS);
        assertNotNull("EmailJS request must be sent", req);
        String body = new String(req.getBody().readByteArray(), StandardCharsets.UTF_8);
        assertTrue(body.contains("Jazz Night"));
        assertTrue(body.contains("customer@example.com"));
        assertTrue(body.contains("booking_confirmation"));
    }

    // ── US-09 + US-19: Confirm button shows "Booking…" while in-flight ────────

    @Test
    public void confirmBooking_buttonShowsBookingWhileInFlight() {
        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(availableEventIntent())) {

            // Repo that never calls back — simulates in-flight network call
            scenario.onActivity(activity ->
                    activity.bookingRepo = new BookingRepository(null, null) {
                        @Override
                        public void bookEvent(Event event, BookingCallback callback) {
                            // intentionally never calls callback
                        }
                    });

            onView(withId(R.id.confirmBookingBtn)).perform(click());
            onView(withId(R.id.confirmBookingBtn))
                    .check(matches(not(isEnabled())))
                    .check(matches(withText("Booking...")));
        }
    }
}
