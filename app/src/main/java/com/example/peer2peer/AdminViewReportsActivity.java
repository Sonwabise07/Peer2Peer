package com.example.peer2peer;

import android.content.DialogInterface; 
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; 
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.adapters.ReportListAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp; 

import java.text.SimpleDateFormat; 
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminViewReportsActivity extends AppCompatActivity implements ReportListAdapter.OnReportClickListener {

    private static final String TAG = "AdminViewReports";
    private Toolbar toolbar;
    private RecyclerView recyclerViewReports;
    private ReportListAdapter reportListAdapter;
    private List<Report> reportList;
    private FirebaseFirestore db;
    private TextView textViewNoReportsMessage;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_reports);

        toolbar = findViewById(R.id.toolbar_admin_view_reports);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("User Reports");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerViewReports = findViewById(R.id.recyclerViewReports);
        textViewNoReportsMessage = findViewById(R.id.textViewNoReportsMessage);
        progressBar = findViewById(R.id.progressBarAdminViewReports);

        db = FirebaseFirestore.getInstance();
        reportList = new ArrayList<>();

        setupRecyclerView();
        fetchReports();
    }

    private void setupRecyclerView() {
        reportListAdapter = new ReportListAdapter(reportList, this);
        recyclerViewReports.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewReports.setAdapter(reportListAdapter);
    }

    private void fetchReports() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (textViewNoReportsMessage != null) textViewNoReportsMessage.setVisibility(View.GONE);
        if (recyclerViewReports != null) recyclerViewReports.setVisibility(View.GONE);

        db.collection("reports")
                .whereEqualTo("reportStatus", "new") // Initially show new reports
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        List<Report> fetchedReports = new ArrayList<>();
                        if (task.getResult() != null && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Report report = document.toObject(Report.class);
                                
                                fetchedReports.add(report);
                            }
                            Log.d(TAG, "Fetched " + fetchedReports.size() + " reports.");
                        } else {
                            Log.d(TAG, "No new reports found from query result.");
                        }

                        reportListAdapter.setReports(fetchedReports);

                        if (fetchedReports.isEmpty()) {
                            if (textViewNoReportsMessage != null) {
                                textViewNoReportsMessage.setText("No new reports found.");
                                textViewNoReportsMessage.setVisibility(View.VISIBLE);
                            }
                            if (recyclerViewReports != null) recyclerViewReports.setVisibility(View.GONE);
                        } else {
                            if (recyclerViewReports != null) recyclerViewReports.setVisibility(View.VISIBLE);
                            if (textViewNoReportsMessage != null) textViewNoReportsMessage.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Error fetching reports: ", task.getException());
                        if (textViewNoReportsMessage != null) {
                            textViewNoReportsMessage.setText("Error loading reports.");
                            textViewNoReportsMessage.setVisibility(View.VISIBLE);
                        }
                        if (recyclerViewReports != null) recyclerViewReports.setVisibility(View.GONE);
                        Toast.makeText(AdminViewReportsActivity.this, "Failed to load reports: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onReportClick(Report report) {
        Log.d(TAG, "Report clicked: " + (report.getReportId() != null ? report.getReportId() : "ID N/A") +
                " Reported Tutor: " + (report.getReportedTutorName() != null ? report.getReportedTutorName() : report.getReportedTutorUid()));

        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Details");

        StringBuilder detailsText = new StringBuilder();
        detailsText.append("Reported Tutor: ").append(report.getReportedTutorName() != null ? report.getReportedTutorName() : report.getReportedTutorUid()).append("\n");
        detailsText.append("UID: ").append(report.getReportedTutorUid()).append("\n\n");

        detailsText.append("Reported By (UID): ").append(report.getReporterUid()).append("\n");
        if (report.getReporterEmail() != null && !report.getReporterEmail().isEmpty()) {
            detailsText.append("Reporter Email: ").append(report.getReporterEmail()).append("\n");
        }
        detailsText.append("\nReason Category: ").append(report.getReasonCategory()).append("\n");
        detailsText.append("Status: ").append(report.getReportStatus() != null ? capitalize(report.getReportStatus()) : "N/A").append("\n");

        if (report.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            detailsText.append("Date: ").append(sdf.format(report.getTimestamp().toDate())).append("\n");
        }
        detailsText.append("\nDetails Provided:\n").append(report.getReasonDetails());

        builder.setMessage(detailsText.toString());

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        
        builder.setNeutralButton("View Tutor Profile", (dialog, which) -> {
            Intent intent = new Intent(AdminViewReportsActivity.this, AdminUserDetailActivity.class);
            intent.putExtra(AdminUserDetailActivity.EXTRA_USER_UID, report.getReportedTutorUid());
            startActivity(intent);
        });

       

        builder.show();
    }

   
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        str = str.replace("_", " "); 
        String[] parts = str.split(" ");
        StringBuilder capitalized = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                capitalized.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                        .append(part.substring(1).toLowerCase(Locale.ROOT))
                        .append(" ");
            }
        }
        return capitalized.toString().trim();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); 
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}