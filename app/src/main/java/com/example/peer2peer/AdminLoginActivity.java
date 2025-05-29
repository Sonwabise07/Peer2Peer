package com.example.peer2peer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem; // Import MenuItem for back arrow
import android.view.View;
import android.widget.Button;
// import android.widget.EditText; // Not used if TextInputEditText is used
import android.widget.ProgressBar;
import android.widget.Toast;

// import com.google.android.gms.tasks.OnCompleteListener; // Not directly used
// import com.google.android.gms.tasks.Task; // Not directly used
import com.google.android.material.textfield.TextInputEditText; // Import TextInputEditText
// import com.google.firebase.auth.AuthResult; // Not directly used
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.Map;

public class AdminLoginActivity extends AppCompatActivity {

    private static final String TAG = "AdminLoginActivity";

    // UI Elements
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    // Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Elements
        toolbar = findViewById(R.id.toolbar_admin_login); // Ensure this ID is in your layout
        editTextEmail = findViewById(R.id.edit_text_admin_email);
        editTextPassword = findViewById(R.id.edit_text_admin_password);
        buttonLogin = findViewById(R.id.button_admin_login);
        progressBar = findViewById(R.id.progress_bar_admin_login);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back arrow
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Admin Login"); // Set a title for consistency
        }

        // Set Login Button Listener
        buttonLogin.setOnClickListener(v -> attemptAdminLogin());
    }

    // Handle Toolbar back arrow click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity, go back to MainActivity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void attemptAdminLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Basic Input Validation
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required.");
            editTextEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Enter a valid email address.");
            editTextEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required.");
            editTextPassword.requestFocus();
            return;
        }

        showLoading(true); // Show progress, disable button

        // Sign in with Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    // No need to hide progress bar here if checkAdminClaim also shows/hides it
                    // showLoading(false) will be called within checkAdminClaim or if this initial task fails.

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Admin signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkAdminClaim(user);
                        } else {
                            showLoading(false);
                            Toast.makeText(AdminLoginActivity.this, "Authentication failed (user null).", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showLoading(false);
                        Log.w(TAG, "Admin signInWithEmail:failure", task.getException());
                        String errorMessage = "Authentication Failed.";
                        try {
                            if (task.getException() != null) {
                                throw task.getException();
                            }
                        } catch (FirebaseAuthInvalidUserException e) {
                            errorMessage = "Invalid email address.";
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            errorMessage = "Invalid password.";
                        } catch (Exception e) {
                            errorMessage = "Login failed. Please try again.";
                            Log.e(TAG, "Unhandled login error", e);
                        }
                        Toast.makeText(AdminLoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAdminClaim(FirebaseUser user) {
        // showLoading(true) was called before signInWithEmailAndPassword,
        // so progress bar is already visible. It will be hidden after this check.
        Log.d(TAG, "checkAdminClaim: Attempting to force refresh ID token...");
        user.getIdToken(true)
                .addOnCompleteListener(task -> {
                    showLoading(false); // Hide progress bar after token check attempt

                    if (task.isSuccessful()) {
                        Log.d(TAG, "checkAdminClaim: Token refresh successful.");
                        GetTokenResult tokenResult = task.getResult();
                        if (tokenResult != null) {
                            Map<String, Object> claims = tokenResult.getClaims();
                            Log.d(TAG, "checkAdminClaim: Claims received: " + (claims != null ? claims.toString() : "null"));
                            Object isAdminClaim = claims != null ? claims.get("isAdmin") : null;
                            Log.d(TAG, "checkAdminClaim: Value of 'isAdmin' claim: " + isAdminClaim +
                                    " (Type: " + (isAdminClaim != null ? isAdminClaim.getClass().getName() : "null") + ")");
                            boolean isAdmin = Boolean.TRUE.equals(isAdminClaim);
                            Log.d(TAG, "checkAdminClaim: isAdmin check result: " + isAdmin);

                            if (isAdmin) {
                                Log.d(TAG, "checkAdminClaim: User IS Admin. Preparing to navigate...");
                                Toast.makeText(AdminLoginActivity.this, "Admin Login Successful", Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                                // These flags make AdminDashboardActivity the new root of the task.
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish(); // Finish AdminLoginActivity
                            } else {
                                Log.w(TAG, "checkAdminClaim: User is NOT an Admin.");
                                Toast.makeText(AdminLoginActivity.this, "Access Denied: Not an authorized admin.", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            Log.e(TAG, "checkAdminClaim: Token result was null after login.");
                            Toast.makeText(AdminLoginActivity.this, "Failed to verify admin status (token result null).", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        Log.e(TAG, "checkAdminClaim: Failed to get ID token with claims.", task.getException());
                        Toast.makeText(AdminLoginActivity.this, "Failed to verify admin status: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (buttonLogin != null) {
            buttonLogin.setEnabled(!isLoading);
        }
    }
}
