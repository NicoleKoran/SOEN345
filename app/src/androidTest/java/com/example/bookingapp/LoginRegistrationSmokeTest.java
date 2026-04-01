package com.example.bookingapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Smoke tests for login and registration screens: UI visibility, light validation, and navigation.
 * Does not perform real sign-in or account creation.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginRegistrationSmokeTest {

    @Before
    public void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    private static Matcher<View> withErrorText(String expected) {
        return new BoundedMatcher<View, TextInputEditText>(TextInputEditText.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with error text: " + expected);
            }

            @Override
            public boolean matchesSafely(TextInputEditText view) {
                CharSequence err = view.getError();
                return err != null && expected.contentEquals(err.toString());
            }
        };
    }

    @Test
    public void loginScreen_showsPrimaryFields() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
            onView(withId(R.id.passwordInput)).check(matches(isDisplayed()));
            onView(withId(R.id.loginBtn)).check(matches(isDisplayed()));
            onView(withId(R.id.registerEmailLink)).check(matches(isDisplayed()));
            onView(withId(R.id.registerPhoneLink)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void loginScreen_emptySubmit_showsEmailError() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.loginBtn)).perform(click());
            onView(withId(R.id.emailInput)).check(matches(withErrorText("Email is required")));
        }
    }

    @Test
    public void registerEmailScreen_showsPrimaryFields() {
        try (ActivityScenario<RegisterEmailActivity> ignored =
                ActivityScenario.launch(RegisterEmailActivity.class)) {
            onView(withId(R.id.emailInput)).check(matches(isDisplayed()));
            onView(withId(R.id.passwordInput)).check(matches(isDisplayed()));
            onView(withId(R.id.confirmPasswordInput)).check(matches(isDisplayed()));
            onView(withId(R.id.registerBtn)).check(matches(isDisplayed()));
            onView(withId(R.id.backToLoginLink)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void registerEmailScreen_emptySubmit_showsEmailError() {
        try (ActivityScenario<RegisterEmailActivity> ignored =
                ActivityScenario.launch(RegisterEmailActivity.class)) {
            onView(withId(R.id.registerBtn)).perform(click());
            onView(withId(R.id.emailInput)).check(matches(withErrorText("Email is required")));
        }
    }

    @Test
    public void registerPhoneScreen_showsPrimaryFields() {
        try (ActivityScenario<RegisterPhoneActivity> ignored =
                ActivityScenario.launch(RegisterPhoneActivity.class)) {
            onView(withId(R.id.phoneInput)).check(matches(isDisplayed()));
            onView(withId(R.id.sendOtpBtn)).check(matches(isDisplayed()));
            onView(withId(R.id.backToLoginLink)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void registerPhoneScreen_emptySubmit_showsPhoneError() {
        try (ActivityScenario<RegisterPhoneActivity> ignored =
                ActivityScenario.launch(RegisterPhoneActivity.class)) {
            onView(withId(R.id.sendOtpBtn)).perform(click());
            onView(withId(R.id.phoneInput)).check(matches(withErrorText("Phone number is required")));
        }
    }

    @Test
    public void login_navigateToRegisterEmail_showsRegisterForm() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.registerEmailLink)).perform(click());
            onView(withId(R.id.registerBtn)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void login_navigateToRegisterPhone_showsPhoneForm() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.registerPhoneLink)).perform(click());
            onView(withId(R.id.phoneInput)).check(matches(isDisplayed()));
            onView(withId(R.id.sendOtpBtn)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void login_navigateToRegisterEmail_back_returnsToLogin() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.registerEmailLink)).perform(click());
            onView(withId(R.id.registerBtn)).check(matches(isDisplayed()));
            pressBack();
            onView(withId(R.id.loginBtn)).check(matches(isDisplayed()));
        }
    }
}
