package com.example.peer2peer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peer2peer.adapters.ReviewListAdapter; // Import the new adapter
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewReviewsActivity extends AppCompatActivity {

    private static final String TAG = "ViewReviewsActivity";
    public static final String EXTRA_TUTOR_UID = "EXTRA_TUTOR_UID"; // Key for intent extra
    public static final String EXTRA_TUTOR_NAME = "EXTRA_TUTOR_NAME"; // Key for intent extra

    private String tutorUid;
    private String tutorName;

    private RecyclerView recyclerViewReviews;
    private ReviewListAdapter adapter;
    private List<Review> reviewList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView textViewNoReviews;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reviews);

        db = FirebaseFirestore.getInstance();

        // Get Tutor UID and Name from Intent
        tutorUid = getIntent().getStringExtra(EXTRA_TUTOR_UID);
        tutorName = getIntent().getStringExtra(EXTRA_TUTOR_NAME);

        if (tutorUid == null || tutorUid.isEmpty()) {
            Log.e(TAG, "Tutor UID not passed in Intent.");
            Toast.makeText(this, "Error: Tutor information missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI
        toolbar = findViewById(R.id.toolbar_view_reviews);
        recyclerViewReviews = findViewById(R.id.recyclerView_reviews);
        progressBar = findViewById(R.id.progressBar_view_reviews);
        textViewNoReviews = findViewById(R.id.textView_no_reviews);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Set title, fallback if name wasn't passed
            getSupportActionBar().setTitle("Reviews for " + (tutorName != null ? tutorName : "Tutor"));
        }

        // Setup RecyclerView
        reviewList = new ArrayList<>();
        adapter = new ReviewListAdapter(this, reviewList);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReviews.setAdapter(adapter);

        // Fetch Reviews
        fetchReviews();
    }

    private void fetchReviews() {
        showLoading(true);
        Log.d(TAG, "Fetching reviews for tutor: " + tutorUid);

        db.collection("users").document(tutorUid).collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Show newest reviews first
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        reviewList.clear();
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Review review = document.toObject(Review.class);
                                    reviewList.add(review);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing review document: " + document.getId(), e);
                                }
                            }
                            Log.d(TAG, "Fetched " + reviewList.size() + " reviews.");
                            adapter.notifyDataSetChanged();
                        } else {
                            Log.d(TAG, "No reviews found for tutor: " + tutorUid);
                        }
                        updateEmptyView();
                    } else {
                        Log.e(TAG, "Error fetching reviews: ", task.getException());
                        Toast.makeText(ViewReviewsActivity.this, "Failed to load reviews.", Toast.LENGTH_SHORT).show();
                        updateEmptyView(); // Show empty view with error text potentially
                        textViewNoReviews.setText("Could not load reviews.");
                    }
                });
    }

    private void updateEmptyView() {
        if (reviewList.isEmpty()) {
            textViewNoReviews.setVisibility(View.VISIBLE);
            recyclerViewReviews.setVisibility(View.GONE);
        } else {
            textViewNoReviews.setVisibility(View.GONE);
            recyclerViewReviews.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // Hide/show list based on loading state only if list is not empty
        recyclerViewReviews.setVisibility(isLoading ? View.GONE : (reviewList.isEmpty() ? View.GONE : View.VISIBLE));
        // Hide/show empty text based on loading state only if list is empty
        textViewNoReviews.setVisibility(isLoading ? View.GONE : (reviewList.isEmpty() ? View.VISIBLE : View.GONE));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to previous
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}