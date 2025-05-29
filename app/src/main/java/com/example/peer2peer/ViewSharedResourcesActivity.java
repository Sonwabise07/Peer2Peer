package com.example.peer2peer;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.adapters.ResourceAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ViewSharedResourcesActivity extends AppCompatActivity {

    private static final String TAG = "ViewSharedResourcesAct";

    private RecyclerView recyclerViewSharedResources;
    private ResourceAdapter resourceAdapter;
    private List<Resource> allSharedResourceList;
    private TextView textViewNoSharedResourcesFound;
    private ProgressBar progressBarSharedResources;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentTuteeUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_shared_resources);

        Toolbar toolbar = findViewById(R.id.toolbar_shared_resources);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Shared Resources");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerViewSharedResources = findViewById(R.id.recycler_view_all_shared_resources);
        textViewNoSharedResourcesFound = findViewById(R.id.text_view_no_shared_resources_found);
        progressBarSharedResources = findViewById(R.id.progress_bar_shared_resources);

        allSharedResourceList = new ArrayList<>();
        // Your ResourceAdapter handles clicks internally, so no listener needed here.
        resourceAdapter = new ResourceAdapter(this, allSharedResourceList);
        recyclerViewSharedResources.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSharedResources.setAdapter(resourceAdapter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to view resources.", Toast.LENGTH_LONG).show();
            Log.w(TAG, "User not logged in.");
            // Optionally, redirect to login screen
            // finish();
            textViewNoSharedResourcesFound.setText("Please log in to view resources.");
            textViewNoSharedResourcesFound.setVisibility(View.VISIBLE);
            return;
        }
        currentTuteeUid = currentUser.getUid();
        fetchTutorIdsFromBookings();
    }

    private void fetchTutorIdsFromBookings() {
        progressBarSharedResources.setVisibility(View.VISIBLE);
        textViewNoSharedResourcesFound.setVisibility(View.GONE);
        recyclerViewSharedResources.setVisibility(View.GONE);

        db.collection("bookings")
                .whereEqualTo("tuteeUid", currentTuteeUid)
                // Consider only "confirmed" or "completed" bookings
                .whereIn("bookingStatus", Arrays.asList("confirmed", "completed", "payment_successful")) // Adjust statuses as needed
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Set<String> tutorUids = new HashSet<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String tutorId = document.getString("tutorUid");
                            if (tutorId != null) {
                                tutorUids.add(tutorId);
                            }
                        }

                        if (tutorUids.isEmpty()) {
                            Log.d(TAG, "No relevant bookings found for tutee: " + currentTuteeUid);
                            progressBarSharedResources.setVisibility(View.GONE);
                            textViewNoSharedResourcesFound.setText("No resources found. Resources appear from tutors you've booked with.");
                            textViewNoSharedResourcesFound.setVisibility(View.VISIBLE);
                        } else {
                            Log.d(TAG, "Found bookings with tutors: " + tutorUids);
                            fetchAllResourcesFromTutors(new ArrayList<>(tutorUids));
                        }
                    } else {
                        progressBarSharedResources.setVisibility(View.GONE);
                        Log.e(TAG, "Error fetching bookings: ", task.getException());
                        textViewNoSharedResourcesFound.setText("Could not load your booking information.");
                        textViewNoSharedResourcesFound.setVisibility(View.VISIBLE);
                        Toast.makeText(ViewSharedResourcesActivity.this, "Error fetching booking data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchAllResourcesFromTutors(List<String> tutorUids) {
        allSharedResourceList.clear(); // Clear before fetching
        if (tutorUids.isEmpty()) {
            progressBarSharedResources.setVisibility(View.GONE);
            textViewNoSharedResourcesFound.setText("No tutors found from your bookings.");
            textViewNoSharedResourcesFound.setVisibility(View.VISIBLE);
            return;
        }

        AtomicInteger queriesToComplete = new AtomicInteger(tutorUids.size());
        List<Resource> tempResourceList = new ArrayList<>(); // Collect all resources here

        for (String tutorId : tutorUids) {
            Log.d(TAG, "Fetching resources for tutor: " + tutorId);
            db.collection("users").document(tutorId)
                    .collection("resources")
                    .orderBy("uploadedAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(resourceTask -> {
                        if (resourceTask.isSuccessful() && resourceTask.getResult() != null) {
                            for (QueryDocumentSnapshot document : resourceTask.getResult()) {
                                Resource resource = document.toObject(Resource.class);
                                tempResourceList.add(resource);
                            }
                            Log.d(TAG, "Fetched " + resourceTask.getResult().size() + " resources for tutor " + tutorId);
                        } else {
                            Log.e(TAG, "Error fetching resources for tutor " + tutorId, resourceTask.getException());
                        }

                        // Check if all queries are done
                        if (queriesToComplete.decrementAndGet() == 0) {
                            progressBarSharedResources.setVisibility(View.GONE);
                            if (tempResourceList.isEmpty()) {
                                textViewNoSharedResourcesFound.setText("No resources shared by the tutors you've booked with.");
                                textViewNoSharedResourcesFound.setVisibility(View.VISIBLE);
                                recyclerViewSharedResources.setVisibility(View.GONE);
                            } else {
                                // Sort all collected resources by date (optional, if not already sorted by query)
                                tempResourceList.sort((r1, r2) -> {
                                    if (r1.getUploadedAt() == null && r2.getUploadedAt() == null) return 0;
                                    if (r1.getUploadedAt() == null) return 1; // nulls last
                                    if (r2.getUploadedAt() == null) return -1;
                                    return r2.getUploadedAt().compareTo(r1.getUploadedAt()); // Descending
                                });
                                allSharedResourceList.addAll(tempResourceList);
                                resourceAdapter.notifyDataSetChanged();
                                textViewNoSharedResourcesFound.setVisibility(View.GONE);
                                recyclerViewSharedResources.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
