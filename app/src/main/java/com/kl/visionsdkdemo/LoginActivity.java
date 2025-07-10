package com.kl.visionsdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {
    TextInputEditText userName, userPass;
    Button submitBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        submitBtn = findViewById(R.id.LoginBtn);
        userName = findViewById(R.id.Username);
        userPass = findViewById(R.id.Password);

        submitBtn.setOnClickListener(View -> {

            String user_name = userName.getText().toString().trim();
            String user_password = userPass.getText().toString().trim();

            if (validateInputs(user_name, user_password)) {

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                Toast.makeText(getApplicationContext(), "Login Successfully!", Toast.LENGTH_SHORT).show();

            }

        });
    }

    private boolean validateInputs(String username, String password) {
        boolean isValid = true;

        if (username.isEmpty()) {
            userName.setError("Username cannot be empty");
            isValid = false;
        } else if (username.length() != 10) {
            userName.setError("Invalid mobile no");
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
}