package com.example.bookingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    public static final String ADMIN_EMAIL = "admin@test.com";
    public static final String ADMIN_PASSWORD = "123456";
    public static final String PREFS_NAME = "booking_app_session";
    public static final String KEY_ADMIN_MODE = "admin_mode";

    private FirebaseAuth mAuth;
    private TextInputEditText emailInput, passwordInput;
    private Button loginBtn;
    private TextView registerEmailLink, registerPhoneLink, statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        registerEmailLink = findViewById(R.id.registerEmailLink);
        registerPhoneLink = findViewById(R.id.registerPhoneLink);
        statusText = findViewById(R.id.statusText);

        loginBtn.setOnClickListener(v -> attemptLogin());

        registerEmailLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterEmailActivity.class)));

        registerPhoneLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterPhoneActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateAfterLogin(currentUser.getEmail());
        }
    }

    private void attemptLogin() {
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }

        if (isHardcodedAdmin(email, password)) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_ADMIN_MODE, true)
                    .apply();
            navigateToMain(true);
            return;
        }

        setLoading(true);
        statusText.setVisibility(View.GONE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        navigateAfterLogin(user != null ? user.getEmail() : email);
                    } else {
                        String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        statusText.setText("Login failed: " + msg);
                        statusText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private boolean isHardcodedAdmin(String email, String password) {
        return ADMIN_EMAIL.equalsIgnoreCase(email) && ADMIN_PASSWORD.equals(password);
    }

    private void navigateAfterLogin(String email) {
        if (email != null && ADMIN_EMAIL.equalsIgnoreCase(email)) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_ADMIN_MODE, true)
                    .apply();
            navigateToMain(true);
            return;
        }

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ADMIN_MODE, false)
                .apply();
        navigateToMain(false);
    }

    private void navigateToMain(boolean isAdmin) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_IS_ADMIN, isAdmin);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        loginBtn.setEnabled(!loading);
        loginBtn.setText(loading ? "Signing in…" : "Log In");
    }
}
