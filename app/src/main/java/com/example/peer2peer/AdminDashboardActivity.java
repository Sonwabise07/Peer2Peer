package com.example.peer2peer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button; 
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton; 
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";

    
    private TextView textViewTotalRegisteredTutors, textViewTotalVerifiedTutors, textViewTotalPendingTutors, textViewTotalRejectedTutors;
    private TextView textViewTotalTutees, textViewTotalActiveUsers, textViewOverallTotalBookings, textViewTotalConfirmedBookings;
    private ProgressBar progressBarStats;
    private TextView textViewStatsError;


  
    private Toolbar toolbar;
    private MaterialButton verifyTutorsButton; 
    private MaterialButton manageUsersButton;  
    private MaterialButton buttonViewReports;  
    private MaterialButton buttonAdminLogout;  


    private FirebaseFunctions mFunctions;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mFunctions = FirebaseFunctions.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbar_admin_dashboard);
        verifyTutorsButton = findViewById(R.id.button_verify_tutors);
        manageUsersButton = findViewById(R.id.button_manage_users);
        buttonAdminLogout = findViewById(R.id.button_admin_logout);
        buttonViewReports = findViewById(R.id.buttonViewReports);

      
        textViewTotalRegisteredTutors = findViewById(R.id.textViewTotalTutors);
        textViewTotalTutees = findViewById(R.id.textViewTotalTutees);
        textViewTotalConfirmedBookings = findViewById(R.id.textViewTotalBookings);
        textViewTotalPendingTutors = findViewById(R.id.text_pending_tutors_stat);
        textViewTotalVerifiedTutors = findViewById(R.id.textViewTotalVerifiedTutors);
        textViewTotalRejectedTutors = findViewById(R.id.textViewTotalRejectedTutors);
        textViewTotalActiveUsers = findViewById(R.id.textViewTotalActiveUsers);
        textViewOverallTotalBookings = findViewById(R.id.textViewAllBookings);
        progressBarStats = findViewById(R.id.progressBarAdminDashboard);
        textViewStatsError = findViewById(R.id.textViewStatsError);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        if (verifyTutorsButton != null) {
            verifyTutorsButton.setOnClickListener(v -> {
                Log.d(TAG, "Verify Tutors button clicked.");
                startActivity(new Intent(AdminDashboardActivity.this, AdminVerifyTutorsActivity.class));
            });
        } else { Log.e(TAG, "Verify Tutors button not found."); }


        if (manageUsersButton != null) {
            manageUsersButton.setOnClickListener(v -> {
                Log.d(TAG, "Manage Users button clicked.");
                startActivity(new Intent(AdminDashboardActivity.this, AdminUserManagementActivity.class));
            });
        } else { Log.e(TAG, "Manage Users button not found."); }

        // **** ADD OnClickListener FOR NEW BUTTON ****
        if (buttonViewReports != null) {
            buttonViewReports.setOnClickListener(v -> {
                Log.d(TAG, "View Reports button clicked.");
                Intent intent = new Intent(AdminDashboardActivity.this, AdminViewReportsActivity.class);
                startActivity(intent);
            });
        } else { Log.e(TAG, "View Reports button not found."); }


        if (buttonAdminLogout != null) {
            buttonAdminLogout.setOnClickListener(v -> {
                Log.d(TAG, "Admin Logout button clicked.");
                performLogout();
            });
        } else { Log.e(TAG, "Admin Logout button not found."); }

        loadDashboardStats();
    }

    
    private void setStatsLoading(boolean isLoading) {
        if (progressBarStats != null) progressBarStats.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (textViewStatsError != null) textViewStatsError.setVisibility(View.GONE);
        String loadingText = isLoading ? "..." : "--";
        if (textViewTotalRegisteredTutors != null) textViewTotalRegisteredTutors.setText(loadingText);
        if (textViewTotalVerifiedTutors != null) textViewTotalVerifiedTutors.setText(loadingText);
        if (textViewTotalPendingTutors != null) textViewTotalPendingTutors.setText(loadingText); 
        if (textViewTotalRejectedTutors != null) textViewTotalRejectedTutors.setText(loadingText);
        if (textViewTotalTutees != null) textViewTotalTutees.setText(loadingText);
        if (textViewTotalActiveUsers != null) textViewTotalActiveUsers.setText(loadingText);
        if (textViewOverallTotalBookings != null) textViewOverallTotalBookings.setText(loadingText);
        if (textViewTotalConfirmedBookings != null) textViewTotalConfirmedBookings.setText(loadingText);
    }

    private void showStatsError(String message) {
        if (textViewStatsError != null) {
            textViewStatsError.setText(message);
            textViewStatsError.setVisibility(View.VISIBLE);
        }
        String errorText = "Err";
        if (textViewTotalRegisteredTutors != null) textViewTotalRegisteredTutors.setText(errorText);
        if (textViewTotalVerifiedTutors != null) textViewTotalVerifiedTutors.setText(errorText);
        if (textViewTotalPendingTutors != null) textViewTotalPendingTutors.setText(errorText);
        if (textViewTotalRejectedTutors != null) textViewTotalRejectedTutors.setText(errorText);
        if (textViewTotalTutees != null) textViewTotalTutees.setText(errorText);
        if (textViewTotalActiveUsers != null) textViewTotalActiveUsers.setText(errorText);
        if (textViewOverallTotalBookings != null) textViewOverallTotalBookings.setText(errorText);
        if (textViewTotalConfirmedBookings != null) textViewTotalConfirmedBookings.setText(errorText);
    }

    private void loadDashboardStats() {
        Log.d(TAG, "Calling getDashboardStats Cloud Function...");
        setStatsLoading(true);
        mFunctions.getHttpsCallable("getDashboardStats")
                .call()
                .addOnCompleteListener(task -> {
                    setStatsLoading(false);
                    if (task.isSuccessful()) {
                        HttpsCallableResult result = task.getResult();
                        if (result != null && result.getData() instanceof Map) {
                            Map<String, Object> data = (Map<String, Object>) result.getData();
                            Log.d(TAG, "getDashboardStats successful raw data: " + data);
                            if (Boolean.TRUE.equals(data.get("success")) && data.containsKey("stats") && data.get("stats") instanceof Map) {
                                Map<String, Object> stats = (Map<String, Object>) data.get("stats");
                                Log.d(TAG, "Parsed stats map: " + stats);
                                Number totalRegisteredTutors = (Number) stats.get("totalRegisteredTutors");
                                Number totalVerifiedTutors = (Number) stats.get("totalVerifiedTutors");
                                Number totalPending = (Number) stats.get("totalPendingTutors");
                                Number totalRejectedTutors = (Number) stats.get("totalRejectedTutors");
                                Number totalTutees = (Number) stats.get("totalTutees");
                                Number totalActiveUsers = (Number) stats.get("totalActiveUsersLast30Days");
                                Number overallBookings = (Number) stats.get("totalBookings");
                                Number confirmedBookings = (Number) stats.get("totalConfirmedBookings");

                                if (this.textViewTotalRegisteredTutors != null && totalRegisteredTutors != null) this.textViewTotalRegisteredTutors.setText(String.valueOf(totalRegisteredTutors.longValue()));
                                if (this.textViewTotalVerifiedTutors != null && totalVerifiedTutors != null) this.textViewTotalVerifiedTutors.setText(String.valueOf(totalVerifiedTutors.longValue()));
                                if (this.textViewTotalPendingTutors != null && totalPending != null) this.textViewTotalPendingTutors.setText(String.valueOf(totalPending.longValue())); // Was text_pending_tutors_stat
                                if (this.textViewTotalRejectedTutors != null && totalRejectedTutors != null) this.textViewTotalRejectedTutors.setText(String.valueOf(totalRejectedTutors.longValue()));
                                if (this.textViewTotalTutees != null && totalTutees != null) this.textViewTotalTutees.setText(String.valueOf(totalTutees.longValue()));
                                if (this.textViewTotalActiveUsers != null && totalActiveUsers != null) this.textViewTotalActiveUsers.setText(String.valueOf(totalActiveUsers.longValue()));
                                if (this.textViewOverallTotalBookings != null && overallBookings != null) this.textViewOverallTotalBookings.setText(String.valueOf(overallBookings.longValue()));
                                if (this.textViewTotalConfirmedBookings != null && confirmedBookings != null) this.textViewTotalConfirmedBookings.setText(String.valueOf(confirmedBookings.longValue()));
                                if (this.textViewStatsError != null) this.textViewStatsError.setVisibility(View.GONE);
                                Log.d(TAG, "Admin dashboard stats displayed successfully.");
                            } else {
                                String err = data.containsKey("message") ? (String) data.get("message") : "'stats' object missing, not a Map, or success flag false.";
                                Log.e(TAG, "Failed to parse stats: " + err + " Full data: " + data);
                                showStatsError("Failed to parse stats: " + err);
                            }
                        } else { Log.e(TAG, "Function result data is not a Map or is null"); showStatsError("Received invalid stats response (null or not Map)."); }
                    } else {
                        Exception e = task.getException(); Log.e(TAG, "Error calling getDashboardStats function", e);
                        String errorMessage = "Failed to load dashboard stats";
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            errorMessage = ffe.getMessage() + " (Code: " + ffe.getCode() + ")";
                            if (ffe.getDetails() != null) errorMessage += " Details: " + ffe.getDetails().toString();
                        } else if (e != null && e.getMessage() != null) errorMessage = e.getMessage();
                        showStatsError(errorMessage);
                        View contentView = findViewById(android.R.id.content);
                        if (contentView != null) Snackbar.make(contentView, errorMessage, Snackbar.LENGTH_LONG).show();
                        else Toast.makeText(AdminDashboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performLogout() {
        if (mAuth != null) { mAuth.signOut(); }
        Toast.makeText(this, "Admin Logged out", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Admin signed out.");
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class); 
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}