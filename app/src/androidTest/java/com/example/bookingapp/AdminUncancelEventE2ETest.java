package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

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
 * E2E / instrumented tests for the "Un-cancel Event" button added to {@link AdminActivity}.
 *
 * All tests inject fakes so no Firestore connection is needed on CI.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminUncancelEventE2ETest {

    @Before
    public void setUp() {
        FirebaseAuth.getInstance().signOut();
        emailNotification.suppressEmailsForTesting = true;
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

    private static Intent adminIntent() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(), AdminActivity.class);
        intent.putExtra(AdminActivity.EXTRA_INSTRUMENTATION_SKIP_FIRESTORE, true);
        return intent;
    }

    /**
     * Makes the un-cancel button visible and fills the event ID field.
     *
     * Uses {@code scenario.onActivity()} because the form container and
     * {@code eventIdInput} are all {@code visibility="gone"} by default.
     */
    private static void showUncancelButton(ActivityScenario<AdminActivity> scenario,
                                           String eventId) {
        scenario.onActivity(activity -> {
            ((android.widget.EditText) activity.findViewById(R.id.eventIdInput))
                    .setText(eventId);
            ((android.widget.EditText) activity.findViewById(R.id.titleInput))
                    .setText("Jazz Night");
            activity.findViewById(R.id.cancelEventButton).setVisibility(android.view.View.GONE);
            activity.findViewById(R.id.uncancelEventButton).setVisibility(android.view.View.VISIBLE);
        });
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    /**
     * Clicking "Un-cancel Event" must display a confirmation dialog.
     */
    @Test
    public void adminActivity_uncancelButton_showsConfirmationDialog() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {
            showUncancelButton(scenario, "evt-uncancel-1");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.uncancelEventButton).performClick());
            onView(withText("Yes, Restore")).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
        }
    }

    /**
     * The confirmation dialog must mention the event title.
     */
    @Test
    public void adminActivity_uncancelDialog_mentionsEventTitle() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {
            showUncancelButton(scenario, "evt-uncancel-2");
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.uncancelEventButton).performClick());
            onView(withText(containsString("Jazz Night"))).check(matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
        }
    }

    /**
     * If no event ID is set, clicking un-cancel must show an error in feedbackText.
     */
    @Test
    public void adminActivity_uncancelWithNoId_showsError() {
        try (ActivityScenario<AdminActivity> scenario =
                     ActivityScenario.launch(adminIntent())) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.uncancelEventButton).setVisibility(android.view.View.VISIBLE));
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.uncancelEventButton).performClick());
            scenario.onActivity(activity ->
                    org.junit.Assert.assertTrue(((android.widget.TextView) activity.findViewById(R.id.feedbackText))
                            .getText().toString().contains("Load an event")));
        }
    }

    // ── Cancel → Un-cancel toggle ─────────────────────────────────────────────

    /**
     * After a successful cancel, the "Cancel Event" button must be hidden and
     * the "Un-cancel Event" button must become visible.
     */
    @Test
    public void adminActivity_afterCancel_uncancelButtonAppears() {
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
                            callback.onSuccess("Event cancelled. 0 customer(s) notified.");
                        }
                    });
                } catch (Exception e) {
                    throw new AssertionError("Could not inject fake repo", e);
                }
            });

            // Put activity in edit mode with cancel button showing
            scenario.onActivity(activity -> {
                ((android.widget.EditText) activity.findViewById(R.id.eventIdInput))
                        .setText("evt-toggle-1");
                ((android.widget.EditText) activity.findViewById(R.id.titleInput))
                        .setText("Jazz Night");
                activity.findViewById(R.id.cancelEventButton).setVisibility(android.view.View.VISIBLE);
            });

            // Cancel the event
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.cancelEventButton).performClick());
            onView(withText("Yes, Cancel Event")).perform(click());

            // Un-cancel button must now be visible; cancel button must be gone
            scenario.onActivity(activity -> {
                org.junit.Assert.assertEquals(android.view.View.VISIBLE,
                        activity.findViewById(R.id.uncancelEventButton).getVisibility());
                org.junit.Assert.assertNotEquals(android.view.View.VISIBLE,
                        activity.findViewById(R.id.cancelEventButton).getVisibility());
            });
        }
    }
}
