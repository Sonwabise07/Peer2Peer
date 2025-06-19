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
import com.example.peer2peer.Tutor;

import com.google.firebase.auth.FirebaseAuth; 
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminVerifyTutorsActivity extends AppCompatActivity {

    private static final String TAG = "AdminVerifyActivity"; 

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
       
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "Admin not authenticated in onStart(). Redirecting to AdminLoginActivity.");
            Intent intent = new Intent(this, AdminLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        
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
                .whereEqualTo("profileStatus", "pending_verification") 
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    pendingTutorList.clear(); 

                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                try {
                                    Tutor tutor = doc.toObject(Tutor.class);
                                    
                                    if (tutor != null) {
                                        tutor.setUid(doc.getId()); 
                                        pendingTutorList.add(tutor);
                                        Log.d(TAG, "Fetched pending tutor: " + tutor.getUid() +
                                                " Name: " + tutor.getFirstName() + 
                                                " Status: " + doc.getString("profileStatus"));
                                    } else {
                                        Log.w(TAG, "Pending tutor data was null after toObject() for doc: " + doc.getId() + ". Check Tutor.java model and Firestore data.");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing pending tutor document ID: " + doc.getId(), e);
                                    
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

                    
                    if (pendingTutorList.isEmpty()) {
                        textViewEmpty.setText("No tutors currently pending verification.");
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        textViewEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged(); 
                });
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
       
        if (isLoading) {
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (textViewEmpty != null) {
                textViewEmpty.setVisibility(View.GONE);
            }
        }
        
    }

   
}