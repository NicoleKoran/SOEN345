package com.example.bookingapp;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

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

    // ── Injected Firestore/Auth: full bookEvent paths ─────────────────────────

    private FirebaseUser mockLoggedInUser(String uid, String email) {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn(uid);
        when(user.getDisplayName()).thenReturn(null);
        when(user.getEmail()).thenReturn(email);
        when(user.getPhoneNumber()).thenReturn(null);
        return user;
    }

    private void stubTxGet(Transaction tx, DocumentReference eventRef, DocumentSnapshot snap) {
        try {
            doReturn(snap).when(tx).get(eventRef);
        } catch (FirebaseFirestoreException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void stubRunTransactionWithSnap(
            FirebaseFirestore db,
            DocumentReference eventRef,
            DocumentSnapshot eventSnap
    ) {
        Transaction tx = mock(Transaction.class);
        stubTxGet(tx, eventRef, eventSnap);
        when(db.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Object> fn = invocation.getArgument(0);
            try {
                fn.apply(tx);
                return Tasks.forResult(null);
            } catch (FirebaseFirestoreException e) {
                return Tasks.forException(e);
            }
        });
    }

    @Test
    public void bookEvent_transactionSuccess_callsOnSuccess() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
        when(auth.getCurrentUser()).thenReturn(user);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);

        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document()).thenReturn(reservationRef);
        when(reservationRef.getId()).thenReturn("res-doc-1");

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());
        when(snap.getLong("availableSeats")).thenReturn(5L);
        stubRunTransactionWithSnap(db, eventRef, snap);

        when(reservationRef.update(anyString(), any(), anyString(), any())).thenReturn(Tasks.forResult(null));

        Event event = new Event(
                "evt-1", "Show", "D", "MTL",
                new Date(1_700_000_000_000L), 100, 50,
                EventCategory.CONCERT, EventStatus.AVAILABLE);

        BookingRepository repo = new BookingRepository(db, auth);
        final boolean[] ok = {false};

        repo.bookEvent(event, new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String reservationId, String eventTitle,
                                  String eventLocation, String eventDate,
                                  String price, String ticketId, String seatNumber) {
                assertEquals("res-doc-1", reservationId);
                assertEquals("Show", eventTitle);
                assertNotNull(ticketId);
                ok[0] = true;
            }

            @Override
            public void onFailure(String errorMessage) {
                fail(errorMessage);
            }
        });

        ShadowLooper.idleMainLooper();
        assertTrue(ok[0]);
    }

    @Test
    public void bookEvent_eventNotExists_callsOnFailure() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
        when(auth.getCurrentUser()).thenReturn(user);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document()).thenReturn(reservationRef);

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(false);
        stubRunTransactionWithSnap(db, eventRef, snap);

        Event event = buildEvent();
        BookingRepository repo = new BookingRepository(db, auth);
        final String[] err = {null};

        repo.bookEvent(event, new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("unexpected success");
            }

            @Override
            public void onFailure(String errorMessage) {
                err[0] = errorMessage;
            }
        });

        ShadowLooper.idleMainLooper();
        assertTrue(err[0] != null && err[0].contains("Event not found"));
    }

    @Test
    public void bookEvent_cancelledEvent_callsOnFailure() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
        when(auth.getCurrentUser()).thenReturn(user);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document()).thenReturn(reservationRef);

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("status")).thenReturn(EventStatus.CANCELLED.toFirestoreValue());
        stubRunTransactionWithSnap(db, eventRef, snap);

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] err = {null};

        repo.bookEvent(buildEvent(), new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("unexpected success");
            }

            @Override
            public void onFailure(String errorMessage) {
                err[0] = errorMessage;
            }
        });

        ShadowLooper.idleMainLooper();
        assertTrue(err[0] != null && err[0].toLowerCase().contains("cancelled"));
    }

    @Test
    public void bookEvent_nullAvailableSeats_callsOnFailure() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
        when(auth.getCurrentUser()).thenReturn(user);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document()).thenReturn(reservationRef);

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());
        when(snap.getLong("availableSeats")).thenReturn(null);
        stubRunTransactionWithSnap(db, eventRef, snap);

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] err = {null};

        repo.bookEvent(buildEvent(), new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("unexpected success");
            }

            @Override
            public void onFailure(String errorMessage) {
                err[0] = errorMessage;
            }
        });

        ShadowLooper.idleMainLooper();
        assertTrue(err[0] != null && err[0].toLowerCase().contains("sold out"));
    }

    @Test
    public void bookEvent_transactionThrows_callsOnFailure() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) {
            FirebaseFirestore db = mock(FirebaseFirestore.class);
            FirebaseAuth auth = mock(FirebaseAuth.class);
            FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
            when(auth.getCurrentUser()).thenReturn(user);

            CollectionReference eventsCol = mock(CollectionReference.class);
            CollectionReference resCol = mock(CollectionReference.class);
            when(db.collection("events")).thenReturn(eventsCol);
            when(db.collection("reservations")).thenReturn(resCol);
            DocumentReference eventRef = mock(DocumentReference.class);
            DocumentReference reservationRef = mock(DocumentReference.class);
            when(eventsCol.document("evt-1")).thenReturn(eventRef);
            when(resCol.document()).thenReturn(reservationRef);

            Task<Void> failed = Tasks.forException(new Exception("tx failed"));
            when(db.runTransaction(any(Transaction.Function.class))).thenReturn(failed);

            BookingRepository repo = new BookingRepository(db, auth);
            final String[] err = {null};
            repo.bookEvent(buildEvent(), new BookingRepository.BookingCallback() {
                @Override
                public void onSuccess(String r, String t, String l, String d,
                                      String p, String ti, String s) {
                    fail("unexpected success");
                }

                @Override
                public void onFailure(String errorMessage) {
                    err[0] = errorMessage;
                }
            });

            ShadowLooper.idleMainLooper();
            assertNotNull(err[0]);
            log.verify(() -> Log.e(eq("BookingRepository"), anyString()));
        }
    }

    @Test
    public void bookEvent_soldOut_callsOnFailure() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
        when(auth.getCurrentUser()).thenReturn(user);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document()).thenReturn(reservationRef);

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());
        when(snap.getLong("availableSeats")).thenReturn(0L);
        stubRunTransactionWithSnap(db, eventRef, snap);

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] err = {null};

        repo.bookEvent(buildEvent(), new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                fail("unexpected success");
            }

            @Override
            public void onFailure(String errorMessage) {
                err[0] = errorMessage;
            }
        });

        ShadowLooper.idleMainLooper();
        assertTrue(err[0] != null && err[0].toLowerCase().contains("sold out"));
    }

    @Test
    public void bookEvent_lastSeat_marksSoldOutOnTransaction() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
        when(auth.getCurrentUser()).thenReturn(user);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document()).thenReturn(reservationRef);
        when(reservationRef.getId()).thenReturn("res-1");

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());
        when(snap.getLong("availableSeats")).thenReturn(1L);

        Transaction tx = mock(Transaction.class);
        stubTxGet(tx, eventRef, snap);
        when(db.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Object> fn = invocation.getArgument(0);
            try {
                fn.apply(tx);
                return Tasks.forResult(null);
            } catch (FirebaseFirestoreException e) {
                return Tasks.forException(e);
            }
        });

        when(reservationRef.update(anyString(), any(), anyString(), any())).thenReturn(Tasks.forResult(null));

        BookingRepository repo = new BookingRepository(db, auth);
        final boolean[] ok = {false};

        repo.bookEvent(buildEvent(), new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String d,
                                  String p, String ti, String s) {
                ok[0] = true;
            }

            @Override
            public void onFailure(String errorMessage) {
                fail(errorMessage);
            }
        });

        ShadowLooper.idleMainLooper();
        assertTrue(ok[0]);
        verify(tx).update(eq(eventRef), eq("availableSeats"), eq(0L));
        verify(tx).update(eq(eventRef), eq("status"),
                eq(EventStatus.SOLDOUT.toFirestoreValue()));
    }

    @Test
    public void bookEvent_nullEventDate_usesDateTbdInCallback() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);
        when(user.getUid()).thenReturn("u1");
        when(user.getDisplayName()).thenReturn("Alice");
        when(user.getEmail()).thenReturn("a@b.com");
        when(user.getPhoneNumber()).thenReturn("+1000");

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);
        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document()).thenReturn(reservationRef);
        when(reservationRef.getId()).thenReturn("res-1");

        DocumentSnapshot snap = mock(DocumentSnapshot.class);
        when(snap.exists()).thenReturn(true);
        when(snap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());
        when(snap.getLong("availableSeats")).thenReturn(3L);
        stubRunTransactionWithSnap(db, eventRef, snap);
        when(reservationRef.update(anyString(), any(), anyString(), any())).thenReturn(Tasks.forResult(null));

        Event event = new Event(
                "evt-1", "NoDate", "D", "MTL",
                null, 10, 3, EventCategory.MOVIE, EventStatus.AVAILABLE);

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] dateOut = {null};

        repo.bookEvent(event, new BookingRepository.BookingCallback() {
            @Override
            public void onSuccess(String r, String t, String l, String eventDate,
                                  String p, String ti, String s) {
                dateOut[0] = eventDate;
            }

            @Override
            public void onFailure(String errorMessage) {
                fail(errorMessage);
            }
        });

        ShadowLooper.idleMainLooper();
        assertEquals("Date TBD", dateOut[0]);
    }

    // ── cancelReservation ─────────────────────────────────────────────────────

    /**
     * Stubs a Firestore transaction that reads two document refs.
     * Used by cancelReservation tests which read both the event and the reservation.
     */
    @SuppressWarnings("unchecked")
    private void stubTwoDocTransaction(
            FirebaseFirestore db,
            DocumentReference ref1, DocumentSnapshot snap1,
            DocumentReference ref2, DocumentSnapshot snap2
    ) {
        Transaction tx = mock(Transaction.class);
        try {
            doReturn(snap1).when(tx).get(ref1);
            doReturn(snap2).when(tx).get(ref2);
        } catch (FirebaseFirestoreException e) {
            throw new AssertionError(e);
        }
        when(db.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Object> fn = invocation.getArgument(0);
            try {
                fn.apply(tx);
                return Tasks.forResult(null);
            } catch (FirebaseFirestoreException e) {
                return Tasks.forException(e);
            }
        });
    }

    @Test
    public void cancelReservation_success_callsOnSuccess() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);

        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document("res-1")).thenReturn(reservationRef);

        DocumentSnapshot eventSnap = mock(DocumentSnapshot.class);
        when(eventSnap.exists()).thenReturn(true);
        when(eventSnap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());
        when(eventSnap.getLong("availableSeats")).thenReturn(5L);

        DocumentSnapshot reservationSnap = mock(DocumentSnapshot.class);
        when(reservationSnap.exists()).thenReturn(true);
        when(reservationSnap.getString("status")).thenReturn(ReservationStatus.CONFIRMED.toFirestoreValue());

        stubTwoDocTransaction(db, eventRef, eventSnap, reservationRef, reservationSnap);

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] result = {null};

        repo.cancelReservation("res-1", "evt-1", "user@test.com",
                "Jazz Night", "Montreal", "Apr 12, 2026",
                new BookingRepository.SimpleCallback() {
                    @Override public void onSuccess(String msg) { result[0] = msg; }
                    @Override public void onFailure(String msg) { fail("Unexpected failure: " + msg); }
                });

        ShadowLooper.idleMainLooper();
        assertNotNull(result[0]);
        assertTrue(result[0].contains("cancelled"));
    }

    @Test
    public void cancelReservation_alreadyCancelled_callsOnFailure() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);

        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document("res-1")).thenReturn(reservationRef);

        DocumentSnapshot eventSnap = mock(DocumentSnapshot.class);
        when(eventSnap.exists()).thenReturn(true);
        when(eventSnap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());

        DocumentSnapshot reservationSnap = mock(DocumentSnapshot.class);
        when(reservationSnap.exists()).thenReturn(true);
        when(reservationSnap.getString("status"))
                .thenReturn(ReservationStatus.CANCELLED.toFirestoreValue());

        stubTwoDocTransaction(db, eventRef, eventSnap, reservationRef, reservationSnap);

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] err = {null};

        repo.cancelReservation("res-1", "evt-1", "user@test.com",
                "Jazz Night", "Montreal", "Apr 12, 2026",
                new BookingRepository.SimpleCallback() {
                    @Override public void onSuccess(String msg) { fail("Should not succeed"); }
                    @Override public void onFailure(String msg) { err[0] = msg; }
                });

        ShadowLooper.idleMainLooper();
        assertNotNull(err[0]);
        assertTrue(err[0].toLowerCase().contains("already cancelled"));
    }

    @Test
    public void cancelReservation_eventNotFound_callsOnFailure() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);

        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-missing")).thenReturn(eventRef);
        when(resCol.document("res-1")).thenReturn(reservationRef);

        DocumentSnapshot eventSnap = mock(DocumentSnapshot.class);
        when(eventSnap.exists()).thenReturn(false);

        DocumentSnapshot reservationSnap = mock(DocumentSnapshot.class);
        when(reservationSnap.exists()).thenReturn(true);

        stubTwoDocTransaction(db, eventRef, eventSnap, reservationRef, reservationSnap);

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] err = {null};

        repo.cancelReservation("res-1", "evt-missing", "user@test.com",
                "Jazz Night", "Montreal", "Apr 12, 2026",
                new BookingRepository.SimpleCallback() {
                    @Override public void onSuccess(String msg) { fail("Should not succeed"); }
                    @Override public void onFailure(String msg) { err[0] = msg; }
                });

        ShadowLooper.idleMainLooper();
        assertNotNull(err[0]);
        assertTrue(err[0].contains("Event not found"));
    }

    @Test
    public void cancelReservation_soldOutEvent_restoresAvailableStatus() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);

        CollectionReference eventsCol = mock(CollectionReference.class);
        CollectionReference resCol = mock(CollectionReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(db.collection("reservations")).thenReturn(resCol);

        DocumentReference eventRef = mock(DocumentReference.class);
        DocumentReference reservationRef = mock(DocumentReference.class);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(resCol.document("res-1")).thenReturn(reservationRef);

        DocumentSnapshot eventSnap = mock(DocumentSnapshot.class);
        when(eventSnap.exists()).thenReturn(true);
        when(eventSnap.getString("status")).thenReturn(EventStatus.SOLDOUT.toFirestoreValue());
        when(eventSnap.getLong("availableSeats")).thenReturn(0L);

        DocumentSnapshot reservationSnap = mock(DocumentSnapshot.class);
        when(reservationSnap.exists()).thenReturn(true);
        when(reservationSnap.getString("status")).thenReturn(ReservationStatus.CONFIRMED.toFirestoreValue());

        Transaction tx = mock(Transaction.class);
        try {
            doReturn(eventSnap).when(tx).get(eventRef);
            doReturn(reservationSnap).when(tx).get(reservationRef);
        } catch (FirebaseFirestoreException e) {
            throw new AssertionError(e);
        }
        when(db.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Object> fn = invocation.getArgument(0);
            try { fn.apply(tx); return Tasks.forResult(null); }
            catch (FirebaseFirestoreException e) { return Tasks.forException(e); }
        });

        BookingRepository repo = new BookingRepository(db, auth);
        final boolean[] ok = {false};

        repo.cancelReservation("res-1", "evt-1", "user@test.com",
                "Concert", "MTL", "Apr 12, 2026",
                new BookingRepository.SimpleCallback() {
                    @Override public void onSuccess(String msg) { ok[0] = true; }
                    @Override public void onFailure(String msg) { fail(msg); }
                });

        ShadowLooper.idleMainLooper();
        assertTrue(ok[0]);
        // Verify seat count was incremented and status was restored to available
        verify(tx).update(eventRef, "availableSeats", 1L);
        verify(tx).update(eventRef, "status", EventStatus.AVAILABLE.toFirestoreValue());
    }

    // ── SimpleCallback interface contract ──────────────────────────────────────

    @Test
    public void simpleCallback_onSuccess_deliversMessage() {
        final String[] captured = {null};
        BookingRepository.SimpleCallback cb = new BookingRepository.SimpleCallback() {
            @Override public void onSuccess(String message) { captured[0] = message; }
            @Override public void onFailure(String errorMessage) { fail("Should not fail"); }
        };
        cb.onSuccess("Done");
        assertEquals("Done", captured[0]);
    }

    @Test
    public void simpleCallback_onFailure_deliversMessage() {
        final String[] captured = {null};
        BookingRepository.SimpleCallback cb = new BookingRepository.SimpleCallback() {
            @Override public void onSuccess(String message) { fail("Should not succeed"); }
            @Override public void onFailure(String errorMessage) { captured[0] = errorMessage; }
        };
        cb.onFailure("Something broke");
        assertEquals("Something broke", captured[0]);
    }

    // ── cancelEventWithNotifications ─────────────────────────────────────────

    @Test
    public void cancelEventWithNotifications_noReservations_reportsNoCustomers() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);

        // cancelEvent path
        CollectionReference eventsCol = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(eventRef.update("status", EventStatus.CANCELLED.toFirestoreValue()))
                .thenReturn(Tasks.forResult(null));

        // getConfirmedReservationsForEvent path — empty
        CollectionReference resCol = mock(CollectionReference.class);
        Query q1 = mock(Query.class);
        Query q2 = mock(Query.class);
        QuerySnapshot emptySnap = mock(QuerySnapshot.class);
        when(db.collection("reservations")).thenReturn(resCol);
        when(resCol.whereEqualTo("eventId", "evt-1")).thenReturn(q1);
        when(q1.whereEqualTo("status", ReservationStatus.CONFIRMED.toFirestoreValue())).thenReturn(q2);
        when(q2.get()).thenReturn(Tasks.forResult(emptySnap));
        when(emptySnap.getDocuments()).thenReturn(List.of());

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] result = {null};

        repo.cancelEventWithNotifications("evt-1", "Jazz Night", "Montreal", "Apr 12, 2026",
                new BookingRepository.SimpleCallback() {
                    @Override public void onSuccess(String msg) { result[0] = msg; }
                    @Override public void onFailure(String msg) { fail("Unexpected: " + msg); }
                });

        ShadowLooper.idleMainLooper();
        assertNotNull(result[0]);
        assertTrue(result[0].contains("No customers to notify"));
    }

    @Test
    public void cancelEventWithNotifications_withReservations_batchCancelsAndNotifies() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);

        // cancelEvent path
        CollectionReference eventsCol = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(eventsCol.document("evt-1")).thenReturn(eventRef);
        when(eventRef.update("status", EventStatus.CANCELLED.toFirestoreValue()))
                .thenReturn(Tasks.forResult(null));

        // getConfirmedReservationsForEvent path — one reservation
        CollectionReference resCol = mock(CollectionReference.class);
        Query q1 = mock(Query.class);
        Query q2 = mock(Query.class);
        QuerySnapshot snap = mock(QuerySnapshot.class);
        DocumentSnapshot resDoc = mock(DocumentSnapshot.class);

        Reservation confirmedRes = new Reservation(
                "evt-1", "concert", "user-1", "user@test.com",
                "Jazz Night", "Montreal", new Date(), 0);
        confirmedRes.setReservationId("res-1");

        when(db.collection("reservations")).thenReturn(resCol);
        when(resCol.whereEqualTo("eventId", "evt-1")).thenReturn(q1);
        when(q1.whereEqualTo("status", ReservationStatus.CONFIRMED.toFirestoreValue())).thenReturn(q2);
        when(q2.get()).thenReturn(Tasks.forResult(snap));
        when(snap.getDocuments()).thenReturn(List.of(resDoc));
        when(resDoc.getId()).thenReturn("res-1");
        when(resDoc.toObject(Reservation.class)).thenReturn(confirmedRes);

        // batch path
        WriteBatch mockBatch = mock(WriteBatch.class);
        DocumentReference resRef = mock(DocumentReference.class);
        when(db.batch()).thenReturn(mockBatch);
        when(resCol.document("res-1")).thenReturn(resRef);
        when(mockBatch.update(any(DocumentReference.class), anyString(), any(), anyString(), any()))
                .thenReturn(mockBatch);
        when(mockBatch.commit()).thenReturn(Tasks.forResult(null));

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] result = {null};

        repo.cancelEventWithNotifications("evt-1", "Jazz Night", "Montreal", "Apr 12, 2026",
                new BookingRepository.SimpleCallback() {
                    @Override public void onSuccess(String msg) { result[0] = msg; }
                    @Override public void onFailure(String msg) { fail("Unexpected: " + msg); }
                });

        ShadowLooper.idleMainLooper();
        assertNotNull(result[0]);
        assertTrue(result[0].contains("1 customer"));
        verify(mockBatch).commit();
    }

    @Test
    public void cancelEventWithNotifications_eventCancelFails_callsOnFailure() {
        FirebaseFirestore db = mock(FirebaseFirestore.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);

        CollectionReference eventsCol = mock(CollectionReference.class);
        DocumentReference eventRef = mock(DocumentReference.class);
        when(db.collection("events")).thenReturn(eventsCol);
        when(eventsCol.document("evt-bad")).thenReturn(eventRef);
        when(eventRef.update("status", EventStatus.CANCELLED.toFirestoreValue()))
                .thenReturn(Tasks.forException(new RuntimeException("network error")));

        BookingRepository repo = new BookingRepository(db, auth);
        final String[] err = {null};

        repo.cancelEventWithNotifications("evt-bad", "Show", "MTL", "Apr 1, 2026",
                new BookingRepository.SimpleCallback() {
                    @Override public void onSuccess(String msg) { fail("Should not succeed"); }
                    @Override public void onFailure(String msg) { err[0] = msg; }
                });

        ShadowLooper.idleMainLooper();
        assertNotNull(err[0]);
        assertTrue(err[0].contains("network error"));
    }

    @Test
    public void bookEvent_reservationUpdateFailure_logsError() {
        try (MockedStatic<Log> log = mockStatic(Log.class)) {
            FirebaseFirestore db = mock(FirebaseFirestore.class);
            FirebaseAuth auth = mock(FirebaseAuth.class);
            FirebaseUser user = mockLoggedInUser("u1", "a@b.com");
            when(auth.getCurrentUser()).thenReturn(user);

            CollectionReference eventsCol = mock(CollectionReference.class);
            CollectionReference resCol = mock(CollectionReference.class);
            when(db.collection("events")).thenReturn(eventsCol);
            when(db.collection("reservations")).thenReturn(resCol);
            DocumentReference eventRef = mock(DocumentReference.class);
            DocumentReference reservationRef = mock(DocumentReference.class);
            when(eventsCol.document("evt-1")).thenReturn(eventRef);
            when(resCol.document()).thenReturn(reservationRef);
            when(reservationRef.getId()).thenReturn("res-1");

            DocumentSnapshot snap = mock(DocumentSnapshot.class);
            when(snap.exists()).thenReturn(true);
            when(snap.getString("status")).thenReturn(EventStatus.AVAILABLE.toFirestoreValue());
            when(snap.getLong("availableSeats")).thenReturn(2L);
            stubRunTransactionWithSnap(db, eventRef, snap);

            Task<Void> bad = mock(Task.class);
            when(bad.addOnFailureListener(any())).thenAnswer(inv -> {
                com.google.android.gms.tasks.OnFailureListener l = inv.getArgument(0);
                l.onFailure(new Exception("write failed"));
                return bad;
            });
            when(reservationRef.update(anyString(), any(), anyString(), any())).thenReturn(bad);

            BookingRepository repo = new BookingRepository(db, auth);
            repo.bookEvent(buildEvent(), new BookingRepository.BookingCallback() {
                @Override
                public void onSuccess(String r, String t, String l, String d,
                                      String p, String ti, String s) { /* still called */ }

                @Override
                public void onFailure(String errorMessage) {
                    fail(errorMessage);
                }
            });

            ShadowLooper.idleMainLooper();
            log.verify(() -> Log.e(eq("BookingRepository"),
                    org.mockito.ArgumentMatchers.argThat((String msg) ->
                            msg != null && msg.contains("Could not update reservation status"))));
        }
    }
}
