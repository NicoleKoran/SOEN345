package com.example.bookingapp;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Date;

/**
 * BookingRepository tests using Robolectric so that Firebase SDK stubs are active.
 * With no user signed in, getCurrentUser() returns null — exercising the
 * "not logged in" guard without needing MockedStatic or class mocking.
 */
@RunWith(RobolectricTestRunner.class)
public class BookingRepositoryTest {

    @Before
    public void initFirebase() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        }
    }

    private Event buildEvent() {
        return new Event(
                "evt-1", "Rock Concert", "Great show", "Montreal",
                new Date(), 100, 50, EventCategory.MOVIE, EventStatus.AVAILABLE
        );
    }

    // ── bookEvent: user not logged in ─────────────────────────────────────────

    @Test
    public void bookEvent_whenNoUserLoggedIn_callsOnFailureWithMessage() {
        BookingRepository repo = new BookingRepository();
        final String[] captured = {null};

        repo.bookEvent(buildEvent(), new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("onSuccess should not be called when user is not logged in");
            }

            @Override
            public void onFailure(String errorMessage) {
                captured[0] = errorMessage;
            }
        });

        assertEquals("You must be logged in to book.", captured[0]);
    }

    @Test
    public void bookEvent_withNullEvent_whenNotLoggedIn_stillCallsOnFailure() {
        BookingRepository repo = new BookingRepository();
        final boolean[] failureCalled = {false};

        // null event — the null-user guard fires before any event field access
        repo.bookEvent(null, new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(String errorMessage) {
                failureCalled[0] = true;
            }
        });

        assertTrue(failureCalled[0]);
    }

    // ── BookingCallback interface contract ────────────────────────────────────

    @Test
    public void bookingCallback_onSuccess_deliversAllParameters() {
        final String[] captured = new String[7];

        BookingRepository.BookingCallback cb = new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String reservationId, String eventTitle,
                                  String eventLocation, String eventDate,
                                  String price, String ticketId, String seatNumber) {
                captured[0] = reservationId;
                captured[1] = eventTitle;
                captured[2] = eventLocation;
                captured[3] = eventDate;
                captured[4] = price;
                captured[5] = ticketId;
                captured[6] = seatNumber;
            }

            @Override
            public void onFailure(String errorMessage) {
                fail("onFailure should not be called");
            }
        };

        cb.onSuccess("res-1", "Concert", "Montreal", "May 1", "50", "TKT-1", "A1");

        assertEquals("res-1",    captured[0]);
        assertEquals("Concert",  captured[1]);
        assertEquals("Montreal", captured[2]);
        assertEquals("May 1",    captured[3]);
        assertEquals("50",       captured[4]);
        assertEquals("TKT-1",    captured[5]);
        assertEquals("A1",       captured[6]);
    }

    @Test
    public void bookingCallback_onFailure_deliversErrorMessage() {
        final String[] captured = {null};

        BookingRepository.BookingCallback cb = new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("onSuccess should not be called");
            }

            @Override
            public void onFailure(String errorMessage) {
                captured[0] = errorMessage;
            }
        };

        cb.onFailure("Network timeout");
        assertEquals("Network timeout", captured[0]);
    }

    // ── Customer.reserveTicket integration (end-to-end, no mocks needed) ─────

    @Test
    public void customer_reserveTicket_propagatesNotLoggedInFailure() {
        Customer customer = new Customer(
                "uid-1", "Alice", "alice@test.com", "+15141234567", "pw");
        BookingRepository repo = new BookingRepository();
        final String[] captured = {null};

        customer.reserveTicket(buildEvent(), repo, new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(String errorMessage) {
                captured[0] = errorMessage;
            }
        });

        assertEquals("You must be logged in to book.", captured[0]);
    }
}
