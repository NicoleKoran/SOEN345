package com.example.bookingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterEmailActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerBtn;
    private TextView statusText, backToLoginLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_email);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerBtn = findViewById(R.id.registerBtn);
        statusText = findViewById(R.id.statusText);
        backToLoginLink = findViewById(R.id.backToLoginLink);

        registerBtn.setOnClickListener(v -> attemptRegister());
        backToLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String confirm = confirmPasswordInput.getText() != null ? confirmPasswordInput.getText().toString().trim() : "";

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirm)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        setLoading(true);
        statusText.setVisibility(View.GONE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        if (mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().sendEmailVerification();
                        }
                        statusText.setText("Account created! A confirmation email has been sent to " + email + ". Please verify before logging in.");
                        statusText.setVisibility(View.VISIBLE);
                        registerBtn.setEnabled(false);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Intent intent = new Intent(RegisterEmailActivity.this, HomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }, 2500);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        statusText.setText("Registration failed: " + msg);
                        statusText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setLoading(boolean loading) {
        registerBtn.setEnabled(!loading);
        registerBtn.setText(loading ? "Creating account…" : "Create Account");
    }
}
