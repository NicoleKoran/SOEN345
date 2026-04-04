package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
 * Emulator tests for booking confirmation UI and the email notification HTTP path.
 * Booking flow uses a debug-only intent extra plus a fake {@link BookingRepository} so no
 * Firestore data is required on CI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BookingEmailInstrumentedTest {

    private MockWebServer emailServer;

    @Before
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    @Before
    public void startEmailMockServer() throws Exception {
        emailServer = new MockWebServer();
        emailServer.start();
    }

    @After
    public void tearDown() throws Exception {
        emailNotification.testEmailEndpointUrl = null;
        if (emailServer != null) {
            emailServer.shutdown();
        }
    }

    private static Intent bookingIntentForInstrumentation() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), BookingActivity.class);
        intent.putExtra(BookingActivity.EXTRA_INSTRUMENTATION_USE_INTENT_EVENT_ONLY, true);
        intent.putExtra("eventId", "instrumented-event-1");
        intent.putExtra("eventTitle", "Jazz Night");
        intent.putExtra("eventLocation", "Montreal");
        intent.putExtra("eventDate", "May 1, 2026 at 8:00 PM");
        intent.putExtra("eventPrice", "75");
        intent.putExtra("eventStatus", EventStatus.AVAILABLE.toFirestoreValue());
        intent.putExtra("availableSeats", 40);
        return intent;
    }

    @Test
    public void bookingConfirmedScreen_showsEventDetailsAndEmailMessage() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), BookingConfirmedActivity.class);
        intent.putExtra("reservationId", "res-xyz");
        intent.putExtra("eventTitle", "Symphony Hall");
        intent.putExtra("eventLocation", "Toronto");
        intent.putExtra("eventDate", "Jun 10, 2026 at 7:30 PM");
        intent.putExtra("price", "120");
        intent.putExtra("ticketId", "TK-9001");
        intent.putExtra("seatNumber", "4B");

        try (ActivityScenario<BookingConfirmedActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.confirmedEventTitle))
                    .check(matches(withText("Symphony Hall")));
            onView(withId(R.id.confirmedReservationId))
                    .check(matches(withText("Reservation ID: res-xyz")));
            onView(withText("A confirmation email has been sent to you from bienvenueBookingSystems."))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void confirmBooking_withFakeRepository_opensConfirmationScreen() {
        Intent intent = bookingIntentForInstrumentation();

        try (ActivityScenario<BookingActivity> scenario =
                     ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> activity.bookingRepo = new BookingRepository() {
                @Override
                public void bookEvent(Event event, BookingCallback callback) {
                    callback.onSuccess(
                            "res-inst-1",
                            event.getTitle(),
                            event.getLocation(),
                            "May 1, 2026 at 8:00 PM",
                            "75",
                            "TK-INST",
                            "12A");
                }
            });

            onView(withId(R.id.confirmBookingBtn)).check(matches(isEnabled()));
            onView(withId(R.id.confirmBookingBtn)).perform(click());

            onView(withId(R.id.confirmedEventTitle))
                    .check(matches(withText("Jazz Night")));
            onView(withId(R.id.confirmedReservationId))
                    .check(matches(withText("Reservation ID: res-inst-1")));
            onView(withId(R.id.confirmedTicketId))
                    .check(matches(withText("Ticket: TK-INST")));
        }
    }

    @Test
    public void bookingScreen_soldOutIntent_disablesConfirm() {
        Intent intent = bookingIntentForInstrumentation();
        intent.putExtra("availableSeats", 0);
        intent.putExtra("eventStatus", EventStatus.SOLDOUT.toFirestoreValue());

        try (ActivityScenario<BookingActivity> ignored =
                     ActivityScenario.launch(intent)) {
            onView(withId(R.id.confirmBookingBtn)).check(matches(withText("Sold Out")));
            onView(withId(R.id.confirmBookingBtn)).check(matches(not(isEnabled())));
        }
    }

    @Test
    public void emailNotification_postsJsonToTestEndpoint() throws Exception {
        emailNotification.testEmailEndpointUrl = emailServer.url("/send").toString();
        emailServer.enqueue(new MockResponse().setResponseCode(200));

        emailNotification notifier = new emailNotification(
                "notif-1",
                "customer@example.com",
                "Concert",
                "Venue",
                "Aug 1, 2026",
                "55",
                "res-42");
        notifier.sendEmail("confirmed");

        RecordedRequest request = emailServer.takeRequest(15, TimeUnit.SECONDS);
        assertNotNull(request);
        String body = new String(request.getBody().readByteArray(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"event_title\""));
        assertTrue(body.contains("Concert"));
        assertTrue(body.contains("customer@example.com"));
    }
}
