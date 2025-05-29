package com.example.peer2peer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
// import android.widget.ImageView; // Commented out as it's not in your XML yet
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Ensure this import for @NonNull
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

// import com.bumptech.glide.Glide; // Commented out as ImageView is not used yet
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AdminUserDetailActivity extends AppCompatActivity {

    private static final String TAG = "AdminUserDetail";
    public static final String EXTRA_USER_UID = "USER_UID";

    // UI Elements
    private Toolbar toolbar;
    private TextView textViewName, textViewEmail, textViewRole, textViewAdminUserStatus, textViewError;
    // textViewAdminUserStatus will be used for "Active/Blocked" and profile status
    private Button buttonBlockUser, buttonUnblockUser;
    private ProgressBar progressBar;
    private ConstraintLayout contentLayout;
    // private ImageView imageViewProfile; // Commented out - Add to XML first if needed

    // Firebase
    private FirebaseFirestore db;
    private FirebaseFunctions mFunctions;

    // Data
    private String userUid;
    private Tutor currentUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_detail);

        db = FirebaseFirestore.getInstance();
        mFunctions = FirebaseFunctions.getInstance();

        toolbar = findViewById(R.id.toolbar_admin_user_detail);
        textViewName = findViewById(R.id.textViewAdminUserName);
        textViewEmail = findViewById(R.id.textViewAdminUserEmail);
        textViewRole = findViewById(R.id.textViewAdminUserRole);
        textViewAdminUserStatus = findViewById(R.id.textViewAdminUserStatus); // This ID is in your XML
        textViewError = findViewById(R.id.textViewAdminUserDetailError);
        buttonBlockUser = findViewById(R.id.buttonBlockUser); // Matches your XML ID
        buttonUnblockUser = findViewById(R.id.buttonUnblockUser); // Matches your XML ID
        progressBar = findViewById(R.id.progressBarAdminUserDetail);
        contentLayout = findViewById(R.id.layoutAdminUserDetailsContent);
        // imageViewProfile = findViewById(R.id.imageViewAdminUserProfile); // Commented out

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Details");
        }

        userUid = getIntent().getStringExtra(EXTRA_USER_UID);

        if (userUid == null || userUid.isEmpty()) {
            Log.e(TAG, "User UID is missing!");
            showError("Error: User ID not found.");
            if (buttonBlockUser != null) buttonBlockUser.setEnabled(false);
            if (buttonUnblockUser != null) buttonUnblockUser.setEnabled(false);
            return;
        }

        Log.d(TAG, "Displaying details for user UID: " + userUid);
        initializeUIState();
        fetchUserData();

        buttonBlockUser.setOnClickListener(v -> {
            if (currentUserData != null && !currentUserData.isBlocked()) {
                showBlockReasonDialog();
            } else {
                Toast.makeText(this, "User cannot be blocked at this time.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonUnblockUser.setOnClickListener(v -> {
            if (currentUserData != null && currentUserData.isBlocked()) {
                callCloudFunction("unblockUser", "Unblocking User...", null);
            } else {
                Toast.makeText(this, "User is not blocked.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeUIState() {
        showLoading(true);
        if (textViewError != null) textViewError.setVisibility(View.GONE);
        if (contentLayout != null) contentLayout.setVisibility(View.GONE);
        if (buttonBlockUser != null) buttonBlockUser.setEnabled(false);
        if (buttonUnblockUser != null) buttonUnblockUser.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchUserData() {
        if (userUid == null) return;
        showLoading(true);

        DocumentReference userRef = db.collection("users").document(userUid);
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        currentUserData = documentSnapshot.toObject(Tutor.class); // Assuming Tutor model has isBlocked
                        if (currentUserData != null) {
                            currentUserData.setUid(documentSnapshot.getId());
                            Log.d(TAG, "User data fetched successfully: " + currentUserData.getFullName() + ", isBlocked: " + currentUserData.isBlocked());
                            populateUI();
                            if (contentLayout != null) contentLayout.setVisibility(View.VISIBLE);
                        } else {
                            Log.e(TAG, "Failed to parse user data for UID: " + userUid);
                            showError("Error: Could not parse user data.");
                        }
                    } else {
                        Log.e(TAG, "No user document found for UID: " + userUid);
                        showError("Error: User not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error fetching user data for UID: " + userUid, e);
                    showError("Error fetching user details: " + e.getMessage());
                });
    }

    private void populateUI() {
        if (currentUserData == null) {
            showError("Error: User data is unavailable.");
            return;
        }

        if (getSupportActionBar() != null && currentUserData.getFullName() != null) {
            getSupportActionBar().setTitle(currentUserData.getFullName());
        }

        // Commented out ImageView logic - add an ImageView to your XML with ID imageViewAdminUserProfile if you want to use it
        // if (imageViewProfile != null && currentUserData.getProfileImageUrl() != null && !currentUserData.getProfileImageUrl().isEmpty()) {
        //     Glide.with(this).load(currentUserData.getProfileImageUrl()).placeholder(R.drawable.ic_person_placeholder_default).into(imageViewProfile);
        // } else if (imageViewProfile != null) {
        //     imageViewProfile.setImageResource(R.drawable.ic_person_placeholder_default);
        // }

        if (textViewName != null) textViewName.setText(currentUserData.getFullName() != null ? currentUserData.getFullName() : "N/A");
        if (textViewEmail != null) textViewEmail.setText(currentUserData.getEmail() != null ? currentUserData.getEmail() : "N/A");
        if (textViewRole != null) textViewRole.setText(currentUserData.getRole() != null ? capitalize(currentUserData.getRole()) : "N/A");

        updateAccountStatusAndButtons(); // This will handle isBlocked text and button states
    }

    private void updateAccountStatusAndButtons() {
        if (currentUserData == null) {
            if (buttonBlockUser != null) buttonBlockUser.setVisibility(View.GONE);
            if (buttonUnblockUser != null) buttonUnblockUser.setVisibility(View.GONE);
            if (textViewAdminUserStatus != null) textViewAdminUserStatus.setText("Status: Unknown");
            return;
        }

        boolean isUserBlocked = currentUserData.isBlocked();
        Log.d(TAG, "updateAccountStatusAndButtons - isUserBlocked: " + isUserBlocked);

        if (textViewAdminUserStatus != null) {
            String profileStatusString = currentUserData.getProfileStatus() != null ? capitalize(currentUserData.getProfileStatus()) : "Status Unknown";
            if (isUserBlocked) {
                textViewAdminUserStatus.setText("Account: Blocked (Profile: " + profileStatusString + ")");
                textViewAdminUserStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            } else {
                textViewAdminUserStatus.setText("Account: Active (Profile: " + profileStatusString + ")");
                textViewAdminUserStatus.setTextColor(ContextCompat.getColor(this, R.color.green_500)); // Using green_500 from your colors.xml
            }
        }

        if (buttonBlockUser != null) {
            buttonBlockUser.setVisibility(isUserBlocked ? View.GONE : View.VISIBLE);
            buttonBlockUser.setEnabled(!isUserBlocked);
        }
        if (buttonUnblockUser != null) {
            buttonUnblockUser.setVisibility(isUserBlocked ? View.VISIBLE : View.GONE);
            buttonUnblockUser.setEnabled(isUserBlocked);
        }
    }

    private void showBlockReasonDialog() {
        if (currentUserData == null || userUid == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Block User: " + currentUserData.getFullName());

        final EditText inputReason = new EditText(this);
        inputReason.setHint("Optional: Reason for blocking");
        inputReason.setPadding(50,30,50,30);
        builder.setView(inputReason);

        builder.setPositiveButton("Block", (dialog, which) -> {
            String reason = inputReason.getText().toString().trim();
            Log.d(TAG, "Admin provided reason for blocking: " + (reason.isEmpty() ? "None" : reason));
            callCloudFunction("blockUser", "Blocking User...", reason);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void callCloudFunction(String functionName, String progressMessage, String reason) { // Added reason parameter
        if (userUid == null || userUid.isEmpty()) {
            Toast.makeText(this, "User ID is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        if (buttonBlockUser != null) buttonBlockUser.setEnabled(false);
        if (buttonUnblockUser != null) buttonUnblockUser.setEnabled(false);
        // Toast.makeText(this, progressMessage, Toast.LENGTH_SHORT).show(); // Snackbar is better

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userUid);
        // Note: Your current 'blockUser' CF does not use a reason.
        // If you update it, you would add:
        // if (reason != null && !reason.isEmpty() && functionName.equals("blockUser")) {
        //    data.put("reason", reason);
        // }

        Log.d(TAG, "Calling cloud function: " + functionName + " for user: " + userUid);

        mFunctions.getHttpsCallable(functionName)
                .call(data)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        String message = "Operation successful.";
                        try {
                            Map<String, Object> resultData = (Map<String, Object>) Objects.requireNonNull(task.getResult()).getData();
                            if (resultData != null && resultData.containsKey("message")) {
                                message = (String) resultData.get("message");
                            }
                        } catch (Exception e) { Log.w(TAG, "Could not parse message from " + functionName + " result", e); }

                        Log.d(TAG, functionName + " successful: " + message);
                        if(contentLayout != null) Snackbar.make(contentLayout, message, Snackbar.LENGTH_LONG).show();
                        else Toast.makeText(this, message, Toast.LENGTH_LONG).show();


                        boolean wasBlockingOperation = functionName.equals("blockUser");
                        if (currentUserData != null) {
                            currentUserData.setBlocked(wasBlockingOperation);
                            populateUI(); // This re-populates text fields and updates button states
                            if (contentLayout != null) contentLayout.setVisibility(View.VISIBLE);
                        } else {
                            fetchUserData();
                        }
                    } else {
                        Exception e = task.getException();
                        Log.e(TAG, "Error calling " + functionName + " function", e);
                        String errorMessage = "Operation failed";
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            errorMessage = ffe.getMessage() + " (Code: " + ffe.getCode() + ")";
                            if (ffe.getDetails() != null) { errorMessage += " Details: " + ffe.getDetails().toString(); }
                        } else if (e != null && e.getMessage() != null) { errorMessage = e.getMessage(); }

                        if(contentLayout != null) Snackbar.make(contentLayout, "Error: " + errorMessage, Snackbar.LENGTH_LONG).show();
                        else Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();

                        if (contentLayout != null) contentLayout.setVisibility(View.VISIBLE); // Still show content on error
                        updateAccountStatusAndButtons(); // Re-enable buttons based on last known state
                    }
                });
    }

    private void showError(String message) {
        if (contentLayout != null) contentLayout.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (textViewError != null) {
            textViewError.setText(message);
            textViewError.setVisibility(View.VISIBLE);
        }
        if (buttonBlockUser != null) {
            buttonBlockUser.setEnabled(false);
            buttonBlockUser.setVisibility(View.GONE);
        }
        if (buttonUnblockUser != null) {
            buttonUnblockUser.setEnabled(false);
            buttonUnblockUser.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            if (contentLayout != null) contentLayout.setVisibility(View.GONE);
            if (textViewError != null) textViewError.setVisibility(View.GONE);
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        if (str.contains("_")) {
            String[] parts = str.split("_");
            StringBuilder capitalized = new StringBuilder();
            for (String part : parts) {
                if (part.length() > 0) {
                    capitalized.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                            .append(part.substring(1).toLowerCase(Locale.ROOT))
                            .append(" ");
                }
            }
            return capitalized.toString().trim();
        }
        return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
    }
}