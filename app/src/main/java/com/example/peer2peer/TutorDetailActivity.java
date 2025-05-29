package com.example.peer2peer; // Ensure this matches your package

// --- Android Core Imports ---
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

// --- AndroidX/Material Imports ---
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.button.MaterialButton;
import com.bumptech.glide.Glide;

// --- Firebase Imports ---
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

// --- Java Util Imports ---
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TutorDetailActivity extends AppCompatActivity {

    private static final String TAG = "TutorDetailActivity";
    public static final String EXTRA_TUTOR_UID = "TUTOR_UID";

    private ImageView imageViewProfile;
    private TextView textViewName, textViewRate, textViewRatingValue, textViewBio, textViewQualifications;
    private ChipGroup chipGroupModules, chipGroupLanguages;
    private RatingBar ratingBar;
    private MaterialButton buttonViewAvailability;
    private MaterialButton buttonMessageTutor;
    private MaterialButton buttonReportTutor;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private String tutorUid;
    private String tutorFullName;
    private Tutor currentTutorData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_detail);

        db = FirebaseFirestore.getInstance();

        toolbar = findViewById(R.id.toolbar_tutor_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        imageViewProfile = findViewById(R.id.image_detail_profile);
        textViewName = findViewById(R.id.text_detail_name);
        textViewRate = findViewById(R.id.text_detail_rate);
        ratingBar = findViewById(R.id.rating_detail);
        textViewRatingValue = findViewById(R.id.text_detail_rating_value);
        textViewBio = findViewById(R.id.text_detail_bio);
        chipGroupModules = findViewById(R.id.chip_group_modules);
        textViewQualifications = findViewById(R.id.text_detail_qualifications);
        chipGroupLanguages = findViewById(R.id.chip_group_languages);
        buttonViewAvailability = findViewById(R.id.button_view_availability);
        buttonMessageTutor = findViewById(R.id.button_message_tutor);
        buttonReportTutor = findViewById(R.id.buttonReportTutor);

        tutorUid = getIntent().getStringExtra(EXTRA_TUTOR_UID);
        if (tutorUid == null || tutorUid.isEmpty()) {
            Log.e(TAG, "No Tutor UID provided in Intent extras.");
            Toast.makeText(this, "Error: Tutor ID missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Received Tutor UID: " + tutorUid);
        // Initial load can be triggered by onResume or here.
        // If you want it to load immediately in onCreate as well as refresh in onResume:
        // loadTutorData(tutorUid);
        // Otherwise, onResume will handle the first load if it's the first time.

        buttonViewAvailability.setOnClickListener(v -> {
            Log.d(TAG,"View Availability button clicked for tutor: " + tutorUid);
            Intent intent = new Intent(TutorDetailActivity.this, SelectSlotActivity.class);
            intent.putExtra(SelectSlotActivity.EXTRA_TUTOR_UID, tutorUid);
            if (currentTutorData != null && currentTutorData.getFullName() != null) {
                intent.putExtra("TUTOR_NAME", currentTutorData.getFullName());
            }
            startActivity(intent);
        });

        buttonMessageTutor.setOnClickListener(v -> {
            FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentFirebaseUser == null) {
                Toast.makeText(TutorDetailActivity.this, "You need to be logged in to message a tutor.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentTutorData != null && currentTutorData.getUid() != null && currentTutorData.getFullName() != null) {
                Intent intent = new Intent(TutorDetailActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CHAT_PARTNER_ID, currentTutorData.getUid());
                intent.putExtra(ChatActivity.EXTRA_CHAT_PARTNER_NAME, currentTutorData.getFullName());
                startActivity(intent);
            } else {
                Toast.makeText(TutorDetailActivity.this, "Tutor details are not fully loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Cannot start chat. Tutor data is not fully loaded.");
            }
        });

        if (buttonReportTutor != null) {
            buttonReportTutor.setOnClickListener(v -> {
                if (currentTutorData != null && tutorUid != null && !tutorUid.isEmpty()) {
                    showReportDialog();
                } else {
                    Toast.makeText(TutorDetailActivity.this, "Tutor information is unavailable to report.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "Report Tutor button (buttonReportTutor) not found in layout. Check ID.");
        }

        View.OnClickListener viewReviewsClickListener = v -> {
            if (tutorUid != null && !tutorUid.isEmpty()) {
                Log.d(TAG, "Rating bar or review count clicked for tutor: " + tutorUid);
                Intent intent = new Intent(TutorDetailActivity.this, ViewReviewsActivity.class);
                intent.putExtra(ViewReviewsActivity.EXTRA_TUTOR_UID, tutorUid);
                String nameToPass = (currentTutorData != null && currentTutorData.getFullName() != null && !currentTutorData.getFullName().isEmpty())
                        ? currentTutorData.getFullName()
                        : "Tutor Reviews";
                intent.putExtra(ViewReviewsActivity.EXTRA_TUTOR_NAME, nameToPass);
                startActivity(intent);
            } else {
                Log.e(TAG, "Tutor UID is null, cannot open reviews.");
                Toast.makeText(TutorDetailActivity.this, "Cannot load reviews at the moment.", Toast.LENGTH_SHORT).show();
            }
        };
        ratingBar.setOnClickListener(viewReviewsClickListener);
        textViewRatingValue.setOnClickListener(viewReviewsClickListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tutorUid != null && !tutorUid.isEmpty()) {
            Log.d(TAG, "onResume: Reloading tutor data for " + tutorUid);
            loadTutorData(tutorUid);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadTutorData(String uid) {
        Log.d(TAG, "loadTutorData called for UID: " + uid);
        DocumentReference tutorDocRef = db.collection("users").document(uid);
        tutorDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    Log.d(TAG, "Tutor document found. Populating UI.");
                    currentTutorData = document.toObject(Tutor.class);
                    if (currentTutorData != null) {
                        currentTutorData.setUid(document.getId());
                        this.tutorFullName = currentTutorData.getFullName();
                        if (getSupportActionBar() != null && this.tutorFullName != null && !this.tutorFullName.isEmpty()) {
                            getSupportActionBar().setTitle(this.tutorFullName);
                        }
                        populateUi();
                    } else {
                        Log.e(TAG, "currentTutorData is null after document.toObject(Tutor.class)");
                        Toast.makeText(TutorDetailActivity.this, "Failed to parse tutor details.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "No such tutor document found for UID: " + uid);
                    Toast.makeText(TutorDetailActivity.this, "Tutor profile not found.", Toast.LENGTH_SHORT).show();
                    if (buttonReportTutor != null) buttonReportTutor.setEnabled(false);
                }
            } else {
                Log.e(TAG, "Failed to fetch tutor document: ", task.getException());
                Toast.makeText(TutorDetailActivity.this, "Failed to load tutor details.", Toast.LENGTH_SHORT).show();
                if (buttonReportTutor != null) buttonReportTutor.setEnabled(false);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void populateUi() {
        if (currentTutorData == null) {
            Log.e(TAG, "populateUi called but currentTutorData is null.");
            return;
        }

        String name = currentTutorData.getFullName();
        if (name == null || name.trim().isEmpty()) name = "Tutor Name";
        textViewName.setText(name);
        if (getSupportActionBar() != null && !name.equals("Tutor Name")) {
            getSupportActionBar().setTitle(name);
        }

        if (currentTutorData.getProfileImageUrl() != null && !currentTutorData.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentTutorData.getProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(imageViewProfile);
        } else {
            imageViewProfile.setImageResource(R.drawable.ic_person);
        }

        Double hourlyRate = currentTutorData.getHourlyRate();
        if (hourlyRate != null) {
            try { textViewRate.setText(String.format(Locale.getDefault(), getString(R.string.rate_format), hourlyRate)); }
            catch (Exception e) { textViewRate.setText(String.format(Locale.getDefault(), "R %.2f /hr", hourlyRate)); }
            textViewRate.setVisibility(View.VISIBLE);
        } else {
            try { textViewRate.setText(getString(R.string.rate_not_available)); }
            catch (Exception e) { textViewRate.setText("Rate N/A"); }
            textViewRate.setVisibility(View.VISIBLE);
        }

        ratingBar.setVisibility(View.VISIBLE);
        textViewRatingValue.setVisibility(View.VISIBLE);
        Double averageRating = currentTutorData.getAverageRating();
        Long ratingCount = currentTutorData.getRatingCount();

        Log.d(TAG, "Populating UI with Average Rating: " + averageRating + ", Count: " + ratingCount);

        if(averageRating != null && averageRating > 0) {
            ratingBar.setRating(averageRating.floatValue());
            String ratingValText = String.format(Locale.getDefault(), "%.1f", averageRating);
            if (ratingCount != null && ratingCount > 0) {
                try { textViewRatingValue.setText(getResources().getQuantityString(R.plurals.review_count_format, ratingCount.intValue(), ratingValText, ratingCount.intValue())); }
                catch (Exception e) { textViewRatingValue.setText(String.format(Locale.getDefault(), "%s (%d review%s)", ratingValText, ratingCount, ratingCount > 1 ? "s" : "")); }
                ratingBar.setClickable(true);
                textViewRatingValue.setClickable(true);
            } else {
                textViewRatingValue.setText(ratingValText + " (No reviews yet)");
                ratingBar.setClickable(false);
                textViewRatingValue.setClickable(false);
            }
        } else {
            ratingBar.setRating(0f);
            try { textViewRatingValue.setText(getString(R.string.no_ratings_yet)); }
            catch (Exception e) { textViewRatingValue.setText("(No ratings yet)"); }
            ratingBar.setClickable(false);
            textViewRatingValue.setClickable(false);
        }

        textViewBio.setText(currentTutorData.getBio() != null && !currentTutorData.getBio().isEmpty() ? currentTutorData.getBio() : "No biography provided.");
        textViewQualifications.setText(currentTutorData.getQualifications() != null && !currentTutorData.getQualifications().isEmpty() ? currentTutorData.getQualifications() : "No qualifications listed.");

        List<String> modules = currentTutorData.getModulesToTutor();
        chipGroupModules.removeAllViews();
        if (modules != null && !modules.isEmpty()) { for (String module : modules) { Chip chip = new Chip(this); chip.setText(module); chip.setClickable(false); chipGroupModules.addView(chip); } }
        else { TextView noModulesText = new TextView(this); noModulesText.setText("No modules specified."); chipGroupModules.addView(noModulesText); }

        List<String> languages = currentTutorData.getTutoringLanguages();
        chipGroupLanguages.removeAllViews();
        if (languages != null && !languages.isEmpty()) { for (String language : languages) { Chip chip = new Chip(this); chip.setText(language); chip.setClickable(false); chipGroupLanguages.addView(chip); } }
        else { TextView noLanguagesText = new TextView(this); noLanguagesText.setText("Languages not specified."); chipGroupLanguages.addView(noLanguagesText); }
    }

    private void showReportDialog() {
        if (currentTutorData == null || tutorUid == null) {
            Toast.makeText(this, "Cannot report, tutor data missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_submit_report, null);
        builder.setView(dialogView);
        builder.setTitle("Report " + currentTutorData.getFullName());

        final Spinner spinnerReportReason = dialogView.findViewById(R.id.spinnerReportReason);
        final EditText editTextReportDetails = dialogView.findViewById(R.id.editTextReportDetails);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.report_reasons_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReportReason.setAdapter(adapter);

        builder.setPositiveButton("Submit", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog reportDialog = builder.create();
        reportDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = reportDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String reasonCategory = spinnerReportReason.getSelectedItem().toString();
                String reasonDetails = editTextReportDetails.getText().toString().trim();

                if (spinnerReportReason.getSelectedItemPosition() == 0 && "Select a reason...".equals(reasonCategory)) {
                    Toast.makeText(TutorDetailActivity.this, "Please select a reason.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (TextUtils.isEmpty(reasonDetails) && "Other (Specify in details)".equalsIgnoreCase(reasonCategory)) {
                    editTextReportDetails.setError("Details are required for 'Other'.");
                    return;
                }
                if (TextUtils.isEmpty(reasonDetails) && (!"No Show for Session".equalsIgnoreCase(reasonCategory) && !"Misleading Profile Information".equalsIgnoreCase(reasonCategory))) {
                    editTextReportDetails.setError("Please provide some details.");
                    return;
                }
                editTextReportDetails.setError(null);

                submitReportToFirestore(tutorUid, currentTutorData.getFullName(), reasonCategory, reasonDetails);
                reportDialog.dismiss();
            });
        });
        reportDialog.show();
    }

    private void submitReportToFirestore(String reportedTutorUid, String reportedTutorName,
                                         String reasonCategory, String reasonDetails) {
        FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentFirebaseUser == null) {
            Toast.makeText(this, "You must be logged in to submit a report.", Toast.LENGTH_SHORT).show();
            return;
        }
        String reporterUid = currentFirebaseUser.getUid();
        String reporterEmail = currentFirebaseUser.getEmail();

        Toast.makeText(this, "Submitting report...", Toast.LENGTH_SHORT).show();

        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reporterUid", reporterUid);
        if (reporterEmail != null) { reportData.put("reporterEmail", reporterEmail); }
        reportData.put("reportedTutorUid", reportedTutorUid);
        if (reportedTutorName != null && !reportedTutorName.isEmpty()) { reportData.put("reportedTutorName", reportedTutorName); }
        reportData.put("reasonCategory", reasonCategory);
        reportData.put("reasonDetails", reasonDetails);
        reportData.put("timestamp", FieldValue.serverTimestamp());
        reportData.put("reportStatus", "new");

        FirebaseFirestore.getInstance().collection("reports")
                .add(reportData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Report submitted successfully: " + documentReference.getId());
                    Toast.makeText(TutorDetailActivity.this, "Report submitted successfully. Thank you.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting report", e);
                    Toast.makeText(TutorDetailActivity.this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}