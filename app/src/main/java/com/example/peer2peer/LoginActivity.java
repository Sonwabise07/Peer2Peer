package com.example.peer2peer; // Ensure this matches your package

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

// Firebase and Task imports
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue; // <-- ADDED IMPORT
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // <-- ADDED IMPORT
import com.google.firebase.messaging.FirebaseMessaging; // <-- ADDED IMPORT

import java.util.HashMap; // <-- ADDED IMPORT
import java.util.Map;    // <-- ADDED IMPORT

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI Elements
    private Toolbar toolbar;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewForgotPassword, textViewSignUpLink;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar_login);
        editTextEmail = findViewById(R.id.edit_text_login_email);
        editTextPassword = findViewById(R.id.edit_text_login_password);
        buttonLogin = findViewById(R.id.button_login);
        textViewForgotPassword = findViewById(R.id.text_forgot_password);
        textViewSignUpLink = findViewById(R.id.text_signup_link);
        progressBar = findViewById(R.id.login_progress_bar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivity.EXTRA_USER_ROLE)) {
            userRole = intent.getStringExtra(MainActivity.EXTRA_USER_ROLE);
            Log.d(TAG, "Received role from MainActivity: " + userRole);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(userRole + " Login");
            }
        } else {
            Log.e(TAG, "No role passed to LoginActivity. Finishing.");
            Toast.makeText(this, "Error: Role not specified.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        buttonLogin.setOnClickListener(v -> performLogin());

        textViewSignUpLink.setOnClickListener(v -> {
            Intent signupIntent = new Intent(LoginActivity.this, SignupActivity.class);
            signupIntent.putExtra(MainActivity.EXTRA_USER_ROLE, userRole); // Pass the role
            startActivity(signupIntent);
        });

        textViewForgotPassword.setOnClickListener(v -> {
            // Implement forgot password functionality
            Toast.makeText(LoginActivity.this, "Forgot Password Clicked (Not Implemented).", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate back to MainActivity, clear other activities on top of it.
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Finish LoginActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogin() {
        String email = String.valueOf(editTextEmail.getText()).trim();
        String password = String.valueOf(editTextPassword.getText()).trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Valid email required."); editTextEmail.requestFocus(); return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password required."); editTextPassword.requestFocus(); return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);
        Log.d(TAG, "Attempting Firebase sign in for: " + email + " as role: " + userRole);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Log.d(TAG, "signInWithEmail:success (Verified). User ID: " + user.getUid());
                            fetchUserDataAndNavigate(user.getUid()); // This will now also handle token
                        } else if (user != null && !user.isEmailVerified()) {
                            Log.w(TAG, "signInWithEmail:success (Email Not Verified)");
                            Toast.makeText(LoginActivity.this, "Please verify your email address.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            progressBar.setVisibility(View.GONE);
                            buttonLogin.setEnabled(true);
                        } else { // Should not happen if task is successful and user is verified.
                            Log.e(TAG, "signInWithEmail:success but FirebaseUser is null after verification check");
                            Toast.makeText(LoginActivity.this, "Login failed: Could not retrieve user details.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            buttonLogin.setEnabled(true);
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        String errorMessage = "Authentication failed.";
                        if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                            errorMessage = "No account found with this email.";
                        } else if (task.getException() instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Incorrect password.";
                        } else if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                        buttonLogin.setEnabled(true);
                    }
                });
    }

    private void fetchUserDataAndNavigate(String userId) {
        // progressBar is typically already visible from performLogin
        // progressBar.setVisibility(View.VISIBLE);

        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.get().addOnCompleteListener(task -> {
            // Don't hide progress bar or enable button yet, wait for token logic

            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String firestoreRole = document.getString("role");
                    String status = document.getString("profileStatus"); // Assuming you have this field
                    boolean profileComplete = Boolean.TRUE.equals(document.getBoolean("profileComplete")); // Assuming this field

                    Log.d(TAG, "User data fetched: Firestore Role=" + firestoreRole + ", Expected Role=" + userRole);

                    if (!userRole.equals(firestoreRole)) {
                        Log.e(TAG, "Role mismatch! Expected: " + userRole + ", Firestore has: " + firestoreRole);
                        Toast.makeText(this, "Login failed: Role selection does not match account type.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        progressBar.setVisibility(View.GONE);
                        buttonLogin.setEnabled(true);
                        return;
                    }

                    // --- START: Get and Update FCM Token ---
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(tokenTask -> {
                                progressBar.setVisibility(View.GONE); // Hide progress bar after all async ops
                                buttonLogin.setEnabled(true); // Re-enable login button here

                                if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                                    String fcmToken = tokenTask.getResult();
                                    Log.d(TAG, "FCM Token fetched successfully: " + fcmToken);
                                    updateFcmTokenInFirestore(userId, fcmToken); // Save/Update token
                                } else {
                                    Log.w(TAG, "Fetching FCM registration token failed or token was null", tokenTask.getException());
                                    Toast.makeText(LoginActivity.this, "Could not update notification service settings. Continuing login.", Toast.LENGTH_SHORT).show();
                                }
                                // Navigate after attempting token update
                                navigateToDashboard(firestoreRole, status, profileComplete);
                            });
                    // --- END: Get and Update FCM Token ---

                } else {
                    Log.e(TAG, "No user document found in Firestore for userId: " + userId);
                    Toast.makeText(this, "Login failed: User profile data not found.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    progressBar.setVisibility(View.GONE);
                    buttonLogin.setEnabled(true);
                }
            } else {
                Log.e(TAG, "Failed to fetch user data from Firestore", task.getException());
                Toast.makeText(this, "Login failed: Could not load user profile.", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                progressBar.setVisibility(View.GONE);
                buttonLogin.setEnabled(true);
            }
        });
    }

    // Helper method to update FCM token in Firestore
    private void updateFcmTokenInFirestore(String userId, String token) {
        if (userId == null || token == null) {
            Log.w(TAG, "User ID or FCM token is null for Firestore update.");
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> tokenUpdate = new HashMap<>();
        tokenUpdate.put("fcmToken", token);
        tokenUpdate.put("fcmTokenLastUpdated", FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(tokenUpdate, SetOptions.merge()) // Use set with merge for robustness
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated in Firestore for user: " + userId))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating FCM token in Firestore for user: " + userId, e));
    }

    private void navigateToDashboard(String actualRole, String status, boolean profileComplete) {
        Intent intent = null;
        if (MainActivity.ROLE_TUTOR.equals(actualRole)) {
            if ("pending_verification".equals(status) || "pending_review".equals(status)) {
                intent = new Intent(this, VerificationPendingActivity.class);
            } else if ("verified".equals(status)) {
                // Check if profile is complete for verified tutors
                // if (!profileComplete) { // Assuming 'profileComplete' is also relevant for tutors
                //     intent = new Intent(this, TutorProfileWizardActivity.class);
                // } else {
                intent = new Intent(this, TutorDashboardActivity.class);
                // }
            } else { // Default to profile wizard if status is unclear or incomplete
                intent = new Intent(this, TutorProfileWizardActivity.class);
            }
        } else if (MainActivity.ROLE_TUTEE.equals(actualRole)) {
            // Tutee navigation logic seems to depend on profileComplete directly
            if (profileComplete) {
                intent = new Intent(this, TuteeDashboardActivity.class);
            } else {
                intent = new Intent(this, TuteeProfileActivity.class);
            }
        } else {
            Log.e(TAG, "Unknown role for navigation: " + actualRole);
            Toast.makeText(this, "Invalid user role.", Toast.LENGTH_SHORT).show();
            mAuth.signOut(); // Sign out if role is problematic
            return; // Do not proceed with null intent
        }

        if (intent != null) {
            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish LoginActivity so user can't go back to it from dashboard
        } else {
            // This case should ideally not be reached if logic above is sound
            Log.e(TAG, "Navigation intent was null after role/status check. Role: " + actualRole + ", Status: " + status);
            Toast.makeText(this, "Login failed: Could not determine navigation path.", Toast.LENGTH_SHORT).show();
            mAuth.signOut(); // Ensure user is signed out if navigation fails
        }
    }
}