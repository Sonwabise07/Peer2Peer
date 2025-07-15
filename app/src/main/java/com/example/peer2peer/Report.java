package com.example.peer2peer; 

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName; 
import com.google.firebase.firestore.ServerTimestamp;

public class Report {
    // Status constants to avoid magic strings
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_REVIEWED = "reviewed";
    public static final String STATUS_RESOLVED = "resolved";
    
    @DocumentId
    private String reportId;

    private String reporterUid;
    private String reporterEmail;
    private String reportedTutorUid;
    private String reportedTutorName;
    private String reasonCategory;
    private String reasonDetails; 
    @ServerTimestamp
    private Timestamp timestamp;
    private String reportStatus;
    private String screenshotUrl;

    public Report() {
        // Firestore requires a public no-argument constructor
    }

    // Constructor (updated to use reasonDetails)
    public Report(String reporterUid, String reporterEmail, String reportedTutorUid, String reportedTutorName,
                  String reasonCategory, String reasonDetails, String reportStatus) {
        this.reporterUid = reporterUid;
        this.reporterEmail = reporterEmail;
        this.reportedTutorUid = reportedTutorUid;
        this.reportedTutorName = reportedTutorName;
        this.reasonCategory = reasonCategory;
        this.reasonDetails = reasonDetails; 
        this.reportStatus = reportStatus;
    }

    // Additional constructor that includes screenshotUrl
    public Report(String reporterUid, String reporterEmail, String reportedTutorUid, String reportedTutorName,
                  String reasonCategory, String reasonDetails, String reportStatus, String screenshotUrl) {
        this(reporterUid, reporterEmail, reportedTutorUid, reportedTutorName, reasonCategory, reasonDetails, reportStatus);
        this.screenshotUrl = screenshotUrl;
    }

    // --- Getters ---
    @PropertyName("reportId")
    public String getReportId() { return reportId; }

    @PropertyName("reporterUid")
    public String getReporterUid() { return reporterUid; }

    @PropertyName("reporterEmail")
    public String getReporterEmail() { return reporterEmail; }

    @PropertyName("reportedTutorUid")
    public String getReportedTutorUid() { return reportedTutorUid; }

    @PropertyName("reportedTutorName")
    public String getReportedTutorName() { return reportedTutorName; }

    @PropertyName("reasonCategory")
    public String getReasonCategory() { return reasonCategory; }

    @PropertyName("reasonDetails") 
    public String getReasonDetails() { return reasonDetails; }

    @PropertyName("timestamp")
    public Timestamp getTimestamp() { return timestamp; }

    @PropertyName("reportStatus")
    public String getReportStatus() { return reportStatus; }

    @PropertyName("screenshotUrl")
    public String getScreenshotUrl() { return screenshotUrl; }

    // --- Setters with validation ---
    public void setReportId(String reportId) { this.reportId = reportId; }

    @PropertyName("reporterUid")
    public void setReporterUid(String reporterUid) { this.reporterUid = reporterUid; }

    @PropertyName("reporterEmail")
    public void setReporterEmail(String reporterEmail) { 
        if (reporterEmail != null) {
            this.reporterEmail = reporterEmail.trim();
        }
    }

    @PropertyName("reportedTutorUid")
    public void setReportedTutorUid(String reportedTutorUid) { this.reportedTutorUid = reportedTutorUid; }

    @PropertyName("reportedTutorName")
    public void setReportedTutorName(String reportedTutorName) { 
        if (reportedTutorName != null) {
            this.reportedTutorName = reportedTutorName.trim();
        }
    }

    @PropertyName("reasonCategory")
    public void setReasonCategory(String reasonCategory) { this.reasonCategory = reasonCategory; }

    @PropertyName("reasonDetails") 
    public void setReasonDetails(String reasonDetails) { 
        if (reasonDetails != null) {
            this.reasonDetails = reasonDetails.trim();
        }
    }

    @PropertyName("timestamp")
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    @PropertyName("reportStatus")
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }

    @PropertyName("screenshotUrl")
    public void setScreenshotUrl(String screenshotUrl) { this.screenshotUrl = screenshotUrl; }

    // Utility method for debugging and logging
    @Override
    public String toString() {
        return "Report{" +
                "reportId='" + reportId + '\'' +
                ", reporterUid='" + reporterUid + '\'' +
                ", reportedTutorUid='" + reportedTutorUid + '\'' +
                ", reasonCategory='" + reasonCategory + '\'' +
                ", reportStatus='" + reportStatus + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}