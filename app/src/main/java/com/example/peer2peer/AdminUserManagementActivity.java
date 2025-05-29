package com.example.peer2peer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView; // Use androidx version

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peer2peer.adapters.AdminUserListAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Added for lowercase conversion

// Implement the listener interface from the adapter
public class AdminUserManagementActivity extends AppCompatActivity implements AdminUserListAdapter.OnUserClickListener {

    private static final String TAG = "AdminUserMgmtActivity";

    // --- UI Elements ---
    private Toolbar toolbar;
    private SearchView searchView;
    private Spinner roleSpinner; // For future filtering
    private RecyclerView recyclerView;
    private TextView textViewNoUsers;
    private ProgressBar progressBar;

    // --- Firebase & Data ---
    private FirebaseFirestore db;
    private AdminUserListAdapter adapter;
    private List<Tutor> allUsersList; // Store the full list fetched from Firestore
    private List<Tutor> filteredUserList; // Store the list currently displayed

    // --- State Variables ---
    // private String currentRoleFilter = "All"; // Keep for later role filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_management);

        // --- Initialize UI ---
        toolbar = findViewById(R.id.toolbar_admin_user_mgmt);
        searchView = findViewById(R.id.search_view_users);
        roleSpinner = findViewById(R.id.spinner_role_filter); // Setup later
        recyclerView = findViewById(R.id.recycler_view_admin_users);
        textViewNoUsers = findViewById(R.id.text_no_users_found);
        progressBar = findViewById(R.id.progress_bar_user_mgmt);

        // --- Initialize Firebase & Data Lists ---
        db = FirebaseFirestore.getInstance();
        allUsersList = new ArrayList<>();
        filteredUserList = new ArrayList<>();

        // --- Setup Toolbar ---
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // --- Setup Components ---
        setupRecyclerView();
        setupSearchView();
        // TODO: Setup Spinner

        // --- Initial Data Load ---
        fetchAllUsersFromFirestore();
    }

    private void setupRecyclerView() {
        adapter = new AdminUserListAdapter(this, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUserList(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUserList(newText);
                return true;
            }
        });

        View closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                searchView.setQuery("", false);
                searchView.setIconified(true);
                searchView.clearFocus();
                filterUserList("");
            });
        } else {
            Log.w(TAG, "Could not find search close button with ID androidx.appcompat.R.id.search_close_btn");
        }
    }

    // --- *** THIS METHOD IS UPDATED *** ---
    // Fetch ALL users initially (ordered by email)
    private void fetchAllUsersFromFirestore() {
        // --- Order by 'email' instead of 'fullName' ---
        Log.d(TAG, "Fetching ALL users from Firestore (ordered by email)...");
        showLoading(true);

        // Query: Fetch all documents from 'users' collection ordered by 'email'
        // Query query = db.collection("users").orderBy("fullName", Query.Direction.ASCENDING); // Old query using non-existent field
        Query query = db.collection("users").orderBy("email", Query.Direction.ASCENDING); // Corrected query

        query.get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    allUsersList.clear();
                    int documentCount = 0;
                    int parsedCount = 0;

                    if (task.isSuccessful() && task.getResult() != null) {
                        documentCount = task.getResult().size();
                        // *** Check this log message carefully when you run ***
                        Log.d(TAG, "Firestore query successful (ordered by email), fetched " + documentCount + " documents.");
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Tutor user = document.toObject(Tutor.class);
                                // Add the missing 'fullName' locally for display/search if needed
                                if (user != null) {
                                    // Combine first and last name if fullName isn't set by default from Firestore
                                    if (user.getFullName() == null || user.getFullName().isEmpty()) {
                                        String first = user.getFirstName() != null ? user.getFirstName() : "";
                                        String last = user.getSurname() != null ? user.getSurname() : "";
                                        user.setFullName((first + " " + last).trim());
                                    }
                                }

                                if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
                                    allUsersList.add(user);
                                    parsedCount++;
                                    // Log name AFTER potentially setting it locally
                                    Log.d(TAG, "Parsed user: " + user.getEmail() + ", Name: " + user.getFullName() + ", UID: " + user.getUid());
                                } else {
                                    Log.w(TAG, "Skipping user document - parsing failed or UID missing. Doc ID: " + document.getId());
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user document: " + document.getId(), e);
                            }
                        }
                        Log.d(TAG, "Finished parsing, successfully parsed " + parsedCount + " of " + documentCount + " users.");
                    } else {
                        Log.e(TAG, "Error fetching all users (ordered by email): ", task.getException());
                        Toast.makeText(AdminUserManagementActivity.this, "Failed to load users: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
                    }
                    filterUserList(searchView.getQuery().toString());
                });
    }
    // --- *** END OF UPDATED METHOD *** ---

    // Filter the displayed list based on search text (checks name and email)
    private void filterUserList(String searchText) {
        String lowerCaseSearchText = searchText.toLowerCase(Locale.getDefault()).trim();
        filteredUserList.clear();

        Log.d(TAG, "Filtering list with search text: '" + lowerCaseSearchText + "'");

        if (TextUtils.isEmpty(lowerCaseSearchText)) {
            filteredUserList.addAll(allUsersList);
            Log.d(TAG, "Search empty, showing all " + allUsersList.size() + " users.");
        } else {
            for (Tutor user : allUsersList) {
                boolean nameMatch = false;
                boolean emailMatch = false;

                // Check Full Name (case-insensitive contains)
                // Make sure getFullName() is available in Tutor.java or was set in fetchAllUsersFromFirestore
                String fullName = user.getFullName();
                if (fullName != null && fullName.toLowerCase(Locale.getDefault()).contains(lowerCaseSearchText)) {
                    nameMatch = true;
                }

                // Check Email (case-insensitive contains)
                String email = user.getEmail();
                if (email != null && email.toLowerCase(Locale.getDefault()).contains(lowerCaseSearchText)) {
                    emailMatch = true;
                }

                if (nameMatch || emailMatch) {
                    filteredUserList.add(user);
                }
            }
            Log.d(TAG, "Filtering complete, found " + filteredUserList.size() + " matches.");
        }

        adapter.setUsers(filteredUserList);

        if (filteredUserList.isEmpty()) {
            if (TextUtils.isEmpty(lowerCaseSearchText) && allUsersList.isEmpty()) {
                textViewNoUsers.setText("No users found in database.");
            } else if (allUsersList.isEmpty()){
                textViewNoUsers.setText("No users found in database.");
            }
            else {
                textViewNoUsers.setText("No users match '" + searchText + "'.");
            }
            textViewNoUsers.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textViewNoUsers.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }


    // Handle Clicks on RecyclerView Items (from OnUserClickListener interface)
    @Override
    public void onUserClick(Tutor user) {
        if (user == null) {
            Log.e(TAG, "onUserClick called with null user object.");
            Toast.makeText(this, "Error: Invalid user data.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userUid = user.getUid();
        if (userUid == null || userUid.isEmpty()) {
            Log.e(TAG, "onUserClick: User UID is null or empty for user: " + user.getFullName());
            Toast.makeText(this, "Error: Cannot open details, user ID missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "User clicked: " + user.getFullName() + " (UID: " + userUid + ")");
        Intent intent = new Intent(this, AdminUserDetailActivity.class);
        intent.putExtra(AdminUserDetailActivity.EXTRA_USER_UID, userUid);
        startActivity(intent);
    }


    // Handle Toolbar Back Arrow
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper to show/hide loading state
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            textViewNoUsers.setVisibility(View.GONE);
        }
    }

} // End of Activity Class