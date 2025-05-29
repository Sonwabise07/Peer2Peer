package com.example.peer2peer; // Ensure this package declaration is correct

// --- Android/Java Imports ---
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
// Import MaterialButton
import com.google.android.material.button.MaterialButton;
import android.widget.Button; // Keep for logout button if it's a standard Button
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
// Removed: androidx.appcompat.app.ActionBarDrawerToggle; (Not used in provided code)
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
// Removed: androidx.drawerlayout.widget.DrawerLayout; (Not used in provided code)
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// --- Custom Class Imports ---
import com.example.peer2peer.fragments.FilterTutorsBottomSheetDialogFragment;
import com.example.peer2peer.adapters.TutorListAdapter;
import com.example.peer2peer.ChatListActivity; // For the "My Chats" button
import com.example.peer2peer.ChatbotActivity; // <-- IMPORT FOR CHATBOT
import com.google.android.material.chip.Chip;

// --- Firebase/Task Imports ---
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FieldPath;


// --- Java Util Imports ---
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
// Removed: import java.util.Map; (Not directly used)
import java.util.Objects;
import java.util.stream.Collectors;

public class TuteeDashboardActivity extends AppCompatActivity
        implements TutorListAdapter.OnItemClickListener, FilterTutorsBottomSheetDialogFragment.FilterListener {

    private static final String TAG = "TuteeDashboardActivity";

    // --- UI Elements ---
    private Toolbar toolbar;
    private SearchView searchViewTutors;
    private Chip chipFilterTutors;
    private MaterialButton myBookingsButton;
    private MaterialButton buttonViewSharedResources;
    private MaterialButton buttonMyChats;
    private Button buttonLogout;

    private RecyclerView recyclerViewTutors;
    private ProgressBar progressBarLoading;
    private TextView textViewNoTutorsFound;

    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // --- RecyclerView Components & Data ---
    private TutorListAdapter tutorListAdapter;
    private List<Tutor> allFetchedTutors;
    private List<Tutor> currentlyDisplayedTutors;

    // --- Filter State Variables ---
    private String currentSearchQuery = "";
    private Float currentMaxRateFilter = null;
    private String currentLanguageFilter = null;
    private Float currentMinRatingFilter = null;

    // --- Constants ---
    private static final float DEFAULT_MAX_RATE = 500f; // Example default
    private static final float DEFAULT_MIN_RATING = 0f; // Example default


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutee_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Auth Check - Placed early
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not authenticated in onCreate(). Redirecting to LoginActivity.");
            Intent intent = new Intent(TuteeDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Important to prevent rest of onCreate from running
        }

        toolbar = findViewById(R.id.toolbar_tutee_dashboard);
        searchViewTutors = findViewById(R.id.search_view_tutors);
        chipFilterTutors = findViewById(R.id.chip_filter_tutors);
        buttonLogout = findViewById(R.id.button_dashboard_logout);
        myBookingsButton = findViewById(R.id.button_my_bookings);
        buttonViewSharedResources = findViewById(R.id.button_view_shared_resources);
        recyclerViewTutors = findViewById(R.id.recycler_view_tutors);
        progressBarLoading = findViewById(R.id.progress_bar_loading);
        textViewNoTutorsFound = findViewById(R.id.text_no_tutors_found);
        buttonMyChats = findViewById(R.id.button_my_chats);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Find a Tutor");
        }

        allFetchedTutors = new ArrayList<>();
        currentlyDisplayedTutors = new ArrayList<>();
        tutorListAdapter = new TutorListAdapter(this, currentlyDisplayedTutors, this);

        if (recyclerViewTutors != null) {
            recyclerViewTutors.setLayoutManager(new LinearLayoutManager(this));
            recyclerViewTutors.setAdapter(tutorListAdapter);
        } else {
            Log.e(TAG, "RecyclerView (recycler_view_tutors) not found in layout!");
        }

        setupButtonClickListeners();
        setupSearchAndFilterListeners();
        loadBaseTutorData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Second Auth Check - good practice for activities that require login
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not authenticated in onStart(). Redirecting to LoginActivity.");
            Intent intent = new Intent(TuteeDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            // No return needed here as super.onStart() is already called and activity will finish.
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_toolbar_menu, menu); // Assuming this menu exists
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_open_chatbot) { // Ensure this ID matches your menu XML
            Log.d(TAG, "Chatbot menu item selected. Launching ChatbotActivity.");
            // --- CORRECTED INTENT TO LAUNCH CHATBOTACTIVITY ---
            Intent intent = new Intent(TuteeDashboardActivity.this, ChatbotActivity.class);
            startActivity(intent);
            return true;
        }
        // Example for another item, if you had one for "Profile"
        // else if (itemId == R.id.action_view_profile) {
        //     startActivity(new Intent(TuteeDashboardActivity.this, TuteeProfileActivity.class));
        //     return true;
        // }
        return super.onOptionsItemSelected(item);
    }

    private void setupButtonClickListeners() {
        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
        if (myBookingsButton != null) {
            myBookingsButton.setOnClickListener(v ->
                    startActivity(new Intent(TuteeDashboardActivity.this, TuteeBookingsActivity.class))
            );
        }
        if (buttonViewSharedResources != null) {
            buttonViewSharedResources.setOnClickListener(v -> {
                Intent intent = new Intent(TuteeDashboardActivity.this, ViewSharedResourcesActivity.class);
                startActivity(intent);
            });
        }
        if (buttonMyChats != null) {
            buttonMyChats.setOnClickListener(v -> {
                Intent intent = new Intent(TuteeDashboardActivity.this, ChatListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("No", null)
                .show();
    }

    private void performLogout() {
        FirebaseUser user = mAuth.getCurrentUser();
        String userRoleForIntent = MainActivity.ROLE_TUTEE; // Default or determine actual role if needed for LoginActivity differentiation

        // If you store the role locally (e.g., in SharedPreferences) upon login, you could fetch it here.
        // For now, assuming TuteeDashboard always logs out a "Tutee" for the LoginActivity's title context.
        // Or, more robustly, don't pass a role to LoginActivity on logout, and let LoginActivity handle it.
        // We already updated LoginActivity to handle a null preSelectedUserRole.

        mAuth.signOut();
        Toast.makeText(TuteeDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(TuteeDashboardActivity.this, LoginActivity.class);
        // No need to pass EXTRA_USER_ROLE from here if LoginActivity handles null preSelectedRole correctly
        // intent.putExtra(MainActivity.EXTRA_USER_ROLE, userRoleForIntent);
        // Log.d(TAG, "Logging out. LoginActivity will determine role flow.");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupSearchAndFilterListeners() {
        if (chipFilterTutors != null) {
            chipFilterTutors.setOnClickListener(v -> {
                Float maxRate = currentMaxRateFilter != null ? currentMaxRateFilter : DEFAULT_MAX_RATE;
                String lang = currentLanguageFilter != null ? currentLanguageFilter : "";
                Float minRating = currentMinRatingFilter != null ? currentMinRatingFilter : DEFAULT_MIN_RATING;

                FilterTutorsBottomSheetDialogFragment filterDialog =
                        FilterTutorsBottomSheetDialogFragment.newInstance(maxRate, lang, minRating);
                filterDialog.show(getSupportFragmentManager(), FilterTutorsBottomSheetDialogFragment.TAG);
            });
        }

        if (searchViewTutors != null) {
            searchViewTutors.setQueryHint("Search by module or tutor name...");
            searchViewTutors.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchViewTutors.clearFocus(); // Hide keyboard
                    currentSearchQuery = query.trim();
                    applyAllClientFilters();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    currentSearchQuery = newText.trim();
                    applyAllClientFilters();
                    return true;
                }
            });

            // Handle search view close button
            View closeButton = searchViewTutors.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> {
                    searchViewTutors.setQuery("", false); // Clear text
                    searchViewTutors.clearFocus();       // Remove focus, hide keyboard
                    currentSearchQuery = "";             // Reset query state
                    applyAllClientFilters();             // Re-apply filters (which should show all if query is empty)
                });
            }
        }
    }

    private void loadBaseTutorData() {
        Log.d(TAG, "Loading base tutor data...");
        showLoading(true);
        Query query = db.collection("users")
                .whereEqualTo("role", "Tutor")
                .whereEqualTo("profileStatus", "verified");

        // Conditional ordering for hourlyRate. Order by documentId as a secondary sort for stable pagination if ever implemented.
        if (currentMaxRateFilter != null && currentMaxRateFilter < DEFAULT_MAX_RATE) {
            query = query.whereLessThanOrEqualTo("hourlyRate", currentMaxRateFilter)
                    .orderBy("hourlyRate", Query.Direction.ASCENDING)
                    .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING); // Add secondary sort
        } else {
            // Default sort if no rate filter is applied or if it's the default max.
            // Consider a more meaningful default sort, e.g., averageRating or name.
            // For now, just documentId to ensure some order.
            query = query.orderBy(FieldPath.documentId(), Query.Direction.ASCENDING);
        }


        query.get(Source.SERVER) // Prefer server data for freshness
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    allFetchedTutors.clear(); // Clear previous full list
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            Log.d(TAG, "Successfully fetched " + querySnapshot.size() + " tutors from server.");
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                try {
                                    Tutor tutor = document.toObject(Tutor.class);
                                    tutor.setUid(document.getId()); // Manually set UID from document ID

                                    // Ensure fullName is populated if not directly in Firestore object
                                    if (TextUtils.isEmpty(tutor.getFullName())) {
                                        String first = tutor.getFirstName() != null ? tutor.getFirstName() : "";
                                        String last = tutor.getSurname() != null ? tutor.getSurname() : "";
                                        tutor.setFullName((first + " " + last).trim());
                                    }
                                    allFetchedTutors.add(tutor);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error converting document to Tutor object: " + document.getId(), e);
                                }
                            }
                        } else {
                            Log.d(TAG, "No tutors found matching base server query.");
                        }
                    } else {
                        Log.e(TAG, "Error getting base tutor data: ", task.getException());
                        if (task.getException() instanceof FirebaseFirestoreException &&
                                ((FirebaseFirestoreException) task.getException()).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            Toast.makeText(TuteeDashboardActivity.this, "Loading tutors failed: Firestore index required. Check Logcat for link.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(TuteeDashboardActivity.this, "Error loading tutors. Please check connection.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    applyAllClientFilters(); // Apply client-side filters (search, language, rating) to the new server data
                });
    }

    private void applyAllClientFilters() {
        List<Tutor> filteredList = new ArrayList<>(allFetchedTutors); // Start with the full list fetched from server (or what matches server-side rate filter)
        final String lowerCaseSearchQuery = currentSearchQuery.toLowerCase(Locale.getDefault());

        // Apply search query filter (name or module)
        if (!TextUtils.isEmpty(lowerCaseSearchQuery)) {
            filteredList = filteredList.stream()
                    .filter(tutor -> {
                        boolean nameMatches = false;
                        if (tutor.getFullName() != null) {
                            nameMatches = tutor.getFullName().toLowerCase(Locale.getDefault()).contains(lowerCaseSearchQuery);
                        }
                        boolean moduleMatches = false;
                        if (tutor.getModulesToTutor() != null) {
                            for (String moduleCode : tutor.getModulesToTutor()) {
                                if (moduleCode != null && moduleCode.toLowerCase(Locale.getDefault()).startsWith(lowerCaseSearchQuery)) { // Using startsWith for module codes
                                    moduleMatches = true;
                                    break;
                                }
                            }
                        }
                        return nameMatches || moduleMatches;
                    })
                    .collect(Collectors.toList());
        }

        // Apply language filter
        final String lowerCaseLanguageFilter = (currentLanguageFilter != null && !currentLanguageFilter.isEmpty())
                ? currentLanguageFilter.toLowerCase(Locale.getDefault()) : null;

        if (lowerCaseLanguageFilter != null) {
            filteredList = filteredList.stream()
                    .filter(tutor -> {
                        if (tutor.getTutoringLanguages() == null) return false;
                        for (String lang : tutor.getTutoringLanguages()) {
                            if (lang != null && lang.toLowerCase(Locale.getDefault()).equals(lowerCaseLanguageFilter)) { // Use equals for exact language match
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        // Apply minimum rating filter
        if (currentMinRatingFilter != null && currentMinRatingFilter > DEFAULT_MIN_RATING) {
            filteredList = filteredList.stream()
                    .filter(tutor -> tutor.getAverageRating() != null && tutor.getAverageRating() >= currentMinRatingFilter)
                    .collect(Collectors.toList());
        }

        // Update the adapter's list
        currentlyDisplayedTutors.clear();
        currentlyDisplayedTutors.addAll(filteredList);

        if (tutorListAdapter != null) {
            tutorListAdapter.notifyDataSetChanged();
        }
        updateEmptyState(); // Update the "No tutors found" message based on the filtered list
    }


    private void updateEmptyState() {
        if (textViewNoTutorsFound != null && recyclerViewTutors != null && tutorListAdapter != null) {
            boolean listIsEmpty = currentlyDisplayedTutors.isEmpty();
            textViewNoTutorsFound.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
            recyclerViewTutors.setVisibility(listIsEmpty ? View.GONE : View.VISIBLE);

            if (listIsEmpty) {
                // Check if any filters are active to provide a more specific message
                if (!TextUtils.isEmpty(currentSearchQuery) || currentLanguageFilter != null ||
                        (currentMinRatingFilter != null && currentMinRatingFilter > DEFAULT_MIN_RATING) ||
                        (currentMaxRateFilter != null && currentMaxRateFilter < DEFAULT_MAX_RATE) ) { // Check actual maxRateFilter too
                    textViewNoTutorsFound.setText(R.string.no_tutors_matching_criteria);
                } else {
                    textViewNoTutorsFound.setText(R.string.no_verified_tutors_available);
                }
            }
        }
    }


    @Override
    public void onFiltersApplied(Float maxRate, String language, Float minRating) {
        Log.d(TAG, "Filters applied from dialog: Max Rate=" + maxRate + ", Language=" + language + ", Min Rating=" + minRating);
        boolean needsServerReload = false;

        // Update maxRateFilter: if it changed and is not the default, server reload might be needed
        Float newMaxRateToStore = (maxRate != null && maxRate < DEFAULT_MAX_RATE) ? maxRate : null;
        if (!Objects.equals(currentMaxRateFilter, newMaxRateToStore)) {
            currentMaxRateFilter = newMaxRateToStore;
            needsServerReload = true; // Max rate filter change always requires server reload
        }

        // Update language and minRating filters (these are client-side for now)
        currentLanguageFilter = TextUtils.isEmpty(language) ? null : language;
        currentMinRatingFilter = (minRating != null && minRating > DEFAULT_MIN_RATING) ? minRating : null;

        if (needsServerReload) {
            loadBaseTutorData(); // Reload from server if max rate changed significantly
        } else {
            applyAllClientFilters(); // Otherwise, just apply client-side filters
        }
    }

    @Override
    public void onFiltersReset() {
        Log.d(TAG, "Filters reset called from dialog.");
        boolean needsServerReload = (currentMaxRateFilter != null); // If a rate filter was active, reset requires server reload

        currentSearchQuery = "";
        if (searchViewTutors != null) {
            searchViewTutors.setQuery("", false);
            searchViewTutors.clearFocus();
        }

        currentMaxRateFilter = null;
        currentLanguageFilter = null;
        currentMinRatingFilter = null;

        if (needsServerReload) {
            loadBaseTutorData();
        } else {
            applyAllClientFilters(); // Should effectively show all tutors now
        }
    }


    @Override
    public void onItemClick(Tutor tutor) {
        if (tutor == null || TextUtils.isEmpty(tutor.getUid())) {
            Log.e(TAG, "onItemClick: Tutor or Tutor UID is null!");
            Toast.makeText(this, "Cannot view tutor details.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Clicked on tutor: " + tutor.getFullName() + " (UID: " + tutor.getUid() + ")");
        Intent intent = new Intent(TuteeDashboardActivity.this, TutorDetailActivity.class);
        intent.putExtra(TutorDetailActivity.EXTRA_TUTOR_UID, tutor.getUid());
        // Pass tutor's name to TutorDetailActivity if needed there, or it will fetch it.
        // For SelectSlotActivity, TutorDetailActivity is responsible for passing the name.
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        if (progressBarLoading != null) {
            progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (isLoading) {
            if (recyclerViewTutors != null) recyclerViewTutors.setVisibility(View.GONE);
            if (textViewNoTutorsFound != null) textViewNoTutorsFound.setVisibility(View.GONE);
        }
        // When not loading, updateEmptyState() will handle visibility of RV and no_bookings text.
        // This needs to be called AFTER data is processed. It is called in loadBaseTutorData's onComplete
        // and applyAllClientFilters.
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Consider if reloading all tutors on every resume is necessary or too data-intensive.
        // It ensures freshness but might be slow.
        // If you want to refresh only if certain conditions change, add logic here.
        // For now, let's keep it or comment it out if it causes too many reads.
        // loadBaseTutorData();
        Log.d(TAG, "onResume called. Data will be as is unless explicitly refreshed by user action or specific logic.");
    }
}