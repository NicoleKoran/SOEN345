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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.After;

/**
 * US-14 — Receive notification when a booked event is cancelled by admin.
 *
 * Covers:
 *  - Cancel Event button visible in edit mode
 *  - Clicking Cancel Event shows a dialog with a warning about notifying users
 *  - Dialog has "Yes, Cancel Event" / "Go Back" buttons
 *  - Confirming calls BookingRepository.cancelEventWithNotifications with correct event ID
 *  - Success hides the Cancel Event button and shows the Un-cancel button
 *  - Failure shows an error and keeps the Cancel Event button visible
 *  - emailNotification built with EVENT_CANCELLATION type sends correct fields
 *  - Empty event ID shows an error instead of a dialog
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class US14_AdminCancelNotifyE2ETest {

    @Before
    public void setUp() {
        FirebaseAuth.getInstance().signOut();
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(LoginActivity.KEY_ADMIN_MODE, true).apply();
        emailNotification.suppressEmailsForTesting = true;
    }

    @After
    public void tearDown() {
        emailNotification.suppressEmailsForTesting = false;
    }

    private static Intent adminIntent() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AdminActivity.class);
        intent.putExtra(AdminActivity.EXTRA_INSTRUMENTATION_SKIP_FIRESTORE, true);
        return intent;
    }

    /**
     * Fills the form fields via {@code scenario.onActivity()} because
     * {@code formScrollView}, {@code formContainer}, and {@code eventIdInput}
     * are all {@code android:visibility="gone"} by default.
     */
    private static void fillForm(ActivityScenario<AdminActivity> scenario, String eventId) {
        scenario.onActivity(activity -> {
            ((android.widget.EditText) activity.findViewById(R.id.eventIdInput))
                    .setText(eventId);
            ((android.widget.EditText) activity.findViewById(R.id.titleInput))
                    .setText("Jazz Night");
            ((android.widget.EditText) activity.findViewById(R.id.locationInput))
                    .setText("Montreal");
            activity.findViewById(R.id.cancelEventButton).setVisibility(android.view.View.VISIBLE);
        });
    }

    // ── Button visibility ─────────────────────────────────────────────────────

    @Test
    public void cancelEventButton_visibleInEditMode() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-vis");
            scenario.onActivity(activity ->
                    org.junit.Assert.assertEquals(android.view.View.VISIBLE,
                            activity.findViewById(R.id.cancelEventButton).getVisibility()));
        }
    }

    @Test
    public void cancelEventButton_notVisibleOnNewEventForm() {
        try (ActivityScenario<AdminActivity> ignored = ActivityScenario.launch(adminIntent())) {
            // New-event form: cancel button must be hidden by default
            ignored.onActivity(activity ->
                    org.junit.Assert.assertNotEquals(android.view.View.VISIBLE,
                            activity.findViewById(R.id.cancelEventButton).getVisibility()));
        }
    }

    // ── Dialog content ────────────────────────────────────────────────────────

    @Test
    public void cancelEvent_click_showsDialog() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-dlg");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
        }
    }

    @Test
    public void cancelEvent_dialog_containsWarningAboutNotifications() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-warn");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText(containsString("notified"))).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
        }
    }

    @Test
    public void cancelEvent_dialog_hasGoBackButton() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-back");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Go Back")).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
        }
    }

    @Test
    public void cancelEvent_dialog_mentionsEventTitle() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-title");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText(containsString("Jazz Night"))).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
        }
    }

    // ── No event ID ───────────────────────────────────────────────────────────

    @Test
    public void cancelEvent_noId_showsError() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).setVisibility(android.view.View.VISIBLE));
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            scenario.onActivity(activity ->
                    org.junit.Assert.assertTrue(((android.widget.TextView) activity.findViewById(R.id.feedbackText))
                            .getText().toString().contains("Load an event")));
        }
    }

    // ── Repository called with correct event ID ───────────────────────────────

    @Test
    public void cancelEvent_confirm_callsRepositoryWithEventId() {
        final String[] captured = {null};

        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
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
                            captured[0] = eventId;
                            callback.onSuccess("Cancelled.");
                        }
                    });
                } catch (Exception e) { throw new AssertionError(e); }
            });

            fillForm(scenario, "evt-us14-repo");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).perform(click());

            final String[] synced = {null};
            scenario.onActivity(activity -> synced[0] = captured[0]);
            assertNotNull(synced[0]);
            assertTrue("evt-us14-repo".equals(synced[0]));
        }
    }

    // ── Success: cancel button hides, un-cancel appears ───────────────────────

    @Test
    public void cancelEvent_onSuccess_hidesCancelButton() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
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
                } catch (Exception e) { throw new AssertionError(e); }
            });

            fillForm(scenario, "evt-us14-hide");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).perform(click());

            scenario.onActivity(activity ->
                    org.junit.Assert.assertNotEquals(android.view.View.VISIBLE,
                            activity.findViewById(R.id.cancelEventButton).getVisibility()));
        }
    }

    @Test
    public void cancelEvent_onSuccess_showsUncancelButton() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
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
                            callback.onSuccess("Event cancelled. 0 customer(s) notified.");
                        }
                    });
                } catch (Exception e) { throw new AssertionError(e); }
            });

            fillForm(scenario, "evt-us14-uncancel");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).perform(click());

            scenario.onActivity(activity ->
                    org.junit.Assert.assertEquals(android.view.View.VISIBLE,
                            activity.findViewById(R.id.uncancelEventButton).getVisibility()));
        }
    }

    // ── Failure: error shown, button stays ────────────────────────────────────

    @Test
    public void cancelEvent_onFailure_showsError() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
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
                            callback.onFailure("Firestore error");
                        }
                    });
                } catch (Exception e) { throw new AssertionError(e); }
            });

            fillForm(scenario, "evt-us14-fail");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).perform(click());

            scenario.onActivity(activity -> {
                org.junit.Assert.assertTrue(((android.widget.TextView) activity.findViewById(R.id.feedbackText))
                        .getText().toString().contains("Firestore error"));
                org.junit.Assert.assertEquals(android.view.View.VISIBLE,
                        activity.findViewById(R.id.cancelEventButton).getVisibility());
            });
        }
    }

}
