package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;


import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AdminFlowE2ETest {

    @Before
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
        MainActivity.skipFirestoreForTesting = true;
        emailNotification.suppressEmailsForTesting = true;
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    @After
    public void tearDown() {
        MainActivity.skipFirestoreForTesting = false;
        emailNotification.suppressEmailsForTesting = false;
    }

    @Test
    public void hardcodedAdminLogin_launchesMainInAdminMode() {
        Intents.init();
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.emailInput))
                    .perform(replaceText(LoginActivity.ADMIN_EMAIL), closeSoftKeyboard());
            onView(withId(R.id.passwordInput))
                    .perform(replaceText(LoginActivity.ADMIN_PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.loginBtn)).perform(click());

            intended(hasComponent(MainActivity.class.getName()));
            intended(hasExtra(MainActivity.EXTRA_IS_ADMIN, true));
        } finally {
            Intents.release();
        }
    }

    @Test
    public void adminMain_showsAddEventButton() {
        ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean(LoginActivity.KEY_ADMIN_MODE, true)
                .apply();

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, true);

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.addEventBtn)).check(matches(isDisplayed()));
        }
    }

}
