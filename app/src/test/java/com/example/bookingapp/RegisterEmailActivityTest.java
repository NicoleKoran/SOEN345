package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.os.Looper;
import android.widget.Button;
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
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class RegisterEmailActivityTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void attemptRegister_emptyEmail_setsError() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            RegisterEmailActivity activity = Robolectric.buildActivity(RegisterEmailActivity.class).setup().get();
            activity.findViewById(R.id.registerBtn).performClick();
            assertEquals(
                    "Email is required",
                    ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.emailInput))
                            .getError()
                            .toString());
        }
    }

    @Test
    public void attemptRegister_shortPassword_setsError() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            RegisterEmailActivity activity = Robolectric.buildActivity(RegisterEmailActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                    .setText("a@b.com");
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.passwordInput))
                    .setText("12345");
            ((com.google.android.material.textfield.TextInputEditText)
                            activity.findViewById(R.id.confirmPasswordInput))
                    .setText("12345");
            activity.findViewById(R.id.registerBtn).performClick();
            assertEquals(
                    "Password must be at least 6 characters",
                    ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.passwordInput))
                            .getError()
                            .toString());
        }
    }

    @Test
    public void attemptRegister_passwordMismatch_setsError() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            RegisterEmailActivity activity = Robolectric.buildActivity(RegisterEmailActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                    .setText("a@b.com");
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.passwordInput))
                    .setText("secret1");
            ((com.google.android.material.textfield.TextInputEditText)
                            activity.findViewById(R.id.confirmPasswordInput))
                    .setText("secret2");
            activity.findViewById(R.id.registerBtn).performClick();
            assertEquals(
                    "Passwords do not match",
                    ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.confirmPasswordInput))
                            .getError()
                            .toString());
        }
    }

    @Test
    public void attemptRegister_failure_showsStatus() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString()))
                    .thenReturn(Tasks.forException(new Exception("email in use")));
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            RegisterEmailActivity activity = Robolectric.buildActivity(RegisterEmailActivity.class).setup().get();
            fillValidRegistration(activity);
            activity.findViewById(R.id.registerBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            TextView status = activity.findViewById(R.id.statusText);
            assertTrue(status.getText().toString().contains("Registration failed"));
            assertEquals(android.view.View.VISIBLE, status.getVisibility());
        }
    }

    @Test
    public void attemptRegister_success_showsMessageAndNavigatesHome() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseUser mockUser = mock(FirebaseUser.class);
            AuthResult authResult = mock(AuthResult.class);
            when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString()))
                    .thenReturn(Tasks.forResult(authResult));
            when(mockAuth.getCurrentUser()).thenReturn(mockUser);
            when(mockUser.sendEmailVerification()).thenReturn(Tasks.forResult(null));
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            RegisterEmailActivity activity = Robolectric.buildActivity(RegisterEmailActivity.class).setup().get();
            fillValidRegistration(activity);
            activity.findViewById(R.id.registerBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            TextView status = activity.findViewById(R.id.statusText);
            assertTrue(status.getText().toString().contains("Account created"));
            assertFalse(((Button) activity.findViewById(R.id.registerBtn)).isEnabled());

            ShadowLooper shadowLooper = Shadows.shadowOf(Looper.getMainLooper());
            shadowLooper.idleFor(3, TimeUnit.SECONDS);

            Intent next = ShadowApplication.getInstance().getNextStartedActivity();
            assertNotNull(next);
            assertEquals(HomeActivity.class.getName(), next.getComponent().getClassName());
        }
    }

    @Test
    public void backToLogin_finishesActivity() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            RegisterEmailActivity activity = Robolectric.buildActivity(RegisterEmailActivity.class).setup().get();
            activity.findViewById(R.id.backToLoginLink).performClick();
            assertTrue(activity.isFinishing());
        }
    }

    private static void fillValidRegistration(RegisterEmailActivity activity) {
        ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.emailInput))
                .setText("new@user.com");
        ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.passwordInput))
                .setText("secret12");
        ((com.google.android.material.textfield.TextInputEditText)
                        activity.findViewById(R.id.confirmPasswordInput))
                .setText("secret12");
    }
}
