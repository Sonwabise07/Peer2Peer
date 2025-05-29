package com.example.peer2peer; // Ensure this matches your package

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View; // Import View

import androidx.appcompat.app.AppCompatActivity;

public class AccountBlockedActivity extends AppCompatActivity {

    private TextView textViewUserEmailForAppeal;
    private Button buttonReturnToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_blocked);

        textViewUserEmailForAppeal = findViewById(R.id.textViewUserEmailForAppeal);
        buttonReturnToLogin = findViewById(R.id.buttonReturnToLogin);

        String userEmail = getIntent().getStringExtra("userEmailForAppeal");
        if (userEmail != null && !userEmail.isEmpty()) {
            textViewUserEmailForAppeal.setText("Account: " + userEmail);
            textViewUserEmailForAppeal.setVisibility(View.VISIBLE);
        } else {
            textViewUserEmailForAppeal.setVisibility(View.GONE);
        }

        buttonReturnToLogin.setOnClickListener(v -> {
            // Clear task and start LoginActivity fresh
            Intent intent = new Intent(AccountBlockedActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish this activity
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Ensure pressing back also leads to a clean login state
        Intent intent = new Intent(AccountBlockedActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}