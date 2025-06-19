package com.example.peer2peer; 

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.peer2peer.fragments.TutorProfileStep1Fragment;
import com.example.peer2peer.fragments.TutorProfileStep2Fragment;
import com.example.peer2peer.fragments.TutorProfileStep3Fragment;
import com.example.peer2peer.fragments.TutorProfileStep4Fragment;
import com.example.peer2peer.fragments.TutorProfileStep5Fragment;
import com.example.peer2peer.viewmodels.TutorProfileViewModel;


import android.app.ProgressDialog; 
import android.app.AlertDialog;    
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap; 
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;



// --- Firebase Imports ---
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue; 
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
// --- End Firebase Imports ---

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; 

public class TutorProfileWizardActivity extends AppCompatActivity {

    private static final String TAG = "TutorProfileWizard"; // Tag for logging

    private ProgressBar progressBar;
    private Button buttonNext, buttonPrevious;
    private FrameLayout fragmentContainer;

    private TutorProfileViewModel viewModel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private ProgressDialog progressDialog; 

    private int currentStep = 1;
    private final int totalSteps = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_profile_wizard);

        viewModel = new ViewModelProvider(this).get(TutorProfileViewModel.class);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

       
        progressDialog = new ProgressDialog(this);
        
        progressDialog.setMessage(getString(R.string.progress_submitting));
        progressDialog.setCancelable(false);

        progressBar = findViewById(R.id.profile_progress_bar);
        buttonNext = findViewById(R.id.button_next_step);
        buttonPrevious = findViewById(R.id.button_previous_step);
        fragmentContainer = findViewById(R.id.fragment_container_profile_wizard);

        if (savedInstanceState == null) {
            loadFragmentForStep(currentStep);
        }
        updateUIControls();

        buttonNext.setOnClickListener(v -> {
            

            if (currentStep < totalSteps) {
                currentStep++;
                loadFragmentForStep(currentStep);
                updateUIControls();
            } else {
                Log.d(TAG, "Submit button clicked.");
                submitProfileData();
            }
        });

        buttonPrevious.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                loadFragmentForStep(currentStep);
                updateUIControls();
            }
        });
    }

    
    private void submitProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            
            Toast.makeText(this, R.string.error_not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        
        if (viewModel.getProfileImageUri().getValue() == null ||
                viewModel.getIdDocumentUri().getValue() == null ||
                viewModel.getProofRegistrationUri().getValue() == null ||
                viewModel.getAcademicRecordUri().getValue() == null) {
           
            Toast.makeText(this, R.string.error_missing_files, Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog.show();
        uploadFilesAndSaveData(userId);
    }

    private void uploadFilesAndSaveData(final String userId) {
        Log.d(TAG, "Starting file uploads for user: " + userId);
        StorageReference storageRef = storage.getReference();

        Uri profileImageUri = viewModel.getProfileImageUri().getValue();
        Uri idDocUri = viewModel.getIdDocumentUri().getValue();
        Uri proofDocUri = viewModel.getProofRegistrationUri().getValue();
        Uri recordDocUri = viewModel.getAcademicRecordUri().getValue();

        
        String profileImageExt = getFileExtension(profileImageUri);
        String idDocExt = getFileExtension(idDocUri);
        String proofDocExt = getFileExtension(proofDocUri);
        String recordDocExt = getFileExtension(recordDocUri);

        StorageReference profileImageRef = storageRef.child("users/" + userId + "/profileImage" + (profileImageExt != null ? "." + profileImageExt : ""));
        StorageReference idDocRef = storageRef.child("users/" + userId + "/documents/idDocument" + (idDocExt != null ? "." + idDocExt : ""));
        StorageReference proofDocRef = storageRef.child("users/" + userId + "/documents/proofRegistration" + (proofDocExt != null ? "." + proofDocExt : ""));
        StorageReference recordDocRef = storageRef.child("users/" + userId + "/documents/academicRecord" + (recordDocExt != null ? "." + recordDocExt : ""));

    
        List<UploadTask> uploadTasks = new ArrayList<>();
        List<Task<Uri>> downloadUrlTasks = new ArrayList<>(); 

       
        UploadTask profileUploadTask = profileImageRef.putFile(profileImageUri);
        uploadTasks.add(profileUploadTask);
        downloadUrlTasks.add(profileUploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) { throw task.getException(); }
            return profileImageRef.getDownloadUrl();
        }));

        // Add ID document upload task
        UploadTask idUploadTask = idDocRef.putFile(idDocUri);
        uploadTasks.add(idUploadTask);
        downloadUrlTasks.add(idUploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) { throw task.getException(); }
            return idDocRef.getDownloadUrl();
        }));

       
        UploadTask proofUploadTask = proofDocRef.putFile(proofDocUri);
        uploadTasks.add(proofUploadTask);
        downloadUrlTasks.add(proofUploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) { throw task.getException(); }
            return proofDocRef.getDownloadUrl();
        }));

    
        UploadTask recordUploadTask = recordDocRef.putFile(recordDocUri);
        uploadTasks.add(recordUploadTask);
        downloadUrlTasks.add(recordUploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) { throw task.getException(); }
            return recordDocRef.getDownloadUrl();
        }));


   
        Task<List<Uri>> allDownloadUrlTasks = Tasks.whenAllSuccess(downloadUrlTasks);

        allDownloadUrlTasks.addOnSuccessListener(downloadUris -> {
            Log.d(TAG, "All download URLs retrieved successfully.");
            if (downloadUris != null && downloadUris.size() == 4) {
                Map<String, String> downloadUrlsMap = new HashMap<>();
                downloadUrlsMap.put("profileImageUrl", downloadUris.get(0) != null ? downloadUris.get(0).toString() : null);
                downloadUrlsMap.put("idDocumentUrl", downloadUris.get(1) != null ? downloadUris.get(1).toString() : null);
                downloadUrlsMap.put("proofRegistrationUrl", downloadUris.get(2) != null ? downloadUris.get(2).toString() : null);
                downloadUrlsMap.put("academicRecordUrl", downloadUris.get(3) != null ? downloadUris.get(3).toString() : null);

                // Check for null URLs (optional, depends if files are mandatory)
                if (downloadUrlsMap.containsValue(null)) {
                    Log.e(TAG, "Error: One or more download URLs are null.");
                    handleSubmissionError(getString(R.string.error_upload_url_mismatch)); // Or a more specific error
                    return;
                }

                saveDataToFirestore(userId, downloadUrlsMap);
            } else {
                Log.e(TAG, "Error: Incorrect number of download URLs received. Expected 4, Got: " + (downloadUris != null ? downloadUris.size() : "null"));
                // Ensure string exists
                handleSubmissionError(getString(R.string.error_upload_url_mismatch));
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get one or more download URLs", e);
            // Ensure string exists
            handleSubmissionError(getString(R.string.error_file_upload_failed));
        });
    }

    // Helper to get file extension from URI
    private String getFileExtension(Uri uri) {
        if (uri == null) return null;
        String extension = null;
        try {
            // Try to get MIME type first
            String mimeType = getContentResolver().getType(uri);
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            Log.d(TAG, "URI: " + uri + ", MimeType: " + mimeType + ", Extension: " + extension);
        } catch (Exception e) {
            Log.w(TAG, "Could not get MIME type for URI: " + uri, e);
        }
        // Fallback if MIME type didn't work or no extension found
        if (extension == null) {
            String path = uri.getPath();
            if (path != null) {
                int lastDot = path.lastIndexOf(".");
                if (lastDot >= 0) {
                    extension = path.substring(lastDot + 1);
                }
            }
        }
        return extension;
    }


    private void saveDataToFirestore(String userId, Map<String, String> downloadUrls) {
        Log.d(TAG, "Saving profile data to Firestore for user: " + userId);

        Map<String, Object> tutorProfileData = new HashMap<>();
        tutorProfileData.put("userId", userId);
        tutorProfileData.put("firstName", viewModel.getFirstName().getValue());
        tutorProfileData.put("surname", viewModel.getSurname().getValue());
        tutorProfileData.put("gender", viewModel.getGender().getValue());
        tutorProfileData.put("race", viewModel.getRace().getValue());
        tutorProfileData.put("email", viewModel.getEmail().getValue());
        tutorProfileData.put("studentId", viewModel.getStudentId().getValue());
        tutorProfileData.put("yearOfStudy", viewModel.getYearOfStudy().getValue());
        tutorProfileData.put("qualifications", viewModel.getQualifications().getValue());
        tutorProfileData.put("modulesToTutor", viewModel.getModulesToTutor().getValue());
        tutorProfileData.put("tutoringLanguages", viewModel.getTutoringLanguages().getValue());
        tutorProfileData.put("bio", viewModel.getBio().getValue());
        tutorProfileData.put("hourlyRate", viewModel.getHourlyRate().getValue());

        // Add download URLs
        tutorProfileData.putAll(downloadUrls);

        // Add original filenames
        tutorProfileData.put("idDocumentFilename", viewModel.getIdDocumentFilename().getValue());
        tutorProfileData.put("proofRegistrationFilename", viewModel.getProofRegistrationFilename().getValue());
        tutorProfileData.put("academicRecordFilename", viewModel.getAcademicRecordFilename().getValue());

        // Set Status & completion flag & timestamp
        tutorProfileData.put("profileStatus", "pending_verification");
        tutorProfileData.put("profileComplete", true);
        tutorProfileData.put("submissionTimestamp", FieldValue.serverTimestamp()); // Use FieldValue


        // Save/Merge data into the user's document
        db.collection("users").document(userId)
                .set(tutorProfileData, SetOptions.merge()) // Merge to avoid overwriting other potential user fields
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile data successfully saved to Firestore.");
                    navigateToPendingScreen();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving profile data to Firestore", e);
                    // Ensure string exists
                    handleSubmissionError(getString(R.string.error_firestore_save_failed));
                });
    }

    private void navigateToPendingScreen() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        // Ensure string exists
        Toast.makeText(this, R.string.submit_success_message, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(TutorProfileWizardActivity.this, VerificationPendingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleSubmissionError(String errorMessage) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        // Ensure string exists
        new AlertDialog.Builder(this)
                .setTitle(R.string.error_submission_title)
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        Log.e(TAG, "Submission failed: " + errorMessage);
    }


    // --- Fragment Loading and UI Update Logic ---
    private void loadFragmentForStep(int step) {
        Fragment fragment = null;
        switch (step) {
            case 1: fragment = new TutorProfileStep1Fragment(); break;
            case 2: fragment = new TutorProfileStep2Fragment(); break;
            case 3: fragment = new TutorProfileStep3Fragment(); break;
            case 4: fragment = new TutorProfileStep4Fragment(); break;
            case 5: fragment = new TutorProfileStep5Fragment(); break;
            default:
                Log.w(TAG, "Invalid step number: " + step);
                // Optional: Reload step 1 or show error
                currentStep = 1;
                fragment = new TutorProfileStep1Fragment();
                // return;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container_profile_wizard, fragment);
        ft.commit();
    }

    private void updateUIControls() {
        int progress = (currentStep * 100) / totalSteps;
        progressBar.setProgress(progress);
        buttonPrevious.setVisibility(currentStep > 1 ? View.VISIBLE : View.INVISIBLE);
        // Ensure strings exist
        buttonNext.setText(currentStep == totalSteps ? getString(R.string.submit_button) : getString(R.string.next_button));
    }
}