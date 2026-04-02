package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

import java.lang.reflect.Method;

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

    @Test
    public void registerEmailLink_opensRegisterEmailActivity() {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();

        activity.findViewById(R.id.registerEmailLink).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(RegisterEmailActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void registerPhoneLink_opensRegisterPhoneActivity() {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();

        activity.findViewById(R.id.registerPhoneLink).performClick();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(RegisterPhoneActivity.class.getName(), next.getComponent().getClassName());
    }

    @Test
    public void navigateAfterLogin_nonAdminClearsAdminSessionAndNavigatesToMain() throws Exception {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
        SharedPreferences preferences = activity.getSharedPreferences(LoginActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        preferences.edit().putBoolean(LoginActivity.KEY_ADMIN_MODE, true).apply();

        invokePrivate(activity, "navigateAfterLogin", String.class, "user@test.com");

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(MainActivity.class.getName(), next.getComponent().getClassName());
        assertTrue(!next.getBooleanExtra(MainActivity.EXTRA_IS_ADMIN, true));
        assertTrue(!preferences.getBoolean(LoginActivity.KEY_ADMIN_MODE, false));
    }

    @Test
    public void setLoading_updatesButtonStateAndText() throws Exception {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
        Button loginButton = activity.findViewById(R.id.loginBtn);

        invokePrivate(activity, "setLoading", boolean.class, true);
        assertTrue(!loginButton.isEnabled());
        assertEquals("Signing in…", loginButton.getText().toString());

        invokePrivate(activity, "setLoading", boolean.class, false);
        assertTrue(loginButton.isEnabled());
        assertEquals("Log In", loginButton.getText().toString());
    }

    @Test
    public void navigateAfterLogin_adminEmailNavigatesInAdminMode() throws Exception {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();

        invokePrivate(activity, "navigateAfterLogin", String.class, LoginActivity.ADMIN_EMAIL);

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertTrue(next.getBooleanExtra(MainActivity.EXTRA_IS_ADMIN, false));
    }

    @Test
    public void onStart_withExistingAdminUserNavigatesToMain() throws Exception {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).create().get();
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getEmail()).thenReturn(LoginActivity.ADMIN_EMAIL);
        when(auth.getCurrentUser()).thenReturn(user);
        setField(activity, "mAuth", auth);

        activity.onStart();

        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(MainActivity.class.getName(), next.getComponent().getClassName());
        assertTrue(next.getBooleanExtra(MainActivity.EXTRA_IS_ADMIN, false));
    }

    private Object invokePrivate(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method.invoke(target, argument);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
