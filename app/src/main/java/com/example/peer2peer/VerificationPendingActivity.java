package com.example.peer2peer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; 
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerificationPendingActivity extends AppCompatActivity {

    private static final String TAG = "VerificationPending"; 
    private static final String LOGOUT_CONFIRMATION_TITLE = "Logout Confirmation";
    private static final String LOGOUT_CONFIRMATION_MESSAGE = "Are you sure you want to logout? You'll need to verify your email again.";
    private static final String LOGOUT_SUCCESS_MESSAGE = "Logged out successfully";

    private Button buttonLogout;
    private FirebaseAuth mAuth;
    private OnBackPressedCallback backPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_pending);

        initializeFirebase();
        initializeViews();
        setupBackPressedCallback();
        
        // Check if user is already verified
        checkUserVerificationStatus();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        buttonLogout = findViewById(R.id.button_logout);
        
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> showLogoutConfirmation());
        } else {
            Log.e(TAG, "Logout button not found in layout");
        }
    }

   
    private void setupBackPressedCallback() {
        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d(TAG, "Back pressed, showing logout confirmation.");
                showLogoutConfirmation();
            }
        };
        
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    
    private void checkUserVerificationStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (currentUser.isEmailVerified()) {
                        Log.d(TAG, "User email is now verified, redirecting to main activity");
                        redirectToMainActivity();
                    }
                } else {
                    Log.e(TAG, "Failed to reload user data", task.getException());
                }
            });
        }
    }


    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(LOGOUT_CONFIRMATION_TITLE)
                .setMessage(LOGOUT_CONFIRMATION_MESSAGE)
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void performLogout() {
        Log.d(TAG, "Performing logout.");
        
        try {
            mAuth.signOut();
            Toast.makeText(this, LOGOUT_SUCCESS_MESSAGE, Toast.LENGTH_SHORT).show();
            redirectToLogin();
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            Toast.makeText(this, "Error during logout. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(VerificationPendingActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

   
    private void redirectToMainActivity() {
        Intent intent = new Intent(VerificationPendingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
       
        checkUserVerificationStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       
        if (backPressedCallback != null) {
            backPressedCallback.remove();
        }
    }
}