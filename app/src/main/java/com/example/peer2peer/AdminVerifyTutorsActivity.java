package com.example.peer2peer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peer2peer.adapters.AdminTutorListAdapter;
import com.example.peer2peer.Tutor; // Assuming Tutor model is used for general user/tutor data

import com.google.firebase.auth.FirebaseAuth; // Not strictly needed here unless you check admin auth state again
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminVerifyTutorsActivity extends AppCompatActivity {

    private static final String TAG = "AdminVerifyActivity"; // Consistent TAG

    // UI Elements
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textViewEmpty;

    // Firebase
    private FirebaseFirestore db;

    // Adapter and Data
    private AdminTutorListAdapter adapter;
    private List<Tutor> pendingTutorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_verify_tutors);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance(); // For auth check

        // --- AUTH CHECK - Ensure admin is logged in ---
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "Admin not authenticated in onCreate(). Redirecting to AdminLoginActivity.");
            Intent intent = new Intent(this, AdminLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        // You might also want to re-check the isAdmin claim here if this activity is sensitive,
        // though typically AdminDashboardActivity would have done that.
        // --- END AUTH CHECK ---


        // Find Views
        toolbar = findViewById(R.id.toolbar_verify_tutors);
        recyclerView = findViewById(R.id.recycler_view_pending_tutors);
        progressBar = findViewById(R.id.progress_bar_verify_tutors);
        textViewEmpty = findViewById(R.id.text_view_no_pending_tutors);

        // Setup Toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Verify Tutors");
        }

        // Setup RecyclerView
        pendingTutorList = new ArrayList<>();
        adapter = new AdminTutorListAdapter(pendingTutorList, tutor -> {
            String tutorId = tutor.getUid();
            if (tutorId == null || tutorId.isEmpty()) {
                Log.e(TAG, "Clicked tutor missing required UID!");
                Toast.makeText(this, "Error: Cannot view details (Missing ID).", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Admin clicked on Tutor: " + tutorId + " - " + (tutor.getFirstName() != null ? tutor.getFirstName() : "N/A"));

            Intent intent = new Intent(AdminVerifyTutorsActivity.this, AdminTutorDetailActivity.class);
            intent.putExtra("TUTOR_UID", tutorId);
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fetchPendingTutors();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-check auth state in onStart as well
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "Admin not authenticated in onStart(). Redirecting to AdminLoginActivity.");
            Intent intent = new Intent(this, AdminLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            // No return needed here for onStart if finish() is called.
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

    private void fetchPendingTutors() {
        showLoading(true);
        textViewEmpty.setVisibility(View.GONE); // Hide initially

        Log.d(TAG, "Fetching tutors with role 'Tutor' and profileStatus 'pending_verification'");
        db.collection("users")
                .whereEqualTo("role", "Tutor")
                .whereEqualTo("profileStatus", "pending_verification") // <-- CORRECTED STATUS
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    pendingTutorList.clear(); // Clear before adding new results

                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                try {
                                    Tutor tutor = doc.toObject(Tutor.class);
                                    // Ensure your Tutor model can handle all fields or has @IgnoreExtraProperties
                                    if (tutor != null) {
                                        tutor.setUid(doc.getId()); // Make sure Tutor model has setUid
                                        pendingTutorList.add(tutor);
                                        Log.d(TAG, "Fetched pending tutor: " + tutor.getUid() +
                                                " Name: " + tutor.getFirstName() + // Example field
                                                " Status: " + doc.getString("profileStatus"));
                                    } else {
                                        Log.w(TAG, "Pending tutor data was null after toObject() for doc: " + doc.getId() + ". Check Tutor.java model and Firestore data.");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing pending tutor document ID: " + doc.getId(), e);
                                    // Consider how to handle individual parsing errors - skip item or show error?
                                }
                            }
                            Log.d(TAG, "Total pending tutors fetched: " + pendingTutorList.size());
                        } else {
                            Log.d(TAG, "No tutors found pending verification that match query criteria (snapshots null or empty).");
                        }
                    } else {
                        Log.e(TAG, "Error fetching pending tutors: ", task.getException());
                        Toast.makeText(AdminVerifyTutorsActivity.this, "Error loading pending tutors.", Toast.LENGTH_SHORT).show();
                    }

                    // Update UI based on final list content
                    if (pendingTutorList.isEmpty()) {
                        textViewEmpty.setText("No tutors currently pending verification.");
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged(); // Notify adapter with the updated pendingTutorList
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Only hide RecyclerView when loading starts
        if (isLoading) {
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (textViewEmpty != null) {
                textViewEmpty.setVisibility(View.GONE);
            }
        }
        // Visibility of RecyclerView and textViewEmpty will be set after data fetch in fetchPendingTutors
    }

    // Consider adding onResume to refresh the list if the admin might navigate away and back
    // while a tutor's status could change.
    // @Override
    // protected void onResume() {
    //     super.onResume();
    //     fetchPendingTutors(); // This will refresh the list every time the activity is resumed
    // }
}