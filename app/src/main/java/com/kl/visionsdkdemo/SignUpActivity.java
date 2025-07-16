package com.kl.visionsdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.kl.visionsdkdemo.db.DatabaseHelper;

public class SignUpActivity extends AppCompatActivity {
    private TextInputEditText userPass, userConfirmPass, userPhone;
    private Button submitBtn;
    private TextView loginNowText;
    private DatabaseHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize database helper and session manager
        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        // Check if user is already logged in
        if (session.isLoggedIn()) {
            redirectToMainActivity();
            return;
        }

        // Initialize views
        submitBtn = findViewById(R.id.SignUpBtn);
        userPass = findViewById(R.id.Password);
        userConfirmPass = findViewById(R.id.ConfirmPassword);
        userPhone = findViewById(R.id.Phone);
        loginNowText = findViewById(R.id.loginNowText);

        submitBtn.setOnClickListener(view -> {
            String password = userPass.getText().toString().trim();
            String confirmPassword = userConfirmPass.getText().toString().trim();
            String phone = userPhone.getText().toString().trim();

            if (validateInputs(phone, password, confirmPassword)) {
                registerUser(phone, password);
            }
        });

        loginNowText.setOnClickListener(view -> {
            navigateToLogin();
        });
    }

    private void registerUser(String phone, String password) {
        // Check if phone already exists
        if (dbHelper.checkUserExists(phone)) {
            userPhone.setError("Phone number already registered");
            return;
        }

        // Add user to database (password should be hashed in production)
        dbHelper.addUser(phone, password);

        // Create login session
        session.createLoginSession(phone);

        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
        redirectToMainActivity();
    }

    private boolean validateInputs(String phone, String password, String confirmPassword) {
        boolean isValid = true;

        if (phone.isEmpty()) {
            userPhone.setError("Phone cannot be empty");
            isValid = false;
        } else if (phone.length() != 10) {
            userPhone.setError("Invalid phone number");
            isValid = false;
        } else {
            userPhone.setError(null);
        }

        if (password.isEmpty()) {
            userPass.setError("Password cannot be empty");
            isValid = false;
        } else if (password.length() < 6) {
            userPass.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            userPass.setError(null);
        }

        if (!confirmPassword.equals(password)) {
            userConfirmPass.setError("Passwords don't match");
            isValid = false;
        } else {
            userConfirmPass.setError(null);
        }

        return isValid;
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}