package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class LoginActivityTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
        SharedPreferences preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

    @Test
    public void attemptLogin_emptyEmail_setsError() {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
        activity.findViewById(R.id.loginBtn).performClick();

        assertEquals(
                "Email is required",
                ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                        .getError()
                        .toString());
    }

    @Test
    public void attemptLogin_emptyPassword_setsError() {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
        ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                .setText("a@b.com");
        activity.findViewById(R.id.loginBtn).performClick();

        assertEquals(
                "Password is required",
                ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.passwordInput))
                        .getError()
                        .toString());
    }

    @Test
    public void attemptLogin_hardcodedAdmin_navigatesToMainInAdminMode() {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
        ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                .setText(LoginActivity.ADMIN_EMAIL);
        ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.passwordInput))
                .setText(LoginActivity.ADMIN_PASSWORD);
        activity.findViewById(R.id.loginBtn).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(MainActivity.class.getName(), next.getComponent().getClassName());
        assertTrue(next.getBooleanExtra(MainActivity.EXTRA_IS_ADMIN, false));

        SharedPreferences preferences = activity.getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        assertTrue(preferences.getBoolean(LoginActivity.KEY_ADMIN_MODE, false));
    }
}
