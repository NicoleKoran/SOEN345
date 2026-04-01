package com.example.bookingapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class RegisterPhoneActivityTest {

    private static org.mockito.MockedStatic<PhoneAuthProvider> mockPhoneAuthProviderStatic() {
        return Mockito.mockStatic(
                PhoneAuthProvider.class,
                Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    }

    @Before
    public void setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void sendOtp_emptyPhone_setsError() {
        try (var auth = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            activity.findViewById(R.id.sendOtpBtn).performClick();
            assertEquals(
                    "Phone number is required",
                    ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.phoneInput))
                            .getError()
                            .toString());
        }
    }

    @Test
    public void sendOtp_codeSent_showsOtpUi() throws Exception {
        try (var auth = mockStatic(FirebaseAuth.class);
                var phone = mockPhoneAuthProviderStatic()) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            phone.when(() -> PhoneAuthProvider.verifyPhoneNumber(any(PhoneAuthOptions.class)))
                    .thenAnswer(
                            invocation -> {
                                PhoneAuthOptions opts = invocation.getArgument(0);
                                PhoneAuthTestHelper.deliverCodeSent(opts, "test-verification-id");
                                return null;
                            });

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.phoneInput))
                    .setText("+15551234567");
            activity.findViewById(R.id.sendOtpBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            assertEquals(View.VISIBLE, activity.findViewById(R.id.otpContainer).getVisibility());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.verifyBtn).getVisibility());
            TextView status = activity.findViewById(R.id.statusText);
            assertTrue(status.getText().toString().contains("OTP sent"));
        }
    }

    @Test
    public void sendOtp_verificationFailed_showsStatus() throws Exception {
        try (var auth = mockStatic(FirebaseAuth.class);
                var phone = mockPhoneAuthProviderStatic()) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            phone.when(() -> PhoneAuthProvider.verifyPhoneNumber(any(PhoneAuthOptions.class)))
                    .thenAnswer(
                            invocation -> {
                                PhoneAuthOptions opts = invocation.getArgument(0);
                                PhoneAuthTestHelper.deliverVerificationFailed(
                                        opts, new FirebaseException("network error"));
                                return null;
                            });

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.phoneInput))
                    .setText("+15551234567");
            activity.findViewById(R.id.sendOtpBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            TextView status = activity.findViewById(R.id.statusText);
            assertTrue(status.getText().toString().contains("Failed to send OTP"));
            assertEquals(View.VISIBLE, status.getVisibility());
        }
    }

    @Test
    public void verifyCode_emptyOtp_setsError() throws Exception {
        try (var auth = mockStatic(FirebaseAuth.class);
                var phone = mockPhoneAuthProviderStatic()) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            phone.when(() -> PhoneAuthProvider.verifyPhoneNumber(any(PhoneAuthOptions.class)))
                    .thenAnswer(
                            invocation -> {
                                PhoneAuthTestHelper.deliverCodeSent(
                                        invocation.getArgument(0), "test-verification-id");
                                return null;
                            });

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.phoneInput))
                    .setText("+15551234567");
            activity.findViewById(R.id.sendOtpBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            activity.findViewById(R.id.verifyBtn).performClick();
            assertEquals(
                    "Enter the OTP first",
                    ((com.google.android.material.textfield.TextInputEditText)
                                    activity.findViewById(R.id.otpInput))
                            .getError()
                            .toString());
        }
    }

    @Test
    public void verifyCode_signInFailure_restoresButtonAndShowsStatus() throws Exception {
        try (var auth = mockStatic(FirebaseAuth.class);
                var phone = mockPhoneAuthProviderStatic()) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            when(mockAuth.signInWithCredential(any(PhoneAuthCredential.class)))
                    .thenReturn(Tasks.forException(new Exception("invalid code")));
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            phone.when(() -> PhoneAuthProvider.verifyPhoneNumber(any(PhoneAuthOptions.class)))
                    .thenAnswer(
                            invocation -> {
                                PhoneAuthTestHelper.deliverCodeSent(
                                        invocation.getArgument(0), "test-verification-id");
                                return null;
                            });

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.phoneInput))
                    .setText("+15551234567");
            activity.findViewById(R.id.sendOtpBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.otpInput))
                    .setText("123456");
            Button verifyBtn = activity.findViewById(R.id.verifyBtn);
            verifyBtn.performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            assertTrue(verifyBtn.isEnabled());
            assertEquals("Verify OTP", verifyBtn.getText().toString());
            assertTrue(
                    ((TextView) activity.findViewById(R.id.statusText))
                            .getText()
                            .toString()
                            .contains("Verification failed"));
        }
    }

    @Test
    public void verifyCode_signInSuccess_navigatesHome() throws Exception {
        try (var auth = mockStatic(FirebaseAuth.class);
                var phone = mockPhoneAuthProviderStatic()) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            AuthResult authResult = mock(AuthResult.class);
            when(mockAuth.signInWithCredential(any(PhoneAuthCredential.class)))
                    .thenReturn(Tasks.forResult(authResult));
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            phone.when(() -> PhoneAuthProvider.verifyPhoneNumber(any(PhoneAuthOptions.class)))
                    .thenAnswer(
                            invocation -> {
                                PhoneAuthTestHelper.deliverCodeSent(
                                        invocation.getArgument(0), "test-verification-id");
                                return null;
                            });

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.phoneInput))
                    .setText("+15551234567");
            activity.findViewById(R.id.sendOtpBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.otpInput))
                    .setText("123456");
            activity.findViewById(R.id.verifyBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

            Intent next = ShadowApplication.getInstance().getNextStartedActivity();
            assertNotNull(next);
            assertEquals(HomeActivity.class.getName(), next.getComponent().getClassName());
        }
    }

    @Test
    public void sendOtp_autoVerification_triggersSignIn() throws Exception {
        try (var auth = mockStatic(FirebaseAuth.class);
                var phone = mockPhoneAuthProviderStatic()) {
            FirebaseAuth mockAuth = mock(FirebaseAuth.class);
            AuthResult authResult = mock(AuthResult.class);
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential("test-verification-id", "123456");
            when(mockAuth.signInWithCredential(any(PhoneAuthCredential.class)))
                    .thenReturn(Tasks.forResult(authResult));
            auth.when(FirebaseAuth::getInstance).thenReturn(mockAuth);
            phone.when(() -> PhoneAuthProvider.verifyPhoneNumber(any(PhoneAuthOptions.class)))
                    .thenAnswer(
                            invocation -> {
                                PhoneAuthOptions opts = invocation.getArgument(0);
                                PhoneAuthTestHelper.deliverVerificationCompleted(opts, credential);
                                return null;
                            });

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            ((com.google.android.material.textfield.TextInputEditText) activity.findViewById(R.id.phoneInput))
                    .setText("+15551234567");
            activity.findViewById(R.id.sendOtpBtn).performClick();
            Shadows.shadowOf(Looper.getMainLooper()).idle();

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

            RegisterPhoneActivity activity = Robolectric.buildActivity(RegisterPhoneActivity.class).setup().get();
            activity.findViewById(R.id.backToLoginLink).performClick();
            assertTrue(activity.isFinishing());
        }
    }
}
