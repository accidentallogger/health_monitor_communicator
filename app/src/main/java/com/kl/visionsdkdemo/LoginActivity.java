package com.kl.visionsdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.kl.visionsdkdemo.db.DatabaseHelper;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText userName, userPass;
    private Button submitBtn;
    private TextView registerNowText;
    private DatabaseHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database helper and session manager
        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        // Check if user is already logged in
        if (session.isLoggedIn()) {
            redirectToMainActivity();
            return;
        }

        // Initialize views
        submitBtn = findViewById(R.id.LoginBtn);
        userName = findViewById(R.id.Username);
        userPass = findViewById(R.id.Password);
        registerNowText = findViewById(R.id.registerNowText);

        submitBtn.setOnClickListener(view -> {
            String phone = userName.getText().toString().trim();
            String password = userPass.getText().toString().trim();

            if (validateInputs(phone, password)) {
                authenticateUser(phone, password);
            }
        });

        registerNowText.setOnClickListener(view -> {
            navigateToSignUp();
        });
    }

    private void authenticateUser(String phone, String password) {
        if (dbHelper.checkUser(phone, password)) {
            // Create login session
            session.createLoginSession(phone);

            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            redirectToMainActivity();
        } else {
            Toast.makeText(this,
                    "Invalid phone number or password", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs(String phone, String password) {
        boolean isValid = true;

        if (phone.isEmpty()) {
            userName.setError("Phone number cannot be empty");
            isValid = false;
        } else if (phone.length() != 10) {
            userName.setError("Invalid phone number");
            isValid = false;
        } else {
            userName.setError(null);
        }

        if (password.isEmpty()) {
            userPass.setError("Password cannot be empty");
            isValid = false;
        } else {
            userPass.setError(null);
        }

        return isValid;
    }

    private void redirectToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}