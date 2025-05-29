package com.example.peer2peer; // Ensure this matches your package

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.widget.Button;

import androidx.activity.OnBackPressedCallback; // Import OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class VerificationPendingActivity extends AppCompatActivity {

    private static final String TAG = "VerificationPending"; // Tag for logging

    private Button buttonLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_pending);

        mAuth = FirebaseAuth.getInstance();
        buttonLogout = findViewById(R.id.button_logout);

        buttonLogout.setOnClickListener(v -> performLogout());

        // --- Handle Back Press using OnBackPressedCallback ---
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Perform the same logout action when back is pressed
                Log.d(TAG, "Back pressed, performing logout.");
                performLogout();
                // Note: We DON'T call isEnabled = false or remove() here if we *always*
                // want the back press to trigger logout in this activity.
            }
        };
        // Add the callback to the dispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
        // --- End of Back Press Handling ---
    }

    private void performLogout() {
        Log.d(TAG, "Performing logout.");
        mAuth.signOut();
        // Navigate back to Login screen, clearing the task stack
        Intent intent = new Intent(VerificationPendingActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this activity
    }

    // REMOVED the overridden onBackPressed() method
    // @Override
    // public void onBackPressed() {
    //     // super.onBackPressed(); // Comment this out or leave empty to disable back press
    //     // Or logout and go to login
    //      buttonLogout.performClick();
    // }
}