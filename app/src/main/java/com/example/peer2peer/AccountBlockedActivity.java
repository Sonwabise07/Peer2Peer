package com.example.peer2peer; 

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View; 

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
            
            Intent intent = new Intent(AccountBlockedActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); 
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
       
        Intent intent = new Intent(AccountBlockedActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}