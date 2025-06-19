package com.example.peer2peer;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.HashMap;
import java.util.List; 
import java.util.Locale;
import java.util.Map;

public class AdminTutorDetailActivity extends AppCompatActivity {

    private static final String TAG = "AdminTutorDetail";

    // UI Elements
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView textViewError;
    private LinearLayout detailsLayout;
    private TextView textName, textEmail, textStatus, textRate, textSubjects, textLanguages, textBio, textQualification, textYear;
    private TextView textDocId, textDocAcademic, textDocProofReg;
    private Button buttonApprove, buttonReject;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseFunctions mFunctions;

    // Data
    private String tutorUid;
    private Tutor currentTutor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_tutor_detail);

        tutorUid = getIntent().getStringExtra("TUTOR_UID");
        if (tutorUid == null || tutorUid.isEmpty()) {
            Log.e(TAG, "Tutor UID not passed to AdminTutorDetailActivity.");
            Toast.makeText(this, "Error: Tutor ID missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Received Tutor UID: " + tutorUid);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFunctions = FirebaseFunctions.getInstance();

        toolbar = findViewById(R.id.toolbar_admin_tutor_detail);
        progressBar = findViewById(R.id.progress_bar_admin_detail);
        textViewError = findViewById(R.id.text_admin_detail_error);
        detailsLayout = findViewById(R.id.layout_admin_details_content);
        textName = findViewById(R.id.text_admin_detail_name);
        textEmail = findViewById(R.id.text_admin_detail_email);
        textStatus = findViewById(R.id.text_admin_detail_status);
        textRate = findViewById(R.id.text_admin_detail_rate);
        textSubjects = findViewById(R.id.text_admin_detail_subjects);
        textLanguages = findViewById(R.id.text_admin_detail_languages);
        textBio = findViewById(R.id.text_admin_detail_bio);
        textQualification = findViewById(R.id.text_admin_detail_qualification);
        textYear = findViewById(R.id.text_admin_detail_year);
        textDocId = findViewById(R.id.text_admin_doc_id);
        textDocAcademic = findViewById(R.id.text_admin_doc_academic);
        textDocProofReg = findViewById(R.id.text_admin_doc_proof_reg);
        buttonApprove = findViewById(R.id.button_admin_approve);
        buttonReject = findViewById(R.id.button_admin_reject);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Verify Tutor");
        }

        buttonApprove.setOnClickListener(v -> handleApproveClick());
        buttonReject.setOnClickListener(v -> handleRejectClick());

        textDocId.setOnClickListener(null);
        textDocAcademic.setOnClickListener(null);
        textDocProofReg.setOnClickListener(null);

        fetchTutorDetails(tutorUid);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchTutorDetails(String uid) {
        showLoading(true);
        textViewError.setVisibility(View.GONE);
        detailsLayout.setVisibility(View.GONE);

        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            try {
                                currentTutor = document.toObject(Tutor.class);
                                if (currentTutor != null) {
                                    currentTutor.setUid(document.getId()); // Assuming Tutor class has setUid
                                    populateUI(currentTutor);
                                    detailsLayout.setVisibility(View.VISIBLE);
                                    // Use "pending_verification" to match what TutorProfileWizard sets
                                    if ("pending_verification".equals(currentTutor.getProfileStatus())) {
                                        buttonApprove.setEnabled(true);
                                        buttonReject.setEnabled(true);
                                    } else {
                                        buttonApprove.setEnabled(false);
                                        buttonReject.setEnabled(false);
                                        Log.d(TAG, "Tutor status (" + currentTutor.getProfileStatus() + ") is not pending_verification, disabling buttons.");
                                    }
                                } else {
                                    Log.e(TAG, "Parsed Tutor object is null for UID: " + uid + ". Check Tutor.java model and Firestore data consistency.");
                                    showError("Error parsing tutor data. Check logs.");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing tutor document for UID: " + uid, e);
                                showError("Error parsing tutor data. Check logs.");
                            }
                        } else {
                            Log.e(TAG, "No tutor document found for UID: " + uid);
                            showError("Tutor data not found.");
                        }
                    } else {
                        Log.e(TAG, "Error fetching tutor document for UID: " + uid, task.getException());
                        showError("Error fetching tutor details: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void populateUI(@NonNull Tutor tutor) {
        String name = tutor.getFullName(); // Using the improved getFullName() from Tutor model
        textName.setText("Name: " + (!TextUtils.isEmpty(name) ? name : "N/A"));

        if (getSupportActionBar() != null && !TextUtils.isEmpty(name)) {
            getSupportActionBar().setTitle(name);
        } else if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Verify Tutor"); // Fallback title
        }

        textEmail.setText("Email: " + (tutor.getEmail() != null ? tutor.getEmail() : "N/A"));
        textStatus.setText("Status: " + (tutor.getProfileStatus() != null ? tutor.getProfileStatus() : "Unknown"));
        textRate.setText("Rate: R" + String.format(Locale.getDefault(), "%.2f", tutor.getHourlyRate() != null ? tutor.getHourlyRate() : 0.0) + " / hour");

        String subjects = (tutor.getModulesToTutor() != null && !tutor.getModulesToTutor().isEmpty())
                ? TextUtils.join(", ", tutor.getModulesToTutor()) : "N/A";
        textSubjects.setText("Subjects: " + subjects);

        String languages = (tutor.getTutoringLanguages() != null && !tutor.getTutoringLanguages().isEmpty())
                ? TextUtils.join(", ", tutor.getTutoringLanguages()) : "N/A";
        textLanguages.setText("Languages: " + languages);

        textBio.setText("Bio: " + (tutor.getBio() != null ? tutor.getBio() : "N/A"));
        textQualification.setText("Qualification: " + (tutor.getQualifications() != null ? tutor.getQualifications() : "N/A"));
        textYear.setText("Year of Study: " + (tutor.getYearOfStudy() != null ? tutor.getYearOfStudy() : "N/A"));

        
        setupDocumentView(textDocId, tutor.getIdDocumentUrl(), "ID Document");
        setupDocumentView(textDocAcademic, tutor.getAcademicRecordUrl(), "Academic Record");
        setupDocumentView(textDocProofReg, tutor.getProofRegistrationUrl(), "Proof of Reg");
    }

    private void setupDocumentView(TextView textView, String url, String docName) {
        int linkColor = ContextCompat.getColor(this, R.color.purple_700); 
        int defaultColor = ContextCompat.getColor(this, R.color.grey_500); 

        if (url != null && !url.isEmpty()) {
            textView.setText(docName + ": Uploaded (Tap to View)");
            textView.setTextColor(linkColor);
            textView.setOnClickListener(v -> viewDocument(url, docName));
            textView.setClickable(true);
            textView.setFocusable(true);
           
        } else {
            textView.setText(docName + ": Not Uploaded");
            textView.setTextColor(defaultColor);
            textView.setOnClickListener(null);
            textView.setClickable(false);
            textView.setFocusable(false);
            textView.setBackground(null); 
        }
    }

    private void viewDocument(String url, String docName) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, docName + " URL is missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Attempting to view document: " + docName + " at URL: " + url);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "No application found to open this link.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to open URL: " + url, e);
            Toast.makeText(this, "Could not open document link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleApproveClick() {
        Log.d(TAG, "Approve button clicked for tutor: " + tutorUid);
        if (tutorUid == null || tutorUid.isEmpty() || currentTutor == null) return;
        updateTutorStatus("approveTutor", "Approving Tutor...", "Tutor Approved Successfully!", "Approval failed");
    }

    private void handleRejectClick() {
        Log.d(TAG, "Reject button clicked for tutor: " + tutorUid);
        if (tutorUid == null || tutorUid.isEmpty() || currentTutor == null) return;
        updateTutorStatus("rejectTutor", "Rejecting Tutor...", "Tutor Rejected Successfully.", "Rejection failed");
    }

    private void updateTutorStatus(String functionName, String progressMessage, String successMessage, String failureBaseMessage) {
        showLoading(true);
        Toast.makeText(this, progressMessage, Toast.LENGTH_SHORT).show();

        Map<String, Object> data = new HashMap<>();
        data.put("tutorId", tutorUid);
       

        mFunctions.getHttpsCallable(functionName)
                .call(data)
                .addOnCompleteListener(task -> {
                    showLoading(false); 

                    if (task.isSuccessful()) {
                        Log.i(TAG, functionName + " function successful for UID: " + tutorUid);
                        Toast.makeText(AdminTutorDetailActivity.this, successMessage, Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK); 
                        finish();
                    } else {
                        
                        if (currentTutor != null && "pending_verification".equals(currentTutor.getProfileStatus())) {
                            buttonApprove.setEnabled(true);
                            buttonReject.setEnabled(true);
                        }

                        Exception e = task.getException();
                        Log.e(TAG, functionName + " function failed for UID: " + tutorUid, e);
                        String errorMessage = failureBaseMessage;
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            Object details = ffe.getDetails();
                            errorMessage = failureBaseMessage + ": (" + ffe.getCode() + ") " + ffe.getMessage() + (details != null ? " Details: " + details : "");
                        } else if (e != null && e.getMessage() != null) {
                            errorMessage = failureBaseMessage + ": " + e.getMessage();
                        }
                        Toast.makeText(AdminTutorDetailActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showError(String message) {
        detailsLayout.setVisibility(View.GONE);
        if (textViewError != null) {
            textViewError.setText(message);
            textViewError.setVisibility(View.VISIBLE);
        }
        if (buttonApprove != null) buttonApprove.setEnabled(false);
        if (buttonReject != null) buttonReject.setEnabled(false);
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            if (detailsLayout != null) detailsLayout.setVisibility(View.GONE);
            if (textViewError != null) textViewError.setVisibility(View.GONE);
            if (buttonApprove != null) buttonApprove.setEnabled(false);
            if (buttonReject != null) buttonReject.setEnabled(false);
        }
        
    }
}