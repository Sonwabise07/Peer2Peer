package com.example.peer2peer; // Ensure this matches your package

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.peer2peer.fragments.TutorProfileStep1Fragment; // Import the fragment
import com.example.peer2peer.viewmodels.TutorProfileViewModel; // Import the ViewModel
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TuteeProfileActivity extends AppCompatActivity {

    private static final String TAG = "TuteeProfileActivity";

    private TutorProfileViewModel viewModel; // Use the same ViewModel
    private Button buttonSave;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutee_profile);

        // Initialize ViewModel scoped to THIS Activity
        viewModel = new ViewModelProvider(this).get(TutorProfileViewModel.class);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // *** This line seems correct based on the XML ***
        buttonSave = findViewById(R.id.button_save_tutee_profile);
        progressBar = findViewById(R.id.tutee_profile_progress);

        // Load the fragment
        if (savedInstanceState == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            // Ensure TutorProfileStep1Fragment exists and is correctly imported
            ft.replace(R.id.fragment_container_tutee_profile, new TutorProfileStep1Fragment());
            ft.commit();
        }

        // Load initial data into the ViewModel for the fragment to observe
        loadInitialData();

        // Ensure buttonSave is not null before setting listener (though findViewById should throw if not found)
        if (buttonSave != null) {
            buttonSave.setOnClickListener(v -> saveProfileAndContinue());
        } else {
            Log.e(TAG, "Button button_save_tutee_profile not found!");
            Toast.makeText(this, "Error initializing save button.", Toast.LENGTH_SHORT).show();
            // Decide how to handle this - maybe disable functionality or finish()
        }
    }

    private void loadInitialData() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userDocRef = db.collection("users").document(userId);
            Log.d(TAG, "Attempting to load initial data for Tutee Profile: " + userId);

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Log.d(TAG, "Tutee data found. Populating ViewModel.");
                    // Populate ViewModel - fragment observers will pick these up
                    viewModel.setFirstName(documentSnapshot.getString("firstName"));
                    viewModel.setSurname(documentSnapshot.getString("surname"));
                    viewModel.setStudentId(documentSnapshot.getString("studentId"));
                    viewModel.setEmail(documentSnapshot.getString("email")); // Or use currentUser.getEmail()
                    viewModel.setGender(documentSnapshot.getString("gender")); // Load if exists
                    viewModel.setRace(documentSnapshot.getString("race"));     // Load if exists
                    // Don't load profileImageUri here unless you store the URL in Firestore
                } else {
                    Log.w(TAG, "No Firestore document found for Tutee: " + userId);
                    // Populate email from Auth as minimum
                    if(currentUser.getEmail() != null) viewModel.setEmail(currentUser.getEmail());
                    Toast.makeText(this, "Could not load profile details.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error loading initial Tutee data", e);
                if(currentUser != null && currentUser.getEmail() != null) viewModel.setEmail(currentUser.getEmail());
                Toast.makeText(this, "Error loading profile.", Toast.LENGTH_SHORT).show();
            });
        } else {
            Log.e(TAG, "Current user is null in loadInitialData.");
            // Handle appropriately - maybe finish activity or redirect to login
        }
    }

    private void saveProfileAndContinue() {
        if (currentUser == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // --- Basic Validation (Example: ensure gender/race selected if required) ---
        // You might want more robust validation by accessing the fragment's isValid method if you implement one
        String selectedGender = viewModel.getGender().getValue();
        String selectedRace = viewModel.getRace().getValue();
        // Add checks if these fields are mandatory for tutees
        // if (TextUtils.isEmpty(selectedGender) || TextUtils.isEmpty(selectedRace)) {
        //     Toast.makeText(this, "Please select Gender and Race.", Toast.LENGTH_SHORT).show();
        //     return;
        // }
        // --- End Validation ---

        // Ensure progressBar is not null before using
        if(progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if(buttonSave != null) buttonSave.setEnabled(false);

        // Data to save/update in Firestore
        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("profileComplete", true); // Mark profile as complete
        // Optionally save Gender/Race selections if they were made/changed
        // Check if values are different from initial load if necessary, or just save current VM state
        if(selectedGender != null) profileUpdate.put("gender", selectedGender);
        if(selectedRace != null) profileUpdate.put("race", selectedRace);
        // If profile picture was added/changed, handle upload and save URL separately (more complex)

        db.collection("users").document(userId)
                .set(profileUpdate, SetOptions.merge()) // Merge to update only specific fields
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tutee profile marked as complete.");
                    if(progressBar != null) progressBar.setVisibility(View.GONE);
                    if(buttonSave != null) buttonSave.setEnabled(true);
                    // Navigate to Tutee Dashboard
                    Intent intent = new Intent(TuteeProfileActivity.this, TuteeDashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                    startActivity(intent);
                    finish(); // Close this activity
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating Tutee profile", e);
                    if(progressBar != null) progressBar.setVisibility(View.GONE);
                    if(buttonSave != null) buttonSave.setEnabled(true);
                    Toast.makeText(TuteeProfileActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}