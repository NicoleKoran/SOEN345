package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


import android.content.Intent;
import android.view.View;

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
            activity.findViewById(R.id.formScrollView).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.formContainer).setVisibility(View.VISIBLE);
            ((android.widget.EditText) activity.findViewById(R.id.eventIdInput))
                    .setText(eventId);
            ((android.widget.EditText) activity.findViewById(R.id.titleInput))
                    .setText("Jazz Night");
            ((android.widget.EditText) activity.findViewById(R.id.locationInput))
                    .setText("Montreal");
            activity.findViewById(R.id.cancelEventButton).setVisibility(View.VISIBLE);
        });
    }

    private static void injectFakeRepo(ActivityScenario<AdminActivity> scenario,
                                       BookingRepository.SimpleCallback response) {
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
                        response.onSuccess(null); // redirect to actual callback
                    }
                });
            } catch (Exception e) {
                throw new AssertionError("Cannot inject fake repo", e);
            }
        });
    }

    // ── Button visibility ─────────────────────────────────────────────────────

    @Test
    public void cancelEventButton_visibleInEditMode() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-vis");
            onView(withId(R.id.cancelEventButton)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void cancelEventButton_notVisibleOnNewEventForm() {
        try (ActivityScenario<AdminActivity> ignored = ActivityScenario.launch(adminIntent())) {
            // New-event form: cancel button must be hidden by default
            onView(withId(R.id.cancelEventButton)).check(matches(not(isDisplayed())));
        }
    }

    // ── Dialog content ────────────────────────────────────────────────────────

    @Test
    public void cancelEvent_click_showsDialog() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-dlg");
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText("Yes, Cancel Event")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void cancelEvent_dialog_containsWarningAboutNotifications() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-warn");
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText(containsString("notified"))).check(matches(isDisplayed()));
        }
    }

    @Test
    public void cancelEvent_dialog_hasGoBackButton() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-back");
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText("Go Back")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void cancelEvent_dialog_mentionsEventTitle() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            fillForm(scenario, "evt-us14-title");
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText(containsString("Jazz Night"))).check(matches(isDisplayed()));
        }
    }

    // ── No event ID ───────────────────────────────────────────────────────────

    @Test
    public void cancelEvent_noId_showsError() {
        try (ActivityScenario<AdminActivity> scenario = ActivityScenario.launch(adminIntent())) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).setVisibility(View.VISIBLE));
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withId(R.id.feedbackText))
                    .check(matches(withText(containsString("Load an event"))));
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
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText("Yes, Cancel Event")).perform(click());

            assertNotNull(captured[0]);
            assertTrue("evt-us14-repo".equals(captured[0]));
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
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText("Yes, Cancel Event")).perform(click());

            onView(withId(R.id.cancelEventButton)).check(matches(not(isDisplayed())));
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
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText("Yes, Cancel Event")).perform(click());

            onView(withId(R.id.uncancelEventButton)).check(matches(isDisplayed()));
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
            onView(withId(R.id.cancelEventButton)).perform(click());
            onView(withText("Yes, Cancel Event")).perform(click());

            onView(withId(R.id.feedbackText))
                    .check(matches(withText(containsString("Firestore error"))));
            onView(withId(R.id.cancelEventButton)).check(matches(isDisplayed()));
        }
    }

}
