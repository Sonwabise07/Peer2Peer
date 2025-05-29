package com.example.peer2peer.adapters; // Make sure this matches your adapter's package

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peer2peer.R; // For R.layout.item_report_admin
import com.example.peer2peer.Report; // Assuming Report.java is in com.example.peer2peer

import com.google.firebase.Timestamp; // For formatting the timestamp
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReportListAdapter extends RecyclerView.Adapter<ReportListAdapter.ReportViewHolder> {

    private List<Report> reportList;
    private OnReportClickListener listener;

    public interface OnReportClickListener {
        void onReportClick(Report report);
    }

    public ReportListAdapter(List<Report> reportList, OnReportClickListener listener) {
        this.reportList = reportList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report_admin, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.bind(report, listener);
    }

    @Override
    public int getItemCount() {
        int count = reportList != null ? reportList.size() : 0;
        // Log.d("ReportListAdapter", "getItemCount() called, returning: " + count); // Keep for debugging if needed
        return count;
    }

    public void setReports(List<Report> newReports) {
        this.reportList.clear();
        if (newReports != null) {
            this.reportList.addAll(newReports);
            // Log.d("ReportListAdapter", "setReports called with " + newReports.size() + " new reports."); // Keep for debugging
        } else {
            // Log.d("ReportListAdapter", "setReports called with null newReports list."); // Keep for debugging
        }
        notifyDataSetChanged(); // For simplicity. Consider DiffUtil for better performance later.
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView textViewReportedTutorName, textViewReporterInfo, textViewReportReasonCategory,
                textViewReportDetailsSnippet, textViewReportStatus, textViewReportTimestamp;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewReportedTutorName = itemView.findViewById(R.id.textViewReportedTutorName);
            textViewReporterInfo = itemView.findViewById(R.id.textViewReporterInfo);
            textViewReportReasonCategory = itemView.findViewById(R.id.textViewReportReasonCategory);
            textViewReportDetailsSnippet = itemView.findViewById(R.id.textViewReportDetailsSnippet);
            textViewReportStatus = itemView.findViewById(R.id.textViewReportStatus);
            textViewReportTimestamp = itemView.findViewById(R.id.textViewReportTimestamp);
        }

        public void bind(final Report report, final OnReportClickListener listener) {
            // Log.d("ReportViewHolder", "Binding report for Tutor: " + report.getReportedTutorName() +
            //                           ", Reason: " + report.getReasonCategory() +
            //                           ", Details: " + report.getReasonDetails() +
            //                           ", Status: " + report.getReportStatus()); // Keep for debugging

            String reportedTutorDisplay = (report.getReportedTutorName() != null && !report.getReportedTutorName().isEmpty()) ?
                    report.getReportedTutorName() : report.getReportedTutorUid();
            if (textViewReportedTutorName != null) textViewReportedTutorName.setText(reportedTutorDisplay);

            String reporterDisplay = (report.getReporterEmail() != null && !report.getReporterEmail().isEmpty()) ?
                    report.getReporterEmail() : report.getReporterUid();
            if (textViewReporterInfo != null) textViewReporterInfo.setText(reporterDisplay);

            if (textViewReportReasonCategory != null) textViewReportReasonCategory.setText(report.getReasonCategory());

            // Ensure you are calling getReasonDetails() from your updated Report.java
            if (textViewReportDetailsSnippet != null) textViewReportDetailsSnippet.setText(report.getReasonDetails());

            if (textViewReportStatus != null) textViewReportStatus.setText(report.getReportStatus() != null ? capitalize(report.getReportStatus()) : "N/A");

            if (report.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                if (textViewReportTimestamp != null) textViewReportTimestamp.setText(sdf.format(report.getTimestamp().toDate()));
            } else {
                if (textViewReportTimestamp != null) textViewReportTimestamp.setText("No date");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(report);
                }
            });
        }

        private String capitalize(String str) {
            if (str == null || str.isEmpty()) {
                return "";
            }
            if (str.contains("_")) {
                String[] parts = str.split("_");
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
            return str.substring(0, 1).toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT);
        }
    }
}