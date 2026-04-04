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
import com.google.firebase.firestore.Transaction;

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
