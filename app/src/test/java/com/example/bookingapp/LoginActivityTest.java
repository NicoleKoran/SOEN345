package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class LoginActivityTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void attemptLogin_emptyEmail_setsError() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            activity.findViewById(R.id.loginBtn).performClick();
            assertEquals(
                    "Email is required",
                    ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.emailInput))
                            .getError()
                            .toString());
        }
    }

    @Test
    public void attemptLogin_emptyPassword_setsError() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                    .setText("a@b.com");
            activity.findViewById(R.id.loginBtn).performClick();
            assertEquals(
                    "Password is required",
                    ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.passwordInput))
                            .getError()
                            .toString());
        }
    }

    @Test
    public void attemptLogin_failure_showsStatus() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.signInWithEmailAndPassword(eq("a@b.com"), eq("secret")))
                    .thenReturn(Tasks.forException(new Exception("invalid")));
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                    .setText("a@b.com");
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.passwordInput))
                    .setText("secret");
            activity.findViewById(R.id.loginBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            TextView status = activity.findViewById(R.id.statusText);
            assertTrue(status.getText().toString().contains("Login failed"));
            assertEquals(android.view.View.VISIBLE, status.getVisibility());
        }
    }

    @Test
    public void attemptLogin_success_navigatesHome() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            AuthResult authResult = mock(AuthResult.class);
            when(mockAuth.signInWithEmailAndPassword(eq("a@b.com"), eq("secret")))
                    .thenReturn(Tasks.forResult(authResult));
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                    .setText("a@b.com");
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.passwordInput))
                    .setText("secret");
            activity.findViewById(R.id.loginBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            Intent next = ShadowApplication.getInstance().getNextStartedActivity();
            assertNotNull(next);
            assertEquals(HomeActivity.class.getName(), next.getComponent().getClassName());
        }
    }

    @Test
    public void onStart_loggedInUser_navigatesHome() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseUser user = mock(FirebaseUser.class);
            when(mockAuth.getCurrentUser()).thenReturn(user);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            ActivityController<LoginActivity> controller = Robolectric.buildActivity(LoginActivity.class).create();
            controller.start();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            Intent next = ShadowApplication.getInstance().getNextStartedActivity();
            assertNotNull(next);
            assertEquals(HomeActivity.class.getName(), next.getComponent().getClassName());
        }
    }

    @Test
    public void registerEmailLink_startsRegisterEmail() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.getCurrentUser()).thenReturn(null);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            activity.findViewById(R.id.registerEmailLink).performClick();
            Intent next = ShadowApplication.getInstance().getNextStartedActivity();
            assertEquals(RegisterEmailActivity.class.getName(), next.getComponent().getClassName());
        }
    }

    @Test
    public void registerPhoneLink_startsRegisterPhone() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.getCurrentUser()).thenReturn(null);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();
            activity.findViewById(R.id.registerPhoneLink).performClick();
            Intent next = ShadowApplication.getInstance().getNextStartedActivity();
            assertEquals(RegisterPhoneActivity.class.getName(), next.getComponent().getClassName());
        }
    }
}
