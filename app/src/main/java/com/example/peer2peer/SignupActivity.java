package com.example.peer2peer; 

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

// Firebase and Task imports
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue; 
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging; 

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {

    // UI Elements
    TextInputEditText editTextFirstName, editTextSurname, editTextStudentId, editTextEmail, editTextPassword, editTextConfirmPassword;
    CheckBox checkBoxTerms;
    TextView textViewTermsLink, textViewLoginLink;
    Button buttonSignup;
    ProgressBar progressBar;

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    private String userRole = "unknown"; // Default if not passed

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +         //at least 1 digit
                    "(?=.*[a-z])" +         //at least 1 lower case letter
                    "(?=.*[A-Z])" +         //at least 1 upper case letter
                    "(?=.*[!@#$%^&+=_*])" + //at least 1 special character
                    "(?=\\S+$)" +           //no white spaces
                    ".{12,}" +              //at least 12 characters
                    "$");
    private static final Pattern DUT_EMAIL_PATTERN =
            Pattern.compile("^[0-9]{8}@dut4life\\.ac\\.za$");

    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivity.EXTRA_USER_ROLE)) {
            userRole = intent.getStringExtra(MainActivity.EXTRA_USER_ROLE);
            if (userRole == null || TextUtils.isEmpty(userRole)) {
                userRole = "unknown"; // Fallback
                Log.w(TAG, "Received null or empty role via intent, defaulting to " + userRole);
            } else {
                Log.d(TAG, "Received role via intent: " + userRole);
            }
        } else {
            Log.w(TAG, "No role passed to SignupActivity via intent, defaulting to " + userRole);
        }

        editTextFirstName = findViewById(R.id.edit_text_signup_firstname);
        editTextSurname = findViewById(R.id.edit_text_signup_surname);
        editTextStudentId = findViewById(R.id.edit_text_signup_student_id);
        editTextEmail = findViewById(R.id.edit_text_signup_email);
        editTextPassword = findViewById(R.id.edit_text_signup_password);
        editTextConfirmPassword = findViewById(R.id.edit_text_signup_confirm_password);
        checkBoxTerms = findViewById(R.id.checkbox_terms);
        textViewTermsLink = findViewById(R.id.text_terms_link);
        buttonSignup = findViewById(R.id.button_signup);
        textViewLoginLink = findViewById(R.id.text_login_link);
        progressBar = findViewById(R.id.signup_progress_bar);

        textViewTermsLink.setMovementMethod(LinkMovementMethod.getInstance());
        buttonSignup.setEnabled(false); // Initially disabled

        checkBoxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> buttonSignup.setEnabled(isChecked));
        buttonSignup.setOnClickListener(v -> registerUser());
        textViewLoginLink.setOnClickListener(v -> {
            Intent loginIntent = new Intent(SignupActivity.this, LoginActivity.class);
            if (!"unknown".equals(userRole)) { 
                loginIntent.putExtra(MainActivity.EXTRA_USER_ROLE, userRole);
            }
            startActivity(loginIntent);
            finish();
        });
    }

    private void registerUser() {
       ..
        final String firstName = String.valueOf(editTextFirstName.getText()).trim();
        final String surname = String.valueOf(editTextSurname.getText()).trim();
        final String studentId = String.valueOf(editTextStudentId.getText()).trim();
        final String email = String.valueOf(editTextEmail.getText()).trim();
        final String password = String.valueOf(editTextPassword.getText()).trim();
        String confirmPassword = String.valueOf(editTextConfirmPassword.getText()).trim();

        // --- Validation (ensure this is complete as per your original) ---
        if (TextUtils.isEmpty(firstName)) { editTextFirstName.setError("First name required."); resetUI(); return; }
        if (TextUtils.isEmpty(surname)) { editTextSurname.setError("Surname required."); resetUI(); return; }
        if (TextUtils.isEmpty(studentId) || studentId.length() != 8 || !TextUtils.isDigitsOnly(studentId)) { editTextStudentId.setError("Valid 8-digit student ID required."); resetUI(); return; }
        if (TextUtils.isEmpty(email) || !DUT_EMAIL_PATTERN.matcher(email).matches()) { editTextEmail.setError("Valid DUT student email required (e.g., 12345678@dut4life.ac.za)."); resetUI(); return; }
        if (TextUtils.isEmpty(password)) { editTextPassword.setError("Password required."); resetUI(); return; }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            editTextPassword.setError("Password: 12+ chars, with uppercase, lowercase, digit, special character, and no spaces.");
            editTextPassword.requestFocus(); resetUI(); return;
        }
        if (TextUtils.isEmpty(confirmPassword)) { editTextConfirmPassword.setError("Confirm password required."); resetUI(); return; }
        if (!password.equals(confirmPassword)) { editTextConfirmPassword.setError("Passwords do not match."); editTextConfirmPassword.requestFocus(); resetUI(); return; }
        if (!checkBoxTerms.isChecked()) { Toast.makeText(this, "You must agree to the terms and conditions.", Toast.LENGTH_SHORT).show(); resetUI(); return; }
        // --- End Validation ---

        progressBar.setVisibility(View.VISIBLE);
        buttonSignup.setEnabled(false);
        checkBoxTerms.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // --- START: Get FCM Token before saving user data ---
                            FirebaseMessaging.getInstance().getToken()
                                    .addOnCompleteListener(tokenTask -> {
                                        String fcmToken = null;
                                        if (tokenTask.isSuccessful() && tokenTask.getResult() != null) {
                                            fcmToken = tokenTask.getResult();
                                            Log.d(TAG, "FCM Token fetched for new user: " + fcmToken);
                                        } else {
                                            Log.w(TAG, "Fetching FCM registration token failed for new user", tokenTask.getException());
                                            
                                        // Proceed to save user data, with or without token
                                        saveUserDataToFirestore(firebaseUser, firstName, surname, studentId, email, this.userRole, fcmToken);
                                        sendEmailVerification(firebaseUser); // Call this after attempting to save data
                                    });
                            // --- END: Get FCM Token ---
                        } else {
                            Log.w(TAG, "createUserWithEmail:success but user is null after creation.");
                            Toast.makeText(SignupActivity.this, "Registration failed: Could not get user details post-creation.", Toast.LENGTH_SHORT).show();
                            resetUI(); // Reset UI as user data saving won't proceed
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = "Authentication failed.";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(SignupActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        resetUI();
                    }
                });
    }

    // --- MODIFIED Method to include FCM Token ---
    private void saveUserDataToFirestore(FirebaseUser firebaseUser, String firstName, String surname, String studentId, String email, String role, String fcmToken) {
        String userId = firebaseUser.getUid();
        String lowercaseEmail = (email != null) ? email.toLowerCase() : null;

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", firstName);
        userMap.put("surname", surname);
        userMap.put("fullName", (firstName + " " + surname).trim());
        userMap.put("studentId", studentId);
        userMap.put("email", email); // Original case email
        if (lowercaseEmail != null) {
            userMap.put("lowercaseEmail", lowercaseEmail);
        }
        userMap.put("role", role);
        userMap.put("profileComplete", false); // Default, Tutee might be set to true below
        userMap.put("accountCreatedTimestamp", FieldValue.serverTimestamp());

        if (fcmToken != null && !fcmToken.isEmpty()) { // Add FCM token if available
            userMap.put("fcmToken", fcmToken);
            userMap.put("fcmTokenLastUpdated", FieldValue.serverTimestamp());
        }

        // Set initial profileStatus based on role
        if (MainActivity.ROLE_TUTOR.equals(role)) {
            userMap.put("profileStatus", "incomplete"); // Tutors need to complete profile wizard
        } else if (MainActivity.ROLE_TUTEE.equals(role)) {
            userMap.put("profileStatus", "active");
            userMap.put("profileComplete", true); // Assuming Tutee profile is simpler and considered complete at signup
        } else {
            userMap.put("profileStatus", "unknown_role_status"); // Should not happen if role is validated
        }

        db.collection("users").document(userId)
                .set(userMap) // Use set() for a new user document
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data successfully written to Firestore! Role: " + role + ", UID: " + userId))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing user data to Firestore", e);
                    
                    Toast.makeText(SignupActivity.this, "Registration succeeded but profile creation failed. Please contact support.", Toast.LENGTH_LONG).show();
                });
    }

    private void sendEmailVerification(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE); // Ensure progress bar is hidden
                    buttonSignup.setEnabled(true);      // Re-enable buttons
                    checkBoxTerms.setEnabled(true);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Email verification sent.");
                        Toast.makeText(SignupActivity.this, "Registration successful! Please check your email ("+(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "")+") to verify your account.", Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "sendEmailVerification failed", task.getException());
                        // Even if email sending fails, user is created. Inform them.
                        Toast.makeText(SignupActivity.this, "Registration successful. Failed to send verification email. You may need to verify manually later.", Toast.LENGTH_LONG).show();
                    }
                    // Navigate to Login screen so user can log in after verifying
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    if (!"unknown".equals(this.userRole)) { // Pass the role to LoginActivity
                        intent.putExtra(MainActivity.EXTRA_USER_ROLE, this.userRole);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish(); // Close SignupActivity
                });
    }

    private void resetUI() {
        progressBar.setVisibility(View.GONE);
        buttonSignup.setEnabled(checkBoxTerms.isChecked()); // Re-enable based on terms
        checkBoxTerms.setEnabled(true);
    }
}