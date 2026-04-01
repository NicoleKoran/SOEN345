package com.example.bookingapp;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.widget.TextView;

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

@RunWith(RobolectricTestRunner.class)
public class HomeActivityTest {

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void welcome_showsEmailWhenPresent() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseUser user = mock(FirebaseUser.class);
            when(user.getEmail()).thenReturn("user@test.com");
            when(mockAuth.getCurrentUser()).thenReturn(user);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            HomeActivity activity = Robolectric.buildActivity(HomeActivity.class).setup().get();
            TextView welcome = activity.findViewById(R.id.welcomeText);
            assertTrue(welcome.getText().toString().contains("user@test.com"));
        }
    }

    @Test
    public void welcome_showsPhoneWhenEmailNull() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            FirebaseUser user = mock(FirebaseUser.class);
            when(user.getEmail()).thenReturn(null);
            when(user.getPhoneNumber()).thenReturn("+15551234567");
            when(mockAuth.getCurrentUser()).thenReturn(user);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            HomeActivity activity = Robolectric.buildActivity(HomeActivity.class).setup().get();
            assertTrue(
                    ((TextView) activity.findViewById(R.id.welcomeText))
                            .getText()
                            .toString()
                            .contains("+15551234567"));
        }
    }

    @Test
    public void logout_signsOutAndOpensLogin() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.getCurrentUser()).thenReturn(null);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            HomeActivity activity = Robolectric.buildActivity(HomeActivity.class).setup().get();
            activity.findViewById(R.id.logoutBtn).performClick();

            verify(mockAuth).signOut();
            ShadowApplication shadowApp = ShadowApplication.getInstance();
            assertTrue(
                    shadowApp.getNextStartedActivity().getComponent().getClassName().contains("LoginActivity"));
        }
    }
}
