package com.example.peer2peer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; 
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class VerificationPendingActivity extends AppCompatActivity {

    private static final String TAG = "VerificationPending"; 

    private Button buttonLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_pending);

        mAuth = FirebaseAuth.getInstance();
        buttonLogout = findViewById(R.id.button_logout);

        buttonLogout.setOnClickListener(v -> performLogout());

        
        OnBackPressedCallback callback = new OnBackPressedCallback(true ) {
            @Override
            public void handleOnBackPressed() {
                
                Log.d(TAG, "Back pressed, performing logout.");
                performLogout();
                
            }
        };
      
        getOnBackPressedDispatcher().addCallback(this, callback);
      
    }

    private void performLogout() {
        Log.d(TAG, "Performing logout.");
        mAuth.signOut();
        
        Intent intent = new Intent(VerificationPendingActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

   
}