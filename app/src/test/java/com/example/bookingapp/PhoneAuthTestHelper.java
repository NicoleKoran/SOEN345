package com.example.bookingapp;

import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.lang.reflect.Field;

/**
 * Extracts Firebase phone verification callbacks from {@link PhoneAuthOptions} for unit tests.
 */
final class PhoneAuthTestHelper {

    private PhoneAuthTestHelper() {}

    static PhoneAuthProvider.OnVerificationStateChangedCallbacks getCallbacks(PhoneAuthOptions options)
            throws Exception {
        Class<?> c = options.getClass();
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (PhoneAuthProvider.OnVerificationStateChangedCallbacks.class.isAssignableFrom(
                        field.getType())) {
                    field.setAccessible(true);
                    return (PhoneAuthProvider.OnVerificationStateChangedCallbacks) field.get(options);
                }
            }
            c = c.getSuperclass();
        }
        throw new AssertionError("OnVerificationStateChangedCallbacks not found on PhoneAuthOptions");
    }

    static void deliverCodeSent(PhoneAuthOptions options, String verificationId) throws Exception {
        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks = getCallbacks(options);
        callbacks.onCodeSent(verificationId, org.mockito.Mockito.mock(PhoneAuthProvider.ForceResendingToken.class));
    }

    static void deliverVerificationFailed(PhoneAuthOptions options, com.google.firebase.FirebaseException e)
            throws Exception {
        getCallbacks(options).onVerificationFailed(e);
    }

    static void deliverVerificationCompleted(PhoneAuthOptions options, PhoneAuthCredential credential)
            throws Exception {
        getCallbacks(options).onVerificationCompleted(credential);
    }
}
