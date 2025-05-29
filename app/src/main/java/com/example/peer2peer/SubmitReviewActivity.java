package com.example.peer2peer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity; // Import Activity for RESULT_OK
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

// Required imports for Firestore and Tasks
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

public class SubmitReviewActivity extends AppCompatActivity {

    private static final String TAG = "SubmitReviewActivity";

    // Intent Extras Keys
    public static final String EXTRA_BOOKING_ID = "EXTRA_BOOKING_ID";
    public static final String EXTRA_TUTOR_UID = "EXTRA_TUTOR_UID";
    public static final String EXTRA_TUTOR_NAME = "EXTRA_TUTOR_NAME";

    // Received Data
    private String bookingId;
    private String tutorUid;
    private String tutorName;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // UI Elements
    private Toolbar toolbar;
    private TextView textViewPrompt;
    private RatingBar ratingBar;
    private EditText editTextReview;
    private Button buttonSubmit;
    private ProgressBar progressBar;

    // To store current user's name
    private String currentTuteeName = "Anonymous"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_review);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "User not logged in. Cannot submit review.");
            Toast.makeText(this, "You must be logged in to submit a review.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        tutorUid = getIntent().getStringExtra(EXTRA_TUTOR_UID);
        tutorName = getIntent().getStringExtra(EXTRA_TUTOR_NAME);

        if (TextUtils.isEmpty(bookingId) || TextUtils.isEmpty(tutorUid)) {
            Log.e(TAG, "Required data (bookingId or tutorUid) not received from Intent.");
            Toast.makeText(this, "Error: Missing booking information.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar_submit_review);
        textViewPrompt = findViewById(R.id.textView_rate_prompt);
        ratingBar = findViewById(R.id.ratingBar_submit);
        editTextReview = findViewById(R.id.textInputEditText_review);
        buttonSubmit = findViewById(R.id.button_submit_review);
        progressBar = findViewById(R.id.progressBar_submit_review);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Submit Review");
        }

        textViewPrompt.setText("Rate your session with " + (tutorName != null && !tutorName.isEmpty() ? tutorName : "the tutor"));

        fetchCurrentUserName();

        buttonSubmit.setOnClickListener(v -> attemptReviewSubmission());
    }

    private void fetchCurrentUserName() {
        if (currentUser == null) return;
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        if (fullName != null && !fullName.trim().isEmpty()){
                            currentTuteeName = fullName.trim();
                        } else {
                            String firstName = documentSnapshot.getString("firstName");
                            String surname = documentSnapshot.getString("surname");
                            currentTuteeName = ((firstName != null ? firstName : "") + " " + (surname != null ? surname : "")).trim();
                        }
                        if (currentTuteeName.isEmpty()) {
                            currentTuteeName = "Anonymous Tutee";
                        }
                        Log.d(TAG, "Fetched current user name: " + currentTuteeName);
                    } else {
                        Log.w(TAG, "Current user document not found.");
                        currentTuteeName = "Anonymous Tutee";
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user name", e);
                    currentTuteeName = "Anonymous Tutee";
                });
    }


    private void attemptReviewSubmission() {
        float rating = ratingBar.getRating();
        String reviewText = editTextReview.getText().toString().trim();

        if (rating <= 0) {
            Toast.makeText(this, "Please select a star rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        Review review = new Review(
                rating,
                reviewText,
                currentUser.getUid(),
                currentTuteeName,
                bookingId,
                tutorUid
        );
        // Assuming your Review model's constructor handles Timestamp or you set it via a setter
        // If your Review model expects Timestamp in constructor, adjust accordingly.
        // For now, assuming a setter or that it handles it internally like @ServerTimestamp
        // review.setTimestamp(Timestamp.now()); // If you have this setter and need to set client-side time

        submitReviewBatch(review);
    }

    private void submitReviewBatch(Review review) {
        WriteBatch batch = db.batch();

        if (review.getTutorUid() == null || review.getTutorUid().isEmpty()){
            Log.e(TAG, "Tutor UID is missing in the review object. Cannot save review.");
            Toast.makeText(this, "Internal error: Missing tutor ID for review.", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }
        if (review.getBookingId() == null || review.getBookingId().isEmpty()){
            Log.e(TAG, "Booking ID is missing in the review object. Cannot save review or update booking.");
            Toast.makeText(this, "Internal error: Missing booking ID for review.", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }


        DocumentReference reviewRef = db.collection("users").document(review.getTutorUid())
                .collection("reviews").document(review.getBookingId()); // Using bookingId also as review doc ID
        batch.set(reviewRef, review);

        DocumentReference bookingRef = db.collection("bookings").document(review.getBookingId());
        batch.update(bookingRef, "isRated", true); // MODIFIED: Use "isRated"

        Log.d(TAG, "Attempting to commit initial batch (create review + update booking 'isRated')");

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Initial batch (create review + update booking 'isRated') successful.");
                    aggregateRatingOnClient(review.getTutorUid(), review.getRating());
                    Toast.makeText(SubmitReviewActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    setResult(Activity.RESULT_OK); // Use Activity.RESULT_OK
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error submitting initial review batch", e);
                    Toast.makeText(SubmitReviewActivity.this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showLoading(false);
                    // Set a different result if the operation failed to inform the calling activity
                    setResult(Activity.RESULT_CANCELED);
                    // finish(); // Optionally, you might not want to finish if it fails, to allow retry
                });
    }

    private void aggregateRatingOnClient(String tutorId, float newRatingValue) {
        Log.d(TAG, "Attempting client-side rating aggregation for tutor: " + tutorId);
        if (tutorId == null || tutorId.isEmpty()) {
            Log.e(TAG, "Client-side aggregation skipped: tutorId is null or empty.");
            return;
        }
        final DocumentReference tutorRef = db.collection("users").document(tutorId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot tutorSnap = transaction.get(tutorRef);

            if (!tutorSnap.exists()) {
                Log.e(TAG, "Tutor document not found during aggregation: " + tutorId + ". Cannot update rating.");
                throw new FirebaseFirestoreException("Tutor document " + tutorId + " not found.",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }

            double currentAvg = 0.0;
            if (tutorSnap.contains("averageRating") && tutorSnap.get("averageRating") instanceof Number) {
                currentAvg = tutorSnap.getDouble("averageRating");
                if (currentAvg < 0) currentAvg = 0.0; // Ensure non-negative
            }

            long currentCount = 0;
            if (tutorSnap.contains("ratingCount") && tutorSnap.get("ratingCount") instanceof Number) {
                currentCount = tutorSnap.getLong("ratingCount");
                if (currentCount < 0) currentCount = 0; // Ensure non-negative
            }

            long newCount = currentCount + 1;
            double newAverage = ((currentAvg * currentCount) + (double)newRatingValue) / newCount;
            newAverage = Math.round(newAverage * 10.0) / 10.0; // Round to 1 decimal place

            Log.d(TAG, "Client-side calculation: CurrentCount=" + currentCount + ", CurrentAvg=" + currentAvg +
                    ", NewRating=" + newRatingValue + ", NewCount=" + newCount + ", NewAvg=" + newAverage);

            transaction.update(tutorRef, "averageRating", newAverage);
            transaction.update(tutorRef, "ratingCount", newCount);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Client-side aggregation transaction success for tutor: " + tutorId);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Client-side aggregation transaction failure for tutor: " + tutorId, e);
            // Optionally, inform the user or try a fallback if this critical update fails.
            // For now, just logging.
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonSubmit.setEnabled(!isLoading);
        ratingBar.setEnabled(!isLoading);
        editTextReview.setEnabled(!isLoading);
    }
}