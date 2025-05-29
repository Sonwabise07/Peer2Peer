package com.example.peer2peer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;         // <-- ADD THIS IMPORT
import android.view.MenuInflater; // <-- ADD THIS IMPORT
import android.view.MenuItem;    // <-- ADD THIS IMPORT
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
// Removed: androidx.appcompat.app.ActionBarDrawerToggle; (if not used for a drawer)
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
// Removed: androidx.core.view.GravityCompat; (if not used for a drawer)
// Removed: androidx.drawerlayout.widget.DrawerLayout; (if not used for a drawer)

import com.example.peer2peer.ChatListActivity;
import com.example.peer2peer.ChatbotActivity; // <-- ADD THIS IMPORT
import com.google.android.material.button.MaterialButton;
// Removed: com.google.android.material.navigation.NavigationView; (if not used for a drawer)
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TutorDashboardActivity extends AppCompatActivity {

    private static final String TAG = "TutorDashboardActivity";

    private Toolbar toolbar;
    private TextView textTutorDashboardTitle;
    private TextView textTutorDashboardMessage;
    private MaterialButton buttonManageAvailability;
    private MaterialButton buttonMySchedule;
    private MaterialButton buttonManageResources;
    private MaterialButton buttonTutorMyChats;
    private Button buttonLogout;
    // No need for private MaterialButton buttonTutorHelpChatbot; if using menu

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- AUTH CHECK - Placed early ---
        mAuth = FirebaseAuth.getInstance(); // Initialize mAuth before using it
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated in onCreate(). Redirecting to LoginActivity.");
            Intent intent = new Intent(TutorDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Important to prevent rest of onCreate from running
        }
        // --- END AUTH CHECK ---

        setContentView(R.layout.activity_tutor_dashboard);

        toolbar = findViewById(R.id.toolbar_tutor_dashboard);
        setSupportActionBar(toolbar);

        db = FirebaseFirestore.getInstance(); // db was initialized after auth check, which is fine

        textTutorDashboardTitle = findViewById(R.id.text_tutor_dashboard_title);
        textTutorDashboardMessage = findViewById(R.id.text_tutor_dashboard_message);
        buttonManageAvailability = findViewById(R.id.button_manage_availability);
        buttonMySchedule = findViewById(R.id.button_my_schedule);
        buttonManageResources = findViewById(R.id.button_tutor_manage_resources);
        buttonLogout = findViewById(R.id.button_tutor_dashboard_logout);
        buttonTutorMyChats = findViewById(R.id.button_tutor_my_chats);

        fetchTutorDetails();
        setupButtonClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Second Auth Check (good practice)
        if (mAuth.getCurrentUser() == null) {
            Log.w(TAG, "User not authenticated in onStart(). Redirecting to LoginActivity.");
            Intent intent = new Intent(TutorDashboardActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            // No return needed here for onStart if finish() is called.
        }
    }


    private void fetchTutorDetails() {
        // ... (your existing fetchTutorDetails method - no changes needed here for the menu)
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String firstName = documentSnapshot.getString("firstName");
                            if (textTutorDashboardMessage != null && !TextUtils.isEmpty(firstName)) {
                                textTutorDashboardMessage.setText("Welcome, " + firstName + "! Manage your tutoring activities below.");
                            } else if (textTutorDashboardMessage != null) {
                                textTutorDashboardMessage.setText(getString(R.string.tutor_dashboard_welcome));
                            }

                            if (getSupportActionBar() != null) {
                                if (!TextUtils.isEmpty(firstName)) {
                                    getSupportActionBar().setTitle(firstName + "'s Dashboard");
                                } else {
                                    getSupportActionBar().setTitle("Tutor Dashboard");
                                }
                            }
                        } else {
                            Log.w(TAG, "Tutor document not found for welcome message.");
                            if (getSupportActionBar() != null) {
                                getSupportActionBar().setTitle("Tutor Dashboard");
                            }
                            if (textTutorDashboardMessage != null) {
                                textTutorDashboardMessage.setText(getString(R.string.tutor_dashboard_welcome));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching tutor details for dashboard", e);
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle("Tutor Dashboard");
                        }
                        if (textTutorDashboardMessage != null) {
                            textTutorDashboardMessage.setText(getString(R.string.tutor_dashboard_welcome));
                        }
                    });
        }
    }

    private void setupButtonClickListeners() {
        // ... (existing button listeners for manage availability, schedule, resources, my chats)
        if (buttonManageAvailability != null) {
            buttonManageAvailability.setOnClickListener(v ->
                    startActivity(new Intent(TutorDashboardActivity.this, ManageAvailabilityActivity.class))
            );
        }

        if (buttonMySchedule != null) {
            buttonMySchedule.setOnClickListener(v ->
                    startActivity(new Intent(TutorDashboardActivity.this, TutorScheduleActivity.class))
            );
        }

        if (buttonManageResources != null) {
            buttonManageResources.setOnClickListener(v -> {
                Intent intent = new Intent(TutorDashboardActivity.this, AddResourceActivity.class);
                intent.putExtra("IS_TUTOR_MANAGING", true);
                startActivity(intent);
            });
        }

        if (buttonLogout != null) {
            buttonLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }

        if (buttonTutorMyChats != null) {
            buttonTutorMyChats.setOnClickListener(v -> {
                Intent intent = new Intent(TutorDashboardActivity.this, ChatListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(TutorDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(TutorDashboardActivity.this, LoginActivity.class);
                    // LoginActivity now handles being started without a role extra
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // --- ADDED FOR TOOLBAR MENU ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tutor_dashboard_menu, menu); // Use the new menu file
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_tutor_open_chatbot) { // Use the ID from tutor_dashboard_menu.xml
            Log.d(TAG, "Tutor Chatbot menu item selected. Launching ChatbotActivity.");
            Intent intent = new Intent(TutorDashboardActivity.this, ChatbotActivity.class);
            startActivity(intent);
            return true;
        }
        // Handle other menu items if you add them, e.g., a profile item
        // else if (itemId == R.id.action_tutor_view_profile) {
        //     startActivity(new Intent(this, TutorProfileWizardActivity.class)); // Or a dedicated view/edit profile screen
        //     return true;
        // }
        return super.onOptionsItemSelected(item);
    }
    // --- END ADDED FOR TOOLBAR MENU ---

}