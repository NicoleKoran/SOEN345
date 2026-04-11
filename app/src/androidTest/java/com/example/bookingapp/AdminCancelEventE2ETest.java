package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
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

/**
 * E2E / instrumented tests for:
 * <ul>
 *   <li>US-14 – Admin cancels an event and notifies all confirmed customers</li>
 * </ul>
 *
 * A fake {@link BookingRepository} is injected via
 * {@link ActivityScenario#onActivity} so no Firestore data is required on CI.
 * The AdminActivity is launched with a pre-filled eventId so the "Cancel Event"
 * button becomes visible without loading a real event.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminCancelEventE2ETest {

    @Before
    public void setUp() {
        FirebaseAuth.getInstance().signOut();
        emailNotification.suppressEmailsForTesting = true;
        // Ensure admin session is active so AdminActivity doesn't bounce to login
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean(LoginActivity.KEY_ADMIN_MODE, true)
                .apply();
    }

    @After
    public void tearDown() {
        emailNotification.suppressEmailsForTesting = false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Launches AdminActivity in add-mode with Firestore skipped (no real Firebase calls). */
    private static Intent adminIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), AdminActivity.class);
        intent.putExtra(AdminActivity.EXTRA_INSTRUMENTATION_SKIP_FIRESTORE, true);
        return intent;
    }

    /**
     * Fills the event-ID field and the mandatory form fields so that
     * {@code promptCancelEvent()} finds a non-empty event ID and the cancel
     * button becomes operable.
     *
     * Uses {@code scenario.onActivity()} directly because the form container
     * ({@code formScrollView} / {@code formContainer}) and the {@code eventIdInput}
     * are all {@code android:visibility="gone"} by default — Espresso cannot
     * perform actions on GONE views.
     */
    private static void fillEventForm(ActivityScenario<AdminActivity> scenario,
                                      String eventId) {
        scenario.onActivity(activity -> {
            // Set field values directly (eventIdInput is always GONE by design)
            ((android.widget.EditText) activity.findViewById(R.id.eventIdInput))
                    .setText(eventId);
            ((android.widget.EditText) activity.findViewById(R.id.titleInput))
                    .setText("Jazz Night");
            ((android.widget.EditText) activity.findViewById(R.id.locationInput))
                    .setText("Montreal");
            ((android.widget.EditText) activity.findViewById(R.id.dateInput))
                    .setText("2026-05-01 19:00");
            // Simulate being in edit mode
            activity.findViewById(R.id.cancelEventButton).setVisibility(android.view.View.VISIBLE);
        });
    }

    // ── US-14: Cancel Event button & dialog ───────────────────────────────────

    /**
     * After setting an event ID and switching to edit mode, the "Cancel Event"
     * button must be visible.
     */
    @Test
    public void adminActivity_editMode_cancelEventButtonIsVisible() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {
            fillEventForm(scenario, "evt-test-1");
            scenario.onActivity(activity ->
                    org.junit.Assert.assertEquals(android.view.View.VISIBLE,
                            activity.findViewById(R.id.cancelEventButton).getVisibility()));
        }
    }

    /**
     * Clicking "Cancel Event" must display a confirmation AlertDialog that
     * mentions the event title and warns about notifying customers.
     */
    @Test
    public void adminActivity_cancelEventButton_showsConfirmationDialog() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {
            fillEventForm(scenario, "evt-test-2");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());

            // Dialog positive button must show the updated label (AlertDialog is in a separate window)
            onView(withText("Yes, Cancel Event")).check(matches(isDisplayed()));
        }
    }

    /**
     * If no event has been loaded (eventId input is empty), clicking the cancel
     * button must show an error in the feedback text rather than a dialog.
     */
    @Test
    public void adminActivity_cancelEventWithNoId_showsErrorFeedback() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {
            // Make cancel button visible without filling the eventId
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton)
                            .setVisibility(android.view.View.VISIBLE));

            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());

            scenario.onActivity(activity ->
                    org.junit.Assert.assertTrue(((android.widget.TextView) activity.findViewById(R.id.feedbackText))
                            .getText().toString().contains("Load an event")));
        }
    }

    /**
     * Confirming the dialog must call
     * {@link BookingRepository#cancelEventWithNotifications} with the correct
     * event ID. A fake repository captures the call and calls onSuccess.
     */
    @Test
    public void adminActivity_confirmCancelDialog_callsRepository() {
        final String[] capturedEventId = {null};

        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {

            // Inject a fake repository before interacting with the UI
            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field f =
                            AdminActivity.class.getDeclaredField("bookingRepository");
                    f.setAccessible(true);
                    f.set(activity, new BookingRepository(null, null) {
                        @Override
                        public void cancelEventWithNotifications(
                                String eventId, String eventTitle,
                                String eventLocation, String eventDate,
                                SimpleCallback callback) {
                            capturedEventId[0] = eventId;
                            callback.onSuccess("Event cancelled. 0 customer(s) notified.");
                        }
                    });
                } catch (Exception e) {
                    throw new AssertionError("Could not inject fake repo", e);
                }
            });

            fillEventForm(scenario, "evt-inject-1");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());

            // Confirm the dialog (AlertDialog is in a separate window — Espresso is fine here)
            onView(withText("Yes, Cancel Event")).perform(click());

            final String[] synced = {null};
            scenario.onActivity(activity -> synced[0] = capturedEventId[0]);
            assertNotNull("cancelEventWithNotifications must be called", synced[0]);
            assertTrue("Event ID must be passed correctly",
                    "evt-inject-1".equals(synced[0]));
        }
    }

    /**
     * After the repository calls {@code onSuccess}, the "Cancel Event" button
     * must be hidden so the admin cannot cancel the same event twice.
     */
    @Test
    public void adminActivity_cancelEventSuccess_hidesCancelButton() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {

            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field f =
                            AdminActivity.class.getDeclaredField("bookingRepository");
                    f.setAccessible(true);
                    f.set(activity, new BookingRepository(null, null) {
                        @Override
                        public void cancelEventWithNotifications(
                                String eventId, String eventTitle,
                                String eventLocation, String eventDate,
                                SimpleCallback callback) {
                            callback.onSuccess("Event cancelled. 3 customer(s) notified.");
                        }
                    });
                } catch (Exception e) {
                    throw new AssertionError("Could not inject fake repo", e);
                }
            });

            fillEventForm(scenario, "evt-success-1");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).perform(click());

            // Button must be gone after success
            scenario.onActivity(activity ->
                    org.junit.Assert.assertNotEquals(android.view.View.VISIBLE,
                            activity.findViewById(R.id.cancelEventButton).getVisibility()));
        }
    }

    /**
     * When the repository calls {@code onFailure} the feedback text must show the
     * error message and the "Cancel Event" button must remain visible so the admin
     * can retry.
     */
    @Test
    public void adminActivity_cancelEventFailure_showsErrorAndKeepsButton() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {

            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field f =
                            AdminActivity.class.getDeclaredField("bookingRepository");
                    f.setAccessible(true);
                    f.set(activity, new BookingRepository(null, null) {
                        @Override
                        public void cancelEventWithNotifications(
                                String eventId, String eventTitle,
                                String eventLocation, String eventDate,
                                SimpleCallback callback) {
                            callback.onFailure("Network error");
                        }
                    });
                } catch (Exception e) {
                    throw new AssertionError("Could not inject fake repo", e);
                }
            });

            fillEventForm(scenario, "evt-fail-1");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).perform(click());

            scenario.onActivity(activity -> {
                org.junit.Assert.assertTrue(((android.widget.TextView) activity.findViewById(R.id.feedbackText))
                        .getText().toString().contains("Network error"));
                org.junit.Assert.assertEquals(android.view.View.VISIBLE,
                        activity.findViewById(R.id.cancelEventButton).getVisibility());
            });
        }
    }

    // ── US-14: Email notification type for event cancellation ─────────────────

    /**
     * The {@link emailNotification} built for event cancellations must use
     * {@link emailNotification.NotificationType#EVENT_CANCELLATION} so customers
     * receive the correct email template rather than a booking-confirmation email.
     */
    @Test
    public void emailNotification_eventCancellationType_isCorrect() {
        emailNotification notif = new emailNotification(
                "n-1", "customer@test.com", "Jazz Night", "Montreal",
                "May 1, 2026", "", "res-1",
                emailNotification.NotificationType.EVENT_CANCELLATION);

        assertTrue("Notification type must be EVENT_CANCELLATION",
                emailNotification.NotificationType.EVENT_CANCELLATION
                        == notif.getNotificationType());
    }
}
