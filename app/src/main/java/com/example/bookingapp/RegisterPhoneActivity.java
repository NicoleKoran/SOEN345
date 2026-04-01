package com.example.bookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterPhoneActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText phoneInput, otpInput;
    private Button sendOtpBtn, verifyBtn;
    private TextView statusText, backToLoginLink;
    private View otpContainer;

    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_phone);

        mAuth = FirebaseAuth.getInstance();

        phoneInput = findViewById(R.id.phoneInput);
        otpInput = findViewById(R.id.otpInput);
        sendOtpBtn = findViewById(R.id.sendOtpBtn);
        verifyBtn = findViewById(R.id.verifyBtn);
        statusText = findViewById(R.id.statusText);
        backToLoginLink = findViewById(R.id.backToLoginLink);
        otpContainer = findViewById(R.id.otpContainer);

        otpContainer.setVisibility(View.GONE);
        verifyBtn.setVisibility(View.GONE);

        sendOtpBtn.setOnClickListener(v -> sendOtp());
        verifyBtn.setOnClickListener(v -> verifyCode());
        backToLoginLink.setOnClickListener(v -> finish());
    }

    private void sendOtp() {
        String phone = phoneInput.getText() != null ? phoneInput.getText().toString().trim() : "";
        if (phone.isEmpty()) {
            phoneInput.setError("Phone number is required");
            return;
        }

        setLoadingSend(true);
        statusText.setVisibility(View.GONE);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        setLoadingSend(false);
                        statusText.setText("Failed to send OTP: " + e.getMessage());
                        statusText.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        setLoadingSend(false);
                        mVerificationId = verificationId;
                        otpContainer.setVisibility(View.VISIBLE);
                        verifyBtn.setVisibility(View.VISIBLE);
                        sendOtpBtn.setText("Resend OTP");
                        statusText.setText("OTP sent to " + phone + ". Enter it below.");
                        statusText.setVisibility(View.VISIBLE);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode() {
        String code = otpInput.getText() != null ? otpInput.getText().toString().trim() : "";
        if (code.isEmpty() || mVerificationId == null) {
            otpInput.setError("Enter the OTP first");
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        verifyBtn.setEnabled(false);
        verifyBtn.setText("Verifying…");

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(RegisterPhoneActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        verifyBtn.setEnabled(true);
                        verifyBtn.setText("Verify OTP");
                        String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        statusText.setText("Verification failed: " + msg);
                        statusText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setLoadingSend(boolean loading) {
        sendOtpBtn.setEnabled(!loading);
        sendOtpBtn.setText(loading ? "Sending…" : "Send OTP");
    }
}
